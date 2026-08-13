# 2. Literature Review

Status notes above each section below mark UNCHANGED vs. CHANGED relative
to the old thesis (`old versions/old thesis/2_old_litrev.md`, old §2.1–2.6),
which the user keeps separately for reference. Renumbering alone doesn't
count as a change. Per-study comparison tables aren't duplicated here since
they're unchanged; see the old file directly.

_Status: CHANGED (final sentence only, updated to point at the new §2.2)._
## 2.1 Introduction
Recent advancements in Artificial Intelligence (AI), particularly
convolutional neural networks (CNNs), have enabled markerless pose
estimation directly from standard RGB video. This unlocks a more accessible
way to track movement, technique, and physical performance without
specialised hardware.

Within striking sports such as boxing, pose estimation research is growing
but still relatively limited. Much of the existing work focuses on isolated
aspects of performance, such as punch count for judging matches, rather than
providing a time-based analysis of technical degradation or fatigue across
rounds. This chapter reviews prior studies that form the foundation for this
research project, then establishes the boxing-domain background (§2.2)
needed to understand why the metrics used later in this thesis were chosen.

_Status: NEW. No old-thesis equivalent; loosely mirrors the structure of
`old versions/reference_thesis_outline.md` §2.1 but the prose is original.
§2.2.1 is now cited (Atif, 2025); see `notes/references.md`._
## 2.2 Boxing Fundamentals

_Status: NEW (part of §2.2). Citation added: Atif (2025)._
### 2.2.1 Overview of Boxing and Rules
Boxing is a combat sport in which two competitors, matched by weight class,
attempt to strike each other above the waist using only closed fists,
inside a roped ring, while avoiding return strikes (Atif, 2025). Bouts are
divided into timed rounds: three 3-minute rounds with one-minute rest
intervals in amateur boxing, up to twelve 3-minute rounds in professional
boxing. A bout ends by knockout, when a boxer fails to rise from a
knockdown within a ten-count, technical knockout, when the referee or a
ringside doctor judges a fighter unable to continue safely, or a judges'
decision. Professional bouts are scored under the ten-point must system,
where the round winner receives ten points and the opponent nine or fewer;
amateur bouts are scored more directly on clean, effective punches. In both
formats, judges evaluate clean punching, effective aggression, ring
generalship, and defense (Atif, 2025).

Because scoring is explicitly tied to countable landed punches and
sustained work-rate, quantities like punch count and pacing are not just
convenient training metrics; they map directly onto how a bout is actually
judged. This is the underlying reason punch volume (§2.2.4) is treated as a
first-class metric in this project rather than an incidental one.

_Status: NEW (part of §2.2). Trimmed to only the terms referenced
elsewhere in this thesis (stance and guard removed)._
### 2.2.2 Key Body Parts and Terminology
These are the important body terms for boxing that will be referenced
throughout this thesis:
- **Fist:** the closed hand, the only legal striking surface in boxing;
  strikes with the forearm, elbow, or an open glove are not permitted
  (Atif, 2025).
- **Wrist:** connects the fist to the forearm; wrist position and velocity
  are a primary signal this project uses to detect a punch (§3.3.4 /
  §4.3.3).
- **Elbow:** the joint that extends to drive the fist outward during a
  punch; elbow velocity relative to the shoulder is tracked alongside the
  wrist for the same purpose.
- **Shoulder:** the joint the arm rotates and extends from; shoulder
  position is the reference point used to measure how far the fist and
  wrist extend during a punch, and how high the guard sits between
  punches.
- **Hips:** the center of the body and the main source of punching power,
  via rotational torque transferred up through the shoulder into the
  strike; also the reference point used for the movement metric (§2.2.4,
  §3.3.9).

These five body parts are highlighted specifically because they correspond
to the exact MediaPipe landmarks the system tracks throughout this thesis.

_Status: NEW (part of §2.2). Simplified to a general description of a
punch; punch-type breakdown and the detector-limitation note removed.
Combinations added._
### 2.2.3 Punches
A punch is a strike thrown with a closed fist, produced by extending the
arm from a guarded position toward a target and driven by rotational force
transferred from the hips, through the shoulder, into the strike (Atif,
2025). As the arm extends, the elbow straightens and the wrist stays
locked so force transfers cleanly into the target, before the arm retracts
back to guard. This project treats a punch as that whole
extension-and-retraction motion: a short, rapid burst of forward arm
movement that stands out from a boxer's regular guard position or footwork
through the wrist and elbow's velocity and displacement relative to the
shoulder (§3.3.4 / §4.3.3).

