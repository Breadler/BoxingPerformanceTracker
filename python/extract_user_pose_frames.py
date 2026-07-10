from __future__ import annotations

import argparse
from pathlib import Path

from pose_extractor import process_video


def default_output_path(video_path: Path) -> Path:
    return Path("data") / f"{video_path.stem}_pose_frames.csv"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Extract frame-level pose rows from one user boxing video for punch prediction.",
    )
    parser.add_argument(
        "--video",
        type=Path,
        required=True,
        help="Path to the user's recorded or uploaded boxing video.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Output pose CSV. Defaults to data/<video_name>_pose_frames.csv.",
    )
    parser.add_argument(
        "--write-annotated",
        action="store_true",
        help="Also write a review video with pose overlay and frame numbers.",
    )
    parser.add_argument(
        "--model",
        type=Path,
        default=None,
        help="Optional path to a pose_landmarker.task file.",
    )
    parser.add_argument(
        "--frame-stride",
        type=int,
        default=1,
        help="Export pose features for every Nth frame. Keep 1 for punch detection.",
    )
    parser.add_argument(
        "--landmark-set",
        choices=["boxing", "full"],
        default="boxing",
        help="Must match the landmark set used to build the Random Forest training CSV.",
    )
    args = parser.parse_args()

    output_path = args.output or default_output_path(args.video)
    process_video(
        args.video,
        output_path,
        write_annotated=args.write_annotated,
        frame_stride=args.frame_stride,
        model_path=args.model,
        landmark_set=args.landmark_set,
        write_header=True,
    )
    print(f"User pose frames ready for punch prediction: {output_path}")


if __name__ == "__main__":
    main()
