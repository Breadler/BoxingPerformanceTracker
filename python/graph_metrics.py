from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

DEFAULT_WINDOW_MS = 250
DEFAULT_STRIDE_MS = 40
DEFAULT_COMBO_GAP_MS = 500

POSE_FRAME_REQUIRED_COLUMNS = {
    "video_id",
    "frame_index",
    "timestamp_ms",
    "left_wrist_y",
    "right_wrist_y",
    "nose_y",
    "left_hip_x",
    "left_hip_z",
    "right_hip_x",
    "right_hip_z",
}
PUNCH_WINDOW_REQUIRED_COLUMNS = {"video_id", "start_ms", "end_ms"}


def validate_pose_frames(pose_frames: pd.DataFrame) -> None:
    missing = POSE_FRAME_REQUIRED_COLUMNS - set(pose_frames.columns)
    if missing:
        raise ValueError(f"pose_frames.csv is missing columns: {sorted(missing)}")


def validate_punch_windows(punch_windows: pd.DataFrame) -> None:
    missing = PUNCH_WINDOW_REQUIRED_COLUMNS - set(punch_windows.columns)
    if missing:
        raise ValueError(f"punch_windows.csv is missing columns: {sorted(missing)}")


def build_punch_combos(punch_windows: pd.DataFrame, *, combo_gap_ms: int) -> pd.DataFrame:
    """Groups individual predicted punches into combos when the gap between one
    punch ending and the next starting is at most combo_gap_ms. Mirrors the
    overlap-merge in predict_punches.merge_punch_windows, one level up."""
    if combo_gap_ms < 0:
        raise ValueError("combo_gap_ms must be 0 or greater.")

    combo_columns = ["video_id", "combo_start_ms", "combo_end_ms", "punch_count"]
    if punch_windows.empty:
        return pd.DataFrame(columns=combo_columns)

    combo_rows: list[dict[str, object]] = []
    for video_id, punches in punch_windows.groupby("video_id"):
        punches = punches.sort_values("start_ms")
        combo_start_ms: int | None = None
        combo_end_ms: int | None = None
        punch_count = 0

        for row in punches.itertuples(index=False):
            start_ms = int(row.start_ms)
            end_ms = int(row.end_ms)

            if combo_start_ms is None:
                combo_start_ms = start_ms
                combo_end_ms = end_ms
                punch_count = 1
                continue

            if start_ms - int(combo_end_ms) <= combo_gap_ms:
                combo_end_ms = max(int(combo_end_ms), end_ms)
                punch_count += 1
            else:
                combo_rows.append(
                    {
                        "video_id": video_id,
                        "combo_start_ms": combo_start_ms,
                        "combo_end_ms": combo_end_ms,
                        "punch_count": punch_count,
                    }
                )
                combo_start_ms = start_ms
                combo_end_ms = end_ms
                punch_count = 1

        if combo_start_ms is not None:
            combo_rows.append(
                {
                    "video_id": video_id,
                    "combo_start_ms": combo_start_ms,
                    "combo_end_ms": combo_end_ms,
                    "punch_count": punch_count,
                }
            )

    return pd.DataFrame(combo_rows, columns=combo_columns)


def punch_count_at(combos_for_video: pd.DataFrame, timestamp_ms: int) -> int:
    if combos_for_video.empty:
        return 0
    covering = combos_for_video[
        (combos_for_video["combo_start_ms"] <= timestamp_ms)
        & (timestamp_ms <= combos_for_video["combo_end_ms"])
    ]
    if covering.empty:
        return 0
    return int(covering["punch_count"].iloc[0])