Punches are rarely thrown one at a time. Boxers typically link two or more
punches into a combination, such as a jab followed by a cross, thrown in
quick succession before returning to guard. This project treats punches
that land close together in time as part of the same combination, which is
the basis for how punch volume is measured (§2.2.4, §3.3.7).

_Status: NEW (part of §2.2). Rewritten: each metric now states its
judging-criterion mapping and its coaching relevance instead of a
per-bullet fatigue note; fatigue discussion consolidated into one paragraph
after the list, plus a new paragraph on the risk of a boxer being strong in
one or two metrics while unknowingly weak in the third._
### 2.2.4 Performance Metrics in Boxing: Punch Volume, Guard Height, Movement
These are the three metrics this project aims to measure:
- **Punch volume:** the rate of punches thrown over a given period, usually
  landed in combinations rather than as single strikes. Punch volume is
  the practical measure of the "effective aggression" criterion judges
  score bouts on, since a boxer who lands more combinations is scored as
  the more active fighter (Atif, 2025). In training, coaches drill
  combination work (e.g. jab-cross-hook sequences) specifically to build
  the technique and stamina needed to sustain output over a full round, and
  watch how many combinations a boxer lands to judge whether they are
  working hard enough.
- **Guard height:** how consistently a boxer keeps their guard raised to
  protect the head when not actively punching. Guard height is the
  practical measure of the "defense" criterion judges score bouts on,
  since a guard that stays up is what stops clean punches from landing in
  the first place. In training, coaches continually cue boxers to keep
  their hands up during pad work and sparring, correcting a dropped guard
  as soon as they see it.
- **Movement:** general footwork and body displacement, independent of
  punches thrown, used to gauge ring activity, distance management, and
  evasive movement. Movement is the practical measure of the "ring
  generalship" criterion judges score bouts on, since controlling the
  range and pace of a bout depends on footwork. In training, coaches drill
  footwork patterns (pivots, lateral steps, cutting angles) and call out
  flat-footedness, since a boxer planted in one spot is easier to hit and
  has less control of an exchange.

Fatigue affects all three metrics, though not necessarily at the same time
or to the same degree. Punch volume tends to drop first and most visibly,
since throwing punches is the most physically demanding action in a round
and the easiest to instinctively cut back on to conserve energy. Guard
height degrades next: holding the arms up for an extended period is tiring
in its own right, so a fatigued boxer's guard sinks even between exchanges.
Movement is often the last to visibly suffer, but also the most
consequential: tired legs leave a boxer flatter-footed and slower to
reposition, which raises the risk of being hit and struggling to recover.

Because these three forms of fatigue don't necessarily show up together, a
boxer can be strong in one or two of these metrics while unknowingly weak
in the third. A boxer who keeps their guard up and stays composed might
still be quietly losing significant work rate, while another who keeps
throwing punches at a good pace might not notice their footwork has gone
flat. This is difficult for a boxer to catch by feel alone during a live
session, and just as hard for a coach to track across all three at once
without dedicated tools. That gap, between how a boxer is actually
performing under fatigue across all three metrics and what they or their
coach can perceive in the moment, is the core motivation for an app that
measures all three directly from video, rather than relying on self-report
or in-person observation.

These three were chosen because each maps to a distinct, coach-recognizable
pillar of boxing performance (offensive output, defensive discipline, and
ring activity) that a boxer training alone, without a coach watching, has
no way to self-assess. That gap is exactly what the pilot study (§2.6) later
confirms users want addressed.

_Status: UNCHANGED (renumbered from old §2.2 only)._
## 2.3 Pose Estimation Studies

_Status: UNCHANGED (part of §2.3, renumbered from old §2.2.1 only)._
### 2.3.1 AI in Sports Biomechanics (Souaifi et al., 2025)
In the study, *Artificial Intelligence in Sports Biomechanics: A Scoping
Review on Wearable Technology, Motion Analysis, and Injury Prevention*,
Souaifi et al. (2025) conducted a broad scoping review examining the
application of AI across sports biomechanics, including wearable sensors,
markerless vision systems, and machine learning-based analytical models.
Their review highlights motion capture technologies as well as macro-level
trends, such as the shift from controlled laboratory environments toward
mobile, real-time solutions. This validates the overall direction of this
project, which aims to bring pose analysis to smartphones.
A strength of their review is its extensive coverage, identifying recurring
issues: high cost of hardware-based systems, limited portability of
lab-grade setups, and the emerging preference for markerless AI pipelines.
The paper does not, however, provide detailed implementation guidance or
evaluate any specific approach experimentally. For this project, this study
confirms that affordable, accessible, AI-driven motion analysis is a
recognised need, and that markerless CNN-based methods are increasingly
feasible on consumer devices. *(See old thesis Table 2.1.)*

