from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd

from guard_height import DEFAULT_SMOOTHING_MS as DEFAULT_GUARD_HEIGHT_SMOOTHING_MS
from guard_height import DEFAULT_STRIDE_MS, DEFAULT_WINDOW_MS, compute_guard_height
from movement import DEFAULT_SMOOTHING_MS as DEFAULT_MOVEMENT_SMOOTHING_MS
from movement import compute_movement
from punch_volume import DEFAULT_COMBO_GAP_MS, compute_punch_volume

DEFAULT_DOWNSAMPLE_BUCKET_MS = 500


def compute_graph_metrics(
    pose_frames: pd.DataFrame,
    punch_windows: pd.DataFrame,
    *,
    window_ms: int = DEFAULT_WINDOW_MS,
    stride_ms: int = DEFAULT_STRIDE_MS,
    combo_gap_ms: int = DEFAULT_COMBO_GAP_MS,
) -> pd.DataFrame:
    """The "graph generation" stage: runs the three independent metric stages -
    punch_volume.compute_punch_volume(), guard_height.compute_guard_height(),
    movement.compute_movement() - on the same uniform grid and merges them into
    one row per window. Each stage file is otherwise unaware of the other two;
    this is the only place that combines them."""
    punch_volume = compute_punch_volume(
        pose_frames,
        punch_windows,
        window_ms=window_ms,
        stride_ms=stride_ms,
        combo_gap_ms=combo_gap_ms,
    )
    guard_height = compute_guard_height(pose_frames, window_ms=window_ms, stride_ms=stride_ms)
    movement = compute_movement(pose_frames, window_ms=window_ms, stride_ms=stride_ms)

    merged = punch_volume.merge(
        guard_height[["video_id", "center_ms", "guard_height"]],
        on=["video_id", "center_ms"],
        how="inner",
    )
    merged = merged.merge(
        movement[["video_id", "center_ms", "movement"]],
        on=["video_id", "center_ms"],
        how="inner",
    )
    return merged[["video_id", "start_ms", "end_ms", "center_ms", "punch_volume", "guard_height", "movement"]]


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
    guard_height_smoothing_ms: int = DEFAULT_GUARD_HEIGHT_SMOOTHING_MS,
    movement_smoothing_ms: int = DEFAULT_MOVEMENT_SMOOTHING_MS,
) -> pd.DataFrame:
    """Returns a copy of [metrics] with guard_height/movement smoothed
    independently, computed per video_id so smoothing never bleeds across
    separate sessions. punch_volume is intentionally left untouched - see
    punch_volume.compute_punch_volume()'s docstring for why. The raw
    compute_graph_metrics() output is left untouched too - this is a
    display-time step, not a change to the source data."""
    if stride_ms < 1:
        raise ValueError("stride_ms must be at least 1.")

    smoothed = metrics.copy()
    windows_ms = {
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
    """Collapses guard_height/movement into bucket_ms-wide buckets (mean per
    bucket, per video_id), so a display consumer sees far fewer points. Meant
    to run after smooth_graph_metrics(): smooth first for a stable trend,
    then downsample so each remaining point still reflects that trend rather
    than one raw sample. punch_volume is dropped here on purpose - read it
    from compute_graph_metrics()'s raw output instead."""
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
        guard_height=("guard_height", "mean"),
        movement=("movement", "mean"),
    )
    downsampled = downsampled.sort_values(["video_id", "center_ms"]).reset_index(drop=True)
    return downsampled[["video_id", "start_ms", "end_ms", "center_ms", "guard_height", "movement"]]


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Graph generation stage: run punch-volume/guard-height/movement and merge them.",
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
