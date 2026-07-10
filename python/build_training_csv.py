from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd


METADATA_COLUMNS = {
    "video_id",
    "frame_index",
    "timestamp_ms",
    "pose_detected",
}
TRAINING_METADATA_COLUMNS = [
    "video_id",
    "start_frame",
    "end_frame",
    "center_frame",
    "start_ms",
    "end_ms",
    "center_ms",
    "duration_ms",
    "source",
]
LABEL_COLUMN = "label"
PUNCH_LABEL = "punch"
NO_PUNCH_LABEL = "no_punch"
VELOCITY_LANDMARKS = ("left_wrist", "right_wrist", "left_elbow", "right_elbow")
ARM_LANDMARKS = ("left_wrist", "right_wrist", "left_elbow", "right_elbow")
SHOULDER_WRIST_PAIRS = (
    ("left_shoulder", "left_wrist"),
    ("right_shoulder", "right_wrist"),
)
BODY_CENTER_LANDMARKS = ("left_shoulder", "right_shoulder", "left_hip", "right_hip")


def get_pose_feature_columns(pose_frames: pd.DataFrame) -> list[str]:
    feature_columns = [
        column
        for column in pose_frames.columns
        if column not in METADATA_COLUMNS and pd.api.types.is_numeric_dtype(pose_frames[column])
    ]
    if not feature_columns:
        raise ValueError("No numeric pose feature columns found in pose_frames.csv.")
    return feature_columns


def has_landmark_columns(frames: pd.DataFrame, landmark: str) -> bool:
    return all(f"{landmark}_{axis}" in frames.columns for axis in ("x", "y", "z"))


def frame_deltas(window: pd.DataFrame) -> pd.Series:
    if "timestamp_ms" in window.columns:
        deltas = window["timestamp_ms"].astype(float).diff() / 1000.0
    else:
        deltas = window["frame_index"].astype(float).diff()
    return deltas.replace(0, np.nan)


def add_velocity_features(row: dict[str, object], window: pd.DataFrame) -> None:
    deltas = frame_deltas(window)
    for landmark in VELOCITY_LANDMARKS:
        if not has_landmark_columns(window, landmark):
            continue

        coordinate_deltas = window[
            [f"{landmark}_x", f"{landmark}_y", f"{landmark}_z"]
        ].astype(float).diff()
        speeds = np.sqrt((coordinate_deltas**2).sum(axis=1, skipna=False)) / deltas
        speeds = speeds.replace([np.inf, -np.inf], np.nan).dropna()

        if speeds.empty:
            row[f"{landmark}_velocity_mean"] = 0.0
            row[f"{landmark}_velocity_std"] = 0.0
            row[f"{landmark}_velocity_max"] = 0.0
        else:
            row[f"{landmark}_velocity_mean"] = speeds.mean()
            row[f"{landmark}_velocity_std"] = speeds.std(ddof=0)
            row[f"{landmark}_velocity_max"] = speeds.max()


def body_center(window: pd.DataFrame) -> pd.DataFrame | None:
    required_columns = [
        f"{landmark}_{axis}"
        for landmark in BODY_CENTER_LANDMARKS
        for axis in ("x", "y", "z")
    ]
    if any(column not in window.columns for column in required_columns):
        return None

    centers = {}
    for axis in ("x", "y", "z"):
        centers[axis] = window[
            [f"{landmark}_{axis}" for landmark in BODY_CENTER_LANDMARKS]
        ].astype(float).mean(axis=1, skipna=True)
    return pd.DataFrame(centers, index=window.index)