_Status: UNCHANGED (part of §2.3, renumbered from old §2.2.2 only)._
### 2.3.2 Pose Estimation for Medical and Exercise Assessment (Patil et al., 2022)
In the study, *Body Posture Detection and Motion Tracking using AI for
Medical Exercises and Recommendation System*, Patil et al. (2022) presented
a pose-based exercise assessment system using a webcam or smartphone camera
to automatically analyse medical rehabilitation movements. Their work
demonstrates that lightweight pose estimation can support structured
feedback without wearables, a practical foundation for sports-oriented
applications, successfully combining pose estimation, rule-based scoring,
and a dashboard interface into a full end-to-end pipeline achievable
outside laboratory settings.
This aligns with this project's requirement to run pose extraction and
analytics on mobile hardware. However, their system is deliberately designed
for slow, controlled movements such as physiotherapy exercises, which are
predictable and low-variability, rarely challenging the pose estimator with
occlusion or high-speed dynamics. This cannot be directly applied to fast
boxing movements, where rapid accelerations and torso rotation introduce
greater landmark noise. *(See old thesis Table 2.2.)*

_Status: UNCHANGED (part of §2.3, renumbered from old §2.2.3 only)._
### 2.3.3 Real-Time Boxing Feedback System (Bulun & Berg, 2024)
In the study, *Real-Time Boxing Feedback Using Human Pose Estimation and
Machine Learning*, Bulun and Berg (2024) developed an AI system targeted
specifically at boxing. Their mobile prototype detects jab punches and
determines whether the athlete's guard drops afterward, using MediaPipe
BlazePose for pose extraction and a Random Forest classifier to detect
punch frames, with guard height computed using threshold-based logic
relative to the shoulder position. This demonstrates that pose-based
analysis already runs at real-time speeds on mobile devices, validating the
feasibility of smartphone-based boxing feedback. Their desktop-prototype-
then-Android engineering path is itself a useful precedent, mirrored in
this project's own Python-prototype-then-Android-port approach (§4).
However, the system is intentionally narrow: it only identifies jabs and
only detects one specific error (guard drop immediately after a jab). It
does not measure performance over an entire session, track hooks or
uppercuts, or analyse overall movement patterns. This project builds
directly on their findings, expanding from a single-event detector to a
multi-metric performance tracker with guard trend analysis, punch volume
tracking, and full-body movement frequency. *(See old thesis Table 2.3.)*

_Status: CHANGED (part of §2.3, renumbered from old §2.2.4 only). In-text
citation corrected from "Magalhães et al." to "Lahkar et al.", matching
the actual author list (Lahkar, Muller, Dumas, Reveret & Robert, 2022)
verified directly against the published paper; the reference list entry
itself was already correct, only the in-text name was wrong._
### 2.3.4 Accuracy of Markerless Motion Capture for Boxing (Lahkar et al., 2022)
In the study, *Accuracy of a Markerless Motion Capture System in Estimating
Upper Extremity Kinematics During Boxing*, Lahkar et al. (2022)
evaluated the accuracy of Theia3D, a high-quality markerless motion capture
system, against a marker-based reference system during boxing tasks,
showing that markerless vision systems can achieve strong kinematic
accuracy, even for fast punch movements, when using multi-camera,
high-frame-rate setups. This is important evidence that boxing motion can
be effectively captured using only video.
Their setup uses professional-grade cameras, careful lighting, and a
controlled environment, making it impractical for casual consumer use. This
project shifts the same concept into a more accessible context: a
smartphone camera and an embedded CNN pose estimator (MediaPipe), accepting
slightly lower accuracy in exchange for usability and low cost.
*(See old thesis Table 2.4.)*