def guard_height_for_window(window: pd.DataFrame) -> float:
    """Mean of (nose_y - min(left_wrist_y, right_wrist_y)) across the window.
    MediaPipe y is inverted (0 = top of frame), so a larger result means the
    higher-guarding wrist is further above the head; near zero or negative
    means the guard has dropped to chin/chest level or below."""
    raw_wrist_y = window[["left_wrist_y", "right_wrist_y"]].astype(float).min(axis=1)
    guard = (window["nose_y"].astype(float) - raw_wrist_y).dropna()
    return float(guard.mean()) if not guard.empty else 0.0


def movement_for_window(window: pd.DataFrame) -> float:
    """Mean frame-to-frame speed of the hip midpoint on x/z only (y excluded,
    since vertical bob isn't footwork). Same shape as
    build_training_csv.add_velocity_features, applied to the hip center."""
    hip_x = window[["left_hip_x", "right_hip_x"]].astype(float).mean(axis=1)
    hip_z = window[["left_hip_z", "right_hip_z"]].astype(float).mean(axis=1)
    deltas_seconds = window["timestamp_ms"].astype(float).diff() / 1000.0

    dx = hip_x.diff()
    dz = hip_z.diff()
    speeds = np.sqrt(dx**2 + dz**2) / deltas_seconds
    speeds = speeds.replace([np.inf, -np.inf], np.nan).dropna()
    return float(speeds.mean()) if not speeds.empty else 0.0


def compute_graph_metrics(
    pose_frames: pd.DataFrame,
    punch_windows: pd.DataFrame,
    *,
    window_ms: int = DEFAULT_WINDOW_MS,
    stride_ms: int = DEFAULT_STRIDE_MS,
    combo_gap_ms: int = DEFAULT_COMBO_GAP_MS,
) -> pd.DataFrame:
    if window_ms < 1:
        raise ValueError("window_ms must be at least 1.")
    if stride_ms < 1:
        raise ValueError("stride_ms must be at least 1.")

    validate_pose_frames(pose_frames)
    validate_punch_windows(punch_windows)

    pose_frames = pose_frames.copy()
    pose_frames["frame_index"] = pose_frames["frame_index"].astype(int)
    pose_frames["timestamp_ms"] = pose_frames["timestamp_ms"].astype(int)

    combos = build_punch_combos(punch_windows, combo_gap_ms=combo_gap_ms)

    rows: list[dict[str, object]] = []
    for video_id, frames in pose_frames.groupby("video_id"):
        frames = frames.sort_values("timestamp_ms").reset_index(drop=True)
        min_ms = int(frames["timestamp_ms"].min())
        max_ms = int(frames["timestamp_ms"].max())
        if max_ms - min_ms < window_ms:
            continue

        video_combos = combos[combos["video_id"] == video_id] if not combos.empty else combos

        for start_ms in range(min_ms, max_ms - window_ms + 1, stride_ms):
            end_ms = start_ms + window_ms
            window = frames[(frames["timestamp_ms"] >= start_ms) & (frames["timestamp_ms"] <= end_ms)]
            if window.empty:
                continue

            center_ms = int(round((start_ms + end_ms) / 2))
            rows.append(
                {
                    "video_id": video_id,
                    "start_ms": start_ms,
                    "end_ms": end_ms,
                    "center_ms": center_ms,
                    "punch_volume": punch_count_at(video_combos, center_ms),
                    "guard_height": guard_height_for_window(window),
                    "movement": movement_for_window(window),
                }
            )

    return pd.DataFrame(
        rows,
        columns=["video_id", "start_ms", "end_ms", "center_ms", "punch_volume", "guard_height", "movement"],
    )


DEFAULT_PUNCH_VOLUME_SMOOTHING_MS = 200
DEFAULT_GUARD_HEIGHT_SMOOTHING_MS = 1500
DEFAULT_MOVEMENT_SMOOTHING_MS = 1500
DEFAULT_DOWNSAMPLE_BUCKET_MS = 500