def add_body_relative_arm_features(row: dict[str, object], window: pd.DataFrame) -> None:
    center = body_center(window)
    if center is None:
        return

    for landmark in ARM_LANDMARKS:
        if not has_landmark_columns(window, landmark):
            continue

        relative = window[[f"{landmark}_{axis}" for axis in ("x", "y", "z")]].astype(float)
        relative.columns = ("x", "y", "z")
        relative = relative - center

        for axis in ("x", "y", "z"):
            values = relative[axis].dropna()
            if values.empty:
                row[f"{landmark}_body_relative_{axis}_mean"] = 0.0
                row[f"{landmark}_body_relative_{axis}_std"] = 0.0
                row[f"{landmark}_body_relative_{axis}_change"] = 0.0
                continue

            row[f"{landmark}_body_relative_{axis}_mean"] = values.mean()
            row[f"{landmark}_body_relative_{axis}_std"] = values.std(ddof=0)
            row[f"{landmark}_body_relative_{axis}_change"] = values.iloc[-1] - values.iloc[0]


def add_shoulder_relative_wrist_features(row: dict[str, object], window: pd.DataFrame) -> None:
    for shoulder, wrist in SHOULDER_WRIST_PAIRS:
        side = shoulder.removesuffix("_shoulder")
        if not has_landmark_columns(window, shoulder) or not has_landmark_columns(window, wrist):
            continue

        wrist_coords = window[[f"{wrist}_{axis}" for axis in ("x", "y", "z")]].astype(float)
        shoulder_coords = window[[f"{shoulder}_{axis}" for axis in ("x", "y", "z")]].astype(float)
        wrist_coords.columns = ("x", "y", "z")
        shoulder_coords.columns = ("x", "y", "z")
        relative = wrist_coords - shoulder_coords

        for axis in ("x", "y", "z"):
            values = relative[axis].dropna()
            if values.empty:
                row[f"{side}_wrist_shoulder_relative_{axis}_mean"] = 0.0
                row[f"{side}_wrist_shoulder_relative_{axis}_std"] = 0.0
                row[f"{side}_wrist_shoulder_relative_{axis}_change"] = 0.0
                continue

            row[f"{side}_wrist_shoulder_relative_{axis}_mean"] = values.mean()
            row[f"{side}_wrist_shoulder_relative_{axis}_std"] = values.std(ddof=0)
            row[f"{side}_wrist_shoulder_relative_{axis}_change"] = values.iloc[-1] - values.iloc[0]


def distance_between_landmarks(
    frames: pd.DataFrame,
    first_landmark: str,
    second_landmark: str,
) -> pd.Series:
    first = frames[
        [f"{first_landmark}_x", f"{first_landmark}_y", f"{first_landmark}_z"]
    ].astype(float)
    second = frames[
        [f"{second_landmark}_x", f"{second_landmark}_y", f"{second_landmark}_z"]
    ].astype(float)
    return np.sqrt(((second.to_numpy() - first.to_numpy()) ** 2).sum(axis=1))


def add_extension_features(row: dict[str, object], window: pd.DataFrame) -> None:
    for shoulder, wrist in SHOULDER_WRIST_PAIRS:
        side = shoulder.removesuffix("_shoulder")
        if not has_landmark_columns(window, shoulder) or not has_landmark_columns(window, wrist):
            continue

        distances = distance_between_landmarks(window, shoulder, wrist)
        valid_distances = pd.Series(distances).dropna()
        if valid_distances.empty:
            row[f"{side}_shoulder_to_wrist_distance_change"] = 0.0
        else:
            row[f"{side}_shoulder_to_wrist_distance_change"] = (
                valid_distances.iloc[-1] - valid_distances.iloc[0]
            )

        relative_depth = (
            window[f"{wrist}_z"].astype(float)
            - window[f"{shoulder}_z"].astype(float)
        ).dropna()
        if relative_depth.empty:
            row[f"{side}_wrist_forward_extension_change"] = 0.0
        else:
            row[f"{side}_wrist_forward_extension_change"] = (
                relative_depth.iloc[0] - relative_depth.iloc[-1]
            )


def add_engineered_motion_features(row: dict[str, object], window: pd.DataFrame) -> None:
    add_velocity_features(row, window)
    add_body_relative_arm_features(row, window)
    add_shoulder_relative_wrist_features(row, window)
    add_extension_features(row, window)