_Status: UNCHANGED (part of §2.3, renumbered from old §2.2.5 only)._
### 2.3.5 2D-to-3D Pose Model Transfer for Boxing (Lin et al., 2023)
In the study, *Model Transfer from 2D to 3D Study for Boxing Pose
Estimation*, Lin et al. (2023) explored improvements in boxing pose
estimation by extending 2D pose models into 3D using transfer learning and
RGB channel patching, demonstrating that monocular RGB video contains
enough implicit depth cues for meaningful 3D reconstruction. This matters
because boxing involves occluded limbs, rapid torso rotation, and complex
multi-planar movement that 2D coordinates alone cannot capture reliably.
Their models are computationally heavy and designed for offline experiments
rather than mobile deployment, making them unsuitable for on-device
inference. This project instead adopts MediaPipe, which achieves efficient
real-time landmark estimation suitable for Android. *(See old thesis
Table 2.5.)*

_Status: UNCHANGED (part of §2.3, renumbered from old §2.2.6 only)._
### 2.3.6 Summary of Literature Findings
The reviewed work collectively demonstrates that camera-based human pose
estimation is a practical technique for analysing athletic movement.
Across sports biomechanics, medical exercise assessment, and boxing-specific
pose estimation, modern markerless systems extract meaningful joint
kinematics with high accuracy even during dynamic upper-body movements.
This establishes a clear foundation for accessible, low-cost motion
analysis tools without wearables or lab-grade equipment.
The literature also reveals limitations that motivate this project: most
existing systems focus on single-event analysis (e.g. counting repetitions)
rather than evaluating performance across an entire session; none of the
reviewed studies implement time-based analytics to monitor how technique
changes as fatigue accumulates; and prior work tends to rely on either
controlled laboratory environments or simplified exercises that don't
generalize to high-intensity striking performance. This motivates a system
that moves beyond single-event detection toward session-level trend
analysis. *(Consolidated comparison in old thesis Table 2.6.)*

_Status: UNCHANGED (renumbered from old §2.3 only)._
## 2.4 Random Forest Studies

_Status: UNCHANGED (part of §2.4, renumbered from old §2.3.1 only)._
### 2.4.1 3D Human Pose Estimation Using Single Depth Images (Park et al., 2017)
In the study, *Accurate and Efficient 3D Human Pose Estimation Algorithm
Using Single Depth Images for Pose Analysis in Golf* (Park et al., 2017),
the authors propose a three-stage random forest pipeline combining random
regression forests with verification forests to improve precision and
efficiency of 3D joint localization from depth images, achieving higher
accuracy and lower computation time than baseline forest approaches on a
golf-swing dataset. The verification stage, which reduces noisy vote
contributions and improves accuracy for occluded joints while lowering
compute cost, is a key strength.
Tree-based ensembles can enhance pose estimation accuracy and robustness,
particularly for occluded or fast movements such as punches, but the
approach relies on depth camera input, unsuitable for standard RGB
smartphone video. This project instead adopts MediaPipe for RGB-only
landmark estimation, while adapting the ensemble/vote-verification concept
to filter noisy frames before feature computation. *(See old thesis
Table 2.7.)*

_Status: UNCHANGED (part of §2.4, renumbered from old §2.3.2 only)._
### 2.4.2 Random Forest for Abnormal Knee Joint Movement (Prodanović et al., 2024)
In the study, *Application of the Random Forest Algorithm for Identifying
Abnormal Patterns of Knee Joint Movement* (Prodanović et al., 2024), the
authors applied Random Forest classifiers to joint kinematic data to detect
abnormal knee movement patterns, with strong classification performance on
clinical motion-capture datasets and detailed discussion of preprocessing,
feature selection, and labeling strategy. This is clear evidence that
Random Forests handle noisy joint coordinate data effectively when features
are engineered correctly.
The dataset was high-fidelity clinical motion-capture data with
domain-specific labeled anomalies, limiting direct transfer to smartphone
video. This project instead applies Random Forest to boxing guard/movement
states using engineered features from MediaPipe landmarks, adopting best
practices in preprocessing, feature selection, careful labeling, and
cross-validation. *(See old thesis Table 2.8.)*