def smooth_series(values: pd.Series, *, window_samples: int) -> pd.Series:
    """Centered rolling mean. Reduces window-to-window / frame-jitter noise
    without shifting the signal in time (fine for offline/batch display;
    a real-time consumer would need a trailing window instead)."""
    if window_samples <= 1:
        return values
    return values.rolling(window=window_samples, center=True, min_periods=1).mean()


def smooth_graph_metrics(
    metrics: pd.DataFrame,
    *,
    stride_ms: int,
    punch_volume_smoothing_ms: int = DEFAULT_PUNCH_VOLUME_SMOOTHING_MS,
    guard_height_smoothing_ms: int = DEFAULT_GUARD_HEIGHT_SMOOTHING_MS,
    movement_smoothing_ms: int = DEFAULT_MOVEMENT_SMOOTHING_MS,
) -> pd.DataFrame:
    """Returns a copy of [metrics] with each column smoothed independently by
    its own window size, computed per video_id so smoothing never bleeds
    across separate sessions. The raw compute_graph_metrics() output is left
    untouched - this is a display-time step, not a change to the source data."""
    if stride_ms < 1:
        raise ValueError("stride_ms must be at least 1.")

    smoothed = metrics.copy()
    # punch_volume starts as int64 (exact combo counts); the rolling mean produces
    # floats, so widen the column first rather than losing the smoothed fractions.
    smoothed["punch_volume"] = smoothed["punch_volume"].astype(float)

    windows_ms = {
        "punch_volume": punch_volume_smoothing_ms,
        "guard_height": guard_height_smoothing_ms,
        "movement": movement_smoothing_ms,
    }

    for _video_id, index in metrics.groupby("video_id").groups.items():
        for column, smoothing_ms in windows_ms.items():
            window_samples = max(1, round(smoothing_ms / stride_ms))
            smoothed.loc[index, column] = smooth_series(
                metrics.loc[index, column],
                window_samples=window_samples,
            )

    return smoothed


def downsample_graph_metrics(metrics: pd.DataFrame, *, bucket_ms: int) -> pd.DataFrame:
    """Collapses [metrics] into bucket_ms-wide buckets (mean per bucket, per
    video_id), so a display consumer sees far fewer points. Meant to run after
    smooth_graph_metrics(): smooth first for a stable trend, then downsample so
    each remaining point still reflects that trend rather than one raw sample."""
    if bucket_ms < 1:
        raise ValueError("bucket_ms must be at least 1.")
    if metrics.empty:
        return metrics

    bucketed = metrics.copy()
    bucketed["_bucket_index"] = (bucketed["center_ms"] // bucket_ms).astype(int)

    downsampled = bucketed.groupby(["video_id", "_bucket_index"], as_index=False).agg(
        start_ms=("start_ms", "min"),
        end_ms=("end_ms", "max"),
        center_ms=("center_ms", "mean"),
        punch_volume=("punch_volume", "mean"),
        guard_height=("guard_height", "mean"),
        movement=("movement", "mean"),
    )
    downsampled = downsampled.sort_values(["video_id", "center_ms"]).reset_index(drop=True)
    return downsampled[["video_id", "start_ms", "end_ms", "center_ms", "punch_volume", "guard_height", "movement"]]


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Compute punch-volume (combo), guard-height, and movement fatigue metrics for graphing.",
    )
    parser.add_argument("--pose-frames", type=Path, required=True, help="Frame-level pose CSV from the extractor.")
    parser.add_argument(
        "--punch-windows",
        type=Path,
        required=True,
        help="Predicted punch windows CSV (predict_punches.py output).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("data/graph_metrics.csv"),
        help="Output CSV for per-window graph metrics.",
    )
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

    metrics = compute_graph_metrics(
        pose_frames,
        punch_windows,
        window_ms=args.window_ms,
        stride_ms=args.stride_ms,
        combo_gap_ms=args.combo_gap_ms,
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    metrics.to_csv(args.output, index=False)
    print(f"Wrote {len(metrics)} rows to {args.output}")


if __name__ == "__main__":
    main()