def validate_inputs(pose_frames: pd.DataFrame, punch_windows: pd.DataFrame) -> None:
    required_pose_columns = {"video_id", "frame_index", "timestamp_ms"}
    missing_pose_columns = required_pose_columns - set(pose_frames.columns)
    if missing_pose_columns:
        raise ValueError(f"pose_frames.csv is missing columns: {sorted(missing_pose_columns)}")

    has_frame_labels = {"start_frame", "end_frame"}.issubset(punch_windows.columns)
    has_time_labels = {"start_ms", "end_ms"}.issubset(punch_windows.columns)
    missing_punch_columns = set() if has_frame_labels or has_time_labels else {"start_frame/end_frame or start_ms/end_ms"}
    if missing_punch_columns:
        raise ValueError(f"punch_windows.csv is missing columns: {sorted(missing_punch_columns)}")

    if has_frame_labels:
        invalid_windows = punch_windows["end_frame"] < punch_windows["start_frame"]
        if invalid_windows.any():
            bad_rows = punch_windows.index[invalid_windows].tolist()
            raise ValueError(f"punch_windows.csv has end_frame before start_frame at rows: {bad_rows}")
    if has_time_labels:
        invalid_windows = punch_windows["end_ms"] < punch_windows["start_ms"]
        if invalid_windows.any():
            bad_rows = punch_windows.index[invalid_windows].tolist()
            raise ValueError(f"punch_windows.csv has end_ms before start_ms at rows: {bad_rows}")


def frame_to_timestamp_ms(frames: pd.DataFrame, frame_index: int) -> int:
    frame_numbers = frames["frame_index"].astype(int)
    nearest_position = (frame_numbers - frame_index).abs().to_numpy().argmin()
    return int(round(float(frames.iloc[nearest_position]["timestamp_ms"])))


def timestamp_to_frame(frames: pd.DataFrame, timestamp_ms: float) -> int:
    timestamps = frames["timestamp_ms"].astype(float)
    nearest_position = (timestamps - timestamp_ms).abs().to_numpy().argmin()
    return int(frames.iloc[nearest_position]["frame_index"])


def aggregate_window(
    frames: pd.DataFrame,
    feature_columns: list[str],
    *,
    video_id: str,
    start_ms: int,
    end_ms: int,
    source: str,
    label: str,
    include_raw_landmark_features: bool = False,
) -> dict[str, object] | None:
    window = frames[
        (frames["timestamp_ms"] >= start_ms)
        & (frames["timestamp_ms"] <= end_ms)
    ]
    if window.empty:
        return None

    start_frame = int(window["frame_index"].iloc[0])
    end_frame = int(window["frame_index"].iloc[-1])
    center_ms = int(round((start_ms + end_ms) / 2))

    row: dict[str, object] = {
        "video_id": video_id,
        "start_frame": start_frame,
        "end_frame": end_frame,
        "center_frame": timestamp_to_frame(window, center_ms),
        "start_ms": start_ms,
        "end_ms": end_ms,
        "center_ms": center_ms,
        "duration_ms": end_ms - start_ms,
        "source": source,
    }

    if include_raw_landmark_features:
        features = window[feature_columns]
        means = features.mean(axis=0, skipna=True)
        stds = features.std(axis=0, skipna=True, ddof=0).fillna(0.0)

        for column in feature_columns:
            row[f"{column}_mean"] = means[column]
            row[f"{column}_std"] = stds[column]

    add_engineered_motion_features(row, window)

    row[LABEL_COLUMN] = label
    return row


def centered_time_window_bounds(
    *,
    center_ms: int,
    window_ms: int,
    min_ms: int,
    max_ms: int,
) -> tuple[int, int] | None:
    if window_ms < 1:
        raise ValueError("window_ms must be at least 1.")
    if max_ms - min_ms < window_ms:
        return None

    half_window = window_ms // 2
    start_ms = center_ms - half_window
    end_ms = start_ms + window_ms

    if start_ms < min_ms:
        start_ms = min_ms
        end_ms = start_ms + window_ms
    if end_ms > max_ms:
        end_ms = max_ms
        start_ms = end_ms - window_ms

    return start_ms, end_ms


