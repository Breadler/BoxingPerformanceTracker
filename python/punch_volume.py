from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd

DEFAULT_WINDOW_MS = 250
DEFAULT_STRIDE_MS = 40
DEFAULT_COMBO_GAP_MS = 500

PUNCH_WINDOW_REQUIRED_COLUMNS = {"video_id", "start_ms", "end_ms"}
POSE_FRAME_REQUIRED_COLUMNS = {"video_id", "timestamp_ms"}


# Validate required columns are present
def validate_punch_windows(punch_windows: pd.DataFrame) -> None:
    missing = PUNCH_WINDOW_REQUIRED_COLUMNS - set(punch_windows.columns)
    if missing:
        raise ValueError(f"punch_windows.csv is missing columns: {sorted(missing)}")


# Validate required columns are present
def validate_pose_frames(pose_frames: pd.DataFrame) -> None:
    missing = POSE_FRAME_REQUIRED_COLUMNS - set(pose_frames.columns)
    if missing:
        raise ValueError(f"pose_frames.csv is missing columns: {sorted(missing)}")


def build_punch_combos(punch_windows: pd.DataFrame, *, combo_gap_ms: int) -> pd.DataFrame:
    """Group punches into combos by gap between end/start times."""
    if combo_gap_ms < 0:
        raise ValueError("combo_gap_ms must be 0 or greater.")

    combo_columns = ["video_id", "combo_start_ms", "combo_end_ms", "punch_count", "punch_end_times_ms"]
    if punch_windows.empty:
        return pd.DataFrame(columns=combo_columns)

    combo_rows: list[dict[str, object]] = []
    for video_id, punches in punch_windows.groupby("video_id"):
        punches = punches.sort_values("start_ms")
        combo_start_ms: int | None = None
        combo_end_ms: int | None = None
        punch_end_times_ms: list[int] = []

        for row in punches.itertuples(index=False):
            start_ms = int(row.start_ms)
            end_ms = int(row.end_ms)

            if combo_start_ms is None:
                combo_start_ms = start_ms
                combo_end_ms = end_ms
                punch_end_times_ms = [end_ms]
                continue

            if start_ms - int(combo_end_ms) <= combo_gap_ms:
                combo_end_ms = max(int(combo_end_ms), end_ms)
                punch_end_times_ms.append(end_ms)
            else:
                combo_rows.append(
                    {
                        "video_id": video_id,
                        "combo_start_ms": combo_start_ms,
                        "combo_end_ms": combo_end_ms,
                        "punch_count": len(punch_end_times_ms),
                        "punch_end_times_ms": punch_end_times_ms,
                    }
                )
                combo_start_ms = start_ms
                combo_end_ms = end_ms
                punch_end_times_ms = [end_ms]

        if combo_start_ms is not None:
            combo_rows.append(
                {
                    "video_id": video_id,
                    "combo_start_ms": combo_start_ms,
                    "combo_end_ms": combo_end_ms,
                    "punch_count": len(punch_end_times_ms),
                    "punch_end_times_ms": punch_end_times_ms,
                }
            )

    return pd.DataFrame(combo_rows, columns=combo_columns)


def punch_count_at(combos_for_video: pd.DataFrame, timestamp_ms: int) -> int:
    """Running punch count within the covering combo at timestamp_ms."""
    if combos_for_video.empty:
        return 0
    covering = combos_for_video[
        (combos_for_video["combo_start_ms"] <= timestamp_ms)
        & (timestamp_ms <= combos_for_video["combo_end_ms"])
    ]
    if covering.empty:
        return 0
    punch_end_times_ms = covering.iloc[0]["punch_end_times_ms"]
    return sum(1 for end_ms in punch_end_times_ms if end_ms <= timestamp_ms)


def compute_punch_volume(
    pose_frames: pd.DataFrame,
    punch_windows: pd.DataFrame,
    *,
    window_ms: int = DEFAULT_WINDOW_MS,
    stride_ms: int = DEFAULT_STRIDE_MS,
    combo_gap_ms: int = DEFAULT_COMBO_GAP_MS,
) -> pd.DataFrame:
    """Punch volume on a uniform sliding-window grid."""
    if window_ms < 1:
        raise ValueError("window_ms must be at least 1.")
    if stride_ms < 1:
        raise ValueError("stride_ms must be at least 1.")

    validate_pose_frames(pose_frames)
    validate_punch_windows(punch_windows)

    combos = build_punch_combos(punch_windows, combo_gap_ms=combo_gap_ms)

    rows: list[dict[str, object]] = []
    for video_id, frames in pose_frames.groupby("video_id"):
        timestamps = frames["timestamp_ms"].astype(int)
        min_ms = int(timestamps.min())
        max_ms = int(timestamps.max())
        if max_ms - min_ms < window_ms:
            continue

        video_combos = combos[combos["video_id"] == video_id] if not combos.empty else combos

        for start_ms in range(min_ms, max_ms - window_ms + 1, stride_ms):
            end_ms = start_ms + window_ms
            if not ((timestamps >= start_ms) & (timestamps <= end_ms)).any():
                continue
            center_ms = int(round((start_ms + end_ms) / 2))
            rows.append(
                {
                    "video_id": video_id,
                    "start_ms": start_ms,
                    "end_ms": end_ms,
                    "center_ms": center_ms,
                    "punch_volume": punch_count_at(video_combos, center_ms),
                }
            )

    return pd.DataFrame(rows, columns=["video_id", "start_ms", "end_ms", "center_ms", "punch_volume"])


