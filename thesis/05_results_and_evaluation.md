# 5. Results and Evaluation

Shows the actual output produced at every pipeline stage, then reports
evaluation and testing results against the plan set out in §3.6.
Python-side output, label-audit, and model-accuracy numbers are nested
directly under each stage's construction output in §5.2 (rather than kept
as separate end-of-chapter sections) so each stage's result sits right
next to what it evaluates. Android-side outputs, on-device performance,
and the field-test/questionnaire results are drawn from the completed
field test. See Appendix B for the questionnaire instrument used.

_Status: CHANGED. Title changed to "Results and Evaluation" (from
"Results and Discussion") to better describe a chapter that is mostly
numeric evaluation rather than discussion; see `00_outline.md`. Every
table is now numbered and captioned (Table 5.1-5.17), none were
previously. Every planned image is now a numbered figure with a
placeholder line and a full write-up, replacing the bare `_TODO_` stubs.
Semicolons throughout the chapter split into separate sentences, and the
stale "Draft," prefixes and TODO-style asides on §5.2.6 and §5.4-§5.7
removed now that every number in the chapter is final. All citations of
`notes/field_test_c6_d_protocol.md` and forward references to the
Limitations chapter (§6.5) removed._
## 5.1 Introduction
This chapter reports what the system built in Chapter 4 actually produces,
and how it performed once evaluated against the plan set out in §3.6.
§5.2 walks through the Python pipeline's real output at every stage, from
raw pose extraction through to the final graph-ready metrics, with the
label-quality audit (§5.2.3) and classifier accuracy evaluation (§5.2.6)
reported directly beneath the pipeline stage each one evaluates, rather
than held back to a separate results section. §5.3 shows the corresponding
output from the finished Android app.

The remaining sections report the results of testing that Android app on
real boxers, covering the evaluation dimensions from §3.6 not already
addressed in §5.2: on-device processing performance (§5.4), the validity
of each computed metric against a manual count and direct video comparison
(§5.5), behavior under adversarial and edge-case conditions such as
multiple people in frame or off-model motion (§5.6), and user-centered
feedback collected by questionnaire immediately after each session (§5.7).
§5.8 closes the chapter by drawing these results together into an overall
assessment of what the system is, and is not, currently reliable for.

_Status: NEW chapter (section header only; see subsections below)._
## 5.2 Python Pipeline: Outputs and Evaluation
This section reports what a complete run of the Python pipeline (§4.3)
actually produced, from raw pose extraction through the trained
classifier to the three graph-ready metrics, with each stage's own
evaluation result shown immediately after it rather than deferred to the
end of the chapter.

_Status: CHANGED (adapted). Close paraphrase of `old versions/old article/04_res.md` §A/§B. Table 5.1 named and numbered. The annotated-frame
TODO replaced with Figure 5.1: a numbered figure, a placeholder line, and
a write-up describing what the image shows. Section retitled from
"Python Pipeline Outputs" to "Python Pipeline: Outputs and Evaluation" to
reflect that evaluation results are nested here too; opener rewritten
from a sentence fragment into a full topic sentence._
### 5.2.1 Pose Landmark Extraction Output (Stage B)
`pose_extractor.py` was run across the UCF101 boxing clips, writing one row
per frame to `pose_frames.csv` with 3D coordinates and a visibility score
for the 27 retained body landmarks (§4.3.1): 112 columns total, **31,420
rows from 163 videos**. Table 5.1 shows a short excerpt.

**Table 5.1: Pose Landmark Extraction Output Sample (`pose_frames.csv`)**

| video_id | frame | timestamp_ms | pose_detected | left_shoulder_x | left_shoulder_y | left_shoulder_z | left_shoulder_vis |
|---|---|---|---|---|---|---|---|
| g01_c01 | 1 | 0 | True | 0.482 | 0.353 | 0.111 | 0.999 |
| g01_c01 | 12 | 440 | True | 0.448 | 0.378 | −0.005 | 0.999 |
| g01_c01 | 18 | 680 | True | 0.490 | 0.334 | −0.011 | 0.999 |

The same stage also renders an annotated review video (skeleton and frame
number overlaid), the video a human reviewer watches in stage C. Figure
5.1 shows a single frame from that output.

**Figure 5.1: Annotated Pose Overlay Frame (Example)**

_[Placeholder: a representative frame from an annotated review video,
showing the MediaPipe skeleton and frame number burned over the source
footage, to be inserted here.]_

