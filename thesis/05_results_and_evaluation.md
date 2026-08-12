# 5. Results: Outputs, Evaluation and Testing

*(Replaces "Results and Discussion" — see `00_outline.md`.)* Shows the
actual output produced at every pipeline stage, then reports evaluation and
testing results against the plan set out in §3.6. Python-side output,
label-audit, and model-accuracy numbers are nested directly under each
stage's construction output in §5.2 (rather than kept as separate
end-of-chapter sections) so each stage's result sits right next to what it
evaluates. Android-side outputs, on-device performance, and the field-test/
questionnaire results are drawn from the completed field test — see
`notes/field_test_c6_d_protocol.md` and `notes/tester_questionnaire.md` for
the instruments used.

## 5.1 Introduction
_TODO — brief roadmap of this chapter once the remaining TODOs below
(intro roadmap itself, §5.3 screenshots, example images in §5.2) are
filled in._

## 5.2 Python Pipeline Outputs
Walkthrough of the actual output at each stage of the Python pipeline
(§4.2), from one completed run. Each construction stage is immediately
followed by its own evaluation result, where one exists, rather than
deferring evaluation to the end of the chapter.

### 5.2.1 Pose Landmark Extraction Output (stage B)
`pose_extractor.py` was run across the UCF101 boxing clips, writing one row
per frame to `pose_frames.csv` with 3D coordinates and a visibility score
for 33 body landmarks — 112 columns total, **31,420 rows from 163 videos**.

| video_id | frame | timestamp_ms | pose_detected | left_shoulder_x | left_shoulder_y | left_shoulder_z | left_shoulder_vis |
|---|---|---|---|---|---|---|---|
| g01_c01 | 1 | 0 | True | 0.482 | 0.353 | 0.111 | 0.999 |
| g01_c01 | 12 | 440 | True | 0.448 | 0.378 | −0.005 | 0.999 |
| g01_c01 | 18 | 680 | True | 0.490 | 0.334 | −0.011 | 0.999 |

The same stage also renders an annotated review video (skeleton + frame
number overlaid) — the video a human reviewer watches in stage C.
_TODO: insert an example annotated frame image._

### 5.2.2 Manually Labelled Punch Windows (stage C)
Reviewing the annotated videos produced `punch_windows.csv`, recording the
start and end frame of each observed punch:

| video_id | start_frame | end_frame |
|---|---|---|
| g01_c01 | 15 | 21 |
| g01_c01 | 22 | 26 |
| g01_c01 | 27 | 31 |

### 5.2.3 Label Quality Audit (stage C)
Results of the audit planned in §3.6.2, executed via `audit_punch_labels.py`
over the labels above:

| Metric | Result |
|---|---|
| Total videos with pose extracted | 163 |
| Pose Detection Coverage | 95.4% (29,990 / 31,420 frames) |
| Videos manually reviewed | 49 |
| Videos usable and labelled | 15 (31%) |
| Total punch windows labelled | 175 |
| Labels with missing pose video | 0 |
| Punch duration — mean | 210.7 ms |
| Punch duration — std. dev. | 56.1 ms |
| Punch duration — median | 240 ms |
| Punch duration — min / max | 80 / 400 ms |
| Audit status | Passed |

Of the 49 reviewed videos, 15 (31%) yielded usable, labellable footage. The
remaining 34 were excluded during review due to low frame rate, subjects
not fully in frame, multiple people in the shot, or partial obstruction —
conditions under which MediaPipe either failed to track the boxer or
produced erratic landmark estimates, so those videos were deliberately
withheld rather than risk feeding unreliable pose data into the classifier.
The resulting 175 punch windows show a consistent duration distribution
(mean 210.7 ms, median 240 ms), supporting the appropriateness of the
250 ms fixed window size used in §4.2.3. No punch label referenced a
missing pose video, and the audit reported an overall **passed** status.

**Per-criterion results** (criteria defined in §3.6.2):

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
of relying on an uncurated action-recognition dataset like UCF101: clips
weren't filmed for pose-estimation research, so a substantial share become
unusable once MediaPipe's tracking requirements (a single, unobstructed,
fully framed subject at a reasonable frame rate) are applied. This
variability, rather than any weakness in the labelling algorithm itself,
accounts for most of the gap between the 163 videos with extracted pose
data and the 15 that contributed to the final training set.

