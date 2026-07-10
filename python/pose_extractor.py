from __future__ import annotations

import argparse
import importlib
from dataclasses import dataclass
from pathlib import Path
from urllib.request import urlretrieve

import cv2
import numpy as np
import pandas as pd

POSE_LANDMARKER_MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/pose_landmarker/"
    "pose_landmarker_lite/float16/1/pose_landmarker_lite.task"
)
DEFAULT_MODEL_PATH = Path(__file__).with_name("models") / "pose_landmarker_lite.task"
VIDEO_EXTENSIONS = {".avi", ".mp4", ".mov", ".mkv", ".webm"}
BOXING_LANDMARK_INDICES = tuple(range(0, 17)) + tuple(range(23, 33))
POSE_LANDMARK_NAMES = [
    "nose",
    "left_eye_inner",
    "left_eye",
    "left_eye_outer",
    "right_eye_inner",
    "right_eye",
    "right_eye_outer",
    "left_ear",
    "right_ear",
    "mouth_left",
    "mouth_right",
    "left_shoulder",
    "right_shoulder",
    "left_elbow",
    "right_elbow",
    "left_wrist",
    "right_wrist",
    "left_pinky",
    "right_pinky",
    "left_index",
    "right_index",
    "left_thumb",
    "right_thumb",
    "left_hip",
    "right_hip",
    "left_knee",
    "right_knee",
    "left_ankle",
    "right_ankle",
    "left_heel",
    "right_heel",
    "left_foot_index",
    "right_foot_index",
]
POSE_CONNECTIONS = [
    (0, 1),
    (1, 2),
    (2, 3),
    (3, 7),
    (0, 4),
    (4, 5),
    (5, 6),
    (6, 8),
    (9, 10),
    (11, 12),
    (11, 13),
    (13, 15),
    (15, 17),
    (15, 19),
    (15, 21),
    (17, 19),
    (12, 14),
    (14, 16),
    (16, 18),
    (16, 20),
    (16, 22),
    (18, 20),
    (11, 23),
    (12, 24),
    (23, 24),
    (23, 25),
    (24, 26),
    (25, 27),
    (26, 28),
    (27, 29),
    (28, 30),
    (29, 31),
    (30, 32),
    (27, 31),
    (28, 32),
]
METADATA_COLUMNS = ("video_id", "frame_index", "timestamp_ms", "pose_detected")


@dataclass
class VideoProcessingResult:
    video_path: Path
    processed_frames: int
    pose_frames: int
    annotated_path: Path | None = None


def get_landmark_indices(landmark_set: str) -> tuple[int, ...]:
    if landmark_set == "boxing":
        return BOXING_LANDMARK_INDICES
    if landmark_set == "full":
        return tuple(range(33))
    raise ValueError(f"Unsupported landmark set: {landmark_set}")


def get_landmark_feature_names(landmark_indices: tuple[int, ...]) -> list[str]:
    feature_names: list[str] = []
    for landmark_index in landmark_indices:
        landmark_name = POSE_LANDMARK_NAMES[landmark_index]
        for component in ("x", "y", "z", "visibility"):
            feature_names.append(f"{landmark_name}_{component}")
    return feature_names


def resolve_model_path(model_path: Path | None) -> Path:
    if model_path is not None:
        if not model_path.exists():
            raise FileNotFoundError(f"Model file not found: {model_path}")
        return model_path

    if DEFAULT_MODEL_PATH.exists():
        return DEFAULT_MODEL_PATH

    DEFAULT_MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading pose model to {DEFAULT_MODEL_PATH}...")
    urlretrieve(POSE_LANDMARKER_MODEL_URL, DEFAULT_MODEL_PATH)
    return DEFAULT_MODEL_PATH


def annotated_output_path(video_path: Path) -> Path:
    return video_path.parent / "annotated" / f"{video_path.stem}_annotated.mp4"