The overlay draws all 27 retained landmarks connected into the boxer's
skeleton, color-coded by confidence, with the frame index printed in a
corner so a reviewer can cross-reference it directly against the frame
numbers recorded in `punch_windows.csv` (§5.2.2). A clean frame shows the
skeleton tracking the boxer's limbs closely through a punch, with no
visible jitter or landmark drift. A frame with low `pose_detected`
confidence instead shows visibly misplaced joints, most often around the
wrists during a fast punch or when the boxer is partially out of frame,
the exact failure mode the label-quality audit in §5.2.3 is designed to
catch downstream.

_Status: CHANGED (adapted). Close paraphrase of old article §C. Table 5.2
named and numbered._
### 5.2.2 Manually Labelled Punch Windows (Stage C)
Reviewing the annotated videos produced `punch_windows.csv`, recording the
start and end frame of each observed punch, shown in Table 5.2.

**Table 5.2: Manually Labelled Punch Windows Sample (`punch_windows.csv`)**

| video_id | start_frame | end_frame |
|---|---|---|
| g01_c01 | 15 | 21 |
| g01_c01 | 22 | 26 |
| g01_c01 | 27 | 31 |

_Status: CHANGED (adapted). Close paraphrase of old article §G/§H. Tables
5.3 and 5.4 named and numbered._
### 5.2.3 Label Quality Audit (Stage C)
Results of the audit planned in §3.6.2, executed via `audit_punch_labels.py`
over the labels above, are summarized in Table 5.3.

**Table 5.3: Label Quality Audit Results**

| Metric | Result |
|---|---|
| Total videos with pose extracted | 163 |
| Pose Detection Coverage | 95.4% (29,990 / 31,420 frames) |
| Videos manually reviewed | 49 |
| Videos usable and labelled | 15 (31%) |
| Total punch windows labelled | 175 |
| Labels with missing pose video | 0 |
| Punch duration, mean | 210.7 ms |
| Punch duration, std. dev. | 56.1 ms |
| Punch duration, median | 240 ms |
| Punch duration, min / max | 80 / 400 ms |
| Audit status | Passed |

Of the 49 reviewed videos, 15 (31%) yielded usable, labellable footage. The
remaining 34 were excluded during review due to low frame rate, subjects
not fully in frame, multiple people in the shot, or partial obstruction of
the boxer. In these cases MediaPipe either failed to track the boxer or
produced erratic landmark estimates, so those videos were deliberately
withheld rather than risk feeding unreliable pose data into the classifier.
The resulting 175 punch windows show a consistent duration distribution
(mean 210.7 ms, median 240 ms), supporting the appropriateness of the
250 ms fixed window size used in §4.3.3. No punch label referenced a
missing pose video, and the audit reported an overall **passed** status.

Table 5.4 breaks the same audit down by individual criterion (criteria
defined in §3.6.2).

**Table 5.4: Label Quality Audit Per-Criterion Results**

| Criterion | Result |
|---|---|
| Pose Detection Coverage | 95.4% of frames had a successfully detected pose (29,990 / 31,420) |
| Label Completeness | 5 of 15 labelled videos spot-checked against their annotated review video; no omitted punches found |
| Frame Boundary Accuracy | 15 punch windows independently re-reviewed frame-by-frame |
| Label Integrity | 0 missing video IDs; 0 invalid frame ranges |
| Frame Range Validation | 0 out-of-range labels |
| Punch Window Validation | 3 of 175 windows flagged as unusually short (80 ms); manually re-checked and start frame adjusted |
| Duplicate Label Detection | 0 duplicate windows detected |

The exclusion of 34 of the 49 reviewed videos illustrates a practical limit
of relying on an uncurated action-recognition dataset like UCF101. Clips
were not filmed for pose-estimation research, so a substantial share become
unusable once MediaPipe's tracking requirements (a single, unobstructed,
fully framed subject at a reasonable frame rate) are applied. This
variability, rather than any weakness in the labelling algorithm itself,
accounts for most of the gap between the 163 videos with extracted pose
data and the 15 that contributed to the final training set.

_Status: CHANGED (adapted). Close paraphrase of old article §D. Tables 5.5
and 5.6 named and numbered._
### 5.2.4 Training Dataset Construction Output (Stage D)
`build_training_csv.py` converts each labelled interval, plus sampled
`no_punch` intervals, into fixed-duration (250 ms) windows and computes
motion- and body-relative features, shown in Table 5.5.

**Table 5.5: Training Dataset Feature Sample (`training.csv`)**