### 5.2.4 Training Dataset Construction Output (stage D)
`build_training_csv.py` converts each labelled interval, plus sampled
`no_punch` intervals, into fixed-duration (250 ms) windows and computes
motion- and body-relative features:

| video_id / window | right_wrist_velocity_mean | left_wrist_velocity_mean | right_shoulder_to_wrist_distance_change | right_wrist_forward_extension_change | label |
|---|---|---|---|---|---|
| g01_c01, 550–800 ms | 3.634 | 1.991 | 0.134 | 0.059 | punch |
| g02_c03, 5030–5280 ms | 2.867 | 2.155 | 0.143 | 0.191 | punch |
| g01_c03, 1000–1250 ms | 0.791 | 1.126 | 0.029 | −0.047 | no_punch |

The larger right-wrist velocity and right-side shoulder-to-wrist distance
change in the two punch examples are consistent with a right-hand punch —
illustrating why body-relative motion features, rather than raw landmark
coordinates, separate punch from no_punch windows.

**Training dataset composition:**

| Metric | Value |
|---|---|
| Punch windows | 175 |
| No_punch windows | 175 |
| Total training rows | 350 |
| Window duration | 250 ms |
| Positive anchor mode | End |
| No_punch sampling stride | 250 ms |
| Raw landmark mean/std features | Excluded |

### 5.2.5 Trained Classifier (stage E)
`train_random_forest.py` trained a Random Forest classifier on the 350-row
`training.csv` above, using the same hyperparameters and full dataset that
ship in `models/random_forest.joblib`. See §5.2.6 below for accuracy/F1/
confusion matrix on a video-grouped, cross-validated evaluation — the
article this section is drawn from evaluated the labelling pipeline, not
final classifier accuracy, so that number is new to this thesis.

### 5.2.6 Model Accuracy & Evaluation (stage E)
_Draft — evaluation run 2026-08-10 via `python/evaluate_random_forest.py`,
a read-only script that trains throwaway in-memory models purely to score
them and never writes to `models/random_forest.joblib` — the artifact
actually exported to Android — so this evaluation cannot affect the shipped
app. Same dataset (350 rows / 175 punch / 175 no_punch / 111 videos, §5.2.4)
and same hyperparameters as production training
(`n_estimators=300, class_weight="balanced"`), with feature selection
imported directly from `train_random_forest.py` so it can't drift from what
actually ships. Full results: `python/data/rf_eval_results.json`._

All three evaluations below are grouped by `video_id`, per §3.6.1 — a
naive per-row shuffle would let overlapping windows from the same clip leak
across train/test and inflate the score.

| Evaluation | Accuracy | Macro F1 |
|---|---|---|
| Single 85/15 held-out split (seed 42) | 45.2% | 0.394 |
| 5-fold grouped cross-validation (pooled) | **69.4%** | **0.692** |
| 15-seed repeated single-split — range | 32.9%–91.3% (mean 72.5%) | — |

**Pooled 5-fold cross-validation** is reported as the headline number,
broken down per class:

| Class | Precision | Recall | F1 |
|---|---|---|---|
| punch | 0.736 | 0.606 | 0.665 |
| no_punch | 0.665 | 0.783 | 0.719 |

Confusion matrix (pooled across folds, rows = actual, columns = predicted,
order [punch, no_punch]):

|  | Predicted punch | Predicted no_punch |
|---|---|---|
| **Actual punch** | 106 | 69 |
| **Actual no_punch** | 38 | 137 |

**Why cross-validation, not a single split:** the single 85/15 split scored
a discouraging 45.2%, close to chance. Repeating that same split with 15
different random seeds shows why that number alone is meaningless — accuracy
ranged from 32.9% to 91.3% depending purely on which ~17 videos happened to
land in the held-out set. With only 111 videos and an uneven number of
labelled rows per video, a single grouped split is simply too small a
sample to trust on its own. The 5-fold cross-validation figure is more
defensible because every video serves as held-out data exactly once,
pooling predictions across all 111 videos rather than one arbitrary 15%
carve-out — and its 69.4% accuracy sits almost exactly at the 15-seed
sensitivity sweep's mean (72.5%), which is the agreement that makes it
trustworthy as the headline figure rather than a favorable outlier.

