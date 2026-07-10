from __future__ import annotations

import argparse
import pickle
from pathlib import Path

import pandas as pd

from build_training_csv import aggregate_window, get_pose_feature_columns


METADATA_COLUMNS = {
    "video_id",
    "start_frame",
    "end_frame",
    "center_frame",
    "start_ms",
    "end_ms",
    "center_ms",
    "duration_ms",
    "source",
}
LABEL_COLUMN = "label"


def load_model_artifact(model_path: Path) -> dict[str, object]:
    try:
        import joblib

        return joblib.load(model_path)
    except ModuleNotFoundError:
        with model_path.open("rb") as model_file:
            return pickle.load(model_file)


def build_prediction_windows(
    pose_frames: pd.DataFrame,
    *,
    window_ms: int,
    stride_ms: int,
    include_raw_landmark_features: bool,
) -> pd.DataFrame:
    if window_ms < 1:
        raise ValueError("window_ms must be at least 1.")
    if stride_ms < 1:
        raise ValueError("stride_ms must be at least 1.")
    if not {"video_id", "frame_index", "timestamp_ms"}.issubset(pose_frames.columns):
        raise ValueError("Pose CSV must include video_id, frame_index, and timestamp_ms columns.")

    pose_frames["frame_index"] = pose_frames["frame_index"].astype(int)
    pose_frames["timestamp_ms"] = pose_frames["timestamp_ms"].astype(int)
    feature_columns = get_pose_feature_columns(pose_frames)
    rows: list[dict[str, object]] = []

    for video_id, frames in pose_frames.groupby("video_id"):
        frames = frames.sort_values("timestamp_ms").reset_index(drop=True)
        min_ms = int(frames["timestamp_ms"].min())
        max_ms = int(frames["timestamp_ms"].max())
        if max_ms - min_ms < window_ms:
            continue

        for start_ms in range(min_ms, max_ms - window_ms + 1, stride_ms):
            end_ms = start_ms + window_ms
            row = aggregate_window(
                frames,
                feature_columns,
                video_id=str(video_id),
                start_ms=start_ms,
                end_ms=end_ms,
                source="prediction_window",
                label="unknown",
                include_raw_landmark_features=include_raw_landmark_features,
            )
            if row is not None:
                rows.append(row)

    if not rows:
        raise ValueError("No prediction windows were created from the pose CSV.")

    predictions = pd.DataFrame(rows)
    return predictions.drop(columns=[LABEL_COLUMN])


def merge_punch_windows(predictions: pd.DataFrame) -> pd.DataFrame:
    punch_rows = predictions[predictions["prediction"] == "punch"].copy()
    if punch_rows.empty:
        return pd.DataFrame(
            columns=["video_id", "start_ms", "end_ms", "start_frame", "end_frame", "window_count"]
        )

    merged_rows: list[dict[str, object]] = []
    for video_id, rows in punch_rows.groupby("video_id"):
        rows = rows.sort_values("start_ms")
        current_start_ms: int | None = None
        current_end_ms: int | None = None
        current_start_frame: int | None = None
        current_end_frame: int | None = None
        window_count = 0

        for row in rows.itertuples(index=False):
            start_ms = int(row.start_ms)
            end_ms = int(row.end_ms)
            start_frame = int(row.start_frame)
            end_frame = int(row.end_frame)
            if current_start_ms is None:
                current_start_ms = start_ms
                current_end_ms = end_ms
                current_start_frame = start_frame
                current_end_frame = end_frame
                window_count = 1
                continue

            if start_ms <= int(current_end_ms):
                current_end_ms = max(int(current_end_ms), end_ms)
                current_end_frame = max(int(current_end_frame), end_frame)
                window_count += 1
            else:
                merged_rows.append(
                    {
                        "video_id": video_id,
                        "start_ms": current_start_ms,
                        "end_ms": current_end_ms,
                        "start_frame": current_start_frame,
                        "end_frame": current_end_frame,
                        "window_count": window_count,
                    }
                )
                current_start_ms = start_ms
                current_end_ms = end_ms
                current_start_frame = start_frame
                current_end_frame = end_frame
                window_count = 1

        if current_start_ms is not None:
            merged_rows.append(
                {
                    "video_id": video_id,
                    "start_ms": current_start_ms,
                    "end_ms": current_end_ms,
                    "start_frame": current_start_frame,
                    "end_frame": current_end_frame,
                    "window_count": window_count,
                }
            )

    return pd.DataFrame(merged_rows)


