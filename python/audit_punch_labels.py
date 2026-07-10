from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


def audit_punch_labels(pose_frames_path: Path, punch_windows_path: Path) -> None:
    pose_frames = pd.read_csv(pose_frames_path)
    punch_windows = pd.read_csv(punch_windows_path)

    required_pose_columns = {"video_id", "frame_index", "timestamp_ms"}
    missing_pose_columns = required_pose_columns - set(pose_frames.columns)
    has_frame_labels = {"start_frame", "end_frame"}.issubset(punch_windows.columns)
    has_time_labels = {"start_ms", "end_ms"}.issubset(punch_windows.columns)
    missing_label_columns = set() if has_frame_labels or has_time_labels else {"start_frame/end_frame or start_ms/end_ms"}
    if missing_pose_columns:
        raise ValueError(f"Pose CSV is missing columns: {sorted(missing_pose_columns)}")
    if missing_label_columns:
        raise ValueError(f"Punch windows CSV is missing columns: {sorted(missing_label_columns)}")

    pose_frames["frame_index"] = pose_frames["frame_index"].astype(int)
    pose_frames["timestamp_ms"] = pose_frames["timestamp_ms"].astype(int)

    if has_frame_labels:
        punch_windows["start_frame"] = punch_windows["start_frame"].astype(int)
        punch_windows["end_frame"] = punch_windows["end_frame"].astype(int)
        punch_windows["duration_frames"] = punch_windows["end_frame"] - punch_windows["start_frame"] + 1

    if not has_time_labels:
        frame_lookup = {
            video_id: frames.sort_values("frame_index").reset_index(drop=True)
            for video_id, frames in pose_frames.groupby("video_id")
        }
        start_ms_values = []
        end_ms_values = []
        for row in punch_windows.itertuples(index=False):
            frames = frame_lookup.get(row.video_id)
            if frames is None:
                start_ms_values.append(None)
                end_ms_values.append(None)
                continue
            start_position = (frames["frame_index"] - int(row.start_frame)).abs().idxmin()
            end_position = (frames["frame_index"] - int(row.end_frame)).abs().idxmin()
            start_ms_values.append(int(frames.loc[start_position, "timestamp_ms"]))
            end_ms_values.append(int(frames.loc[end_position, "timestamp_ms"]))
        punch_windows["start_ms"] = start_ms_values
        punch_windows["end_ms"] = end_ms_values

    punch_windows["duration_ms"] = punch_windows["end_ms"] - punch_windows["start_ms"]

    pose_video_ids = set(pose_frames["video_id"])
    label_video_ids = set(punch_windows["video_id"])
    missing_videos = sorted(label_video_ids - pose_video_ids)

    print("Punch label audit")
    print(f"  pose videos: {len(pose_video_ids)}")
    print(f"  labeled videos: {len(label_video_ids)}")
    print(f"  punch windows: {len(punch_windows)}")
    print(f"  labels with missing pose video: {len(missing_videos)}")
    if missing_videos:
        for video_id in missing_videos[:20]:
            print(f"    - {video_id}")

    print("\nPunch duration summary, milliseconds")
    print(punch_windows["duration_ms"].describe().to_string())
    if "duration_frames" in punch_windows.columns:
        print("\nPunch duration summary, frames")
        print(punch_windows["duration_frames"].describe().to_string())

    if has_frame_labels:
        invalid_windows = punch_windows[punch_windows["end_frame"] < punch_windows["start_frame"]]
        if not invalid_windows.empty:
            print("\nInvalid frame windows")
            print(invalid_windows.to_string(index=False))

    invalid_time_windows = punch_windows[punch_windows["end_ms"] < punch_windows["start_ms"]]
    if not invalid_time_windows.empty:
        print("\nInvalid time windows")
        print(invalid_time_windows.to_string(index=False))

    very_short = punch_windows[punch_windows["duration_ms"] <= 80]
    very_long = punch_windows[punch_windows["duration_ms"] >= 800]
    if not very_short.empty:
        print("\nVery short labels, check these manually")
        print(very_short.to_string(index=False))
    if not very_long.empty:
        print("\nVery long labels, check these manually")
        print(very_long.to_string(index=False))

    frame_ranges = pose_frames.groupby("video_id")["frame_index"].agg(["min", "max"])
    out_of_range_rows = []
    if has_frame_labels:
        for row in punch_windows.itertuples(index=False):
            if row.video_id not in frame_ranges.index:
                continue
            min_frame = int(frame_ranges.loc[row.video_id, "min"])
            max_frame = int(frame_ranges.loc[row.video_id, "max"])
            if int(row.start_frame) < min_frame or int(row.end_frame) > max_frame:
                out_of_range_rows.append(
                    {
                        "video_id": row.video_id,
                        "start_frame": row.start_frame,
                        "end_frame": row.end_frame,
                        "pose_min_frame": min_frame,
                        "pose_max_frame": max_frame,
                    }
                )

    if out_of_range_rows:
        print("\nLabels outside available pose frame range")
        print(pd.DataFrame(out_of_range_rows).to_string(index=False))


def main() -> None:
    parser = argparse.ArgumentParser(description="Audit punch label windows against pose frames.")
    parser.add_argument("--pose-frames", type=Path, default=Path("data/pose_frames.csv"))
    parser.add_argument("--punch-windows", type=Path, default=Path("data/punch_windows.csv"))
    args = parser.parse_args()
    audit_punch_labels(args.pose_frames, args.punch_windows)


if __name__ == "__main__":
    main()