def collect_video_paths(video: Path | None, input_dir: Path | None) -> list[Path]:
    if video is not None and input_dir is not None:
        raise ValueError("Pass either --video or --input-dir, not both.")
    if video is None and input_dir is None:
        raise ValueError("Pass one of --video or --input-dir.")

    if video is not None:
        if not video.exists():
            raise FileNotFoundError(f"Video file not found: {video}")
        return [video]

    if not input_dir.exists():
        raise FileNotFoundError(f"Input directory not found: {input_dir}")
    if not input_dir.is_dir():
        raise ValueError(f"Input path is not a directory: {input_dir}")

    videos = sorted(
        path
        for path in input_dir.iterdir()
        if path.is_file() and path.suffix.lower() in VIDEO_EXTENSIONS
    )
    if not videos:
        raise ValueError(f"No supported video files found in: {input_dir}")
    return videos


def draw_pose(frame, landmarks) -> None:
    height, width = frame.shape[:2]
    points: list[tuple[int, int]] = []

    for landmark in landmarks:
        x = int(landmark.x * width)
        y = int(landmark.y * height)
        points.append((x, y))

    for point in points:
        cv2.circle(frame, point, 4, (0, 255, 255), -1, lineType=cv2.LINE_AA)

    for start_index, end_index in POSE_CONNECTIONS:
        if start_index >= len(points) or end_index >= len(points):
            continue
        cv2.line(
            frame,
            points[start_index],
            points[end_index],
            (0, 200, 0),
            2,
            lineType=cv2.LINE_AA,
        )


def draw_frame_label(frame, frame_index: int) -> None:
    cv2.putText(
        frame,
        f"Frame {frame_index}",
        (20, 40),
        cv2.FONT_HERSHEY_SIMPLEX,
        1.0,
        (255, 255, 255),
        2,
        lineType=cv2.LINE_AA,
    )


def landmarks_to_features(landmarks, landmark_indices: tuple[int, ...]) -> list[float]:
    return [
        coord
        for index in landmark_indices
        for coord in (
            landmarks[index].x,
            landmarks[index].y,
            landmarks[index].z,
            landmarks[index].visibility,
        )
    ]


def append_rows_to_csv(
    rows: list[dict[str, object]],
    output_csv: Path,
    columns: list[str],
    write_header: bool,
) -> None:
    if not rows:
        return

    output_csv.parent.mkdir(parents=True, exist_ok=True)
    dataframe = pd.DataFrame(rows, columns=columns)
    dataframe.to_csv(
        output_csv,
        mode="w" if write_header else "a",
        index=False,
        header=write_header,
    )


