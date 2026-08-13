# 6. Conclusion and Recommendations for Future Research

Structure reused from `old versions/old thesis/5_old_conc.md`; content
updated to reflect what was actually built (Chapters 4–5) rather than the
pre-implementation plan. Contribution and limitation candidates below also
draw on `old versions/old article/05_conc_ref.md`'s discussion section,
which already did this analysis for the labelling pipeline specifically.

_Status: NEW. Drafted now that Chapter 5's results exist to reference._
## 6.1 Introduction
This chapter closes the thesis by drawing together the results reported in
Chapter 5. §6.2 revisits the three research objectives set out in §1.4
against what Chapters 4 and 5 actually delivered. §6.3 states the
contributions this project makes beyond a working boxing-tracking app,
methodological points relevant to anyone building a similar pose-based
classification pipeline. §6.4 considers who benefits from the system as
built and how. §6.5 states the study's main limitations, and §6.6 sets out
the future work that follows directly from them.

_Status: NEW. Drafted with real evidence from Chapters 4-5, replacing the
old thesis's pre-implementation "objectives achieved" framing._
## 6.2 Summary of the Research Objectives
All three research objectives set out in §1.4 were met, with concrete
evidence rather than the pre-implementation projection the old thesis
relied on.

Objective (i), a pose-landmark extraction and labelling algorithm
producing a punch-labelled training dataset, was delivered as the Stage
B-D Python pipeline (§4.3.1-§4.3.3): MediaPipe-based extraction across 163
UCF101 videos, manual labelling with automated quality auditing
(§5.2.2-§5.2.3), and feature engineering into a balanced 350-row training
set (175 punch / 175 no_punch windows) drawn from the 15 videos that
passed review.

Objective (ii), an AI-driven boxing performance tracker using a Random
Forest classifier ported for on-device use, was delivered as the trained
classifier (§4.3.4, §5.2.5-§5.2.6) transpiled via m2cgen and running
natively in the Android app (§4.4.2) with no network dependency,
processing footage at roughly four to eight times real-time (§5.4).

Objective (iii), evaluating the system across punch volume, guard height,
and movement through model evaluation and real boxer testing, was
delivered through the cross-validated classifier evaluation (§5.2.6, 69.4%
accuracy / 0.692 macro F1) and the three-boxer field test (§5.5-§5.7):
guard height and movement tracked accurately for all three testers, punch
volume recall averaged 58.8%, with clear stance- and pace-dependent
variation, and overall usability was rated 4.83 out of 5.

_Status: CHANGED. Drafted fresh, drawing on old §5.3's prose contributions plus the article's discussion; not a reuse of either's actual sentences. Language check: the third point's colon-then-independent-clause and semicolon split into separate sentences. Converted from a bullet list to paragraphs, per the user's request._
## 6.3 Research Contributions
This project's contributions, carried over and updated from the old thesis
(§5.3) and the article, which independently reached some of the same
conclusions for the labelling pipeline specifically, extend beyond a
working app in four ways.

The system is a camera-only, hardware-free boxing performance tracker
running entirely on-device, addressing the accessibility gap identified
against Jabbr, Growl, and Tempo in §2.5.

It also delivers a documented, evaluated manual-labelling-plus-audit
pipeline for turning raw pose landmarks into a punch-detection training
set (§4.3.2, §5.2.3). The article notes this specifically as something no
reviewed boxing or Random-Forest study documents or validates for their
own training data.

A further contribution is the video-grouped train/validation split
finding (§3.6.3, §4.4.2, §5.2.6). A naive per-row split on windowed pose
data silently leaks near-duplicate windows across train/validation and
produces a misleadingly high accuracy figure, a methodological point
relevant beyond this project, to anyone windowing time-series pose data
for classification. `python/evaluate_random_forest.py` (§5.2.6)
demonstrates this directly. A single grouped split on this project's own
111-video dataset still swung between 32.9% and 91.3% accuracy across 15
random seeds, showing that grouping alone is not sufficient at small
dataset scale, and that pooled cross-validation is needed too.

Finally, the project provides a worked example of porting a validated
classical ML model directly to on-device native code (RandomForest via
m2cgen, §4.4.2) as an alternative to independently retraining a smaller
on-device model, backed by a concrete accuracy comparison: the RF's
cross-validated 69.4% accuracy/0.692 macro F1 (§5.2.6) vs. the abandoned
TFLite network's ~50%/chance-level result. The TFLite figure's own split
methodology could not be confirmed as grouped when re-checked (§5.2.6,
`notes/decisions.md`), so the directional conclusion holds, but a fully
rigorous side-by-side would re-run TFLite with matched cross-validation.

