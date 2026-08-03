from __future__ import annotations

import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy.interpolate import make_interp_spline

from graph_metrics import (
    DEFAULT_DOWNSAMPLE_BUCKET_MS,
    DEFAULT_GUARD_HEIGHT_SMOOTHING_MS,
    DEFAULT_MOVEMENT_SMOOTHING_MS,
    DEFAULT_PUNCH_VOLUME_SMOOTHING_MS,
    compute_graph_metrics,
    downsample_graph_metrics,
    smooth_graph_metrics,
)


def curve_for_display(
    x: pd.Series,
    y: pd.Series,
    *,
    points_per_segment: int = 12,
    clip_min: float | None = None,
) -> tuple[np.ndarray, np.ndarray]:
    """Cubic-spline interpolation purely to round off the line for readability.
    Passes through the same already-smoothed/downsampled points - this doesn't
    add or hide information, it just curves the segments between them instead
    of drawing straight lines, so peaks/valleys look rounded instead of sharp."""
    x_values = x.to_numpy(dtype=float)
    y_values = y.to_numpy(dtype=float)
    if len(x_values) < 4:
        return x_values, y_values

    x_smooth = np.linspace(x_values.min(), x_values.max(), len(x_values) * points_per_segment)
    spline = make_interp_spline(x_values, y_values, k=3)
    y_smooth = spline(x_smooth)
    if clip_min is not None:
        y_smooth = np.clip(y_smooth, clip_min, None)
    return x_smooth, y_smooth


def plot_graph_metrics(
    pose_frames_path: Path,
    punch_windows_path: Path,
    output_path: Path,
    *,
    window_ms: int,
    stride_ms: int,
    combo_gap_ms: int,
    punch_volume_smoothing_ms: int,
    guard_height_smoothing_ms: int,
    movement_smoothing_ms: int,
    downsample_bucket_ms: int,
) -> None:
    pose_frames = pd.read_csv(pose_frames_path)
    punch_windows = pd.read_csv(punch_windows_path)

    metrics = compute_graph_metrics(
        pose_frames,
        punch_windows,
        window_ms=window_ms,
        stride_ms=stride_ms,
        combo_gap_ms=combo_gap_ms,
    )
    if metrics.empty:
        raise ValueError("No graph metrics were computed - check the input files are for the same video.")

    metrics = smooth_graph_metrics(
        metrics,
        stride_ms=stride_ms,
        punch_volume_smoothing_ms=punch_volume_smoothing_ms,
        guard_height_smoothing_ms=guard_height_smoothing_ms,
        movement_smoothing_ms=movement_smoothing_ms,
    )
    metrics = downsample_graph_metrics(metrics, bucket_ms=downsample_bucket_ms)

    time_seconds = metrics["center_ms"] / 1000.0

    fig, axes = plt.subplots(3, 1, figsize=(14, 9), sharex=True)

    punch_volume_x, punch_volume_y = curve_for_display(time_seconds, metrics["punch_volume"], clip_min=0.0)
    axes[0].plot(punch_volume_x, punch_volume_y, color="crimson")
    axes[0].fill_between(punch_volume_x, punch_volume_y, alpha=0.15, color="crimson")
    axes[0].set_ylabel("Punches per combo")
    axes[0].set_title("Punch Volume")

    guard_height_x, guard_height_y = curve_for_display(time_seconds, metrics["guard_height"])
    axes[1].plot(guard_height_x, guard_height_y, color="royalblue")
    axes[1].axhline(0, color="gray", linewidth=0.8, linestyle="--")
    axes[1].set_ylabel("nose_y - wrist_y")
    axes[1].set_title("Guard Height")

    movement_x, movement_y = curve_for_display(time_seconds, metrics["movement"], clip_min=0.0)
    axes[2].plot(movement_x, movement_y, color="seagreen")
    axes[2].set_ylabel("hip speed (x/z)")
    axes[2].set_xlabel("Time (s)")
    axes[2].set_title("Movement")

    fig.suptitle(f"Fatigue Metrics — {metrics['video_id'].iloc[0]}", fontsize=14, fontweight="bold")
    fig.tight_layout(rect=(0, 0, 1, 0.96))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_path, dpi=150)
    print(f"Saved plot to {output_path}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Plot punch-volume, guard-height, and movement graph metrics for a test session.",
    )
    parser.add_argument("--pose-frames", type=Path, default=Path("data/user_pose_frames.csv"))
    parser.add_argument("--punch-windows", type=Path, default=Path("data/predicted_punch_windows.csv"))
    parser.add_argument("--output", type=Path, default=Path("data/graph_metrics_plot.png"))
    parser.add_argument("--window-ms", type=int, default=250)
    parser.add_argument("--stride-ms", type=int, default=40)
    parser.add_argument("--combo-gap-ms", type=int, default=500)
    parser.add_argument(
        "--punch-volume-smoothing-ms",
        type=int,
        default=DEFAULT_PUNCH_VOLUME_SMOOTHING_MS,
        help="Rolling-average window for punch volume. 0 disables smoothing.",
    )
    parser.add_argument(
        "--guard-height-smoothing-ms",
        type=int,
        default=DEFAULT_GUARD_HEIGHT_SMOOTHING_MS,
        help="Rolling-average window for guard height. 0 disables smoothing.",
    )
    parser.add_argument(
        "--movement-smoothing-ms",
        type=int,
        default=DEFAULT_MOVEMENT_SMOOTHING_MS,
        help="Rolling-average window for movement. 0 disables smoothing.",
    )
    parser.add_argument(
        "--downsample-bucket-ms",
        type=int,
        default=DEFAULT_DOWNSAMPLE_BUCKET_MS,
        help="Bucket width for reducing point density after smoothing.",
    )
    args = parser.parse_args()

    plot_graph_metrics(
        args.pose_frames,
        args.punch_windows,
        args.output,
        window_ms=args.window_ms,
        stride_ms=args.stride_ms,
        combo_gap_ms=args.combo_gap_ms,
        punch_volume_smoothing_ms=args.punch_volume_smoothing_ms,
        guard_height_smoothing_ms=args.guard_height_smoothing_ms,
        movement_smoothing_ms=args.movement_smoothing_ms,
        downsample_bucket_ms=args.downsample_bucket_ms,
    )


if __name__ == "__main__":
    main()
