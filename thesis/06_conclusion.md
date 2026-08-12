# 6. Conclusion and Recommendations for Future Research

Structure reused from `old versions/old thesis/5_old_conc.md`; content
updated to reflect what was actually built (Chapters 4–5) rather than the
pre-implementation plan. Contribution and limitation candidates below also
draw on `old versions/old article/05_conc_ref.md`'s discussion section,
which already did this analysis for the labelling pipeline specifically.

## 6.1 Introduction
_TODO — adapt old thesis §5.1 once the results in §5.2 (label audit, model
accuracy) and §5.4–§5.7 exist to reference._

## 6.2 Summary of the Research Objectives
_TODO — revisit the three research objectives from §1.4 against what was
actually delivered (old thesis §5.2 listed them as achieved prematurely,
before implementation; now there's real evidence to cite instead)._

## 6.3 Research Contributions
Candidates, carried over/updated from the old thesis (§5.3) and the
article (which independently reached some of the same conclusions for the
labelling pipeline specifically):
- A camera-only, hardware-free boxing performance tracker running entirely
  on-device, addressing the accessibility gap identified against Jabbr/
  Growl/Tempo in §2.5.
- A documented, evaluated manual-labelling-plus-audit pipeline for turning
  raw pose landmarks into a punch-detection training set (§4.2.2, §5.2.3) —
  the article notes this specifically as something no reviewed boxing or
  Random-Forest study documents or validates for their own training data.
- The video-grouped train/validation split finding (§3.6.1, §4.3.2, §5.2.6):
  a naive per-row split on windowed pose data silently leaks near-duplicate
  windows across train/validation and produces a misleadingly high accuracy
  figure — a methodological point relevant beyond this project to anyone
  windowing time-series pose data for classification. Directly demonstrated
  in `python/evaluate_random_forest.py` (§5.2.6): a single grouped split on
  this project's own 111-video dataset still swung between 32.9% and 91.3%
  accuracy across 15 random seeds, showing that grouping alone isn't
  sufficient at small dataset scale — pooled cross-validation is needed too.
- A worked example of porting a validated classical ML model directly to
  on-device native code (RandomForest via m2cgen, §4.3.2) as an alternative
  to independently retraining a smaller on-device model, backed by a
  concrete accuracy comparison: the RF's cross-validated 69.4%
  accuracy/0.692 macro F1 (§5.2.6) vs. the abandoned TFLite network's
  ~50%/chance-level result. (The TFLite figure's own split methodology
  could not be confirmed as grouped when re-checked — §5.2.6,
  `notes/decisions.md` — so the directional conclusion holds but a fully
  rigorous side-by-side would re-run TFLite with matched cross-validation.)

## 6.4 Practical Implications and Beneficiaries
_TODO — adapt old thesis §5.4 (boxers/athletes, coaches, mobile developers/
AI practitioners) — largely still applicable, reused with light editing
once final results are in._

## 6.5 Limitations of the Present Study
- **Dataset size and diversity.** Only 175 labelled punch windows across 15
  usable videos contributed to the final training set, out of 163 videos
  with pose extracted and 49 manually reviewed (§5.2.3) — most training data
  came from an uncurated public dataset (UCF101) rather than footage
  collected specifically for this project, which directly caused the low
  (31%) usable-video yield.
- **Single annotator.** All manual punch labelling was performed by one
  reviewer; the audit and duration-consistency checks (§5.2.3) provide
  indirect evidence of internal consistency, but inter-rater reliability
  cannot be reported, since no second annotator's labels exist for
  comparison.
- **Punch type not labelled.** Due to time constraints, the labelling
  schema distinguishes punch vs. no_punch only — not straight/hook/
  uppercut — so the system cannot yet support technique-level analysis,
  only punch-volume/timing analysis.
- **No 3D visualization.** The originally planned rigged 3D skeleton viewer
  (old thesis §3.3.5) was dropped in favor of a 2D skeleton-overlay video,
  due to time constraint relative to getting the detection/metrics pipeline
  working end to end — a scope cut, not a technical failure (see
  `notes/decisions.md`).
- **Landmark detection quality.** The system depends on MediaPipe's
  landmark detection, which degrades under occlusion, rapid movement, or
  poor lighting. The multi-person field test (`notes/field_test_c6_d_protocol.md`,
  results in §5.6) found this to be less severe than expected in practice:
  with two people in frame, tracking locked onto one subject for the full
  clip rather than jumping between people or producing garbage output, and
  brief partial occlusion (a wrist or foot leaving frame) was estimated
  reasonably rather than breaking downstream metrics.
- **Southpaw/off-orthodox technique under-detection.** The training set
  (§5.2.4) contains no labelled southpaw footage. In the field test (§5.5),
  the one southpaw participant's punch recall (32.6%) was far below the two
  orthodox participants' (61.8%, 89.7%), with straight punches mostly caught
  but hooks and uppercuts frequently missed — consistent with a genuine
  stance/technique coverage gap in training data rather than a general model
  weakness. This is the most direct, numbers-backed limitation from the
  field test and the clearest target for future data collection (§6.6).
- **Combo-collapsing (punch undercounting).** Across all three field-test
  participants, rapid combos of 2+ punches were often recorded as a single
  punch on the 250 ms sliding window, though no combo went entirely
  undetected. Pooled recall across all three sessions was 58.8% (87/148),
  with zero false positives observed. The 250 ms window size (chosen in
  §4.2.3 based on labelled punch duration statistics, §5.2.3) trades off
  against distinguishing closely spaced punches within a combo; a shorter
  window or a non-sliding peak-detection approach could recover this at the
  cost of the duration-distribution justification in §5.2.3.
- **Modest, sample-size-sensitive classifier accuracy.** Even under a
  grouped, cross-validated evaluation (§5.2.6), the RF classifier scores
  69.4% accuracy / 0.692 macro F1 — solidly above chance and above the
  abandoned TFLite alternative, but far from reliable enough for precise
  punch counting, consistent with the combo-collapsing and southpaw
  under-detection observed directly in the field test above. The 111-video
  dataset is also small enough that a single train/test split can swing
  ±30 points depending on which videos are held out (§5.2.6) — more
  labelled data (§6.6) would raise accuracy and stabilize it.
- **Incomplete multi-person and on-device performance test coverage.** The
  C6 field test protocol (`notes/field_test_c6_d_protocol.md`) planned four
  multi-person-in-frame scenarios; only "two people boxing simultaneously"
  was run (§5.6) before in-person testing concluded, so the brief
  walk-through and static-bystander cases remain unverified. Similarly, the
  on-device performance figures (§5.4) confound clip length and device
  (15s tested across three phones, 60s tested only on one), so no clean
  per-device, matched-length comparison exists. Both are closed items now
  that testing is complete, not gaps to be filled before submission.

## 6.6 Future Works
- Extend the label schema to distinguish punch types (straight, hook,
  uppercut), and/or add hip rotation as a tracked feature to capture a
  biomechanically important component of punching technique the current
  wrist/elbow-centred feature set doesn't directly represent (both
  suggested independently in the article's own future-work section).
- Label additional UCF101 videos beyond the 15 currently used, and/or
  source cleaner footage, to directly improve dataset size and offset the
  yield limitation in §6.5.
- Revisit 3D visualization now that the core pipeline is validated and
  ported — the original plan (rigged skeleton, synchronized playback) is
  still a reasonable target, just deferred rather than abandoned.
- Real-time, in-session feedback (e.g. live guard-drop alerts), rather than
  the current post-session review model.
- Extend the pose-based pipeline beyond boxing to other striking sports.
- Re-run the abandoned TFLite comparison (§5.2.6) with the same grouped
  cross-validation methodology used for the RF classifier, so the
  RF-vs-TFLite ablation is fully apples-to-apples rather than a rigorous
  number for one side and a less-certain one for the other.

## 6.7 Summary
_TODO — once the above sections are finalized._