_Status: CHANGED. Old thesis §5.4's three beneficiary groups (boxers/
athletes, coaches, mobile developers/AI practitioners) held up; each is now
grounded in a real Chapter 5 result instead of a pre-implementation claim._
## 6.4 Practical Implications and Beneficiaries
This system has practical value for three groups.

**Boxers and athletes** get low-cost, hardware-free feedback on training
volume, guard consistency, and movement activity directly from a
smartphone recording, without needing a coach present or specialized
sensors (§1.1-§1.2). The field test (§5.5-§5.7) showed this is most useful
today as a trend and review tool, guard-height and movement graphs the
boxer can check against their own memory of the session, rather than as a
precise punch counter, particularly for a boxer training at a fast pace or
fighting southpaw (§6.5).

**Coaches** gain an objective, timestamped record of a session that can be
reviewed alongside the boxer without requiring the coach's own presence
during training, useful for remote or asynchronous coaching relationships
and for spotting guard drops or reduced movement over the course of a
session that might otherwise be missed in real time.

**Mobile developers and AI practitioners** building similar on-device,
pose-based classification systems benefit from two concrete,
evidence-backed methodological findings from this project (§6.3): the
video-grouped cross-validation requirement for windowed pose data, and the
worked comparison between porting a validated classical model (Random
Forest via m2cgen) and training a smaller model from scratch for on-device
deployment.

_Status: CHANGED. Consolidated from 9 evidence-backed bullets into the 3
main limitations the study is scoped around, so the section reads as a
short, honest account rather than an exhaustive list of every observed
imperfection. Every factual point from the previous 9 bullets is folded
into one of the 3 below, except the 3D-visualization scope cut, which is
already documented elsewhere as a deliberate cut rather than a
performance limitation (§4.6, `notes/decisions.md`); it was originally
also tracked as a §6.6 future-work item, but no longer is, per the
user's decision to drop it (see the status note preceding §6.7). Language
check: a colon-then-independent-clause in the second
point and a semicolon plus a comma splice in the third point split into
separate sentences. Second point reworked: it previously covered only
recall/undercounting; it now leads with the intended meaning, that
detected punch timing is not always exact, grounded in the 250 ms
window / 40 ms stride merging behavior (§4.4.2), with recall kept as a
second, separately measured effect of the same fixed-window approach.
Converted from a bullet list to paragraphs, per the user's request._
## 6.5 Limitations of the Present Study
This study has three main limitations.

The first is the lack of a benchmark against comparable commercial apps.
This thesis evaluates the system against ground truth, manual punch
counts and direct video comparison in the field test (§5.5), and
cross-validated held-out accuracy (§5.2.6), rather than against existing
camera-based boxing trackers such as Jabbr, Growl, or Tempo (§2.5). No
head-to-head comparison was run, so it is not yet possible to state how
this system's accuracy or usability compares to those existing products.
The accessibility contribution, a camera-only, hardware-free tracker, is
established on its own terms (§6.3), but a relative benchmark against
competing apps is not. Running one would require the same footage and a
shared evaluation protocol across products, neither of which was
available within the scope of this project.

A second limitation is that outputs do not yet perfectly synchronize with
ground truth. Guard height and movement tracked closely with the source
video for all three field-test boxers (§5.5), but punch-related outputs
did not, in two ways. A detected punch's reported timing is not always
the exact moment of impact, since classification runs on a fixed 250 ms
window (40 ms stride) merged into a span rather than pinpointing a single
instant (§4.4.2), most likely a consequence of this window-based
approach. Separately, pooled punch-count recall was only 58.8% (§5.5),
driven mainly by combo-collapsing and by the training set containing no
southpaw footage. Both trace back to the same fixed-window classifier,
whose cross-validated accuracy (69.4%, §5.2.6) is solidly above chance
but not yet reliable enough for exact counting or precise timing.

A third limitation is the small training and testing scale. Only 175
labelled punch windows across 15 usable videos, out of 163 videos with
pose extracted and 49 manually reviewed (§5.2.3), contributed to the
final training set, since most source footage came from an uncurated
public dataset (UCF101) rather than footage collected specifically for
this project. The same constraints kept the labelling schema to
punch/no_punch only, without distinguishing punch types, and left all
labelling to a single annotator. Results are correspondingly sensitive to
which videos are held out, with a single train/test split swinging more
than ±30 points in accuracy depending on the split (§5.2.6). The field
test itself was similarly small in scale (n=3 boxers), so only a subset
of planned multi-person scenarios and on-device performance comparisons
were exercised (§5.6).

