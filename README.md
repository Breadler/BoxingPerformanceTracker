# BoxingPerformanceTracker

An Android app that turns a recorded or imported boxing round video into an on-device punch count, guard height, and movement analysis using MediaPipe pose tracking and a RandomForest classifier.

BoxingPerformanceTracker is:

- an **Android app written in Kotlin (Jetpack Compose)** that records a boxing round in-app (with a start/end bell countdown) or imports an existing video, then runs the **entire inference pipeline fully on-device** — MediaPipe pose extraction, an annotated skeleton video, and a RandomForest punch classifier — with no server and no network connection.
- a **Python offline pipeline** used to build the training dataset, train the models bundled into the Android app, and prototype new analysis before it gets ported to Kotlin.

## Results

The final RandomForest classifier reaches **69.4% held-out accuracy** and **0.692 macro F1** under video-grouped cross-validation (350 labeled rows across 111 videos), well above chance. Ported to on-device Java via m2cgen, it runs at 4-8x real-time with no network dependency. A field test with three boxers found guard height and movement tracking closely with the source video, punch-count recall averaging 58.8%, and an overall usability rating of 4.83/5.

## Repository layout

- `android/` – the Kotlin/Compose app. All inference (`data/processing/`) runs on-device; sessions persist locally in Room.
- `python/` – dataset building, model training, and pipeline prototyping. Nothing here runs at app runtime; it's the offline workshop that produces the assets in `android/app/src/main/assets/` and lets you preview new metrics before porting them.
- `thesis/` – the written thesis (chapters, notes, decisions log) documenting this project's design, implementation, and evaluation. Not part of the app or pipeline.

## Python setup

Any recent Python works; this repo's `.venv` currently uses 3.14:

```bash
cd python
pip install -r requirements.txt
```

## The pipeline, in order

### 1. Extract pose features from your training video dataset

`pose_extractor.py` reads each video once, exports **one CSV row per processed frame**, and can optionally write an annotated review video with a full pose skeleton and frame numbers burned in.

The first run downloads a default pose model into `python/models/`. If you already have a `.task` model file, pass it with `--model`.

By default, the extractor uses a boxing-focused landmark subset that keeps the head, shoulders, elbows, wrists, and torso while omitting finger landmarks. Use `--landmark-set full` if you want all 33 pose landmarks instead.

```bash
cd python
python pose_extractor.py --input-dir "C:\path\to\BoxingPunchingBag" --output data/pose_frames.csv --write-annotated
```

Single video: use `--video` instead of `--input-dir`. Supported extensions: `.avi`, `.mp4`, `.mov`, `.mkv`, `.webm`. Omit `--write-annotated` for faster re-extraction without review videos.

CSV columns: `video_id`, `frame_index`, `timestamp_ms`, `pose_detected`, plus landmark feature columns such as `left_wrist_x`, `left_wrist_y`, `left_wrist_z`, `left_wrist_visibility`.

### 2. Label punches

Watch the annotated videos and create `data/punch_windows.csv` by hand, with either `video_id`, `start_frame`, `end_frame` or `video_id`, `start_ms`, `end_ms` for each punch event. Use `frame_index` from the annotated video; the pipeline converts frame labels into milliseconds via `timestamp_ms`, so the same punch event covers the same duration even if frame rates differ. Keep `--frame-stride 1` while labeling.

If a model trained on your labels starts missing obvious punches or flagging obvious non-punches, audit the label file before retraining:

```bash
python audit_punch_labels.py --pose-frames data/pose_frames.csv --punch-windows data/punch_windows.csv
```

### 3. Build the training CSV

```bash
cd python
python build_training_csv.py --pose-frames data/pose_frames.csv --punch-windows data/punch_windows.csv --output data/training.csv --window-ms 250 --negative-stride-ms 250 --positive-anchor end
```

By default, fixed-size positive windows are end-anchored so the labeled punch end pose (impact/contact frame) is preserved while the start can shift as needed. Use `--positive-anchor center` for midpoint anchoring, or `--use-full-punch-window` to keep each positive row's full labeled start/end range.

### 4. Train the model

```bash
cd python
python train_random_forest.py --input data/training.csv --output models/random_forest.joblib
```

This RandomForest is the model used for all Python-side testing and the one the Android app ships with.

### 5. Export the RandomForest for Android

```bash
cd python
python export_random_forest_java.py --model models/random_forest.joblib
```

