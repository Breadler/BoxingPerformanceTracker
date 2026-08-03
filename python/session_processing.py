from __future__ import annotations

from dataclasses import asdict, dataclass
from pathlib import Path
from time import time
from uuid import uuid4

import cv2
import numpy as np
import pandas as pd

from graph_metrics import compute_graph_metrics
from predict_punches import predict_punches
from pose_extractor import (
    BOXING_LANDMARK_INDICES,
    DEFAULT_MODEL_PATH,
    POSE_LANDMARK_NAMES,
    get_landmark_feature_names,
    get_landmark_indices,
    resolve_model_path,
)


BOXING_CONNECTIONS = (
    (11, 12),
    (11, 13),
    (13, 15),
    (12, 14),
    (14, 16),
    (11, 23),
    (12, 24),
    (23, 24),
    (23, 25),
    (24, 26),
    (25, 27),
    (26, 28),
)


@dataclass(slots=True)
class PerformancePoint:
    timestamp_ms: int
    punch_volume: int
    guard_height: float
    movement: float


@dataclass(slots=True)
class PunchWindowResult:
    start_ms: int
    end_ms: int
    window_count: int
    punch_probability: float


@dataclass(slots=True)
class ProcessingResult:
    session_id: str
    title: str
    date_label: str
    duration_label: str
    duration_ms: int
    source_video_name: str
    source_video_path: str
    annotated_video_path: str
    punch_count: int
    punch_windows: list[PunchWindowResult]
    performance_points: list[PerformancePoint]
    prediction_windows_path: str
    pose_frames_path: str

    def to_response(self, *, base_url: str, storage_root: Path) -> dict[str, object]:
        source_relative = Path(self.source_video_path).resolve().relative_to(storage_root.resolve())
        annotated_relative = Path(self.annotated_video_path).resolve().relative_to(storage_root.resolve())
        return {
            "sessionId": self.session_id,
            "title": self.title,
            "dateLabel": self.date_label,
            "durationLabel": self.duration_label,
            "durationMs": self.duration_ms,
            "sourceVideoName": self.source_video_name,
            "sourceVideoUrl": f"{base_url.rstrip('/')}/files/{source_relative.as_posix()}",
            "annotatedVideoUrl": f"{base_url.rstrip('/')}/files/{annotated_relative.as_posix()}",
            "punchCount": self.punch_count,
            "punchWindows": [asdict(window) for window in self.punch_windows],
            "performancePoints": [asdict(point) for point in self.performance_points],
        }


@dataclass(slots=True)
class FrameObservation:
    frame_index: int
    timestamp_ms: int
    pose_detected: bool
    landmark_values: dict[str, float]
    keypoints: dict[str, tuple[float, float, float, float]]

    def to_row(self) -> dict[str, object]:
        row: dict[str, object] = {
            "frame_index": self.frame_index,
            "timestamp_ms": self.timestamp_ms,
            "pose_detected": self.pose_detected,
        }
        row.update(self.landmark_values)
        return row


def _landmark_feature_map(landmarks: list[object], landmark_indices: tuple[int, ...]) -> tuple[dict[str, float], dict[str, tuple[float, float, float, float]]]:
    feature_values: dict[str, float] = {}
    keypoints: dict[str, tuple[float, float, float, float]] = {}
    for index in landmark_indices:
        landmark = landmarks[index]
        landmark_name = POSE_LANDMARK_NAMES[index]
        keypoints[landmark_name] = (
            float(landmark.x),
            float(landmark.y),
            float(landmark.z),
            float(landmark.visibility),
        )
        for component, value in (
            ("x", landmark.x),
            ("y", landmark.y),
            ("z", landmark.z),
            ("visibility", landmark.visibility),
        ):
            feature_values[f"{landmark_name}_{component}"] = float(value)
    return feature_values, keypoints


