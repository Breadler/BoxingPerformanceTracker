# 4. Implementation

*(Renamed from "System Development"; see `00_outline.md`.)* Documents what
was actually built: the specific technology, algorithm, calculation, and
(where relevant) database used at each stage, and why, for both the Python
prototype and the Android port. Chapter 3 is the plan; this is the record of
what actually happened, including everywhere the plan changed on contact
with implementation. The Python-side detail here draws heavily on
`old versions/old article/03_meth.md`, which documents stages B–F more
precisely than anything written before it. Section headers below carry the
same pipeline-stage letters (A-K) as Figure 3.1 (§3.3), and each stage's
input and output are named in the text that follows, so the code-level
detail here stays traceable to that diagram. Rationale that Chapter 3
already covers (why a metric is computed the way it is, why a feature set
was chosen) is not repeated here; only cross-referenced. Only the
configuration that actually ships is described; optional flags and
alternate code paths that exist but are not used in the final pipeline are
left out.

_Status: NEW. No old-thesis chapter equivalent; Chapter 4 ("System
Development" in the old 5-chapter thesis) did not exist as its own
chapter there. Added to match the Introduction/Summary bookend pattern
already used in Chapters 3, 5, and 6._
## 4.1 Introduction
This chapter documents what was actually built to carry out the
methodology set out in Chapter 3: the specific technologies, algorithms,
calculations, and database used at each pipeline stage, and the decisions
made where the plan changed on contact with implementation. §4.2 lists
the tools and technologies used across both platforms. §4.3 documents the
Python prototype, the offline pipeline that produces and validates the
trained Random Forest classifier before any Android work begins. §4.4
documents the Android port, the on-device pipeline that reimplements the
same detection and metric-computation logic natively, including the
classifier's transpilation to Java. §4.5 summarizes the differences
between the two implementations, and §4.6 closes the chapter.

_Status: NEW. No old-thesis chapter equivalent; table format echoes old
§3.5.1's table shape but every row is a different technology (RF+m2cgen+
Room vs. old's SceneView+TFLite+Maya). Tools table expanded and named
Table 4.1; the separate Logical Modules table has been removed as
redundant with Figure 3.1 (§3.3)._
## 4.2 Development Environment & Tools
Table 4.1 lists the technology used at each stage, across both platforms.

**Table 4.1: Development Environment and Tools**

| Component | Technology | Notes |
|---|---|---|
| Python pipeline | Python, pandas, NumPy, scikit-learn | Batch/offline processing and model training |
| Pose estimation (both platforms) | MediaPipe Pose Landmarker (`pose_landmarker_lite.task`) | Lite variant chosen for speed; same model family used Python-side and Android-side |
| Model export | m2cgen | Transpiles the trained scikit-learn RandomForest to a plain Java class; see §4.4.2 for why this replaced a TensorFlow Lite model |
| Android app | Kotlin, Jetpack Compose | UI and on-device pipeline |
| Video capture | CameraX (`androidx.camera.video`) | Records session video directly to file (`CameraCapture.kt`, §4.4.5) |
| Screen navigation | Jetpack Navigation Compose | Routes between the five screens (§4.4.5) |
| Async / reactive data | Kotlin Coroutines, Flow | Room queries exposed as `Flow`; session processing runs off the main thread |
| On-device persistence | Room (SQLite) | See §4.4.4 |
| Model serialization | joblib | `random_forest.joblib`: model plus exact feature-column order, so training and inference (Python and Android) can never silently drift out of sync |

No server/database is used at inference time on the Android side beyond the
local Room database. The app has no network dependency.

The system is built as two cooperating phases: a developer-run, offline
Python pipeline (§4.3) that produces the trained model, and a per-session,
on-device Android pipeline (§4.4) that bundles that model and reimplements
the same detection and metric-computation logic natively, with no network
dependency. The full pipeline, spanning both phases and every module
listed in Table 4.1, is diagrammed as Figure 3.1 in the methodology
chapter.

_Status: NEW chapter (section header only; see subsections below). Pipeline
diagram removed from here since Figure 3.1 in `03_methodology.md` already
covers the same stages (A-K) for both platforms; §4.3.1-§4.3.9 below
document the Python side's stages in turn._
## 4.3 Python Implementation
This section documents stages A-E and the per-video inference stages F-J
from Figure 3.1 (§3.3), as implemented in Python.

_Status: CHANGED (adapted). Close paraphrase of `old versions/old article/03_meth.md` §B. Stage letter added to the header, input/output named in
prose instead of a separate callout. CSV column table removed (Chapter 5
already shows a real example); replaced with the actual per-frame
extraction loop and a landmark-set correction (27 landmarks by default,
not 33). Frame-stride and the unused "full" landmark-set option dropped,
since only every-frame, boxing-subset extraction is what ships._
### 4.3.1 Pose Extraction Pipeline (Stage B)
`pose_extractor.py` carries out Stage B's pose extraction. For each raw
training video it takes as input, Stage A's output (or, at inference time,
a single new video), it opens the file with OpenCV, steps through it frame
by frame, and runs MediaPipe Pose Landmarker
(`models/pose_landmarker_lite.task`) on each one, writing one output row
per frame: `pose_frames.csv` for training, `user_pose_frames.csv` for
inference. Listing 4.1 shows the core per-frame extraction loop.