def end_anchored_time_window_bounds(
    *,
    end_ms: int,
    window_ms: int,
    min_ms: int,
    max_ms: int,
) -> tuple[int, int] | None:
    if window_ms < 1:
        raise ValueError("window_ms must be at least 1.")
    if max_ms < min_ms:
        return None

    clipped_end_ms = min(max(end_ms, min_ms), max_ms)
    start_ms = clipped_end_ms - window_ms
    if start_ms < min_ms:
        start_ms = min_ms

    return start_ms, clipped_end_ms


def build_labeled_time_mask(
    windows: pd.DataFrame,
    *,
    min_ms: int,
    max_ms: int,
) -> np.ndarray:
    mask = np.zeros(max_ms - min_ms + 1, dtype=bool)
    for window in windows.itertuples(index=False):
        start_ms = max(int(window.start_ms), min_ms)
        end_ms = min(int(window.end_ms), max_ms)
        if end_ms >= start_ms:
            mask[start_ms - min_ms : end_ms - min_ms + 1] = True
    return mask


def labels_with_timestamps(pose_frames: pd.DataFrame, punch_windows: pd.DataFrame) -> pd.DataFrame:
    labels = punch_windows.copy()
    if {"start_ms", "end_ms"}.issubset(labels.columns):
        labels["start_ms"] = labels["start_ms"].astype(int)
        labels["end_ms"] = labels["end_ms"].astype(int)
        return labels

    frames_by_video = {
        video_id: frames.sort_values("frame_index").reset_index(drop=True)
        for video_id, frames in pose_frames.groupby("video_id")
    }
    start_ms_values: list[int | None] = []
    end_ms_values: list[int | None] = []
    for row in labels.itertuples(index=False):
        frames = frames_by_video.get(row.video_id)
        if frames is None:
            start_ms_values.append(None)
            end_ms_values.append(None)
            continue
        start_ms_values.append(frame_to_timestamp_ms(frames, int(row.start_frame)))
        end_ms_values.append(frame_to_timestamp_ms(frames, int(row.end_frame)))

    labels["start_ms"] = start_ms_values
    labels["end_ms"] = end_ms_values
    return labels.dropna(subset=["start_ms", "end_ms"])


def build_positive_rows(
    pose_frames: pd.DataFrame,
    punch_windows: pd.DataFrame,
    feature_columns: list[str],
    *,
    window_ms: int,
    positive_anchor: str,
    use_full_punch_window: bool,
    include_raw_landmark_features: bool,
) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    frames_by_video = {
        video_id: frames.sort_values("frame_index").set_index("frame_index", drop=False)
        for video_id, frames in pose_frames.groupby("video_id")
    }

    for window in punch_windows.itertuples(index=False):
        video_id = str(window.video_id)
        frames = frames_by_video.get(video_id)
        if frames is None:
            print(f"Skipping punch window for unknown video_id: {video_id}")
            continue

        labeled_start_ms = int(window.start_ms)
        labeled_end_ms = int(window.end_ms)
        if use_full_punch_window:
            start_ms = labeled_start_ms
            end_ms = labeled_end_ms
        else:
            timestamps = frames["timestamp_ms"].astype(int)
            if positive_anchor == "end":
                bounds = end_anchored_time_window_bounds(
                    end_ms=labeled_end_ms,
                    window_ms=window_ms,
                    min_ms=int(timestamps.min()),
                    max_ms=int(timestamps.max()),
                )
            else:
                bounds = centered_time_window_bounds(
                    center_ms=int(round((labeled_start_ms + labeled_end_ms) / 2)),
                    window_ms=window_ms,
                    min_ms=int(timestamps.min()),
                    max_ms=int(timestamps.max()),
                )
            if bounds is None:
                continue
            start_ms, end_ms = bounds

        row = aggregate_window(
            frames,
            feature_columns,
            video_id=video_id,
            start_ms=start_ms,
            end_ms=end_ms,
            source="punch_window",
            label=PUNCH_LABEL,
            include_raw_landmark_features=include_raw_landmark_features,
        )
        if row is not None:
            rows.append(row)

    return rows