**Comparison to the abandoned TFLite network:** the decision log
(`notes/decisions.md`, 2026-08-07) originally reported the on-device TFLite
network scoring ~50% (chance-level) on a claimed video-grouped split of the
same dataset. Re-checking the actual removed script while writing this
evaluation found it used a plain per-row shuffle, not a grouped split — so
that specific number should not be cited as using the same leak-proof
methodology as the RF numbers above without re-running it (see the
2026-08-10 correction in `notes/decisions.md`). The directional conclusion
(RF meaningfully outperforming a from-scratch on-device network) still
holds — 69.4% vs. ~50% — but for a clean, fully apples-to-apples ablation
in the final defense, re-running the TFLite training with proper
`GroupKFold` cross-validation before citing the two side by side is the
more defensible path.

### 5.2.7 Predictions from New Footage (stage F)
`predict_punches.py` applied the trained classifier to a new, previously
unseen user video:

| video_id / window | right_wrist_velocity_mean | left_wrist_velocity_mean | right_shoulder_to_wrist_distance_change | right_wrist_forward_extension_change |
|---|---|---|---|---|
| test, 0–250 ms | 3.141 | 2.054 | −0.136 | −0.136 |

Merged punch windows:

| video_id | start_ms | end_ms |
|---|---|---|
| testvideo | 280 | 570 |

### 5.2.8 Punch Volume Metric Output (stage G)
`punch_volume.py` run against the merged punch windows above, on the same
250 ms/40 ms grid. The rows below span the first combo (280–570 ms): the
combo count sits at 0 until the combo's own punch lands, then steps to 1
and holds — the step lines up with `end_ms`, not `start_ms`, since
`punch_count_at()` marks a punch as landed at its end time:

| video_id | start_ms | end_ms | center_ms | punch_volume |
|---|---|---|---|---|
| testvideo | 320 | 570 | 445 | 0 |
| testvideo | 360 | 610 | 485 | 0 |
| testvideo | 400 | 650 | 525 | 0 |
| testvideo | 440 | 690 | 565 | 0 |
| testvideo | 480 | 730 | 605 | 1 |
| testvideo | 520 | 770 | 645 | 1 |

### 5.2.9 Guard Height Metric Output (stage H)
`guard_height.py` over the same grid — `nose_y − highest-guarding wrist_y`,
negative here because the guarding wrist sits below the nose landmark for
this window:

| video_id | start_ms | end_ms | center_ms | guard_height |
|---|---|---|---|---|
| testvideo | 0 | 250 | 125 | −0.0352 |
| testvideo | 40 | 290 | 165 | −0.0399 |
| testvideo | 80 | 330 | 205 | −0.0416 |
| testvideo | 120 | 370 | 245 | −0.0427 |
| testvideo | 160 | 410 | 285 | −0.0445 |

### 5.2.10 Movement Metric Output (stage I)
`movement.py` over the same grid — hip-midpoint x/z speed, rising here as
the boxer starts moving into range:

| video_id | start_ms | end_ms | center_ms | movement |
|---|---|---|---|---|
| testvideo | 0 | 250 | 125 | 0.0428 |
| testvideo | 40 | 290 | 165 | 0.0684 |
| testvideo | 80 | 330 | 205 | 0.0941 |
| testvideo | 120 | 370 | 245 | 0.1240 |
| testvideo | 160 | 410 | 285 | 0.1533 |

### 5.2.11 Graph Generation Output (stage J)
`graph_metrics.py` inner-joins the three stages above on
`(video_id, center_ms)`:

| video_id | start_ms | end_ms | center_ms | punch_volume | guard_height | movement |
|---|---|---|---|---|---|---|
| testvideo | 0 | 250 | 125 | 0 | −0.0352 | 0.0428 |
| testvideo | 40 | 290 | 165 | 0 | −0.0399 | 0.0684 |
| testvideo | 80 | 330 | 205 | 0 | −0.0416 | 0.0941 |
| testvideo | 120 | 370 | 245 | 0 | −0.0427 | 0.1240 |
| testvideo | 160 | 410 | 285 | 0 | −0.0445 | 0.1533 |

The rendered plot (smoothed/downsampled guard height and movement, raw
punch volume, per §4.2.7) is `python/data/graph_metrics_plot.png`, already
in the repo.

## 5.3 Android App Outputs
_TODO — screenshots: capture/import screen, processing screen, the
skeleton-overlay playback synced to graphs, and the session history list._

