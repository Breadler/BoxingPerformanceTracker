# 3. Methodology

This chapter is **the plan** — what was going to be built, and why. It
should read the same whether or not the system had actually been built yet.
What was actually built — the real technology choices, algorithms, exact
calculations, and the decisions made along the way (including where the
plan changed) — belongs in `04_implementation.md`. Keep that boundary clean.

Reused where unaffected by implementation-specific changes from
`old versions/old thesis/3_old_method.md` (§3.1, §3.2, §3.4.3 UX flow);
everything describing the system itself is rewritten to match the plan that
was actually followed (Python-first, on-device Android port, no 3D
visualization, no Olympic Boxing Punch dataset — see `00_outline.md` for why).

## 3.1 Introduction
This chapter describes the methodology used to design, develop, and
evaluate a boxing performance tracker: first validated end-to-end as a
Python prototype, then ported to run entirely on-device as an Android app.
The project prioritizes a practical balance — models and processing must be
accurate enough to be useful while remaining lightweight enough to run on a
phone with no server dependency.
The methodology is organized as a sequence of interlinked stages: data
collection, landmark extraction, manual labelling, feature engineering,
model development, validation on new footage, per-metric computation, graph
generation, and finally system design and on-device porting. Each stage
includes iteration loops for refining features, thresholds, and model
parameters based on empirical results.

## 3.2 Research Design
This research follows a quantitative experimental design combined with
software engineering practice. The experimental component develops and
evaluates a supervised classifier (punch / no_punch) on engineered features
derived from MediaPipe pose landmarks. The engineering component implements
first a Python prototype, then an Android application that reimplements the
same pipeline on-device, with skeleton-overlay video and synchronized graphs
as output.
The first key decision concerns the pose estimator: MediaPipe Pose
Landmarker was selected for its CNN-based architecture optimized for mobile
inference, extracting 33 landmarks/frame with x/y/z coordinates.
The second decision concerns modeling strategy: an interpretable,
lightweight Random Forest classifier trained on engineered features (not
raw coordinates), chosen for robustness on a limited, heterogeneous dataset.
The third decision concerns validation order: the full pipeline — including
inference on new footage — is validated in Python *before* any on-device
work begins, so that any accuracy problems are caught before they're
compounded by a second implementation (see §3.3.6).

## 3.3 Proposed Methodology

### 3.3.1 Data Collection
Two video sources are used: public boxing footage (the UCF101 action
recognition dataset's boxing subset) for volume and variety, and
self-recorded smartphone video to match the app's own real-world recording
conditions. Because UCF101 clips weren't collected for pose-estimation
research, they vary uncontrolled in camera angle, lighting, background,
recording quality, and frame rate — this variation is treated as a
characteristic of the dataset, not a confound to eliminate. All videos are
processed through the same extraction pipeline to keep landmarks consistent
across sources.

### 3.3.2 MediaPipe Landmark Extraction
Each video is processed with MediaPipe Pose Landmarker, which detects 33
landmarks per frame (x, y, z, plus a per-landmark visibility/confidence
score). Output is written to a structured, per-frame CSV
(`video_id, frame_index, timestamp_ms`, coordinates per landmark,
`pose_detected`) for subsequent labelling and feature engineering. An
annotated review video — the same skeleton drawn over the source footage,
with the frame number burned in — is generated alongside it, specifically
so a human reviewer can identify punches in the next stage.

### 3.3.3 Manual Punch Labelling
A human reviewer with boxing knowledge watches the annotated review video
and records, for each punch observed, the video identifier and the punch's
start and end frame. This produces one label per punch — a punch window —
rather than a frame-level label, since a punch is inherently an interval,
not an instant. Frame-based labels are later converted to millisecond
ranges using each frame's timestamp, so the manual review workflow itself
doesn't need to account for source videos having different frame rates.
_(Checking these labels for correctness — an audited quality-control step —
is deliberately not part of this stage; see §3.6.2 for why it's planned as
an evaluation activity instead.)_

### 3.3.4 Feature Engineering for Boxing Metrics
Each labelled punch window, plus a matching set of sampled `no_punch`
windows, is converted into one feature row rather than being used as raw
coordinates. Motion-based and body-relative features are computed: wrist
and elbow velocity, and wrist/arm position relative to the shoulder and
body center. These are chosen because a punch is not simply "movement in
general" but a specific pattern of arm extension relative to the body,
which raw coordinates alone don't capture well (see §2.2.2 for why wrist,
elbow, and shoulder specifically). The result is a labelled training
dataset: one row per window, `label ∈ {punch, no_punch}`.

