# Change tracking — old thesis vs. current draft

**Superseded as the source of truth.** Every chapter file now carries its
own `_Status: UNCHANGED/CHANGED/NEW_` note directly above each section
heading; check the chapter file itself, not this table, for the current
status of any given section. This file is kept as a standalone bird's-eye
summary and as the record of the original classification pass.

Section-by-section classification of every chapter against its old-thesis
(or old-article) source. Two statuses only, per the request that produced
this file:

- **UNCHANGED** — verbatim or near-verbatim (trivial factual updates only,
  no rewording of sentence structure).
- **CHANGED** — reworded, condensed, restructured, or has no old-thesis
  counterpart at all (new content). All of these get the em-dash-removal +
  old-thesis-style pass.

**Correction to the chapter headers' own claims:** `02_literature_review.md`
and `03_methodology.md` both say their reused sections are "verbatim" or
"reused." Line-by-line comparison against the actual old files shows this
isn't accurate — the prose was condensed and reworded throughout (dropped
the "In the study, *Title*, Author (year)..." framing, merged
strength/weakness paragraphs, cut inline tables to pointers, added ~130 em
dashes where the old thesis has zero). The old thesis (all 8 files) and the
old article's methodology/conclusion sections contain **no em dashes at
all**; the old article's other two sections have 7 combined. The six current
chapter files contain **219** between them. That gap is itself the main
signal used below to separate "genuinely reused" from "rewritten and
mislabeled as reused."

## Chapter 1 — Introduction (`01_introduction.md`)
| § | Status | Note |
|---|---|---|
| 1.1 Background | **UNCHANGED** | Verbatim except 2 factual tweaks: dropped "3D" before "pose estimation," and the final sentence's metric names ("form accuracy, activity levels, fatigue trends" → "punch volume, guard consistency, movement activity") to match what was actually built. |
| 1.2 Problem Statement | CHANGED | Two old sentences merged into one via em dash; dropped "Forbes highlights" attribution; fixed a stat typo (3.69110 → 3.69/5). |
| 1.3 Research Question | CHANGED | Minor wording only: dropped "3D," "several training rounds" → "across a training session." No em dash. |
| 1.4 Research Objective | CHANGED | Substantially rewritten — old objectives were generic (investigate/develop/evaluate); new ones name the actual deliverables. |
| 1.5 Scope of Research | CHANGED | Bullets edited + new inline editorial annotations added explaining each change; one bullet (visualization) substantively changed. |
| 1.6 Thesis Outline | CHANGED | Fully rewritten for the 6-chapter structure (old had 5). |

## Chapter 2 — Literature Review (`02_literature_review.md`)
| § | Status | Note |
|---|---|---|
| 2.1 Introduction | CHANGED (minor) | First two sentences verbatim; final sentence rewritten to point at new §2.2 instead of the old "strengths/weaknesses" framing. |
| 2.2 Boxing Fundamentals (2.2.1–2.2.4) | **NEW** | No old-thesis equivalent. Loosely mirrors the *structure* of `reference_thesis_outline.md` §2.1 but the prose is original and still needs the citation flagged in its own draft note. |
| 2.3 Pose Estimation Studies (2.3.1–2.3.6) | CHANGED | Despite the file header's "reused verbatim" claim, this is a condensed rewrite of old §2.2 — dropped the "In the study, *Title*..." lead-in, merged each study's two paragraphs, cut per-study tables to pointers, added em dashes throughout. |
| 2.4 Random Forest Studies (2.4.1–2.4.4) | CHANGED | Same treatment as 2.3, condensed from old §2.3. |
| 2.5 Comparative Analysis of Existing Applications (2.5.1–2.5.4) | CHANGED | Condensed from old §2.4; dropped figure references and some detail. |
| 2.6 Data Analysis from Pilot Study (2.6.1–2.6.3) | CHANGED | Condensed from old §2.5; dropped figure references, merged sentences. |
| 2.7 Summary | CHANGED | Condensed/reworded from old §2.6, plus one new paragraph pointing at §2.2. |