def _format_duration_label(duration_ms: int) -> str:
    total_seconds = max(0, duration_ms // 1000)
    minutes = total_seconds // 60
    seconds = total_seconds % 60
    return f"{minutes}:{seconds:02d}"


def _timestamp_label(timestamp_ms: int) -> str:
    total_seconds = max(0, timestamp_ms // 1000)
    minutes = total_seconds // 60
    seconds = total_seconds % 60
    return f"{minutes}:{seconds:02d}"


def extract_frame_observations(
    video_path: Path,
    *,
    model_path: Path | None = None,
    landmark_set: str = "boxing",
    frame_stride: int = 1,
) -> tuple[list[FrameObservation], float, tuple[int, int]]:
    if frame_stride < 1:
        raise ValueError("frame_stride must be at least 1")

    resolved_model_path = resolve_model_path(model_path)
    landmark_indices = get_landmark_indices(landmark_set)

    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise ValueError(f"Could not open video file: {video_path}")

    fps = capture.get(cv2.CAP_PROP_FPS)
    if fps <= 0:
        fps = 30.0

    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH) or 0)
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT) or 0)

    import importlib

    mp = importlib.import_module("mediapipe")
    pose_module = importlib.import_module("mediapipe.tasks.python.vision.pose_landmarker")
    base_options_module = importlib.import_module("mediapipe.tasks.python.core.base_options")
    running_mode_module = importlib.import_module(
        "mediapipe.tasks.python.vision.core.vision_task_running_mode"
    )

    options = pose_module.PoseLandmarkerOptions(
        base_options=base_options_module.BaseOptions(model_asset_path=str(resolved_model_path)),
        running_mode=running_mode_module.VisionTaskRunningMode.VIDEO,
        num_poses=1,
    )

    observations: list[FrameObservation] = []
    frame_index = 0

    with pose_module.PoseLandmarker.create_from_options(options) as pose_landmarker:
        while True:
            success, frame = capture.read()
            if not success:
                break

            frame_index += 1
            if (frame_index - 1) % frame_stride != 0:
                continue

            timestamp_ms = int((frame_index - 1) * 1000 / fps)
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            results = pose_landmarker.detect_for_video(mp_image, timestamp_ms)

            if results.pose_landmarks:
                feature_values, keypoints = _landmark_feature_map(results.pose_landmarks[0], landmark_indices)
                observations.append(
                    FrameObservation(
                        frame_index=frame_index,
                        timestamp_ms=timestamp_ms,
                        pose_detected=True,
                        landmark_values=feature_values,
                        keypoints=keypoints,
                    )
                )
            else:
                empty_values = {
                    feature_name: float("nan")
                    for feature_name in get_landmark_feature_names(landmark_indices)
                }
                observations.append(
                    FrameObservation(
                        frame_index=frame_index,
                        timestamp_ms=timestamp_ms,
                        pose_detected=False,
                        landmark_values=empty_values,
                        keypoints={},
                    )
                )

    capture.release()

    if width <= 0 or height <= 0:
        raise ValueError(f"Could not determine video dimensions for: {video_path}")

    return observations, fps, (width, height)


def merge_prediction_windows(prediction_windows: pd.DataFrame) -> pd.DataFrame:
    punch_rows = prediction_windows[prediction_windows["prediction"] == "punch"].copy()
    if punch_rows.empty:
        return pd.DataFrame(columns=["start_ms", "end_ms", "window_count", "punch_probability"])

    merged_rows: list[dict[str, object]] = []
    current_start_ms: int | None = None
    current_end_ms: int | None = None
    current_window_count = 0
    current_probabilities: list[float] = []

    for row in punch_rows.sort_values("start_ms").itertuples(index=False):
        start_ms = int(row.start_ms)
        end_ms = int(row.end_ms)
        probability = float(getattr(row, "punch_probability", 1.0))

        if current_start_ms is None:
            current_start_ms = start_ms
            current_end_ms = end_ms
            current_window_count = 1
            current_probabilities = [probability]
            continue

        if start_ms <= int(current_end_ms):
            current_end_ms = max(int(current_end_ms), end_ms)
            current_window_count += 1
            current_probabilities.append(probability)
            continue

        merged_rows.append(
            {
                "start_ms": current_start_ms,
                "end_ms": current_end_ms,
                "window_count": current_window_count,
                "punch_probability": float(np.mean(current_probabilities)),
            }
        )
        current_start_ms = start_ms
        current_end_ms = end_ms
        current_window_count = 1
        current_probabilities = [probability]

    if current_start_ms is not None:
        merged_rows.append(
            {
                "start_ms": current_start_ms,
                "end_ms": current_end_ms,
                "window_count": current_window_count,
                "punch_probability": float(np.mean(current_probabilities)),
            }
        )

    return pd.DataFrame(merged_rows)