### 3.3.5 Random Forest Model Development
A Random Forest classifier is trained on the engineered features to
distinguish punch from no_punch windows. Random Forest is chosen — over a
neural network — because it performs well on small-to-moderate,
heterogeneous datasets, handles noisy pose-derived input effectively, is
resilient to overfitting, and remains interpretable. Class balancing is
planned to account for the natural imbalance between punch and no_punch
windows in real footage. The trained model, along with the exact list and
order of feature columns used, is saved so that later inference (§3.3.6) and
the eventual Android port (§4.3.2) use an identical feature structure.

### 3.3.6 Python-Side Inference Validation
Before any on-device work begins, the trained model is validated against
new footage entirely in Python: a fixed-duration sliding window is applied
across a new video's extracted landmarks, each window is classified, and
consecutive "punch" predictions are merged into punch spans. This is a
deliberate methodological choice, not an afterthought — the same
prediction logic validated here is what later gets ported to Android
(§4.3.2), so any accuracy or edge-case problems are caught once, in the
faster-to-iterate Python environment, rather than being discovered only
after porting.

### 3.3.7 Punch Volume Metric Computation
Punch volume — defined in §2.2.4 as the rate of punches thrown — is
computed from the merged punch predictions (§3.3.6): punches occurring
close together in time are grouped into a "combo," and each point on the
uniform metric grid reports the running punch count within whichever combo
covers that point. Because punch volume is a naturally sparse, bursty
signal (a handful of punches within a short combo, then nothing), it is
planned to be left unsmoothed, unlike the other two metrics below —
smoothing would flatten short combos into near-nothing.

### 3.3.8 Guard Height Metric Computation
Guard height — defined in §2.2.4 as how consistently the guard is held up
— is computed per window directly from the extracted landmarks, using the
vertical distance between the head (nose landmark) and whichever guarding
wrist sits higher. A larger value means the guard is held further above
the head; a value near zero or negative means the guard has dropped to
chin/chest height or below. Because a boxer may extend one hand to punch
while the other still guards, the higher of the two wrists is used for each
window, rather than averaging both.

### 3.3.9 Movement Metric Computation
Movement — defined in §2.2.4 as general footwork/ring activity — is
computed per window as the average frame-to-frame speed of the hip
midpoint (the average of the left and right hip landmarks), using only
horizontal-plane displacement. Vertical displacement is deliberately
excluded, since bobbing up and down is not footwork and would otherwise be
conflated with actual repositioning.

### 3.3.10 Graph Generation
The three independently computed metrics (§3.3.7–§3.3.9) are merged into a
single per-window result, aligned on the same window grid so they can be
plotted together. For display, guard height and movement are planned to be
smoothed (to reduce frame-to-frame jitter) and downsampled (to keep the
number of plotted points reasonable for a full session); punch volume is
planned to stay raw and at full resolution through both steps, per §3.3.7.

## 3.4 System Design

### 3.4.1 System Architecture (Logical Modules)
The system is planned as two cooperating phases rather than one: a
developer-run, offline Python pipeline (§3.3.1–§3.3.10, producing a trained
model) and a per-session, on-device Android pipeline that reimplements
detection and metric computation without a network dependency. Logical
modules: capture/import, pose extraction, feature engineering, punch
classification, per-metric computation, graph generation, and
visualization/UX.

### 3.4.2 Data Schema
Planned CSV-based schema for the Python side:
- **Pose landmarks**: `video_id, frame_index, timestamp_ms`, x/y/z per
  landmark, `pose_detected`.
- **Punch windows (labels)**: `video_id, start_frame/start_ms,
  end_frame/end_ms`.
- **Training data**: one row per window — engineered features + `label`.
- **Per-metric outputs**: `video_id, start_ms, end_ms, center_ms`, metric
  value.

### 3.4.3 Interface & UX Flow
Primary screens, reused from the old thesis's UX plan (still accurate):
capture or import a video, a processing/analysis step, and a playback
screen presenting the session video alongside performance graphs (guard
height, punch volume, movement) and playback controls, plus a session
history view for comparing past sessions.

## 3.5 Experimental Setup
*(renumbered from old §3.6; old §3.5 "Implementation" is promoted to its
own chapter — see `04_implementation.md`)*