Ports the trained model to a plain Java class via [m2cgen](https://github.com/BayesWitnesses/m2cgen), writing it **directly into the Android source tree**:

- `android/app/src/main/java/com/breadler/boxingperformancetracker/data/processing/PunchForestModel.java`

`RandomForestPunchClassifier.kt` wraps it with the feature-column ordering (must match `feature_columns` in the `.joblib` artifact exactly - the generated class takes a plain positional `double[]`, with no column-name metadata of its own) and the punch/no-punch threshold. If you change the feature set or retrain with a different `classes_` order, the script prints the new feature list and a reminder to update `RandomForestPunchClassifier.kt` to match.

`android/app/src/main/assets/pose_landmarker_lite.task` is checked in separately and doesn't need regenerating unless you want a different MediaPipe pose model.

The pipeline is time-aware throughout: `pose_frames.csv` stores `timestamp_ms`, and frame-based punch labels are converted to milliseconds before training windows are built. Keep the same `--window-ms` everywhere (training, prediction, graph metrics), because windows must cover the same duration even when video frame rates differ.

### 6. From here, the Android app runs everything itself

Once `PunchForestModel.java` exists, the Android app needs nothing else from Python at runtime. Starting a session, either by recording a round in-app (front/back camera, with a start/end bell countdown) or by importing an existing video file:

1. Copies the video into local app storage. Sessions can be queued — start another recording or import while one is still processing, and they run one after another with progress shown on a processing screen.
2. Runs MediaPipe pose extraction on-device and writes a local annotated skeleton video (no server, no network).
3. Runs the bundled RandomForest classifier over sliding windows to produce punch predictions and merged punch windows.
4. Persists everything to a local Room database so it survives app restarts and shows up under Previous Sessions.

The three fatigue graph metrics — punch volume, guard height, movement — are computed on-device too (`GraphMetrics.kt`, ported from the Python modules below) and shown on the session playback graph.

### 7. Test prediction and fatigue metrics against a new (unlabeled) video

This is the Python-side preview of what the app does on-device.

```bash
cd python
python extract_user_pose_frames.py --video "C:\path\to\user-round.mp4" --output data/user_pose_frames.csv

python predict_punches.py --pose-frames data/user_pose_frames.csv --model models/random_forest.joblib --output data/predicted_punch_windows.csv --windows-output data/user_window_predictions.csv --window-ms 250 --stride-ms 40
```

If the model is too conservative, lower the punch probability threshold (e.g. `--punch-threshold 0.35`).

Then compute and preview the three fatigue metrics — punch volume (punches grouped into combos), guard height (how far the higher-guarding wrist sits above the nose), and movement (hip x/z speed). Each metric is its own stage/script, all on the same sliding-window grid; `graph_metrics.py` is the graph-generation stage that runs the other three and merges them:

- `punch_volume.py` — combo-grouped punch count. Deliberately never smoothed/downsampled (a sparse, bursty signal — averaging it dilutes short combos almost to nothing).
- `guard_height.py` — `nose_y - highest-guarding wrist_y` per window.
- `movement.py` — hip-midpoint x/z speed per window.
- `graph_metrics.py` — runs all three and merges them on `(video_id, center_ms)`; also owns `smooth_graph_metrics()`/`downsample_graph_metrics()`, applied to guard height/movement only.

Each stage script also runs standalone (`python punch_volume.py --pose-frames ... --punch-windows ...`, etc.) if you want to inspect one metric in isolation.

```bash
python graph_metrics.py --pose-frames data/user_pose_frames.csv --punch-windows data/predicted_punch_windows.csv --output data/graph_metrics.csv --combo-gap-ms 500

python plot_graph_metrics.py --pose-frames data/user_pose_frames.csv --punch-windows data/predicted_punch_windows.csv --output data/graph_metrics_plot.png
```

`graph_metrics.py`'s CSV output is the exact, unsmoothed data (punch volume always is; guard height/movement here too). `plot_graph_metrics.py` additionally smooths + downsamples guard height/movement and curve-fits all three purely for a readable chart — see the module docstrings for the smoothing/downsampling knobs (`--*-smoothing-ms`, `--downsample-bucket-ms`) if a plot looks too noisy or too flat.

## Data files reference

| File | What it is |
|------|------------|
| `data/pose_frames.csv` | Frame-level pose data for the training video set, no labels. Large, regenerated by `pose_extractor.py`, not tracked in git. |
| `data/punch_windows.csv` | Hand-labeled punch windows from reviewing the annotated videos. Small, tracked — this is your actual manual work. |
| `data/training.csv` | Generated time-window rows labeled `punch`/`no_punch`, with velocity, body-relative arm, and extension-change features. Tracked. |
| `data/user_pose_frames.csv` | Frame-level pose data for one test/user video. Large, regenerated by `extract_user_pose_frames.py`, not tracked in git. |
| `data/predicted_punch_windows.csv` | Merged predicted punch events for a test video. Small, tracked as a reference sample. |
| `data/user_window_predictions.csv` | Every raw sliding-window prediction (not just merged punches) for a test video. Large, regenerated, not tracked. |
| `data/graph_metrics.csv` | Output of `graph_metrics.py` — punch volume/guard height/movement per window. Tracked as a reference sample. |
| `models/pose_landmarker_lite.task` | MediaPipe pose model, also checked into Android assets. |
| `models/random_forest.joblib` | The trained scikit-learn model. Not bundled directly — `export_random_forest_java.py` ports it to `PunchForestModel.java`, which is what ships in the app. |

Untracked-but-regeneratable files stay on your disk as normal — they're just excluded from git so clones/pulls don't have to move tens of megabytes of derived data. Rerun the relevant script to reproduce them.