def build_negative_rows(
    pose_frames: pd.DataFrame,
    punch_windows: pd.DataFrame,
    feature_columns: list[str],
    *,
    window_ms: int,
    stride_ms: int,
    max_rows: int,
    random_seed: int,
    include_raw_landmark_features: bool,
) -> list[dict[str, object]]:
    if window_ms < 1:
        raise ValueError("window_ms must be at least 1.")
    if stride_ms < 1:
        raise ValueError("stride_ms must be at least 1.")
    if max_rows < 0:
        raise ValueError("max_rows must be 0 or greater.")

    candidate_rows: list[dict[str, object]] = []
    windows_by_video = {
        video_id: windows
        for video_id, windows in punch_windows.groupby("video_id")
    }

    for video_id, frames in pose_frames.groupby("video_id"):
        frames = frames.sort_values("timestamp_ms").reset_index(drop=True)
        timestamps = frames["timestamp_ms"].astype(int).to_numpy()
        if len(timestamps) < 2:
            continue

        min_ms = int(timestamps.min())
        max_ms = int(timestamps.max())
        if max_ms - min_ms < window_ms:
            continue
        labeled_mask = build_labeled_time_mask(
            windows_by_video.get(video_id, pd.DataFrame(columns=["start_ms", "end_ms"])),
            min_ms=min_ms,
            max_ms=max_ms,
        )

        for start_ms in range(min_ms, max_ms - window_ms + 1, stride_ms):
            end_ms = start_ms + window_ms
            window_mask = labeled_mask[start_ms - min_ms : end_ms - min_ms + 1]
            if window_mask.any():
                continue

            row = aggregate_window(
                frames,
                feature_columns,
                video_id=str(video_id),
                start_ms=start_ms,
                end_ms=end_ms,
                source="sampled_no_punch",
                label=NO_PUNCH_LABEL,
                include_raw_landmark_features=include_raw_landmark_features,
            )
            if row is not None:
                candidate_rows.append(row)

    if max_rows and len(candidate_rows) > max_rows:
        rng = np.random.default_rng(random_seed)
        selected = rng.choice(len(candidate_rows), size=max_rows, replace=False)
        return [candidate_rows[index] for index in sorted(selected)]

    return candidate_rows


def infer_window_ms(punch_windows: pd.DataFrame) -> int:
    lengths = punch_windows["end_ms"] - punch_windows["start_ms"]
    if lengths.empty:
        return 250
    return max(1, int(round(float(lengths.median()))))