## Chapter 3 — Methodology (`03_methodology.md`)
All of Chapter 3 is **CHANGED** — the plan itself changed (Python-first,
on-device port, no 3D SceneView skeleton, no Olympic Boxing Punch dataset,
no TensorFlow Lite conversion), so almost nothing could stay verbatim even
where the old section title matches.
| § | Note |
|---|---|
| 3.1 Introduction | Rewritten stage list and framing (Python prototype → Android port, not the old 6-stage prototyping list). |
| 3.2 Research Design | MediaPipe Pose Landmarker replaces BlazePose/TFLite framing; adds the "validate in Python first" decision (new). |
| 3.3.1 Data Collection | Dropped the Olympic Boxing Punch Dataset; condensed. |
| 3.3.2 MediaPipe Landmark Extraction | Dropped 30fps-normalization detail; rewritten around CSV + review-video output. |
| 3.3.3 Manual Punch Labelling | Restructured — labelling audit explicitly moved out to §3.6.2 (new organizational decision, no old equivalent in this exact form). |
| 3.3.4 Feature Engineering | Rewritten: old's 3-category guard/punch/footwork framing replaced with the actual wrist/elbow motion features. |
| 3.3.5 Random Forest Model Development | Dropped TFLite conversion + hyperparameter-tuning table; dropped multi-task (guard/punch/movement) framing. |
| 3.3.6 Python-Side Inference Validation | **NEW** — no old equivalent; this methodological step (validate before porting) didn't exist in the old plan. |
| 3.3.7–3.3.9 Punch Volume / Guard Height / Movement Computation | **NEW** — split out of old's single combined "3D and Graph Visualization" §3.3.5; actual formulas are new. |
| 3.3.10 Graph Generation | **NEW** — not separated out in the old plan. |
| 3.4.1 System Architecture | Dropped 3D SceneView/TFLite architecture; "two cooperating phases" framing is new. |
| 3.4.2 Data Schema | Schema updated to match the actual pipeline; dropped `GuardHeight`/`PunchMotion`/`MovementVar` column names from the old plan. |
| 3.4.3 Interface & UX Flow | File header claims this is "reused... still accurate," but it's actually a heavily condensed 2-sentence version of old's numbered i)/ii)/iii) list, with the SceneView-specific "session setup/calibration" step and 3D skeleton removed. Reclassify as CHANGED. |
| 3.5.1 Data Sources and Recording Protocol | Dropped Olympic dataset references; otherwise close. |
| 3.5.2 Evaluation Metrics & Procedures | Dropped RMSE/joint-error and guard-accuracy metrics from old; replaced with punch precision/recall/F1 + manual tally + Likert. |
| 3.5.3 Testing Conditions & Controls | Minor addition (bare-chested/clothing cross-reference to §3.6.3). |
| 3.6.1 Model Accuracy Evaluation | Adds the grouped-split methodology explanation (new reasoning, not in old). |
| 3.6.2 Label Quality Auditing | **NEW** as a standalone evaluation subsection (moved in from construction). |
| 3.6.3 Pose Quality and Visualization Validation | Dropped RMSE-vs-motion-capture approach from old; now about tracking stability + multi-person locking. |
| 3.6.4 User-Centered Evaluation | Condensed from old §3.7.3; similar substance. |
| 3.7 Summary | Rewritten to reflect the actual (not originally planned) pipeline. |