_Status: CHANGED. Consolidated from 7 mixed-scope bullets into the same 3
main future-work directions the study is organized around, mirroring the
§6.5 consolidation. The benchmark item, the 3D-visualization revisit, the
real-time-feedback item, the other-sports item, and the TFLite re-run item
are folded out of the list; see the status note preceding §6.7 for what
happened to each. Converted from a bullet list to paragraphs, per the
user's request._
## 6.6 Future Works
Three future-work directions follow from these limitations and from the
system as built.

The first is to grow the training dataset. Classifier accuracy could
improve by reviewing and labelling more of the 148 UCF101 videos not
currently used (§5.2.3, §6.5), rather than relying on the 15 that passed
review so far, since the majority of pose-extracted footage was never
reviewed for punches at all. This alone would directly address the
small-training-scale limitation above (§6.5) without needing a new data
source. A second, complementary path is adding self-recorded videos
filmed under controlled conditions, similar to the smartphone footage
already reserved for testing (§3.3.1), specifically covering technique
the current dataset lacks, such as southpaw stance. Both target the same
root cause: too few examples covering too narrow a range of technique.

A second direction is to extend the same pose-estimation foundation to
more boxing-specific metrics. Detecting distinct poses is the core
strength pose estimation offers over simpler motion tracking, and this
project's pipeline already separates boxing-specific motion from
engineered landmark features well enough to distinguish punch from
no_punch (§4.3.3, §5.2.6). The natural next step is to track a wider
range of poses and metrics on that same foundation rather than building a
new one: punch types (straight, hook, uppercut), which would mean
expanding the current punch/no_punch labelling schema (§5.2.4) to a
multi-class one, chin position as a guard indicator, and head movement
such as slipping and rolling. Each of these is, at its core, a pose the
same landmarks already capture, just not yet labelled or engineered as a
separate feature.

A third direction is to move from assisted review toward automatic trend
detection. The app currently presents guard height, movement, and punch
volume graphs for a boxer or coach to interpret themselves (§4.4.5),
which is useful but places the entire burden of noticing a pattern on
the person reviewing it. A further step would have the app identify
trends directly, using either a simple algorithmic rule over the same
per-session metrics already stored in Room (§4.4.4), such as a guard that
drops further as a session progresses, or a small model trained across
many stored sessions to flag a punch volume that declines across
combinations. Either approach surfaces the trend to the boxer directly,
rather than requiring manual comparison across sessions.

_Status: CHANGED. Drafted now that the rest of the chapter is finalized.
Language check: an opening semicolon split into separate sentences.
Updated to reference the consolidated 3-item §6.6 rather than the
individual old bullets (benchmark, more data, TFLite re-run), since the
mapping from the 3 limitations to the 3 future-work items is no longer a
clean one-to-one correspondence; the sentence no longer claims each
future-work item directly answers a specific limitation. The
3D-visualization revisit, previously tracked only as a §6.6 bullet
(§4.6, `notes/decisions.md`), no longer has a place it is mentioned as
future work; flagged for the user rather than silently dropped._
## 6.7 Summary
This chapter has closed the thesis by revisiting the three research
objectives set out in §1.4 against the evidence produced in Chapters 4 and
5. All three were met, moving from a pre-implementation plan to a
validated, on-device boxing performance tracker built around a Random
Forest classifier and evaluated on real boxers (§6.2). The project's
contributions extend beyond the app itself: an evaluated
manual-labelling-and-audit pipeline, a video-grouped cross-validation
finding relevant to any windowed pose-classification task, and a worked
comparison between porting a validated classical model and training a new
one from scratch for on-device use (§6.3), while boxers, coaches, and
other developers building similar systems stand to benefit from the
working app and its documented methodology (§6.4). Three limitations, the
lack of a direct benchmark against comparable commercial apps, outputs
that do not yet perfectly synchronize with ground truth, and a small
training and testing scale, set clear boundaries on what can currently be
claimed (§6.5). §6.6 sets out where the project goes next: growing the
training dataset, extending the same pose-estimation foundation to a
wider set of boxing-specific metrics, and moving from assisted review
toward automatic trend detection.
