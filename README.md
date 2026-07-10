# BoxingPerformanceTracker

BoxingPerformanceTracker is a starter project for:

- an **Android app written in Kotlin** for capturing or loading boxing training videos
- a **Python ML pipeline** that uses **MediaPipe** pose landmarks and trains a **Random Forest** classifier

## Repository layout

- `android/` – minimal Android Kotlin app scaffold
- `python/` – MediaPipe feature extraction and Random Forest training scripts

## Python setup

```bash
cd python
pip install -r requirements.txt
```

## Extract pose features from videos

`pose_extractor.py` reads each video once, exports **one CSV row per processed frame**, and can optionally write an annotated review video with a full pose skeleton and frame numbers burned in.

The first run downloads a default pose model into `python/models/`. If you already have a `.task` model file, pass it with `--model`.

By default, the extractor uses a boxing-focused landmark subset that keeps the head, shoulders, elbows, wrists, and torso while omitting finger landmarks. Use `--landmark-set full` if you want all 33 pose landmarks instead.

### Single video

```bash
cd python
python pose_extractor.py --video "C:\path\to\boxing-round.mp4" --output data/pose_frames.csv --write-annotated
```

### Whole folder

```bash
cd python
python pose_extractor.py --input-dir "C:\path\to\BoxingPunchingBag" --output data/pose_frames.csv --write-annotated
```

Supported video extensions: `.avi`, `.mp4`, `.mov`, `.mkv`, `.webm`.

### Outputs

| Output | Location |
|--------|----------|
| Frame-level pose CSV | Path passed to `--output` (for example `data/pose_frames.csv`) |
| Annotated review video | `<video_folder>/annotated/<video_stem>_annotated.mp4` (only with `--write-annotated`) |

CSV columns:

- `video_id`, `frame_index`, `timestamp_ms`, `pose_detected`
- landmark feature columns such as `left_wrist_x`, `left_wrist_y`, `left_wrist_z`, `left_wrist_visibility`

Use `frame_index` from the annotated video when creating punch window labels. The training pipeline converts those frame labels into milliseconds using `timestamp_ms`, so the same punch event covers the same duration even if frame rates differ. Keep `--frame-stride 1` while labeling.

For faster re-extraction without review videos, omit `--write-annotated`:

```bash
python pose_extractor.py --input-dir "C:\path\to\BoxingPunchingBag" --output data/pose_frames.csv
```

## Extract pose features from user footage

Use `extract_user_pose_frames.py` for a single recorded or uploaded user video. It writes the same kind of frame-level pose CSV as `pose_extractor.py`, but keeps the output separate from the training dataset.

```bash
cd python
python extract_user_pose_frames.py --video "C:\path\to\user-round.mp4" --output data/user_pose_frames.csv
```

That output is the unlabeled pose CSV the trained Random Forest should run on to identify punch moments.

To test the trained Random Forest on that user pose CSV:

```bash
cd python
python predict_punches.py --pose-frames data/user_pose_frames.csv --model models/random_forest.joblib --output data/predicted_punch_windows.csv --windows-output data/user_window_predictions.csv --window-ms 250 --stride-ms 40
```

If the model becomes too conservative after adding more varied labels, lower the punch probability threshold:

```bash
python predict_punches.py --pose-frames data/user_pose_frames.csv --model models/random_forest.joblib --output data/predicted_punch_windows.csv --windows-output data/user_window_predictions.csv --window-ms 250 --stride-ms 40 --punch-threshold 0.35
```

## Label punches and train a model

Workflow:

1. Run `pose_extractor.py` on your dataset and review the annotated videos.
2. Create `data/punch_windows.csv` with either `video_id`, `start_frame`, `end_frame` or `video_id`, `start_ms`, `end_ms` for each punch event.
3. Build `data/training.csv` from `pose_frames.csv` and `punch_windows.csv`.
4. Train the classifier:

```bash
cd python
python build_training_csv.py --pose-frames data/pose_frames.csv --punch-windows data/punch_windows.csv --output data/training.csv --window-ms 250 --negative-stride-ms 250 --positive-anchor end
python train_random_forest.py --input data/training.csv --output models/random_forest.joblib
```

The pipeline is time-aware. `pose_frames.csv` stores `timestamp_ms`, and frame-based punch labels are converted into milliseconds before training windows are built. Keep the same `--window-ms` value when running `predict_punches.py`, because prediction windows must cover the same duration as training windows even when video frame rates differ.

By default, fixed-size positive windows are end-anchored so the labeled punch end pose (impact/contact frame) is preserved while the start can shift as needed. Use `--positive-anchor center` if you want midpoint anchoring instead. Use `--use-full-punch-window` if you want each positive row to keep the full labeled start/end range.

If new labels make the model worse, audit the label file before retraining:

```bash
python audit_punch_labels.py --pose-frames data/pose_frames.csv --punch-windows data/punch_windows.csv
```

The main CSV files are:

- `data/pose_frames.csv` - frame-level pose data, no labels, all videos
- `data/punch_windows.csv` - hand-labeled punch windows from overlay review videos, stored as frame ranges or millisecond ranges
- `data/training.csv` - generated time-window rows labeled `punch` or `no_punch`, with velocity, body-relative arm, and extension-change features

`data/features.csv` and the old per-video averaged export format are obsolete. Use `data/pose_frames.csv` and regenerate `data/training.csv` instead.

## Obsolete files

- `python/visualize_pose.py` – removed; use `pose_extractor.py --write-annotated` instead.