| video_id / window | right_wrist_velocity_mean | left_wrist_velocity_mean | right_shoulder_to_wrist_distance_change | right_wrist_forward_extension_change | label |
|---|---|---|---|---|---|
| g01_c01, 550–800 ms | 3.634 | 1.991 | 0.134 | 0.059 | punch |
| g02_c03, 5030–5280 ms | 2.867 | 2.155 | 0.143 | 0.191 | punch |
| g01_c03, 1000–1250 ms | 0.791 | 1.126 | 0.029 | −0.047 | no_punch |

The larger right-wrist velocity and right-side shoulder-to-wrist distance
change in the two punch examples are consistent with a right-hand punch,
illustrating why body-relative motion features, rather than raw landmark
coordinates, separate punch from no_punch windows.

Table 5.6 summarizes the composition of the resulting training set.

**Table 5.6: Training Dataset Composition**

| Metric | Value |
|---|---|
| Punch windows | 175 |
| No_punch windows | 175 |
| Total training rows | 350 |
| Window duration | 250 ms |
| Positive anchor mode | End |
| No_punch sampling stride | 250 ms |
| Raw landmark mean/std features | Excluded |

_Status: CHANGED (adapted). Close paraphrase of old article §E._
### 5.2.5 Trained Classifier (Stage E)
`train_random_forest.py` trained a Random Forest classifier on the 350-row
`training.csv` above, using the same hyperparameters and full dataset that
ship in `models/random_forest.joblib`. See §5.2.6 below for accuracy, F1,
and a confusion matrix from a video-grouped, cross-validated evaluation.
The article this section is drawn from evaluated the labelling pipeline,
not final classifier accuracy, so that number is new to this thesis.

_Status: NEW. The article evaluated the labelling pipeline only, not classifier accuracy; this is the 2026-08-10 evaluation, new to this thesis. Tables 5.7-5.9 named and numbered; "Draft," prefix and semicolons removed from the opening note._
### 5.2.6 Model Accuracy & Evaluation (Stage E)
_Evaluation run 2026-08-10 via `python/evaluate_random_forest.py`, a
read-only script that trains throwaway in-memory models purely to score
them and never writes to `models/random_forest.joblib` (the artifact
actually exported to Android), so this evaluation cannot affect the
shipped app. Same dataset (350 rows / 175 punch / 175 no_punch / 111
videos, §5.2.4) and same hyperparameters as production training
(`n_estimators=300, class_weight="balanced"`), with feature selection
imported directly from `train_random_forest.py` so it cannot drift from
what actually ships. Full results: `python/data/rf_eval_results.json`._

All three evaluations below are grouped by `video_id`, per §3.6.3. A naive
per-row shuffle would let overlapping windows from the same clip leak
across train/test and inflate the score. Table 5.7 compares the three
evaluation methods used.

**Table 5.7: Random Forest Evaluation Comparison**

| Evaluation | Accuracy | Macro F1 |
|---|---|---|
| Single 85/15 held-out split (seed 42) | 45.2% | 0.394 |
| 5-fold grouped cross-validation (pooled) | **69.4%** | **0.692** |
| 15-seed repeated single-split, range | 32.9%–91.3% (mean 72.5%) | N/A |

Pooled 5-fold cross-validation is reported as the headline number, broken
down per class in Table 5.8.

**Table 5.8: Per-Class Precision, Recall, and F1 (Pooled 5-Fold Cross-Validation)**

| Class | Precision | Recall | F1 |
|---|---|---|---|
| punch | 0.736 | 0.606 | 0.665 |
| no_punch | 0.665 | 0.783 | 0.719 |

Table 5.9 gives the corresponding confusion matrix, pooled across folds
(rows are actual class, columns are predicted class, order
[punch, no_punch]).

**Table 5.9: Confusion Matrix (Pooled Across Folds)**

|  | Predicted punch | Predicted no_punch |
|---|---|---|
| **Actual punch** | 106 | 69 |
| **Actual no_punch** | 38 | 137 |

The 5-fold cross-validation figure in Table 5.7 is the more trustworthy of
the three, for a reason grounded in the data rather than convention. The
single 85/15 split scored a discouraging 45.2%, close to chance.
Repeating that same split with 15 different random seeds shows why that
number alone is meaningless. Accuracy ranged from 32.9% to 91.3% depending
purely on which ~17 videos happened to land in the held-out set. With only
111 videos and an uneven number of labelled rows per video, a single
grouped split is simply too small a sample to trust on its own. The
5-fold cross-validation figure is more defensible because every video
serves as held-out data exactly once, pooling predictions across all 111
videos rather than one arbitrary 15% carve-out. Its 69.4% accuracy sits
almost exactly at the 15-seed sensitivity sweep's mean (72.5%), the
agreement that makes it trustworthy as the headline figure rather than a
favorable outlier.