def build_training_csv(
    pose_frames_path: Path,
    punch_windows_path: Path,
    output_path: Path,
    *,
    window_ms: int | None,
    negative_stride_ms: int | None,
    positive_anchor: str,
    negative_ratio: float,
    random_seed: int,
    use_full_punch_window: bool,
    include_raw_landmark_features: bool,
) -> pd.DataFrame:
    pose_frames = pd.read_csv(pose_frames_path)
    punch_windows = pd.read_csv(punch_windows_path)
    validate_inputs(pose_frames, punch_windows)

    pose_frames["frame_index"] = pose_frames["frame_index"].astype(int)
    pose_frames["timestamp_ms"] = pose_frames["timestamp_ms"].astype(int)
    punch_windows = labels_with_timestamps(pose_frames, punch_windows)

    feature_columns = get_pose_feature_columns(pose_frames)
    resolved_window_ms = window_ms or infer_window_ms(punch_windows)
    positive_rows = build_positive_rows(
        pose_frames,
        punch_windows,
        feature_columns,
        window_ms=resolved_window_ms,
        positive_anchor=positive_anchor,
        use_full_punch_window=use_full_punch_window,
        include_raw_landmark_features=include_raw_landmark_features,
    )

    resolved_negative_stride_ms = negative_stride_ms or resolved_window_ms
    max_negative_rows = int(round(len(positive_rows) * negative_ratio))
    negative_rows = build_negative_rows(
        pose_frames,
        punch_windows,
        feature_columns,
        window_ms=resolved_window_ms,
        stride_ms=resolved_negative_stride_ms,
        max_rows=max_negative_rows,
        random_seed=random_seed,
        include_raw_landmark_features=include_raw_landmark_features,
    )

    training_rows = [*positive_rows, *negative_rows]
    if not training_rows:
        raise ValueError("No training rows were created. Check pose_frames.csv and punch_windows.csv.")

    training = pd.DataFrame(training_rows)
    feature_output_columns = [
        column
        for column in training.columns
        if column not in {*TRAINING_METADATA_COLUMNS, LABEL_COLUMN}
    ]
    training = training[[*TRAINING_METADATA_COLUMNS, *feature_output_columns, LABEL_COLUMN]]
    training = training.dropna(subset=feature_output_columns, how="all")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    training.to_csv(output_path, index=False)

    print(f"Wrote {len(training)} rows to {output_path}")
    print(f"  punch: {(training[LABEL_COLUMN] == PUNCH_LABEL).sum()}")
    print(f"  no_punch: {(training[LABEL_COLUMN] == NO_PUNCH_LABEL).sum()}")
    print(f"  punch window mode: {'full labels' if use_full_punch_window else 'fixed-size windows'}")
    if not use_full_punch_window:
        print(f"  positive anchor: {positive_anchor}")
    print(f"  raw landmark mean/std features: {'included' if include_raw_landmark_features else 'excluded'}")
    print(f"  window duration: {resolved_window_ms} ms")
    print(f"  no_punch stride: {resolved_negative_stride_ms} ms")
    return training


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build window-level punch/no_punch training rows from pose frames and punch labels.",
    )
    parser.add_argument(
        "--pose-frames",
        type=Path,
        default=Path("data/pose_frames.csv"),
        help="Frame-level pose CSV exported by pose_extractor.py.",
    )
    parser.add_argument(
        "--punch-windows",
        type=Path,
        default=Path("data/punch_windows.csv"),
        help="CSV with video_id,start_frame,end_frame punch labels.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("data/training.csv"),
        help="Output window-level training CSV.",
    )
    parser.add_argument(
        "--window-ms",
        type=int,
        default=None,
        help="Window duration in milliseconds. Defaults to the median labeled punch duration.",
    )
    parser.add_argument(
        "--negative-stride-ms",
        type=int,
        default=None,
        help="Stride between sampled no_punch windows in milliseconds. Defaults to window-ms.",
    )
    parser.add_argument(
        "--positive-anchor",
        choices=("end", "center"),
        default="end",
        help="How to place fixed-size positive windows. 'end' keeps the labeled punch end aligned as the impact pose. 'center' centers on the labeled window.",
    )
    parser.add_argument(
        "--negative-ratio",
        type=float,
        default=1.0,
        help="Maximum no_punch rows per punch row. Use 0 to create punch rows only.",
    )
    parser.add_argument(
        "--random-seed",
        type=int,
        default=42,
        help="Random seed used when down-sampling no_punch rows.",
    )
    parser.add_argument(
        "--use-full-punch-window",
        action="store_true",
        help="Use each full labeled punch range as a punch row. By default, creates fixed-size windows centered on each label to match prediction.",
    )
    parser.add_argument(
        "--include-raw-landmark-features",
        action="store_true",
        help="Also include raw absolute landmark mean/std features. By default, training uses body-relative arm and motion features to reduce false positives from whole-body movement.",
    )
    args = parser.parse_args()

    build_training_csv(
        args.pose_frames,
        args.punch_windows,
        args.output,
        window_ms=args.window_ms,
        negative_stride_ms=args.negative_stride_ms,
        positive_anchor=args.positive_anchor,
        negative_ratio=args.negative_ratio,
        random_seed=args.random_seed,
        use_full_punch_window=args.use_full_punch_window,
        include_raw_landmark_features=args.include_raw_landmark_features,
    )


if __name__ == "__main__":
    main()