def process_video(
    video_path: Path,
    output_csv: Path,
    *,
    write_annotated: bool,
    frame_stride: int,
    model_path: Path | None,
    landmark_set: str,
    write_header: bool,
) -> VideoProcessingResult:
    if frame_stride < 1:
        raise ValueError("frame_stride must be at least 1")

    resolved_model_path = resolve_model_path(model_path)
    landmark_indices = get_landmark_indices(landmark_set)
    feature_names = get_landmark_feature_names(landmark_indices)
    csv_columns = [*METADATA_COLUMNS, *feature_names]
    video_id = video_path.stem

    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise ValueError(f"Could not open video file: {video_path}")

    fps = capture.get(cv2.CAP_PROP_FPS)
    if fps <= 0:
        fps = 30.0

    frame_width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    if frame_width <= 0 or frame_height <= 0:
        capture.release()
        raise ValueError(f"Could not read video dimensions: {video_path}")

    writer: cv2.VideoWriter | None = None
    annotated_path: Path | None = None
    if write_annotated:
        annotated_path = annotated_output_path(video_path)
        annotated_path.parent.mkdir(parents=True, exist_ok=True)
        writer = cv2.VideoWriter(
            str(annotated_path),
            cv2.VideoWriter_fourcc(*"mp4v"),
            fps,
            (frame_width, frame_height),
        )

    mp = importlib.import_module("mediapipe")
    pose_module = importlib.import_module("mediapipe.tasks.python.vision.pose_landmarker")
    base_options_module = importlib.import_module("mediapipe.tasks.python.core.base_options")
    running_mode_module = importlib.import_module(
        "mediapipe.tasks.python.vision.core.vision_task_running_mode"
    )

    options = pose_module.PoseLandmarkerOptions(
        base_options=base_options_module.BaseOptions(
            model_asset_path=str(resolved_model_path),
        ),
        running_mode=running_mode_module.VisionTaskRunningMode.VIDEO,
        num_poses=1,
    )

    frame_rows: list[dict[str, object]] = []
    processed_frames = 0
    pose_frames = 0
    frame_index = 0

    with pose_module.PoseLandmarker.create_from_options(options) as pose_landmarker:
        while True:
            success, frame = capture.read()
            if not success:
                break

            frame_index += 1
            should_process = (frame_index - 1) % frame_stride == 0
            pose_landmarks = None

            if should_process:
                processed_frames += 1
                timestamp_ms = int((frame_index - 1) * 1000 / fps)
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
                results = pose_landmarker.detect_for_video(mp_image, timestamp_ms)

                if results.pose_landmarks:
                    pose_landmarks = results.pose_landmarks[0]
                    pose_frames += 1
                    row: dict[str, object] = {
                        "video_id": video_id,
                        "frame_index": frame_index,
                        "timestamp_ms": timestamp_ms,
                        "pose_detected": 1,
                    }
                    for name, value in zip(
                        feature_names,
                        landmarks_to_features(pose_landmarks, landmark_indices),
                        strict=True,
                    ):
                        row[name] = value
                    frame_rows.append(row)
                else:
                    row = {
                        "video_id": video_id,
                        "frame_index": frame_index,
                        "timestamp_ms": timestamp_ms,
                        "pose_detected": 0,
                    }
                    for name in feature_names:
                        row[name] = np.nan
                    frame_rows.append(row)

            if writer is not None:
                output_frame = frame.copy()
                if pose_landmarks is not None:
                    draw_pose(output_frame, pose_landmarks)
                draw_frame_label(output_frame, frame_index)
                writer.write(output_frame)

    capture.release()
    if writer is not None:
        writer.release()

    if processed_frames == 0:
        raise ValueError(f"No frames were processed for video: {video_path}")

    append_rows_to_csv(frame_rows, output_csv, csv_columns, write_header=write_header)

    print(
        f"{video_id}: processed {processed_frames} frames, "
        f"exported {len(frame_rows)} CSV rows ({pose_frames} with pose landmarks).",
    )
    if annotated_path is not None:
        print(f"{video_id}: wrote annotated video to {annotated_path}")

    return VideoProcessingResult(
        video_path=video_path,
        processed_frames=processed_frames,
        pose_frames=pose_frames,
        annotated_path=annotated_path,
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Extract per-frame MediaPipe pose features and optionally write annotated review videos.",
    )
    input_group = parser.add_mutually_exclusive_group(required=True)
    input_group.add_argument("--video", type=Path, help="Path to a single input video file")
    input_group.add_argument(
        "--input-dir",
        type=Path,
        help="Directory containing input video files (.avi, .mp4, .mov, .mkv, .webm)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        required=True,
        help="CSV output file path for per-frame pose features",
    )
    parser.add_argument(
        "--write-annotated",
        action="store_true",
        help="Write annotated review videos to <video_dir>/annotated/<video_stem>_annotated.mp4",
    )
    parser.add_argument(
        "--model",
        type=Path,
        default=None,
        help="Optional path to a pose_landmarker.task file. If omitted, downloads a default model.",
    )
    parser.add_argument(
        "--frame-stride",
        type=int,
        default=1,
        help="Export pose features for every Nth frame (default: 1). Use 1 when labeling punch windows.",
    )
    parser.add_argument(
        "--landmark-set",
        choices=["boxing", "full"],
        default="boxing",
        help="Landmark subset to export: boxing keeps face/head, shoulders, elbows, wrists, and torso; full keeps all 33 landmarks.",
    )
    args = parser.parse_args()

    video_paths = collect_video_paths(args.video, args.input_dir)
    write_header = not args.output.exists()
    failures: list[tuple[Path, str]] = []
    successes: list[VideoProcessingResult] = []

    for index, video_path in enumerate(video_paths):
        try:
            result = process_video(
                video_path,
                args.output,
                write_annotated=args.write_annotated,
                frame_stride=args.frame_stride,
                model_path=args.model,
                landmark_set=args.landmark_set,
                write_header=write_header,
            )
            successes.append(result)
            write_header = False
        except Exception as exc:
            failures.append((video_path, str(exc)))
            print(f"Failed to process {video_path.name}: {exc}")

    print(
        f"\nFinished: {len(successes)} succeeded, {len(failures)} failed, "
        f"CSV output at {args.output}",
    )
    if failures:
        print("Failures:")
        for video_path, message in failures:
            print(f"  - {video_path.name}: {message}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
