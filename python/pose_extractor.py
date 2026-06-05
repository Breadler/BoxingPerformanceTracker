from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np
import pandas as pd
from mediapipe.python.solutions import pose as mp_pose


def extract_pose_features(video_path: Path) -> np.ndarray:
    capture = cv2.VideoCapture(str(video_path))
    frame_features: list[np.ndarray] = []

    with mp_pose.Pose(static_image_mode=False, min_detection_confidence=0.5) as pose:
        while True:
            success, frame = capture.read()
            if not success:
                break

            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = pose.process(rgb)

            if not results.pose_landmarks:
                continue

            landmarks = results.pose_landmarks.landmark
            frame_features.append(
                np.array(
                    [coord for lm in landmarks for coord in (lm.x, lm.y, lm.z, lm.visibility)],
                    dtype=np.float32,
                ),
            )

    capture.release()

    if not frame_features:
        raise ValueError(f"No pose landmarks found in video: {video_path}")

    return np.mean(frame_features, axis=0)


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract MediaPipe pose features from a video.")
    parser.add_argument("--video", type=Path, required=True, help="Path to input video file")
    parser.add_argument("--label", required=True, help="Class label for this sample")
    parser.add_argument("--output", type=Path, required=True, help="CSV output file path")
    args = parser.parse_args()

    features = extract_pose_features(args.video)
    feature_names = [f"f_{i}" for i in range(features.shape[0])]
    row = pd.DataFrame([features], columns=feature_names)
    row["label"] = args.label

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        row.to_csv(args.output, mode="a", index=False, header=False)
    else:
        row.to_csv(args.output, index=False)


if __name__ == "__main__":
    main()