def compute_punch_volume_keyframes(
    pose_frames: pd.DataFrame,
    punch_windows: pd.DataFrame,
    *,
    combo_gap_ms: int = DEFAULT_COMBO_GAP_MS,
) -> pd.DataFrame:
    """Sparse per-punch keyframes for display, with 0 markers bounding each combo."""
    validate_pose_frames(pose_frames)
    validate_punch_windows(punch_windows)

    rows: list[dict[str, object]] = []
    for video_id, frames in pose_frames.groupby("video_id"):
        timestamps = frames["timestamp_ms"].astype(int)
        min_ms = int(timestamps.min())
        max_ms = int(timestamps.max())

        video_combos = build_punch_combos(
            punch_windows[punch_windows["video_id"] == video_id],
            combo_gap_ms=combo_gap_ms,
        )
        if video_combos.empty:
            rows.append({"video_id": video_id, "timestamp_ms": min_ms, "punch_volume": 0})
            rows.append({"video_id": video_id, "timestamp_ms": max_ms, "punch_volume": 0})
            continue

        if min_ms < int(video_combos.iloc[0]["combo_start_ms"]):
            rows.append({"video_id": video_id, "timestamp_ms": min_ms, "punch_volume": 0})

        for combo in video_combos.itertuples(index=False):
            rows.append({"video_id": video_id, "timestamp_ms": combo.combo_start_ms, "punch_volume": 0})
            for punch_number, end_ms in enumerate(combo.punch_end_times_ms, start=1):
                rows.append({"video_id": video_id, "timestamp_ms": end_ms, "punch_volume": punch_number})
            # Reset to 0 after combo ends
            rows.append({"video_id": video_id, "timestamp_ms": combo.combo_end_ms + 1, "punch_volume": 0})

        if max_ms > int(video_combos.iloc[-1]["combo_end_ms"]):
            rows.append({"video_id": video_id, "timestamp_ms": max_ms, "punch_volume": 0})

    keyframes = pd.DataFrame(rows, columns=["video_id", "timestamp_ms", "punch_volume"])
    keyframes = keyframes.drop_duplicates(subset=["video_id", "timestamp_ms"], keep="last")
    return keyframes.sort_values(["video_id", "timestamp_ms"]).reset_index(drop=True)


# CLI entry point
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Compute combo-based punch volume on a uniform sliding-window grid.",
    )
    parser.add_argument(
        "--pose-frames",
        type=Path,
        required=True,
        help="Frame-level pose CSV from the extractor (used only for the window grid).",
    )
    parser.add_argument(
        "--punch-windows",
        type=Path,
        required=True,
        help="Predicted punch windows CSV (predict_punches.py output).",
    )
    parser.add_argument("--output", type=Path, default=Path("data/punch_volume.csv"))
    parser.add_argument("--window-ms", type=int, default=DEFAULT_WINDOW_MS, help="Must match predict_punches.py.")
    parser.add_argument("--stride-ms", type=int, default=DEFAULT_STRIDE_MS, help="Must match predict_punches.py.")
    parser.add_argument(
        "--combo-gap-ms",
        type=int,
        default=DEFAULT_COMBO_GAP_MS,
        help="Max gap between the end of one punch and the start of the next to count as the same combo.",
    )
    args = parser.parse_args()

    pose_frames = pd.read_csv(args.pose_frames)
    punch_windows = pd.read_csv(args.punch_windows)
    result = compute_punch_volume(
        pose_frames,
        punch_windows,
        window_ms=args.window_ms,
        stride_ms=args.stride_ms,
        combo_gap_ms=args.combo_gap_ms,
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    result.to_csv(args.output, index=False)
    print(f"Wrote {len(result)} rows to {args.output}")


if __name__ == "__main__":
    main()