The abandoned on-device TFLite network offers a point of comparison,
though not a perfectly matched one. The decision log (`notes/decisions.md`,
2026-08-07) originally reported the network scoring approximately 50
percent, chance-level, on a claimed video-grouped split of the same
dataset. Re-checking the actual removed script while writing this
evaluation found it used a plain per-row shuffle, not a grouped split.
That specific number should not be cited as using the same leak-proof
methodology as the RF numbers above without re-running it (see the
2026-08-10 correction in `notes/decisions.md`). The directional
conclusion, that the Random Forest meaningfully outperforms a
from-scratch on-device network, still holds (69.4% vs. approximately 50
percent), but a clean, fully apples-to-apples comparison for the final
defense would re-run the TFLite training with proper `GroupKFold`
cross-validation before citing the two side by side.

_Status: CHANGED (adapted). Close paraphrase of old article §F. Tables
5.10 and 5.11 named and numbered._
### 5.2.7 Predictions from New Footage (Stage F)
`predict_punches.py` applied the trained classifier to a new, previously
unseen user video, producing feature rows like the one in Table 5.10.

**Table 5.10: Predicted Feature Sample from New Footage**

| video_id / window | right_wrist_velocity_mean | left_wrist_velocity_mean | right_shoulder_to_wrist_distance_change | right_wrist_forward_extension_change |
|---|---|---|---|---|
| test, 0–250 ms | 3.141 | 2.054 | −0.136 | −0.136 |

The merged punch windows that result are shown in Table 5.11.

**Table 5.11: Merged Punch Windows from New Footage**

| video_id | start_ms | end_ms |
|---|---|---|
| testvideo | 280 | 570 |

_Status: NEW. The article did not cover this metric. Table 5.12 named and
numbered._
### 5.2.8 Punch Volume Metric Output (Stage G)
`punch_volume.py` run against the merged punch windows above, on the same
250 ms/40 ms grid. Table 5.12 spans the first combo (280–570 ms). The
combo count sits at 0 until the combo's own punch lands, then steps to 1
and holds. The step lines up with `end_ms`, not `start_ms`, since
`punch_count_at()` marks a punch as landed at its end time.

**Table 5.12: Punch Volume Metric Output Sample**

| video_id | start_ms | end_ms | center_ms | punch_volume |
|---|---|---|---|---|
| testvideo | 320 | 570 | 445 | 0 |
| testvideo | 360 | 610 | 485 | 0 |
| testvideo | 400 | 650 | 525 | 0 |
| testvideo | 440 | 690 | 565 | 0 |
| testvideo | 480 | 730 | 605 | 1 |
| testvideo | 520 | 770 | 645 | 1 |

_Status: NEW. Same as §5.2.8. Table 5.13 named and numbered._
### 5.2.9 Guard Height Metric Output (Stage H)
`guard_height.py` over the same grid, computing `nose_y − highest-guarding
wrist_y`, shown in Table 5.13. The result is negative here because the
guarding wrist sits below the nose landmark for this window.

**Table 5.13: Guard Height Metric Output Sample**

| video_id | start_ms | end_ms | center_ms | guard_height |
|---|---|---|---|---|
| testvideo | 0 | 250 | 125 | −0.0352 |
| testvideo | 40 | 290 | 165 | −0.0399 |
| testvideo | 80 | 330 | 205 | −0.0416 |
| testvideo | 120 | 370 | 245 | −0.0427 |
| testvideo | 160 | 410 | 285 | −0.0445 |

_Status: NEW. Same as §5.2.8. Table 5.14 named and numbered._
### 5.2.10 Movement Metric Output (Stage I)
`movement.py` over the same grid, computing hip-midpoint x/z speed, shown
in Table 5.14. The value rises here as the boxer starts moving into range.

**Table 5.14: Movement Metric Output Sample**

| video_id | start_ms | end_ms | center_ms | movement |
|---|---|---|---|---|
| testvideo | 0 | 250 | 125 | 0.0428 |
| testvideo | 40 | 290 | 165 | 0.0684 |
| testvideo | 80 | 330 | 205 | 0.0941 |
| testvideo | 120 | 370 | 245 | 0.1240 |
| testvideo | 160 | 410 | 285 | 0.1533 |