## 5.4 On-Device Performance
_Draft — field test 2026-08-10 (`notes/field_test_c6_d_protocol.md`), n=3
devices/3 boxers. Needs polish + per-device breakdown before final copy._

End-to-end processing time (capture → pose extraction → classification →
graph generation), all on-device with no network dependency:
- A 15s clip processed in ~1–2 minutes, pooled across all three test
  devices (Samsung S20 FE, A36 5G, A50 — upper-mid to budget tier).
- A 60s clip processed in ~6–8 minutes on the S20 FE.

Both figures land at roughly 4–8x real-time, consistent in order of
magnitude with each other. The app also queued and successfully processed 4
videos back-to-back on a single device without failure. Clip length and
device are confounded across the two test batches (15s tested on all three
phones; 60s tested only on the S20 FE) — in-person testing is now complete,
so this stays a noted limitation on the performance figures above rather
than a gap to close; see §6.5.

## 5.5 Metric Validity
_Draft — same field test, n=3 boxers, 1 min session each on the S20 FE
(device held constant to isolate the person/stance variable, per
`notes/field_test_c6_d_protocol.md` Part 1)._

**Guard height and movement:** rated fully accurate by direct comparison
against the source video for all three boxers, with variation (guard
dropping/recovering around punches, footwork spikes in movement) clearly
readable on the rendered graph in every session.

**Punch volume**, checked against a manual live count of punches thrown:

| Boxer | Experience | Stance | Thrown (manual) | Recorded (app) | Recall |
|---|---|---|---|---|---|
| A | 3+ yrs | Orthodox | 76 | 47 | 61.8% |
| B | 1+ yrs | Southpaw | 43 | 14 | 32.6% |
| C | <1 yr | Orthodox | 29 | 26 | 89.7% |

Pooled recall: 87/148 = 58.8%. Zero false positives across all three
sessions — idle/no-activity stretches were consistently recorded as such.

The dominant error mode was undercounting rather than misclassification:
rapid combos of 2+ punches were often collapsed into a single recorded
punch, but every combo thrown registered as *at least* one punch — none
went entirely undetected. Recall tracked technique conformity rather than
punch speed, elaborated per boxer below.

**Boxer A (3+ yrs, orthodox) — 76 thrown, 47 recorded (61.8%):** rapid
2-punch combos were sometimes collapsed into a single recorded punch, but
every combo thrown — including longer 3–5 punch combinations — registered
as at least one punch and was visually distinguishable on the rendered
graph; none were missed outright. Idle stretches were correctly recorded as
no activity, and no false positives occurred, though this boxer was
continuously active for nearly the whole session, so the test gave few
opportunities to falsely fire during idle time in the first place. Guard
height and movement were both accurate.

**Boxer B (1+ yrs, southpaw) — 43 thrown, 14 recorded (32.6%):** the
clearest limitation case in the field test. Straight punches were mostly
detected; hooks and uppercuts were frequently missed. Idle periods were
still clearly visible on the graph and no false positives occurred, and
guard height/movement remained fully accurate and readable — the
undercounting was specific to punch classification, not a general tracking
failure. The pattern was attributed during testing to a combination of
southpaw stance and technique variation, consistent with the training set
(§5.2.4) containing no labelled southpaw footage: the classifier has
effectively never seen a southpaw's mirrored wrist/shoulder motion for
anything but straight punches.

**Boxer C (<1 yr, orthodox) — 29 thrown, 26 recorded (89.7%):** highest
recall of the three. Punches were thrown more slowly and with consistently
correct form throughout; a small number of 2-punch sequences were still
collapsed to one, the same pattern seen with Boxer A but at lower
frequency — plausibly because slower throws are less likely to land inside
the same 250 ms classification window. Idle periods were correctly
recorded and no false positives occurred. Guard height and movement were
fully accurate.

**Synthesis — usability by purpose:** recall correlated with technique
conformity and pace, not boxing experience — the least experienced boxer
(C) had the best punch-count accuracy because their punches were slower,
cleaner, and closest to the orthodox technique the training data
represents; the most experienced boxer (A) still undercounted because
rapid, tightly spaced combos are the hardest case for a fixed 250 ms
sliding window; the southpaw boxer (B) was undercounted for reasons
independent of skill or speed entirely. In its current state, the app is
most usable as a **volume/activity trend and guard-height/movement review
tool for orthodox boxers training at a moderate pace** — e.g. a beginner
drilling fundamentals, or a coach checking whether guard drops and footwork
happened when expected — and least reliable as a **precise punch counter
for fast elite-pace combination work or for southpaw boxers**, where
undercounting is frequent enough to misrepresent actual output. This maps
directly onto the punch-type-labelling and dataset-diversity limitations in
§6.5.