def _draw_landmarks(frame: np.ndarray, keypoints: dict[str, tuple[float, float, float, float]]) -> None:
    height, width = frame.shape[:2]

    for start_index, end_index in BOXING_CONNECTIONS:
        if start_index >= len(POSE_LANDMARK_NAMES) or end_index >= len(POSE_LANDMARK_NAMES):
            continue
        start_name = POSE_LANDMARK_NAMES[start_index]
        end_name = POSE_LANDMARK_NAMES[end_index]
        if start_name not in keypoints or end_name not in keypoints:
            continue
        start_x, start_y, _, start_visibility = keypoints[start_name]
        end_x, end_y, _, end_visibility = keypoints[end_name]
        if min(start_visibility, end_visibility) < 0.15:
            continue
        start_point = (int(start_x * width), int(start_y * height))
        end_point = (int(end_x * width), int(end_y * height))
        cv2.line(frame, start_point, end_point, (40, 219, 255), 3)

    for _name, (x, y, _z, visibility) in keypoints.items():
        if visibility < 0.1:
            continue
        center = (int(x * width), int(y * height))
        cv2.circle(frame, center, 4, (255, 255, 255), -1)
        cv2.circle(frame, center, 6, (40, 219, 255), 2)


def _draw_overlay(frame: np.ndarray, *, timestamp_ms: int, punch_windows: list[PunchWindowResult]) -> None:
    active_window = next((window for window in punch_windows if window.start_ms <= timestamp_ms <= window.end_ms), None)
    overlay = frame.copy()
    if active_window is not None:
        cv2.rectangle(overlay, (0, 0), (frame.shape[1], 110), (30, 30, 30), -1)
        cv2.addWeighted(overlay, 0.55, frame, 0.45, 0, frame)
        cv2.putText(
            frame,
            f"Punch detected {active_window.start_ms}-{active_window.end_ms} ms",
            (20, 42),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.86,
            (40, 219, 255),
            2,
            cv2.LINE_AA,
        )
        cv2.putText(
            frame,
            f"Confidence {active_window.punch_probability:.2f}",
            (20, 78),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.72,
            (255, 255, 255),
            2,
            cv2.LINE_AA,
        )
    else:
        cv2.rectangle(overlay, (0, 0), (frame.shape[1], 90), (18, 18, 18), -1)
        cv2.addWeighted(overlay, 0.38, frame, 0.62, 0, frame)
        cv2.putText(
            frame,
            f"Time {_timestamp_label(timestamp_ms)}",
            (20, 54),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.8,
            (255, 255, 255),
            2,
            cv2.LINE_AA,
        )


def write_annotated_video(
    video_path: Path,
    observations: list[FrameObservation],
    punch_windows: list[PunchWindowResult],
    output_path: Path,
    fps: float,
) -> None:
    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise ValueError(f"Could not open video file: {video_path}")

    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH) or 0)
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT) or 0)
    if width <= 0 or height <= 0:
        raise ValueError(f"Could not determine video dimensions for: {video_path}")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    writer = cv2.VideoWriter(
        str(output_path),
        cv2.VideoWriter_fourcc(*"mp4v"),
        fps,
        (width, height),
    )

    observations_by_index = {observation.frame_index: observation for observation in observations}
    frame_index = 0

    while True:
        success, frame = capture.read()
        if not success:
            break

        frame_index += 1
        observation = observations_by_index.get(frame_index)
        timestamp_ms = observation.timestamp_ms if observation is not None else int((frame_index - 1) * 1000 / fps)

        _draw_overlay(frame, timestamp_ms=timestamp_ms, punch_windows=punch_windows)
        if observation is not None and observation.pose_detected:
            _draw_landmarks(frame, observation.keypoints)

        cv2.putText(
            frame,
            f"Frame {frame_index}",
            (20, height - 20),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.7,
            (255, 255, 255),
            2,
            cv2.LINE_AA,
        )
        writer.write(frame)

    capture.release()
    writer.release()