_Status: NEW. Same as §5.2.8. Table 5.15 named and numbered. The
graph_metrics_plot.png mention replaced with Figure 5.2: a numbered
figure, a placeholder line, and a write-up describing what the plot
shows._
### 5.2.11 Graph Generation Output (Stage J)
`graph_metrics.py` inner-joins the three stages above on
`(video_id, center_ms)`, producing rows like those in Table 5.15.

**Table 5.15: Graph Generation Output Sample**

| video_id | start_ms | end_ms | center_ms | punch_volume | guard_height | movement |
|---|---|---|---|---|---|---|
| testvideo | 0 | 250 | 125 | 0 | −0.0352 | 0.0428 |
| testvideo | 40 | 290 | 165 | 0 | −0.0399 | 0.0684 |
| testvideo | 80 | 330 | 205 | 0 | −0.0416 | 0.0941 |
| testvideo | 120 | 370 | 245 | 0 | −0.0427 | 0.1240 |
| testvideo | 160 | 410 | 285 | 0 | −0.0445 | 0.1533 |

The rendered plot (smoothed/downsampled guard height and movement, raw
punch volume, per §4.3.9) is `python/data/graph_metrics_plot.png`, shown
here as Figure 5.2.

**Figure 5.2: Rendered Graph Metrics Plot (Example)**

_[Placeholder: `python/data/graph_metrics_plot.png`, three stacked or
overlaid line plots for punch volume, guard height, and movement against
session time, to be inserted here.]_

Guard height and movement appear as smooth, continuous curves, since both
are rolling-averaged and downsampled for display (§4.3.9), while punch
volume appears as a sparse step function that holds at 0 between combos
and steps upward once per landed punch within a combo, left unsmoothed so
individual punches within a burst stay visible rather than being averaged
away. Reading the three curves together against the session timeline is
what lets a boxer or coach connect a guard drop or a movement spike to
the punches happening around it, the same reading exercised directly in
the field test (§5.5).

_Status: CHANGED. The four single-screen figures (one per screen) combined
into three multi-panel figures grouped by purpose: starting a session,
reviewing its results, and browsing history. `HomeScreen.kt` is now
pictured as part of the first figure rather than left out._
## 5.3 Android App Outputs
The finished Android app presents the same pipeline output through five
screens (§4.4.5, Table 4.7). Figures 5.3-5.5 show that flow end to end,
from starting a session through reviewing its results and browsing past
sessions.

**Figure 5.3: App Screens: Home, Capture, Import, and Processing**

_[Placeholder: four panels, (a) `HomeScreen.kt`, the landing screen, (b)
`NewSessionScreen.kt` with `CameraCapture.kt` in its recording state, (c)
`NewSessionScreen.kt` in its import state, and (d) `ProcessingScreen.kt`
with `ProcessingStatusBar.kt` showing a queue of videos being processed,
to be inserted here.]_

Panel (a) is the landing screen, from which a boxer starts a new session
or opens session history (Figure 5.5). `NewSessionScreen.kt` splits the
"Home / Capture" screen planned in §3.4 into separate recording and
importing states, shown in panels (b) and (c). A boxer either records a
new session directly with the device camera or imports an existing video
file. The app then moves on to processing, shown in panel (d) with more
than one video queued at once against the on-device pipeline (§4.4.1-
§4.4.4).

**Figure 5.4: Session Playback and Per-Metric Graphs**

_[Placeholder: four panels, (a) `SessionPlaybackScreen.kt` with
`SessionVideoPlayer.kt`, `PerformanceGraph.kt`, and `PlaybackControls.kt`
showing the skeleton-overlay video alongside all three performance
graphs, and (b)-(d) close-ups of the punch volume, guard height, and
movement graphs individually, to be inserted here.]_

Panel (a) is the playback stage described in §3.3.11: the session video,
synced to the three graphed metrics on a shared timeline, with standard
playback controls so a boxer or coach can step through a session and see
exactly which moment on a graph corresponds to which moment in the
footage. Panels (b)-(d) isolate each of the three metrics for closer
reading. This is the screen exercised directly in the field test's
metric-validity checks (§5.5).

**Figure 5.5: Session History Screen**

_[Placeholder: `PreviousSessionsScreen.kt` with `SessionCard.kt`, showing
a list of past sessions with thumbnails, dates, and durations, to be
inserted here.]_