## 5.6 Error / Robustness Testing
_Draft — field test 2026-08-10 (`notes/field_test_c6_d_protocol.md` Part
2), plus incidental observations from the §5.5 sessions. Split out from
metric validity since these probe failure behavior under adversarial/edge
conditions rather than accuracy under normal use._

**Multi-person-in-frame (C6):** with two people shadowboxing simultaneously
in frame for 30s, pose extraction locked onto one person for the full clip
and computed metrics for that person only, without jumping between
subjects. Only the "two people boxing simultaneously" scenario from the
planned C6 protocol was run — with in-person testing now complete, the
brief walk-through and static-bystander variants were not tested; noted as
a scope limitation in §6.5 rather than an open action item.

**Entering/leaving frame:** tracking engages when a person enters frame and
stops cleanly when they leave, without producing garbage output in between;
brief partial occlusion (a wrist or foot leaving frame momentarily) is
estimated reasonably by MediaPipe rather than breaking the metric.

**Off-model motion:** kicks or punches thrown with technique dissimilar to
the training data's boxing punches were not counted as punches, while guard
height and movement continued tracking normally throughout. Read alongside
Boxer B's southpaw result in §5.5, this suggests the classifier is
conservative — it under-detects unfamiliar motion patterns rather than
hallucinating punches on unrelated movement — the safer failure direction
for an app whose output a boxer or coach might rely on, but it reinforces
the same training-data coverage limitation (§6.5).

None of the three conditions produced a crash, a silently-plausible-but-
wrong result, or an unrecoverable app state — every failure mode observed
was under-detection, not fabrication.

## 5.7 User-Centered Evaluation
_Draft — questionnaire responses collected 2026-08-10 immediately after
each boxer's session; raw data in `notes/tester_questionnaire.md`. n=3 —
not a statistically powered sample per the caveat in that file; scores
below are directional, read alongside the open-ended answers and the
objective results in §5.5/§5.6._

Install → record → process → review ran successfully end-to-end on all
three boxers' own devices, including previous-session history, thumbnails,
and playback — no crashes across any of the three real-device sessions, the
three body-type/stance sessions, or the error-condition tests in §5.6.

**Likert results (1–5, 5 = strongly agree):**

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

Overall mean 4.83/5 across all 30 responses. Item 5 (punch count matched
what was thrown) is the only item scored below 5 by more than one tester,
and the lowest-scoring item overall (mean 4.0) — Boxer B, whose objective
recall was also lowest (32.6%, §5.5), rated it 3; Boxer A, whose recall gap
came from combo-collapsing rather than missed punches (61.8%), rated it 4.
That's a direct convergence between the subjective usability rating and the
objective punch-recall measurement, not two disconnected results. Item 8
(trust in the feedback for technique review) follows the same pattern —
Boxer B alone scored it 4 instead of 5, the one tester whose punches were
most often missed also being the one least willing to fully trust the
app's feedback.

**Open-ended answers** were consistent with the same pattern. Boxer A and
Boxer C — the two orthodox boxers with higher recall — reported nothing
unclear or surprising, and both independently asked for the same missing
feature: a breakdown by **punch type** (jab/cross/hook/uppercut), not
improved raw accuracy. Boxer B — the southpaw with the lowest recall — was
the only tester to report a concrete problem ("punches not detected") and
had no strong opinion on what to add next ("idk"), consistent with an
unresolved accuracy gap crowding out interest in additional features. This
lines up directly with the existing "punch type not labelled" limitation
and the southpaw training-data gap (both §6.5): the two testers whose core
experience already worked well want the next layer of detail; the tester
whose core experience didn't work as well wants the foundation fixed
first.

## 5.8 Summary
_TODO — once §5.1's roadmap and the remaining screenshot/example-image
TODOs (§5.2.1, §5.2.8–§5.2.11, §5.3) are filled in. All evaluation numbers
(§5.2.3, §5.2.6, §5.4–§5.7) are final._
