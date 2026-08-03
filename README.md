# BoxingPerformanceTracker

BoxingPerformanceTracker is:

- an **Android app written in Kotlin (Jetpack Compose)** that imports a boxing training video and runs the **entire inference pipeline fully on-device** — MediaPipe pose extraction, an annotated skeleton video, and a TFLite punch classifier — with no server and no network connection.
- a **Python offline pipeline** used to build the training dataset, train the models bundled into the Android app, and prototype new analysis before it gets ported to Kotlin.

## Repository layout

- `android/` – the Kotlin/Compose app. All inference (`data/processing/`) runs on-device; sessions persist locally in Room.
- `python/` – dataset building, model training, and pipeline prototyping. Nothing here runs at app runtime; it's the offline workshop that produces the assets in `android/app/src/main/assets/` and lets you preview new metrics before porting them.

## Python setup

The pipeline needs two separate environments:

**Main environment** (everything except TFLite export) — any recent Python works; this repo's `.venv` currently uses 3.14:

```bash
cd python
pip install -r requirements.txt
```

**TFLite export environment** (`train_tflite_punch_model.py` only) — needs **Python ≤3.12**. TensorFlow has no wheel for very new Python versions, and only `tensorflow==2.17.0` is confirmed to produce a model your Android `tensorflow-lite` runtime version can actually load (a newer TF release will happily export a model your on-device interpreter then refuses to open with an op-version error). If you don't have Python 3.12 installed:

```bash
py install 3.12
py -3.12 -m venv .venv-tflite-export
.venv-tflite-export\Scripts\pip install tensorflow==2.17.0 pandas numpy
```

Use `.venv-tflite-export` only for the export step below, then discard it — it's gitignored and not needed at runtime.

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

### 4. Train the models

Two training paths produce two different artifacts:

```bash
cd python
# Reference/experimentation model (not used by the Android app)
python train_random_forest.py --input data/training.csv --output models/random_forest.joblib

# The model the Android app actually ships with
python train_tflite_punch_model.py --training-csv data/training.csv
```

`train_tflite_punch_model.py` must run in the **Python ≤3.12 TFLite export environment** described above. It trains a small TensorFlow model from the same engineered motion features and writes the `.tflite` model plus feature metadata **directly into the Android assets folder**:

- `android/app/src/main/assets/punch_model.tflite`
- `android/app/src/main/assets/punch_model_metadata.json`

`android/app/src/main/assets/pose_landmarker_lite.task` is checked in separately and doesn't need regenerating unless you want a different MediaPipe pose model.

The pipeline is time-aware throughout: `pose_frames.csv` stores `timestamp_ms`, and frame-based punch labels are converted to milliseconds before training windows are built. Keep the same `--window-ms` everywhere (training, prediction, graph metrics), because windows must cover the same duration even when video frame rates differ.

### 5. From here, the Android app runs everything itself

Once the two asset files above exist, the Android app needs nothing else from Python at runtime. Importing a video in-app:

1. Copies the video into local app storage.
2. Runs MediaPipe pose extraction on-device and writes a local annotated skeleton video (no server, no network).
3. Runs the bundled TFLite classifier over sliding windows to produce punch predictions and merged punch windows.
4. Persists everything to a local Room database so it survives app restarts and shows up under Previous Sessions.

**Current gap:** the richer fatigue graph metrics below (punch volume as combos, guard height, movement) are Python-only right now. The Android graph currently shows a binary (not combo-aware) punch volume and flat placeholder lines for guard/movement, pending a Kotlin port of `graph_metrics.py`.

### 6. Test prediction and fatigue metrics against a new (unlabeled) video

This is the Python-side preview of what the app does on-device, plus the metrics not yet ported to it.

```bash
cd python
python extract_user_pose_frames.py --video "C:\path\to\user-round.mp4" --output data/user_pose_frames.csv

python predict_punches.py --pose-frames data/user_pose_frames.csv --model models/random_forest.joblib --output data/predicted_punch_windows.csv --windows-output data/user_window_predictions.csv --window-ms 250 --stride-ms 40
```

If the model is too conservative, lower the punch probability threshold (e.g. `--punch-threshold 0.35`).

Then compute and preview the three fatigue metrics — punch volume (punches grouped into combos), guard height (how far the higher-guarding wrist sits above the nose), and movement (hip x/z speed):

```bash
python graph_metrics.py --pose-frames data/user_pose_frames.csv --punch-windows data/predicted_punch_windows.csv --output data/graph_metrics.csv --combo-gap-ms 500

python plot_graph_metrics.py --pose-frames data/user_pose_frames.csv --punch-windows data/predicted_punch_windows.csv --output data/graph_metrics_plot.png
```

`graph_metrics.py`'s CSV output is the exact, unsmoothed data. `plot_graph_metrics.py` additionally smooths, downsamples, and curve-fits it purely for a readable chart — see the module docstrings in `graph_metrics.py` for the smoothing/downsampling knobs (`--*-smoothing-ms`, `--downsample-bucket-ms`) if a plot looks too noisy or too flat.

### Optional: legacy FastAPI server

`api_server.py` predates the on-device Android app (it originally served `session_processing.py`'s pipeline over HTTP for a thin Android client). The app no longer talks to it — it's kept only as a convenience for running the full pipeline from a browser/curl without the Android app. `session_processing.py` itself is still current: it's the Python analogue of `OnDeviceSessionProcessor.kt`, useful for testing pipeline changes before porting them to Kotlin.

## Data files reference

| File | What it is |
|------|------------|
| `data/pose_frames.csv` | Frame-level pose data for the training video set, no labels. Large, regenerated by `pose_extractor.py`, not tracked in git. |
| `data/punch_windows.csv` | Hand-labeled punch windows from reviewing the annotated videos. Small, tracked — this is your actual manual work. |
| `data/training.csv` | Generated time-window rows labeled `punch`/`no_punch`, with velocity, body-relative arm, and extension-change features. Tracked. |
| `data/user_pose_frames.csv` | Frame-level pose data for one test/user video. Large, regenerated by `extract_user_pose_frames.py`, not tracked in git. |
| `data/predicted_punch_windows.csv` | Merged predicted punch events for a test video. Small, tracked as a reference sample. |
| `data/user_window_predictions.csv` | Every raw sliding-window prediction (not just merged punches) for a test video. Large, regenerated, not tracked. |
| `data/graph_metrics.csv` | Output of `graph_metrics.py` — punch volume/guard height/movement per window. Not tracked; regenerate as needed. |
| `models/pose_landmarker_lite.task` | MediaPipe pose model, also checked into Android assets. |
| `models/random_forest.joblib` | Reference scikit-learn model; not what ships in the app. |

Untracked-but-regeneratable files stay on your disk as normal — they're just excluded from git so clones/pulls don't have to move tens of megabytes of derived data. Rerun the relevant script to reproduce them.