Past sessions are listed newest-first, read reactively from the local
Room database (§4.4.4), each entry showing a thumbnail, date, and duration
before opening back into the same playback screen (Figure 5.4) on tap.

_Status: NEW. Real field-test data (2026-08-10), no old-thesis or article equivalent. "Draft," prefix and TODO-style aside removed from the opening note; semicolons split. A 3-minute clip data point added (~12-14 minutes on the S20 FE); an opening topic sentence added ahead of the processing-time list._
## 5.4 On-Device Performance
_Field test 2026-08-10, n=3 devices/3 boxers._

This section reports how the on-device pipeline performs on real phones
rather than a development machine: how long a session takes to process
end to end, and whether the app holds up processing several sessions
back to back. End-to-end processing time, from capture through pose
extraction, classification, and graph generation, all on-device with no
network dependency:
- A 15s clip processed in ~1–2 minutes, pooled across all three test
  devices (Samsung S20 FE, A36 5G, A50, upper-mid to budget tier).
- A 60s clip processed in ~6–8 minutes on the S20 FE.
- A 3 min clip processed in ~12–14 minutes on the S20 FE.

All three figures fall within roughly the same four-to-eight-times-real-time
range, consistent in order of magnitude with each other, with the 3-minute
clip sitting toward the lower end of that range. The app also queued and
successfully processed 4 videos back-to-back on a single device without
failure. Clip length and device are confounded across the test batches.
The 15-second clip was tested on all three phones, while the 60-second
and 3-minute clips were tested only on the S20 FE, so no clean,
matched-length comparison exists across devices.

_Status: NEW. "Draft," prefix removed from the opening note._
## 5.5 Metric Validity
_Same field test, n=3 boxers, 1 min session each on the S20 FE (device
held constant to isolate the person/stance variable)._

Guard height and movement were rated fully accurate by direct comparison
against the source video for all three boxers, with variation (guard
dropping and recovering around punches, footwork spikes in movement)
clearly readable on the rendered graph in every session.

Punch volume, checked against a manual live count of punches thrown, is
summarized in Table 5.16.

**Table 5.16: Punch Volume Recall by Boxer**

| Boxer | Experience | Stance | Thrown (manual) | Recorded (app) | Recall |
|---|---|---|---|---|---|
| A | 3+ yrs | Orthodox | 76 | 47 | 61.8% |
| B | 1+ yrs | Southpaw | 43 | 14 | 32.6% |
| C | <1 yr | Orthodox | 29 | 26 | 89.7% |

Pooled recall is 87/148, or 58.8%. Zero false positives occurred across
all three sessions, and idle/no-activity stretches were consistently
recorded as such.

The dominant error mode was undercounting rather than misclassification.
Rapid combos of 2+ punches were often collapsed into a single recorded
punch, but every combo thrown registered as *at least* one punch. None
went entirely undetected. Recall tracked technique conformity rather than
punch speed, elaborated per boxer below.

**Figure 5.6: Boxer A, B, and C Session Comparison**

_[Placeholder: three panels, (a) Boxer A's session playback with graphs,
(b) Boxer B's session playback with graphs, (c) Boxer C's session
playback with graphs, to be inserted here.]_

Figure 5.6 shows the three sessions side by side, each panel the
skeleton-overlay playback and performance graphs from that boxer's
recording.

Boxer A (panel a), an orthodox boxer with three or more years of
experience, threw 76 punches and had 47 recorded, a recall of 61.8%.
Rapid 2-punch combos were sometimes collapsed into a single recorded
punch, but every combo thrown, including longer 3–5 punch combinations,
registered as at least one punch and was visually distinguishable on the
rendered graph. None
were missed outright. Idle stretches were correctly recorded as no
activity, and no false positives occurred, though this boxer was
continuously active for nearly the whole session, so the test gave few
opportunities to falsely fire during idle time in the first place. Guard
height and movement were both accurate.

Boxer B (panel b), a southpaw boxer with one or more years of experience,
threw 43 punches and had 14 recorded, a recall of 32.6%, the lowest of
the three. Straight punches were mostly detected.
Hooks and uppercuts were frequently missed. Idle periods were still
clearly visible on the graph and no false positives occurred, and guard
height and movement remained fully accurate and readable. The
undercounting was specific to punch classification, not a general
tracking failure. The pattern was attributed during testing to a
combination of southpaw stance and technique variation, consistent with
the training set (§5.2.4) containing no labelled southpaw footage. The
classifier has effectively never seen a southpaw's mirrored wrist/shoulder
motion for anything but straight punches.