### 3.5.1 Data Sources and Recording Protocol
Evaluation is planned to use two primary sources: public datasets (UCF101
boxing subset) as a standardized benchmark, and self-recorded smartphone
videos to validate under realistic mobile conditions — reserved exclusively
for testing to avoid data leakage from training. Self-recorded sessions
follow a consistent protocol: mid-range Android smartphone, 1080p, frontal
or slight-angle camera placement, indoor gym lighting, structured drills
(jab-cross combinations, hook combinations, movement/combination work, rest
intervals).

### 3.5.2 Evaluation Metrics & Procedures
Because the system prioritizes accuracy and interpretability over raw
throughput, planned evaluation metrics are: pose detection coverage
(% of frames with a successfully detected pose), punch detection precision/
recall/F1, a comparison against manually tallied punch counts for
metric-validity checking, and user-rated clarity of the visualization
(Likert scale).

### 3.5.3 Testing Conditions & Controls
Testing conditions are planned to capture real-world variability: different
camera angles, clothing (loose vs. tight, bare-chested vs. covered — see
§3.6.3 for why this specifically matters for landmark tracking), background
clutter, and multiple people in frame. Ambient conditions (lighting,
camera/device model, subject distance) are recorded as metadata per test
session to support correlation analysis against any observed accuracy
degradation.

## 3.6 Evaluation Strategy
*(renumbered from old §3.7)*
The evaluation strategy determines whether the system reliably extracts
pose data from mobile footage, classifies punches with sufficient accuracy,
computes metrics that reflect what actually happened in the video, and
presents results in a way that meaningfully supports training analysis.
Assessment is planned across four dimensions:

### 3.6.1 Model Accuracy Evaluation
The Random Forest classifier is evaluated on a held-out partition of the
engineered feature dataset, reporting accuracy, precision, recall, and
F1-score for punch detection, with confusion matrices to identify
misclassification patterns. Critically, the held-out split is planned to be
**grouped by video**, not a per-row shuffle — a per-row shuffle would let
near-duplicate, overlapping windows from the same clip leak across both
sides of the split and produce an inflated, misleading accuracy figure
(this exact failure mode is documented for the abandoned on-device
alternative in §4.3.2, which is why it's called out explicitly here as
planned methodology rather than left implicit).

### 3.6.2 Label Quality Auditing
*(moved in from §3.3.3 — this is planned as an evaluation/QC activity, not
a construction step)* Before manual punch labels are trusted for training,
an automated audit is planned to check them: video identifiers exist and
match extracted pose data, start/end frames are valid and non-reversed,
punch durations fall within a plausible range (flagging implausibly short
or long labels), and no frame range references data outside what was
actually extracted for that video. This functions as quality control on
top of the manual labelling process (§3.3.3), not a replacement for it.

### 3.6.3 Pose Quality and Visualization Validation
Extracted landmarks and the resulting skeleton overlay are validated for
tracking stability under varied conditions — different lighting, camera
distance, clothing/skin contrast (loose vs. tight clothing, bare-chested
recordings), and, on the Android side specifically, whether tracking stays
correctly locked onto the intended subject when other people are visible in
frame (see the multi-person test scenarios planned in
`notes/field_test_c6_d_protocol.md`).

### 3.6.4 User-Centered Evaluation of Interface and Clarity
A small user evaluation is planned with boxers of varying experience level,
reviewing a recorded session on the app's playback screen and rating clarity
of the skeleton overlay, ease of graph interpretation, and whether the
reported punch count/guard trend/movement matched their own sense of what
happened, on a Likert scale plus open-ended questions (instrument:
`notes/tester_questionnaire.md`).

## 3.7 Summary
This chapter outlined the planned methodological framework: a Python
prototype — data collection, MediaPipe landmark extraction, manual punch
labelling, feature engineering, Random Forest training, and Python-side
inference validation — followed by three independently computed
performance metrics (punch volume, guard height, movement) merged into a
single graph-ready output, and finally ported to run entirely on-device in
an Android app. Label auditing, model accuracy evaluation, pose/
visualization validation, and user-centered evaluation are planned as the
evaluation strategy (§3.6) rather than construction steps, keeping this
chapter focused on what was going to be built. Chapter 4 documents what was
actually implemented, and Chapter 5 reports the resulting outputs and
evaluation results.