**Listing 4.1: Extracting pose landmarks frame-by-frame in Python**

```python
capture = cv2.VideoCapture(str(video_path))
with PoseLandmarker.create_from_options(options) as pose_landmarker:
    while True:
        success, frame = capture.read()
        if not success:
            break
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
        results = pose_landmarker.detect_for_video(mp_image, timestamp_ms)
```

If `results.pose_landmarks` comes back non-empty, the output row is
written with `pose_detected=1` and each landmark's `x`, `y`, `z`, and
`visibility`. If MediaPipe finds no pose in that frame, the row is still
written, with `pose_detected=0` and every landmark value set to `NaN`
rather than the row being dropped, so later stages can see exactly which
input frames failed detection instead of silently losing them.

MediaPipe Pose Landmarker always detects the full 33-landmark skeleton,
but the pipeline keeps only 27 of them in its output: the head, shoulders,
elbows, wrists, torso, hips, knees, ankles, and feet, excluding the six
fine finger landmarks (pinky, index, and thumb tips on each hand) that
MediaPipe also reports.

The same script additionally renders an annotated review video, the input
a human reviewer uses for Stage C. `extract_user_pose_frames.py` mirrors
this logic for a single new video at inference time, feeding Stage F
directly instead of Stage C, through an independent code path from the
training extraction, so that training data and live predictions never
share a path that could introduce data leakage.

_Status: CHANGED (adapted). Close paraphrase of old article §C. Stage
letter added; input/output named in prose. Trimmed a sentence duplicating
Chapter 3's rationale for auditing._
### 4.3.2 Manual Labelling & Label-Auditing Tooling (Stage C)
A human reviewer watches the annotated review video, Stage B's output,
and for each punch observed records the video identifier and the punch's
start/end frame, building the output, `punch_windows.csv`: one row per
manually identified punch, expressed in frame numbers. Frame-based labels
are automatically converted to millisecond ranges downstream using each
frame's `timestamp_ms`, so the manual review step itself is unaffected by
a source video's frame rate.

`audit_punch_labels.py` then runs over that output before it becomes
Stage D's input: the automated implementation of the quality-control step
planned in §3.6.2. Table 4.2 lists what it checks.

**Table 4.2: `audit_punch_labels.py` Checks**

| Check | Catches |
|---|---|
| Video identifier | Labels referencing a video with no extracted pose data |
| Frame range validity | Invalid or reversed start/end frames |
| Duration plausibility | Implausibly short or long punch durations |
| Frame coverage | Labels that reference frames outside the range actually extracted for that video |