Boxer C (panel c), an orthodox boxer with less than a year of experience,
threw 29 punches and had 26 recorded, a recall of 89.7% and the highest
of the three. Punches were thrown more slowly and with consistently
correct form throughout. A small number of 2-punch sequences were still
collapsed to one, the same pattern seen with Boxer A but at lower
frequency, plausibly because slower throws are less likely to land inside
the same 250 ms classification window. Idle periods were correctly
recorded and no false positives occurred. Guard height and movement were
fully accurate.

Taken together, recall correlated with technique conformity and pace, not
boxing experience. The least experienced boxer (C) had the best
punch-count accuracy because their punches were slower,
cleaner, and closest to the orthodox technique the training data
represents. The most experienced boxer (A) still undercounted because
rapid, tightly spaced combos are the hardest case for a fixed 250 ms
sliding window. The southpaw boxer (B) was undercounted for reasons
independent of skill or speed entirely. In its current state, the app is
most usable as a **volume/activity trend and guard-height/movement review
tool for orthodox boxers training at a moderate pace**, for example a
beginner drilling fundamentals, or a coach checking whether guard drops and
footwork happened when expected, and least reliable as a **precise punch
counter for fast elite-pace combination work or for southpaw boxers**,
where undercounting is frequent enough to misrepresent actual output.

_Status: CHANGED. "Draft," prefix removed from the opening note;
semicolons split. Figure 5.6 added: a three-panel comparison of the
Boxer A, B, and C sessions, placed before the individual boxer
breakdowns, with panel letters added to each so the write-up does not
repeat what the paragraphs already say._
## 5.6 Error Testing
_Field test 2026-08-10, plus incidental observations from the §5.5
sessions. Split out from metric validity since these probe failure
behavior under adversarial and edge conditions rather than accuracy under
normal use._

This section checks how the system behaves under conditions beyond the
single-subject, on-model case tested for accuracy above: a second person
in frame, a person entering or leaving mid-recording, and motion that
does not resemble a punch. The goal is not another accuracy figure, but
to check whether unfamiliar input causes a crash, a silently wrong
result, or a clean recovery.

**Figure 5.7: Error Testing Conditions**

_[Placeholder: three panels, (a) two people shadowboxing simultaneously
in frame, with tracking locked onto one subject, (b) a person entering or
leaving frame mid-recording, (c) off-model motion such as a kick, not
counted as a punch, to be inserted here.]_

Figure 5.7 shows the three conditions tested.

With two people shadowboxing simultaneously in frame for 30 seconds
(panel a), pose extraction locked onto one person for the full clip and
computed metrics for that person only, without jumping between subjects.

For a person entering or leaving frame (panel b), tracking engages when a
person enters frame and stops cleanly when they leave, without producing
garbage output in between. Brief partial occlusion (a wrist or foot
leaving frame momentarily) is estimated reasonably by MediaPipe rather
than breaking the metric.

For off-model motion (panel c), kicks or punches thrown with technique
dissimilar to the training data's boxing punches were not counted as
punches, while guard height and movement continued tracking normally
throughout. Read alongside Boxer B's southpaw result in §5.5, this
suggests the classifier is conservative. It under-detects unfamiliar
motion patterns rather than hallucinating punches on unrelated movement.
That is the safer failure direction for an app whose output a boxer or
coach might rely on.

None of the three conditions produced a crash, a silently plausible but
wrong result, or an unrecoverable app state. Every failure mode observed
was under-detection, not fabrication.

_Status: CHANGED. Neither the old thesis (pre-implementation) nor the
article covered this. "Draft," prefix removed from the opening note;
semicolons split. Section renamed from "Error / Robustness Testing" to
"Error Testing". An opening paragraph added ahead of Figure 5.7,
explaining what this section tests and why, before the figure is shown.
Figure 5.7 moved to the top of the section, before the three condition
paragraphs, with panel letters added to each so the write-up does not
repeat what the paragraphs already say._
## 5.7 User-Centered Evaluation
_Questionnaire responses collected 2026-08-10 immediately after each
boxer's session, with the full instrument and raw data in Appendix B. With
n=3, this is not a statistically powered sample, per the caveat noted
there. Scores below are directional, read alongside the open-ended
answers and the objective results in §5.5-§5.6._