def predict_punches(
    pose_frames_path: Path,
    model_path: Path,
    output_path: Path,
    *,
    windows_output_path: Path | None,
    window_ms: int,
    stride_ms: int,
    punch_threshold: float | None,
) -> pd.DataFrame:
    artifact = load_model_artifact(model_path)
    model = artifact["model"]
    feature_columns = list(artifact["feature_columns"])

    pose_frames = pd.read_csv(pose_frames_path)
    prediction_windows = build_prediction_windows(
        pose_frames,
        window_ms=window_ms,
        stride_ms=stride_ms,
        include_raw_landmark_features=False,
    )

    missing_columns = [column for column in feature_columns if column not in prediction_windows.columns]
    if missing_columns:
        prediction_windows = build_prediction_windows(
            pose_frames,
            window_ms=window_ms,
            stride_ms=stride_ms,
            include_raw_landmark_features=True,
        )
        missing_columns = [column for column in feature_columns if column not in prediction_windows.columns]

    if missing_columns:
        raise ValueError(
            "User pose CSV does not match the model's training features. "
            f"Missing columns: {missing_columns[:10]}"
        )

    x = prediction_windows[feature_columns].fillna(0.0)
    prediction_windows["prediction"] = model.predict(x)

    if hasattr(model, "predict_proba") and "punch" in getattr(model, "classes_", []):
        punch_index = list(model.classes_).index("punch")
        prediction_windows["punch_probability"] = model.predict_proba(x)[:, punch_index]
        if punch_threshold is not None:
            prediction_windows["prediction"] = prediction_windows["punch_probability"].map(
                lambda probability: "punch" if probability >= punch_threshold else "no_punch"
            )

    merged = merge_punch_windows(prediction_windows)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    merged.to_csv(output_path, index=False)

    if windows_output_path is not None:
        windows_output_path.parent.mkdir(parents=True, exist_ok=True)
        prediction_windows.to_csv(windows_output_path, index=False)

    print(f"Predicted punch count: {len(merged)}")
    print(f"Punch windows saved to: {output_path}")
    return merged


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Run a trained Random Forest on user pose frames to find punch moments.",
    )
    parser.add_argument("--pose-frames", type=Path, required=True, help="User pose CSV.")
    parser.add_argument("--model", type=Path, required=True, help="Trained model artifact.")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("data/predicted_punch_windows.csv"),
        help="Output CSV for merged predicted punch windows.",
    )
    parser.add_argument(
        "--windows-output",
        type=Path,
        default=None,
        help="Optional CSV for every sliding window prediction.",
    )
    parser.add_argument(
        "--window-ms",
        type=int,
        default=250,
        help="Sliding-window duration in milliseconds. Must match training.",
    )
    parser.add_argument(
        "--stride-ms",
        type=int,
        default=40,
        help="Sliding-window stride in milliseconds.",
    )
    parser.add_argument(
        "--punch-threshold",
        type=float,
        default=None,
        help="Optional punch probability cutoff. Lower values make detection more sensitive, for example 0.35.",
    )
    args = parser.parse_args()

    predict_punches(
        args.pose_frames,
        args.model,
        args.output,
        windows_output_path=args.windows_output,
        window_ms=args.window_ms,
        stride_ms=args.stride_ms,
        punch_threshold=args.punch_threshold,
    )


if __name__ == "__main__":
    main()