_Status: UNCHANGED (part of §2.4, renumbered from old §2.3.3 only)._
### 2.4.3 Automatic Activity Classification During a Sports Training Session (Ahmadi et al., 2014)
In the study, *Automatic Activity Classification and Movement Assessment
During a Sports Training Session Using Wearable Inertial Sensors* (Ahmadi et
al., 2014), the authors developed an ambulatory motion analysis system
using wearable IMUs, extracting time-frequency features via Discrete
Wavelet Transform (DWT) and classifying training activities with a Random
Forest model, achieving up to approximately 98% accuracy for certain
activity classes. This is a lightweight, computationally efficient pipeline
in unconstrained training environments.
The input modality was wearable IMUs, requiring rethinking for
vision-based landmarks. This project instead computes temporal features
from pose landmarks (velocity, and short-window statistics of joint
trajectories) to capture punch bursts and movement patterns, feeding a
Random Forest classifier, mirroring Ahmadi et al.'s success with a
lightweight pipeline on small datasets. *(See old thesis Table 2.9.)*

_Status: UNCHANGED (part of §2.4, renumbered from old §2.3.4 only)._
### 2.4.4 Summary of Literature Findings
The reviewed studies demonstrate that Random Forest classifiers are highly
effective for recognizing and classifying human movement patterns from pose
or motion-related data, consistently achieving strong accuracy with low
computational complexity. This makes them suitable for tasks requiring
interpretability, robustness to noise, and efficiency. Studies in golf
swing analysis and athletic movement recognition confirm that engineered
features combined with RF models deliver high-performance classification
without relying on deep neural networks, supporting the use of
feature-based methods over resource-heavy deep learning on small datasets.
Research on gait detection and sensor-based activity recognition further
highlights RF's adaptability to heterogeneous inputs and limited training
data, aligning with this project's constraints of mixed public and
smartphone video sources. *(Consolidated comparison in old thesis
Table 2.10.)*

_Status: UNCHANGED (renumbered from old §2.4 only)._
## 2.5 Comparative Analysis of Existing Applications

_Status: UNCHANGED (part of §2.5, renumbered from old §2.4.1 only)._
### 2.5.1 Jabbr (AI Boxing Judge)
Jabbr is a mobile application functioning as an automated "AI boxing
judge," analysing recorded footage to generate punch statistics and
classify punch types via 2D computer vision, without specialized hardware.
Its scope remains narrow: almost exclusively 2D punch-event metrics (count,
speed, category), which misses performance issues like fatigue-related
guard drop or deteriorating stance that 2D punch-event data alone can't
detect, and it provides no time-series visualisation of body posture. This
project improves on Jabbr by adding guard-height trend tracking and
timeline-based visual analytics alongside punch statistics. *(See old
thesis Table 2.11.)*

_Status: UNCHANGED (part of §2.5, renumbered from old §2.4.2 only)._
### 2.5.2 Growl (Smart Boxing Ecosystem)
Growl is a hardware-integrated system combining wearable inertial sensors,
a smart punching pad, and a companion app, generating precise punch force,
speed, and impact power in real time via accelerometers/gyroscopes/force
sensors embedded in the equipment. Its reliance on proprietary hardware
imposes substantial cost and accessibility barriers, and it provides little
insight into form degradation, guard mechanics, or full-body posture
changes. This project offers a purely camera-based, hardware-free
alternative, replicating metrics like punch count and movement intensity
without sensors while also covering guard height and footwork, which Growl
does not monitor. *(See old thesis Table 2.12.)*

_Status: UNCHANGED (part of §2.5, renumbered from old §2.4.3 only)._
### 2.5.3 Tempo (AI Home Gym System)
Tempo is an AI-powered home fitness system using 3D sensing for
personalized strength-training guidance (rep counting, form scoring,
technique correction) via structured sensing hardware and 3D skeletal
modeling. It requires dedicated 3D sensing hardware and a subscription, and
is tailored to general fitness movements (squats, deadlifts, presses)
rather than boxing-specific patterns like rotational strikes, guard
maintenance, or footwork variability. This project targets smartphone-only
tracking with no dedicated hardware, focused specifically on boxing
biomechanics. *(See old thesis Table 2.13.)*

_Status: UNCHANGED (part of §2.5, renumbered from old §2.4.4 only)._
### 2.5.4 Comparative Analysis Summary
Jabbr, Growl, and Tempo each advance AI-assisted sports training in
meaningful ways, yet none fully address the combined needs of
accessibility, posture monitoring, and boxing-specific temporal form
analysis: Jabbr offers punch analytics but lacks posture tracking; Growl
offers precise telemetry but requires costly hardware; Tempo provides
advanced feedback but is limited by proprietary equipment and a general
fitness focus. This confirms the need for a mobile-only, AI-driven boxing
performance tracker capable of extracting pose landmarks, visualizing
posture over time, and generating meaningful metrics without external
devices. *(Consolidated comparison in old thesis Table 2.14.)*