The full flow, from installing the app through recording, processing, and
reviewing a session, ran successfully end-to-end on all three boxers' own
devices, including previous-session history, thumbnails,
and playback, with no crashes across any of the three real-device sessions,
the three body-type/stance sessions, or the error-condition tests in §5.6.

Ten statements were rated on a 1-5 Likert scale (5 = strongly agree).
Table 5.17 gives the full results.

**Table 5.17: Likert Results (1-5, 5 = Strongly Agree)**

| # | Statement | A | B | C | Mean |
|---|---|---|---|---|---|
| 1 | Easy to start/stop a recording | 5 | 5 | 5 | 5.00 |
| 2 | Understood what the app was doing while processing | 5 | 5 | 4 | 4.67 |
| 3 | Skeleton overlay was easy to follow | 5 | 5 | 5 | 5.00 |
| 4 | Graphs were easy to understand | 5 | 5 | 5 | 5.00 |
| 5 | Punch count roughly matched what was thrown | 4 | 3 | 5 | 4.00 |
| 6 | Guard height graph reflected actual guard | 5 | 5 | 5 | 5.00 |
| 7 | Movement graph reflected actual movement | 5 | 5 | 5 | 5.00 |
| 8 | Trust the feedback enough to check technique | 5 | 4 | 5 | 4.67 |
| 9 | App felt reliable (no crash/freeze) | 5 | 5 | 5 | 5.00 |
| 10 | Overall easy to use | 5 | 5 | 5 | 5.00 |

Overall mean is 4.83 out of 5 across all 30 responses. Item 5 (punch count
matched what was thrown) is the only item scored below 5 by more than one
tester, and the lowest-scoring item overall (mean 4.00). Boxer B, whose
objective recall was also lowest (32.6%, §5.5), rated it 3. Boxer A, whose
recall gap came from combo-collapsing rather than missed punches (61.8%),
rated it 4. This is a direct convergence between the subjective usability
rating and the objective punch-recall measurement, not two disconnected
results. Item 8 (trust in the feedback for technique review) follows the
same pattern: Boxer B alone scored it 4 instead of 5, the one tester whose
punches were most often missed also being the one least willing to fully
trust the app's feedback. Every other item was rated 5 by all three
boxers.

In the open-ended answers, Boxer A and Boxer C both independently asked
for the same missing feature: a breakdown by **punch type**
(jab/cross/hook/uppercut).

_Status: CHANGED. Reverted from a bar chart (Figure 5.7) back to the full
Likert table, now named and numbered as Table 5.17. Open-ended answers
trimmed to the one point carried forward, the requested punch-type
breakdown; Boxer B's specific quotes and the training-set tie-back
dropped. Figures 5.1-5.7 still need their real images inserted in place
of the placeholders, which does not affect any number summarized below._
## 5.8 Summary
The Python pipeline (§5.2) produced a complete, workable dataset from an
uncurated source: MediaPipe successfully tracked 95.4% of extracted frames
across 163 UCF101 videos, though only 15 of the 49 manually reviewed
videos (31%) met the framing and tracking requirements needed to be
labelled, yielding a balanced 350-row training set. Evaluated with a
video-grouped 5-fold cross-validation, the methodology appropriate for a
dataset this small (§3.6.3), the Random Forest classifier reached 69.4%
accuracy and a 0.692 macro F1 score, meaningfully outperforming the
abandoned from-scratch on-device network it replaced (§4.4.2), which
scored no better than chance.

On real hardware (§5.4), the full on-device pipeline processed footage at
roughly four to eight times real-time with no crashes across repeated
sessions, confirming the app is practical to use without a network
dependency. Field testing with three boxers (§5.5-§5.7) found guard height
and movement tracking fully accurate for all three testers, while punch
volume recall varied by technique and stance: 61.8% to 89.7% for the two
orthodox boxers, against 32.6% for the one southpaw boxer, a gap traced to
the training set containing no southpaw footage rather than to a general
tracking failure. Error testing (§5.6) found the same conservative
failure pattern under adversarial conditions. The classifier under-detects
unfamiliar motion rather than producing false positives, and no crash or
unrecoverable state was observed under any tested condition.
User-centered feedback (§5.7) rated the app 4.83 out of 5 overall, with
the one below-average item, punch count accuracy, tracking directly with
each boxer's own objective recall score rather than reflecting a general
usability problem.

Taken together, these results characterize the system as most reliable as
a volume and activity trend tool, and for guard-height and movement
review, for orthodox boxers training at a moderate pace, and least
reliable as a precise punch counter for fast combination work or for
southpaw boxers.