_Status: CHANGED (adapted). Close paraphrase of old article §D. Stage
letter added; input/output named in prose; a code example now shown for
all four feature families, not just velocity; the paragraph duplicating
Chapter 3's feature-choice rationale removed. Window/anchor/stride flags
condensed to the fixed values actually used; the unused raw-landmark-
feature option dropped entirely. The feature-family table had been left
uncaptioned; it is now named and numbered Table 4.3, and every table from
the old Table 4.3 onward shifted up by one to make room (4.3-4.7 became
4.4-4.8)._
### 4.3.3 Feature Engineering (Stage D)
`build_training_csv.py` builds `training.csv`, Stage D's output and the
input Stage E trains on, from two inputs: `pose_frames.csv` (Stage B's
output) and `punch_windows.csv` (Stage C's output). Windows are a fixed 250 ms
duration rather than a fixed frame count, necessary because UCF101 clips
do not share a common frame rate, so a fixed frame count would not
correspond to a fixed amount of real-world time across the dataset.
Positive (punch) windows are anchored to the end of the labelled interval,
keeping the punch's impact frame at a fixed position within the window
rather than an arbitrary offset. Negative (`no_punch`) windows are sampled
every 250 ms, for the same frame-rate-independence reason.

For each window, four feature families are computed, giving 70 output
columns in total. Table 4.3 lists each family, the landmarks it covers,
and how many columns it contributes.

**Table 4.3: Feature Families and Column Counts**

| Family | Landmarks Covered | Per-Landmark Stats | Columns |
|---|---|---|---|
| Velocity | Left/right wrist, left/right elbow (4) | Mean, std, max frame-to-frame speed | 12 |
| Body-relative position | Left/right wrist, left/right elbow (4) | Mean, std, change, per x/y/z axis, relative to the body center (mean of both shoulders and both hips) | 36 |
| Shoulder-relative wrist position | Left/right wrist-shoulder pair (2) | Mean, std, change, per x/y/z axis, relative to the same-side shoulder | 18 |
| Extension | Left/right shoulder-wrist pair (2) | Shoulder-to-wrist distance change; wrist forward (z-axis) extension change | 4 |

Each family reduces per-frame differences or distances between landmarks
to mean/std/change statistics over the window. Listing 4.2 computes
velocity as frame-to-frame 3D displacement over time.

**Listing 4.2: Computing wrist and elbow velocity features in Python**

```python
coordinate_deltas = window[[f"{landmark}_x", f"{landmark}_y", f"{landmark}_z"]].diff()
speeds = np.sqrt((coordinate_deltas ** 2).sum(axis=1)) / frame_deltas(window)
row[f"{landmark}_velocity_mean"] = speeds.mean()
row[f"{landmark}_velocity_std"] = speeds.std(ddof=0)
row[f"{landmark}_velocity_max"] = speeds.max()
```

Listing 4.3 computes body-relative position, each landmark's offset from
the body center (the mean of both shoulders and both hips), per axis.

**Listing 4.3: Computing body-relative landmark position features in Python**

```python
center = window[["left_shoulder_x", "right_shoulder_x", "left_hip_x", "right_hip_x"]].mean(axis=1)
relative = window[f"{landmark}_x"] - center
row[f"{landmark}_body_relative_x_mean"] = relative.mean()
row[f"{landmark}_body_relative_x_std"] = relative.std(ddof=0)
row[f"{landmark}_body_relative_x_change"] = relative.iloc[-1] - relative.iloc[0]
```

(repeated for the y and z axes). Listing 4.4 computes shoulder-relative
wrist position the same way, offset from the same-side shoulder instead
of the body center.

**Listing 4.4: Computing shoulder-relative wrist position features in Python**

```python
relative = window[f"{side}_wrist_x"] - window[f"{side}_shoulder_x"]
row[f"{side}_wrist_shoulder_relative_x_mean"] = relative.mean()
row[f"{side}_wrist_shoulder_relative_x_std"] = relative.std(ddof=0)
row[f"{side}_wrist_shoulder_relative_x_change"] = relative.iloc[-1] - relative.iloc[0]
```

(repeated for y/z and both sides). Listing 4.5 computes extension, the
shoulder-to-wrist distance and forward reach, each measured as a change
across the window rather than a mean.

**Listing 4.5: Computing shoulder-to-wrist extension features in Python**

```python
distance = np.sqrt(((wrist_coords - shoulder_coords) ** 2).sum(axis=1))
row[f"{side}_shoulder_to_wrist_distance_change"] = distance.iloc[-1] - distance.iloc[0]
depth = window[f"{side}_wrist_z"] - window[f"{side}_shoulder_z"]
row[f"{side}_wrist_forward_extension_change"] = depth.iloc[0] - depth.iloc[-1]
```

`WindowFeatures.kt` on the Android side (§4.4.3) computes the identical 70
features by name, in the exact order `RandomForestPunchClassifier.kt`
expects (§4.4.2), so training and on-device inference can never silently
diverge.

_Status: CHANGED (adapted). Close paraphrase of old article §E. Stage
letter added; input/output named in prose; Table 4.4 added for
hyperparameters and the feature-selection rule; the sentence duplicating
Chapter 3's "why Random Forest" rationale trimmed, keeping only the
algorithm mechanism, which Chapter 3 doesn't cover. CLI-flag names dropped
from the table; only the values actually used are listed._
### 4.3.4 Random Forest Training (Stage E)
A Random Forest is an ensemble of decision trees whose individual
predictions are averaged together. `train_random_forest.py` builds that
ensemble with scikit-learn's `RandomForestClassifier`, training on
`training.csv` (Stage D's output) as input and writing the trained model
bundle as output, the input Stage F consumes, shown in Listing 4.6.

**Listing 4.6: Training the Random Forest classifier in Python**

```python
feature_columns = [
    column for column in data.columns
    if column not in ignored_columns and pd.api.types.is_numeric_dtype(data[column])
]
model = RandomForestClassifier(
    n_estimators=n_estimators,
    random_state=random_state,
    class_weight="balanced",
)
model.fit(data[feature_columns].fillna(0.0), data["label"])
```

Table 4.4 lists every setting this call fixes, explicitly or by
scikit-learn default, and the rule used to select feature columns.

**Table 4.4: RandomForestClassifier Configuration**

| Setting | Value | Source |
|---|---|---|
| `n_estimators` | 300 | Explicit |
| `random_state` | 42 | Explicit |
| `class_weight` | `"balanced"` | Explicit; corrects for the natural punch/no_punch class imbalance |
| `criterion` | Gini impurity | scikit-learn default |
| `max_depth` | Unbounded | scikit-learn default |
| Features considered per split | Square root of the feature count (~8 of 70) | scikit-learn default |
| Feature columns | Every numeric column in `training.csv` not in the metadata or label column sets | `get_feature_columns()`, computed dynamically each run |

Feature columns are not hardcoded. `get_feature_columns()` takes every
numeric column in the input that is not one of the known metadata or label
columns, so the feature set stays in sync with whatever
`build_training_csv.py` (§4.3.3) actually produces without the two
scripts needing to separately agree on a fixed list. Every setting past
the three set explicitly is left at scikit-learn's default, a deliberate
simplicity choice given how small the dataset is (§5.2.4), since there
is not enough data to reliably tune further without overfitting to a
validation split that is itself too small to trust.

Each of the 300 trees is grown independently on its own bootstrap sample
(a random sample of the training rows, drawn with replacement, so some
rows repeat and others are left out entirely) and, at every split, chooses
the best threshold from a random subset of features rather than
considering all 70. Both sources of randomness, the row sample and the
feature subset, decorrelate the trees from one another, so averaging their
votes cancels out individual trees' overfitting rather than repeating the
same mistake 300 times over. A prediction is each tree's vote, averaged
into a class probability across the whole forest. §4.4.2 shows exactly how
that averaging is implemented once the model is ported to Android.

The output artifact (`models/random_forest.joblib`, via `joblib` with a
`pickle` fallback if it is not installed) bundles four things together: the
trained model itself, the exact list and order of the feature columns used
(§4.3.3), the label column name, and the metadata column names, so that
later inference (Python or Android) can never silently use a different
feature structure than the one the model was trained on. No database is
used here. `training.csv` and the `.joblib` artifact are sufficient for a
batch, offline training job, and keeping everything as flat, inspectable
files made debugging the pipeline considerably easier during development
than a database would have.

_Status: CHANGED (adapted). Close paraphrase of old article §F. Stage
letter added; input/output named in prose; trimmed a clause duplicating
Chapter 3's "validate in Python first" rationale down to a bare
cross-reference._
### 4.3.5 Python-Side Inference (Stage F)
`predict_punches.py` runs Stage F's inference. Given a new video's pose
CSV, Stage B's output for that video, and the trained model, Stage E's
output, as its two inputs, it produces `predicted_punch_windows.csv` as
output, merged punch spans that become the input to Stages G, H, and I.
`extract_user_pose_frames.py` produces the input pose CSV in the same
format as the training data
(§4.3.1); `predict_punches.py` then slides the same fixed-duration window
used during training across it, computing the identical feature set
(§4.3.3) at every step and classifying each window with the trained model,
shown in Listing 4.7.

**Listing 4.7: Sliding-window inference on new footage in Python**

```python
for start_ms in range(min_ms, max_ms - window_ms + 1, stride_ms):
    end_ms = start_ms + window_ms
    row = aggregate_window(frames, feature_columns, start_ms=start_ms, end_ms=end_ms)
    rows.append(row)

x = prediction_windows[feature_columns].fillna(0.0)
prediction_windows["prediction"] = model.predict(x)
```

Per-window predictions are written out, and windows predicted "punch" that
overlap or sit adjacent in time are then merged into single punch spans.
Each new punch window either extends the current span, if its start falls
inside or right after the current span's end, or starts a new one, if
there is a gap. The merged spans are the output, reporting both millisecond
ranges and frame numbers, from which punch count and timing are derived.
This is the exact logic later ported to Android (§4.4.2). Validating it
here first is why §3.3.6 treats this as a methodological step.

_Status: NEW. The article covered the labelling pipeline only, not these
three metrics. Split into three subsections, one per metric. Stage letters
added; input/output named in prose; sentences duplicating Chapter 3's
per-metric rationale (§3.3.7-§3.3.9) trimmed to bare cross-references._
### 4.3.6 Punch Volume Computation (Stage G)
`punch_volume.py` computes Stage G's output, `punch_volume.csv`, one row
per window on the shared grid feeding into Stage J, from
`predicted_punch_windows.csv`, Stage F's output, as input. It first groups
the input punches into combos, matching how a combination is defined in
§2.2.3, shown in Listing 4.8.

**Listing 4.8: Grouping predicted punches into combos in Python**

```python
if start_ms - combo_end_ms <= combo_gap_ms:
    combo_end_ms = max(combo_end_ms, end_ms)
    punch_end_times_ms.append(end_ms)
else:
    combo_rows.append(current_combo)
    combo_start_ms, combo_end_ms = start_ms, end_ms
    punch_end_times_ms = [end_ms]
```

A punch extends the current combo if it starts within 500 ms of the
previous punch's end. Otherwise it starts a new combo. For each window on
the shared 250 ms/40 ms grid, Listing 4.9 looks up the output value from
whichever combo covers that window's center timestamp `t`.

**Listing 4.9: Looking up punch volume for a window in Python**

```python
covering = combos[(combos["combo_start_ms"] <= t) & (t <= combos["combo_end_ms"])]
punch_end_times_ms = covering.iloc[0]["punch_end_times_ms"]
punch_volume = sum(1 for end_ms in punch_end_times_ms if end_ms <= t)
```

This counts only the punches within the covering combo whose own end time
has passed by `t`, so the value steps up by one at each individual punch's
end time rather than jumping straight to the combo's full count partway
through. A window outside every combo reports 0. Left unsmoothed, per
§3.3.7.

_Status: NEW. No article or old-thesis equivalent. Stage letter added;
input/output named in prose; trimmed the clause duplicating Chapter 3's
rationale for using whichever wrist is higher, keeping the MediaPipe
axis-direction fact, which Chapter 3 doesn't state._
### 4.3.7 Guard Height Computation (Stage H)
`guard_height.py` produces Stage H's output, `guard_height.csv`, part of
Stage J's input, reading `pose_frames.csv`/`user_pose_frames.csv`, Stage
B's output, directly as input rather than Stage F's predictions. For
every frame in a window, it computes the vertical gap between the head and
whichever wrist sits higher, then averages that gap over the window,
shown in Listing 4.10.

**Listing 4.10: Computing guard height for a window in Python**

```python
raw_wrist_y = window[["left_wrist_y", "right_wrist_y"]].min(axis=1)
guard_height = (window["nose_y"] - raw_wrist_y).mean()
```

MediaPipe's y-axis is inverted (0 = top of frame), so `min(...)` of the
two wrists' y-coordinates selects whichever sits higher on screen (§3.3.8).
A larger result means the guarding wrist sits further above the head. A
value near zero or negative means the guard has dropped to chin/chest
height or below.

_Status: NEW. No article or old-thesis equivalent. Stage letter added;
input/output named in prose; trimmed the clause duplicating Chapter 3's
rationale for excluding vertical displacement._
### 4.3.8 Movement Computation (Stage I)
`movement.py` produces Stage I's output, `movement.csv`, part of Stage J's
input, reading the same `pose_frames.csv`/`user_pose_frames.csv`, Stage
B's output, directly as input. It computes the hip midpoint's
frame-to-frame speed on the horizontal plane only, then averages that
speed over the window, shown in Listing 4.11.

**Listing 4.11: Computing hip movement speed for a window in Python**

```python
hip_x = window[["left_hip_x", "right_hip_x"]].mean(axis=1)
hip_z = window[["left_hip_z", "right_hip_z"]].mean(axis=1)
dx, dz = hip_x.diff(), hip_z.diff()
dt = window["timestamp_ms"].diff() / 1000.0
speed = np.sqrt(dx ** 2 + dz ** 2) / dt
movement = speed.mean()
```

Vertical displacement is excluded, per §3.3.9.

_Status: NEW. No article or old-thesis equivalent; renumbered from §4.3.7
to §4.3.9 now that §4.3.6 covers punch volume only. Stage letter added;
input/output named in prose._
### 4.3.9 Graph Generation (Stage J)
`graph_metrics.py` merges the three per-metric outputs from Stages G, H,
and I, `punch_volume.csv`, `guard_height.csv`, and `movement.csv`, as
input, into `graph_metrics.csv` as output, the input to Stage K's
playback. It inner-joins the three inputs on `(video_id, center_ms)` into
one row per window, then applies two display-time steps to guard height
and movement only, shown in Listing 4.12.

**Listing 4.12: Smoothing and downsampling graph metrics in Python**

```python
metrics[column] = metrics[column].rolling(
    window=window_samples, center=True, min_periods=1,
).mean()

bucket_index = (metrics["center_ms"] // bucket_ms).astype(int)
downsampled = metrics.groupby(["video_id", bucket_index]).agg(
    guard_height=("guard_height", "mean"),
    movement=("movement", "mean"),
)
```

A centered rolling mean (1500 ms window) smooths frame-to-frame jitter
without shifting the signal in time, then grouping by 500 ms buckets and
averaging within each collapses the output to a reasonable number of
plotted points for a full session, instead of one point per 40 ms stride
step. Punch volume is left raw and full-resolution through both steps, per
§3.3.7/§4.3.6.

_Status: NEW (section header only; see subsections below). Pipeline
diagram removed from here for the same reason as §4.3; §4.4.1-§4.4.5 below
document the Android side's stages in turn. Added a note on why Android
work started when it did._
## 4.4 Android Implementation
Android implementation began only once the Python prototype (§4.3)
produced satisfactory results in the dry run described in §3.5, with the
explicit goal of recreating that already-validated pipeline on-device
rather than designing a new one. This section documents the same Figure
3.1 (§3.3) stages, reimplemented natively on-device rather than in Python.

_Status: NEW. No old-thesis or article equivalent; the Android app didn't
exist when either was written. Stage letter added; input/output named in
prose._
### 4.4.1 On-Device Pose Extraction (Stage B)
`PoseFrameExtractor.kt` reimplements Stage B on-device. It runs the same
MediaPipe Pose Landmarker model (bundled `.task` file, no network fetch)
used in Python (§4.3.1) against the captured or imported video, Stage A's
output, as input, producing per-frame landmark observations as output.
`PoseVideoEncoder.kt` renders the
same kind of skeleton-overlay video the Python prototype produces for
manual review, except here it is the final output shown to the user
(§4.4.5), the input to Stage K, not a labelling aid.

_Status: CHANGED. Exact threshold/window/stride constants added; porting
files/roles previously named inline in prose are now Table 4.5;
substantially expanded with how the transpiled forest actually runs
(per-tree conditionals, vote averaging), since this is the classifier the
thesis title names directly. Stage letter added; input/output named in
prose. TFLite paragraph corrected: it previously stated the ~50% figure
came from a verified video-grouped split; per the 2026-08-10 correction
in `notes/decisions.md`, re-checking the removed script found it actually
used a per-row shuffle, so this no longer claims the split was grouped
without qualification, matching the caveat already in §5.2.6. Added a
citation (Malcher et al., 2024) for the m2cgen transpilation decision,
resolving the "still needed" gap in `notes/references.md`._
### 4.4.2 On-Device Classification: RandomForest (m2cgen) vs. TensorFlow Lite (Stage F)
This was the single most consequential implementation decision on the
Android side, decided by a direct accuracy comparison between the two
candidate approaches rather than by convention.

An on-device TensorFlow Lite network was first trained from scratch on the
same `training.csv` (§4.3.3), independent of the already-validated Python
RandomForest, on the reasoning that a network trained natively for the
target format would be the more conventional choice for on-device ML. Its
held-out accuracy came in at approximately 50 percent, essentially
chance, on 350 labelled rows across 111 videos. The split was recorded at
the time as video-grouped (grouping by `video_id`, not a per-row shuffle,
per §3.6.3), but re-checking the removed training script later found it
actually used a plain per-row shuffle, so this specific number should not
be cited as coming from a leak-proof, grouped methodology without
re-verifying it (§5.2.6). A per-row shuffle would ordinarily be expected
to inflate accuracy through near-duplicate window leakage, not suppress
it, so a chance-level result under the more favorable split is if
anything further evidence that the network did not learn a usable
signal.

Rather than ship a second, independently-trained, unvalidated model, the
already-validated Python RandomForest (§4.3.4) is instead transpiled
directly to a plain Java class via
[m2cgen](https://github.com/BayesWitnesses/m2cgen), a language-based
deployment strategy for tree ensembles consistent with recent embedded
systems work on optimizing Random Forest deployment through code
generation rather than a general-purpose interpreter (Malcher et al.,
2024). Table 4.5 lists the files involved and what each does.

**Table 4.5: RandomForest-via-m2cgen Porting**

| File | Role |
|---|---|
| `python/export_random_forest_java.py` | Runs m2cgen against the trained `.joblib` model to generate the Java class below |
| `PunchForestModel.java` | Generated Java class exposing `score(double[]) → predict_proba`; no column-name metadata of its own |
| `RandomForestPunchClassifier.kt` | Wraps `PunchForestModel`, feeding it window feature vectors in the exact 70-column order baked in at export time (§4.3.3), and calls a window a punch when the predicted punch-class probability is at least `0.5` |

`RandomForestPunchClassifier.kt` takes the window feature vectors
aggregated on-device (§4.4.3), in the same 70-column order as Stage E's
training data, as input, and produces a punch/no_punch prediction per
window as output, merged into punch spans the same way Stage F's Python
output is (§4.3.5).

m2cgen works by walking the trained model's internal tree structure once,
at export time, and generating equivalent nested-conditional code
directly, rather than shipping a general-purpose decision-tree
interpreter that Android would need to run at inference time. Each of the
300 trees becomes its own block of nested `if`/`else` statements over
`input[]` array indices, terminating in a leaf vote, as in Listing 4.13.

**Listing 4.13: A transpiled decision tree's nested conditionals in Java**

```java
if (input[40] <= 0.0776971168816089) {
    if (input[34] <= 0.0110635897144675) {
        var0 = new double[] {1.0, 0.0};
    } else {
        var0 = new double[] {0.0, 1.0};
    }
} else {
    if (input[54] <= -0.3577852845191956) {
        var0 = new double[] {0.0, 1.0};
    } else {
        var0 = new double[] {1.0, 0.0};
    }
}
```

Every tree's `{no_punch_vote, punch_vote}` leaf pair is then summed across
all 300 trees and divided by 300, shown in Listing 4.14.

**Listing 4.14: Averaging votes across the Random Forest ensemble in Java**

```java
return mulVectorNumber(
    addVectors(addVectors(addVectors(tree0, tree1), tree2), ...),
    0.0033333333333333335,
);
```

`0.0033333333333333335` is `1/300` exactly, so the two-element result is
the same class-probability average described algorithmically in §4.3.4,
just computed by generated code at inference time instead of by
scikit-learn. `RandomForestPunchClassifier.kt` reads index 1 of that array
(the punch-class probability) and compares it against the `0.5` threshold.

The same `250` ms window and `40` ms stride used in Python (§4.3.3) are
hardcoded as the classifier's operating parameters, so window boundaries
line up identically between the two implementations. This removes an
entire class of train/inference divergence between the app and the
Python-side testing, at the cost of requiring the caller to supply
features in exactly the right order, since the generated class has no
column-name metadata of its own to validate against.

_Status: NEW. Stage letters added; input/output named in prose; this
section spans several Figure 3.1 stages since Android implements them in
fewer, combined files than the Python side does._
### 4.4.3 Window Feature Aggregation & On-Device Metric Computation (Stages D, G-I)
`WindowFeatures.kt` aggregates the on-device landmark observations, Stage
B's output (§4.4.1), as input into window feature vectors as output for
Stage F's classifier (§4.4.2), using the same 250 ms window / 40 ms stride
features used in Python (§4.3.3). `GraphMetrics.kt` consumes the same
landmark observations to produce guard height (Stage H) and movement
(Stage I) as output, reimplementing §4.3.7/§4.3.8
identically, and reimplements punch volume (Stage G) as sparse
*keyframes* tied to individual punch events, ported directly from
`punch_volume.compute_punch_volume_keyframes()`, the representation
`punch_volume.py` itself uses for display, built on the same
combo-detection logic (§4.3.6) but reporting a value at each punch's own
end time instead of sampling onto the shared grid. On-device this is also
the cheaper option: no per-window combo lookup across the entire video and
no extra Room storage, computed on demand from data already held in
memory.

_Status: NEW. Exact table schema and DAO operations added, plus the
JSON-blob design rationale. No Figure 3.1 stage letter: persistence is a
cross-cutting concern, not one of the pipeline stages. Input/output named
in prose._
### 4.4.4 Data Persistence (Room)
Processed sessions, the punch windows, per-metric performance points, and
session metadata produced across §4.4.1-§4.4.3, are the input. A
queryable session history, read back by §4.4.5, is the output. Sessions
are persisted locally in a single Room (SQLite) table, `sessions`
(`SessionEntity.kt`), accessed through `SessionDao.kt` and
`SessionRepository.kt`. The database itself (`SessionDatabase.kt`) is at
schema version 2 with no real migrations written yet, so a future version
bump destructively recreates the table rather than crashing existing
installs, an acceptable tradeoff at this stage of development. Table 4.6
lists what each row stores.

**Table 4.6: `sessions` Table Schema**

| Column | Type | Purpose |
|---|---|---|
| `id` | String (primary key) | Session identifier |
| `title`, `dateLabel`, `durationLabel` | String | Display-ready labels for the session list |
| `durationMs` | Long | Session duration |
| `sourceVideoName`, `sourceVideoUri` | String | The original captured/imported video |
| `annotatedVideoUri` | String | The rendered skeleton-overlay video (§4.4.1) |
| `thumbnailUri` | String | Session-list thumbnail |
| `punchCount` | Int | Total punches detected |
| `processedAtMs` | Long | Used to order the session history newest-first |
| `punchWindowsJson`, `predictionWindowsJson`, `performancePointsJson` | String | The punch windows, raw predictions, and per-metric performance points (§4.4.3), each serialized to JSON |

Punch windows, predictions, and performance points are stored as JSON
blobs in the same row rather than as separate normalized tables, since the
app only ever needs to load or write a whole session at once and never
queries into their internals directly. A single denormalized table is
simpler than a multi-table schema for that access pattern. `SessionDao.kt`
exposes sessions as a Kotlin `Flow`, ordered newest-first by
`processedAtMs`, so the session history screen (§4.4.5) updates reactively
as sessions are added, plus a single-session lookup, an upsert
(replace-on-conflict), and a delete.

Room was chosen over flat files (the Python side's approach, §4.3.4)
because the app needs structured querying across many past sessions for
the session history view (§4.4.5), which a directory of CSVs does not
support well. It also gives type-safe access from Kotlin without
hand-written SQL for the simple read/write patterns the app needs.

_Status: NEW. Named all five screens and the supporting UI modules, rather
than only the playback/history ones; the twelve files involved are now
Table 4.7 rather than a single dense paragraph. Stage letter added;
input/output named in prose._
### 4.4.5 UI & Visualization (Stage K)
The skeleton-overlay video (§4.4.1) and performance points (§4.4.3), read
back from Room (§4.4.4), are the input. The playback and session-history
experience shown to the user is the output. It is built in Jetpack
Compose, with Jetpack Navigation Compose routing between five screens, one
more than the four planned in §3.4 (`NewSessionScreen` splits the planned
"Home / Capture" screen into two composables), all driven by a shared
`StrykoViewModel.kt`. Table 4.7 lists every screen and component involved
and what each is responsible for.

**Table 4.7: UI Screens and Components**

| File | Role |
|---|---|
| `HomeScreen.kt` | Landing screen |
| `NewSessionScreen.kt` | Start a recording or import a video (§3.4's planned "Home / Capture" screen) |
| `CameraCapture.kt` | Records session video via CameraX, used by `NewSessionScreen` |
| `ProcessingScreen.kt` | Shown while a session runs through §4.4.1-§4.4.4 |
| `ProcessingStatusBar.kt` | Live progress indicator used by `ProcessingScreen` |
| `SessionPlaybackScreen.kt` | Plays the skeleton-overlay video (§4.4.1) synced to the performance graphs |
| `SessionVideoPlayer.kt` | Video playback component used by `SessionPlaybackScreen` |
| `PerformanceGraph.kt` | Plots the three metrics (§4.4.3) against playback position |
| `PlaybackControls.kt` | Scrubbing controls used by `SessionPlaybackScreen` |
| `PreviousSessionsScreen.kt` | Lists past sessions from Room (§4.4.4) |
| `SessionCard.kt` | Per-session list item used by `PreviousSessionsScreen` |
| `StrykoViewModel.kt` | Shared view-model driving all five screens |

This replaces the originally planned rigged 3D SceneView skeleton (old
thesis §3.3.5/§3.4.1), dropped due to time constraint relative to getting
the detection/metrics pipeline itself working end to end (see
`notes/decisions.md`), noted as a scope limitation in §6.5.

_Status: NEW. Added a lead-in sentence and a table caption (Table 4.8)._
## 4.5 Differences Between Prototype and Android App
Table 4.8 summarizes the differences between the two implementations
detailed in §4.3 and §4.4.

**Table 4.8: Prototype vs. Android App**

| Aspect | Python prototype | Android app |
|---|---|---|
| Classifier | scikit-learn RandomForest (`.joblib`) | Same model, transpiled to Java via m2cgen (§4.4.2) |
| Pose extraction | MediaPipe Python Tasks | MediaPipe Android Tasks, bundled `.task` file |
| Storage | Flat CSV files + joblib | Room (SQLite) |
| Visualization | Matplotlib plots for validation (`plot_graph_metrics.py`) | Skeleton-overlay video synced to live graphs (Compose) |
| Visualization plan vs. actual | N/A | Originally a rigged 3D SceneView skeleton; dropped for time (§4.4.5) |

Despite running on two different platforms, the classifier itself does
not change between the prototype and the app. Only its runtime does: the
same trained Random Forest model, ported via m2cgen (§4.4.2) rather than
retrained from scratch. Pose extraction, storage, and visualization
diverge more, each substituted for a mobile-appropriate equivalent: a
bundled on-device MediaPipe model in place of the desktop Python Tasks
build, a local Room database in place of flat CSV and joblib files, and a
skeleton-overlay video synced to live graphs in place of static
Matplotlib plots. The one deliberate scope change carried over from the
original plan is also a visualization change: a rigged 3D skeleton,
dropped in favor of the simpler 2D overlay actually shipped (§4.4.5).

_Status: CHANGED. Added a short writeup after Table 4.8 synthesizing the
comparison (same classifier, different runtime; the rest substituted for
a mobile-appropriate equivalent), rather than leaving the table
unexplained. Also rewritten to summarize this chapter's own sections
directly, rather than a general recap of the stage letters, and to open
each paragraph with real prose rather than a bare section number._
## 4.6 Summary
This chapter has documented what was actually built to carry out the plan
set out in Chapter 3. Python, pandas, NumPy, and scikit-learn make up the
offline pipeline, while Kotlin, Jetpack Compose, and Room make up the
on-device Android app, sharing the same MediaPipe Pose Landmarker model
across both platforms (§4.2).

The Python implementation carries out Figure 3.1's core pipeline end to
end (§4.3). `pose_extractor.py` extracts and timestamps landmarks from
both the UCF101 training set and self-recorded footage (§4.3.1), a human
reviewer labels punch windows that `audit_punch_labels.py` then checks
for quality (§4.3.2), and `build_training_csv.py` turns those labels into
a 70-feature training set (§4.3.3). `train_random_forest.py` trains the
Random Forest classifier that the thesis title names directly on those
features (§4.3.4), and `predict_punches.py` applies it to new footage
(§4.3.5) before punch volume, guard height, and movement are each
computed and merged into graph-ready output (§4.3.6-§4.3.9). This entire
prototype is validated end to end before any Android work begins.

That same pipeline is then reimplemented natively on Android (§4.4). The
most consequential decision here was porting the already-validated
Random Forest via m2cgen rather than shipping a second,
independently-trained, unvalidated TensorFlow Lite network trained from
scratch, a choice backed by a direct accuracy comparison rather than
convention (§4.4.2). Pose extraction, feature aggregation, and metric
computation are reimplemented natively in Kotlin (§4.4.1, §4.4.3),
sessions are persisted locally in a Room database with no network
dependency (§4.4.4), and the results are presented through five Jetpack
Compose screens built around a skeleton-overlay video rather than the
originally planned rigged 3D skeleton (§4.4.5).

A direct comparison between the two implementations closes the chapter
(§4.5). Chapter 5 reports the actual outputs and evaluation results from
both.
