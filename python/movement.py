from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

DEFAULT_WINDOW_MS = 250
DEFAULT_STRIDE_MS = 40
DEFAULT_SMOOTHING_MS = 1500

POSE_FRAME_REQUIRED_COLUMNS = {"video_id", "timestamp_ms", "left_hip_x", "left_hip_z", "right_hip_x", "right_hip_z"}


def validate_pose_frames(pose_frames: pd.DataFrame) -> None:
    missing = POSE_FRAME_REQUIRED_COLUMNS - set(pose_frames.columns)
    if missing:
        raise ValueError(f"pose_frames.csv is missing columns: {sorted(missing)}")


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


def compute_movement(
    pose_frames: pd.DataFrame,
    *,
    window_ms: int = DEFAULT_WINDOW_MS,
    stride_ms: int = DEFAULT_STRIDE_MS,
) -> pd.DataFrame:
    """Movement (hip x/z speed) on a uniform sliding-window grid. Raw -
    smoothing/downsampling for display is graph_metrics.py's job
    (smooth_graph_metrics() / downsample_graph_metrics()), not this stage's."""
    if window_ms < 1:
        raise ValueError("window_ms must be at least 1.")
    if stride_ms < 1:
        raise ValueError("stride_ms must be at least 1.")

    validate_pose_frames(pose_frames)
    pose_frames = pose_frames.copy()
    pose_frames["timestamp_ms"] = pose_frames["timestamp_ms"].astype(int)

    rows: list[dict[str, object]] = []
    for video_id, frames in pose_frames.groupby("video_id"):
        frames = frames.sort_values("timestamp_ms").reset_index(drop=True)
        min_ms = int(frames["timestamp_ms"].min())
        max_ms = int(frames["timestamp_ms"].max())
        if max_ms - min_ms < window_ms:
            continue

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
                    "movement": movement_for_window(window),
                }
            )

    return pd.DataFrame(rows, columns=["video_id", "start_ms", "end_ms", "center_ms", "movement"])


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Compute hip x/z movement speed on a uniform sliding-window grid.",
    )
    parser.add_argument("--pose-frames", type=Path, required=True, help="Frame-level pose CSV from the extractor.")
    parser.add_argument("--output", type=Path, default=Path("data/movement.csv"))
    parser.add_argument("--window-ms", type=int, default=DEFAULT_WINDOW_MS, help="Must match predict_punches.py.")
    parser.add_argument("--stride-ms", type=int, default=DEFAULT_STRIDE_MS, help="Must match predict_punches.py.")
    args = parser.parse_args()

    pose_frames = pd.read_csv(args.pose_frames)
    result = compute_movement(pose_frames, window_ms=args.window_ms, stride_ms=args.stride_ms)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    result.to_csv(args.output, index=False)
    print(f"Wrote {len(result)} rows to {args.output}")


if __name__ == "__main__":
    main()