def process_video(
    video_path: Path,
    output_dir: Path,
    *,
    pose_frames_output_path: Path | None = None,
    model_path: Path | None = None,
    landmark_set: str = "boxing",
    write_annotated: bool = True,
    window_ms: int = 250,
    stride_ms: int = 40,
    frame_stride: int = 1,
    combo_gap_ms: int = 500,
) -> ProcessingResult:
    if not video_path.exists():
        raise FileNotFoundError(f"Video file not found: {video_path}")

    session_id = f"{video_path.stem}-{uuid4().hex[:10]}"
    session_dir = output_dir / session_id
    session_dir.mkdir(parents=True, exist_ok=True)

    observations, fps, _dimensions = extract_frame_observations(
        video_path,
        model_path=model_path,
        landmark_set=landmark_set,
        frame_stride=frame_stride,
    )

    if not observations:
        raise ValueError(f"No frames were extracted from {video_path}")

    pose_frames = pd.DataFrame([observation.to_row() for observation in observations])
    pose_frames["video_id"] = session_id
    pose_frames_path = pose_frames_output_path or (session_dir / "pose_frames.csv")
    pose_frames_path.parent.mkdir(parents=True, exist_ok=True)
    pose_frames.to_csv(pose_frames_path, index=False)

    prediction_windows_path = session_dir / "prediction_windows.csv"
    merged_windows_path = session_dir / "predicted_punch_windows.csv"
    model_path = model_path or DEFAULT_MODEL_PATH
    predict_punches(
        pose_frames_path,
        model_path,
        merged_windows_path,
        windows_output_path=prediction_windows_path,
        window_ms=window_ms,
        stride_ms=stride_ms,
        punch_threshold=None,
    )

    prediction_windows = pd.read_csv(prediction_windows_path)
    punch_windows_df = merge_prediction_windows(prediction_windows)
    punch_windows_df.to_csv(merged_windows_path, index=False)

    punch_windows = [
        PunchWindowResult(
            start_ms=int(row.start_ms),
            end_ms=int(row.end_ms),
            window_count=int(getattr(row, "window_count", 1)),
            punch_probability=float(getattr(row, "punch_probability", 1.0)),
        )
        for row in punch_windows_df.itertuples(index=False)
    ]

    graph_metrics = compute_graph_metrics(
        pose_frames,
        punch_windows_df.assign(video_id=session_id),
        window_ms=window_ms,
        stride_ms=stride_ms,
        combo_gap_ms=combo_gap_ms,
    )
    performance_points = [
        PerformancePoint(
            timestamp_ms=int(row.center_ms),
            punch_volume=int(row.punch_volume),
            guard_height=float(row.guard_height),
            movement=float(row.movement),
        )
        for row in graph_metrics.itertuples(index=False)
    ]
    annotated_path = session_dir / f"{video_path.stem}_annotated.mp4"
    if write_annotated:
        write_annotated_video(video_path, observations, punch_windows, annotated_path, fps)
    else:
        annotated_path = video_path

    duration_ms = int(observations[-1].timestamp_ms + max(1, round(1000 / fps)))
    source_copy_path = session_dir / video_path.name
    if not source_copy_path.exists():
        source_copy_path.write_bytes(video_path.read_bytes())

    return ProcessingResult(
        session_id=session_id,
        title=video_path.stem.replace("_", " ").title(),
        date_label=pd.Timestamp.now().strftime("%d/%m/%Y"),
        duration_label=_format_duration_label(duration_ms),
        duration_ms=duration_ms,
        source_video_name=video_path.name,
        source_video_path=str(source_copy_path),
        annotated_video_path=str(annotated_path),
        punch_count=len(punch_windows),
        punch_windows=punch_windows,
        performance_points=performance_points,
        prediction_windows_path=str(prediction_windows_path),
        pose_frames_path=str(pose_frames_path),
    )