## Chapter 4 — Implementation (`04_implementation.md`)
The **entire chapter is new** — the old thesis had no standalone
Implementation chapter (old §3.5 was a thin Methodology subsection covering
SceneView/Maya/TFLite, none of which shipped). Sub-classification:
| § | Status | Note |
|---|---|---|
| 4.1 Development Environment & Tools | **NEW** | Table format echoes old §3.5.1's table shape, but every row is a different technology (RF+m2cgen+Room vs. old's SceneView+TFLite+Maya). |
| 4.2.1–4.2.5 (Pose Extraction, Labelling/Auditing, Feature Engineering, RF Training, Inference) | CHANGED (adapted) | Close paraphrase of `old versions/old article/03_meth.md` §B–F — same facts and structure, condensed with em dashes added. Good candidate for restoring closer to the article's own (dash-free) phrasing. |
| 4.2.6 Punch Volume / Guard Height / Movement Computation | **NEW** | The article covered the labelling pipeline only, not these three metrics. |
| 4.2.7 Graph Generation | **NEW** | No article/old-thesis equivalent. |
| 4.3 Android Implementation (4.3.1–4.3.5) | **NEW** | No old-thesis or article equivalent — the Android app didn't exist yet when either was written. |
| 4.4 Differences Between Prototype and Android App | **NEW** | |
| 4.5 Summary | **NEW** | |

## Chapter 5 — Results: Outputs, Evaluation and Testing (`05_results_and_evaluation.md`)
| § | Status | Note |
|---|---|---|
| 5.1 Introduction | TODO stub | Not yet written. |
| 5.2.1–5.2.4 (Pose extraction, labelled windows, label audit, training dataset) | CHANGED (adapted) | Close paraphrase of `old article/04_res.md` §A–D, same tables/numbers, em dashes added in the connecting prose. |
| 5.2.5 Trained Classifier | CHANGED (adapted) | Paraphrase of article §E. |
| 5.2.6 Model Accuracy & Evaluation | **NEW** | Article evaluated the labelling pipeline only, not classifier accuracy — this is genuinely new content (2026-08-10 evaluation). |
| 5.2.7 Predictions from New Footage | CHANGED (adapted) | Paraphrase of article §F. |
| 5.2.8–5.2.11 (Punch Volume / Guard Height / Movement / Graph Generation outputs) | **NEW** | Article didn't cover these metrics at all. |
| 5.3 Android App Outputs | TODO stub | Not yet written (screenshots pending). |
| 5.4 On-Device Performance | **NEW** | Real field-test data, no old equivalent. |
| 5.5 Metric Validity | **NEW** | |
| 5.6 Error / Robustness Testing | **NEW** | |
| 5.7 User-Centered Evaluation | **NEW** | Neither old thesis (pre-implementation) nor article covered this. |
| 5.8 Summary | TODO stub | |

## Chapter 6 — Conclusion (`06_conclusion.md`)
| § | Status | Note |
|---|---|---|
| 6.1 Introduction | TODO stub | |
| 6.2 Summary of the Research Objectives | TODO stub | |
| 6.3 Research Contributions | CHANGED | Drafted fresh as a bullet list, drawing on old §5.3's prose contributions plus the article's discussion — not a reuse of either's actual sentences. |
| 6.4 Practical Implications and Beneficiaries | TODO stub | Draft note says old §5.4 is "largely still applicable" — pending final drafting. |
| 6.5 Limitations of the Present Study | **NEW** | Old §5.5 was 3 generic, pre-implementation sentences; this is 9 detailed, evidence-backed limitations from real results. |
| 6.6 Future Works | CHANGED | Partial reuse of old ideas (additional metrics, real-time feedback, other sports) combined with new items (re-run TFLite comparison, label more videos) from the article's own future-work section. |
| 6.7 Summary | TODO stub | |

## Priority order for the style/em-dash pass
1. Ch. 1 — smallest, only §1.2/1.5/1.6 need touching.
2. Ch. 4 §4.2 and Ch. 5 §5.2.1–5.2.4/5.2.7 — these are paraphrases of
   dash-free source material (the old article), so restoring old-thesis
   phrasing is the most mechanical and lowest-risk of the whole set.
3. Ch. 2 §2.3–2.7 — high em-dash density, but same story: a dash-free old
   source (`2_old_litrev.md`) exists to restore toward.
4. Ch. 3, Ch. 4 §4.3–4.5, Ch. 5 §5.2.6/5.2.8–5.2.11/5.4–5.7, Ch. 6 §6.3/6.5/6.6
   — genuinely new content with no old-thesis sentences to restore toward;
   these just get de-dashed and reworded into the old thesis's plainer,
   fully-spelled-out declarative register.