_Status: CHANGED (renumbered from old §2.5, and added a cross-reference to
Appendix A now that the questionnaire instrument has its own chapter)._
## 2.6 Data Analysis from Pilot Study
To complement the literature review and comparative app analysis, a pilot
study was conducted to understand training habits, feedback methods, and
expectations of potential end-users. An online questionnaire (reproduced
in full in Appendix A) was distributed across university students,
recreational fitness groups, and boxing communities. 40 responses were
collected.

_Status: UNCHANGED (part of §2.6, renumbered from old §2.5.1 only)._
### 2.6.1 Training Demographics, Insights from Respondents, and Feature Interest
82.5% of respondents train regularly. Of those, 33.3% participate in boxing
or other striking sports, and an additional 42.4% engage in form-focused
disciplines such as strength training and dance, both of which benefit
naturally from visual feedback and motion-analysis tools. 51.5% train 3–6
times per week, indicating routines where fatigue and form degradation are
likely to occur over extended sessions.
Despite high training frequency, feedback access is limited: only 27.3%
receive in-person coaching, 33.3% rely on subjective, time-consuming video
self-review, and 45.5% receive no feedback at all. Notably, all boxers
surveyed record their training sessions, but only 18% have those recordings
reviewed by a coach. This confirms that visual data exists but isn't being
effectively analyzed, directly motivating an automated system that
extracts guard maintenance, punch volume, and movement patterns from video.
Among non-trainers, 71.4% cite lack of affordable coaching and 57.1% cite
lack of training knowledge as barriers to starting exercise, while 85.7%
indicated the proposed app could encourage them to begin training.
On feature interest specifically: 82.5% want to track fatigue/form
degradation trends, 70% want a replay of their training, and 87.5% want
visual graphs summarizing performance changes over time, a strong
preference for visual, easy-to-interpret analytics over raw numeric
statistics. Among athletes/boxers specifically, interest in form
degradation and fatigue-related monitoring reached 100%.

_Status: UNCHANGED (part of §2.6, renumbered from old §2.5.2 only)._
### 2.6.2 Identified User Needs and Implications for System Design
Key needs identified: automated movement/form feedback without reliance on
professional coaching; visual analysis features (replay, performance
graphs); trend-tracking to help identify fatigue-driven degradation over
extended sessions; low barriers to entry for beginners; and affordable
alternatives to wearables or proprietary hardware. These directly inform
the functional requirements in Chapter 3: camera-only pose estimation and
visualization, timeline playback for reviewing form changes, graphical
dashboards for punch volume/movement/guard trends, and form-degradation
analysis from temporal wrist/shoulder/hip patterns.

_Status: UNCHANGED (part of §2.6, renumbered from old §2.5.3 only)._
### 2.6.3 Pilot Study Summary
The pilot study confirms both frequent athletes and occasional exercisers
face major limitations in obtaining clear, objective performance feedback.
With over 90% of participants expressing strong interest in AI-driven
movement analysis and visual tracking, the findings validate the need for
the proposed system, and directly guide the design and development
priorities carried into Chapter 3.

_Status: CHANGED (one paragraph added at the end pointing to the new §2.2;
otherwise same as old §2.6, renumbered to §2.7)._
## 2.7 Summary
The literature review confirms that AI-based pose estimation and
camera-only motion analysis have advanced significantly, enabling accurate
landmark extraction and biomechanical assessment without specialized
hardware, and that Random Forest classifiers offer strong accuracy at low
computational cost. This is a solid foundation for accessible, mobile-based
sports analysis. Major gaps remain, however: existing research focuses on
isolated tasks (punch detection, repetition counting), and commercial
systems (Jabbr, Growl, Tempo) depend on proprietary hardware or limit
analysis to basic 2D metrics. None deliver a camera-only solution capable
of session-level trend analysis for boxing. The pilot study reinforces this
gap, with over 90% of participants interested in fatigue tracking, replay,
and automated feedback.
Section 2.2 additionally established the boxing-domain vocabulary, namely
guard, stance, punch types, and the punch volume / guard height / movement
metric definitions, that the methodology (Chapter 3) and implementation
(Chapter 4) build on directly. Chapter 3 next outlines the methodology and
system design developed to address the identified gaps.
