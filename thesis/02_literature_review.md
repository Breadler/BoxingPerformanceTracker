# 2. Literature Review

§2.3–2.7 are reused close to verbatim from the old thesis
(`old versions/old thesis/2_old_litrev.md`, old §2.2–2.6) — that content is
about external research and the pilot study, neither of which depends on
implementation specifics that later changed. Only §2.2 is genuinely new.
Per-study comparison tables are not duplicated here; where one is skipped,
a pointer back to the exact table in the old file is left instead, to avoid
maintaining the same table in two places.

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

## 2.2 Boxing Fundamentals
_New section — provides the domain background a reader needs before either
the literature review or the methodology chapter makes sense. Definitions
below are drafted from general boxing knowledge; §2.2.1 in particular needs
a proper citation (an official rules body, e.g. World Boxing/AIBA, or a
sports-science textbook) before this goes into a final draft — see the open
question in `00_outline.md`._

### 2.2.1 Overview of Boxing and Rules
Boxing is a combat sport in which two competitors, matched by weight class,
attempt to strike each other above the waist using only their fists, inside
a roped ring, while avoiding return strikes. Bouts are divided into timed
rounds — three 3-minute rounds in amateur/Olympic boxing, up to twelve
3-minute rounds in professional boxing — separated by short rest intervals.
A bout ends by knockout, technical knockout (referee stoppage), or a
judges' decision based on criteria that typically include effective
aggression, ring generalship, defense, and — most relevant here — clean,
landed punches. _[CITATION NEEDED]_

Because scoring is explicitly tied to countable landed punches and
sustained work-rate, quantities like punch count and pacing are not just
convenient training metrics — they map directly onto how a bout is actually
judged. This is the underlying reason punch volume (§2.2.4) is treated as a
first-class metric in this project rather than an incidental one.

### 2.2.2 Key Body Parts and Terminology
- **Stance** — orthodox (left foot/hand forward) or southpaw (right
  foot/hand forward); affects which side leads punches and how guard/hip
  rotation should be interpreted.
- **Guard** — the defensive hand/arm position held near the head to block
  or parry incoming strikes when not actively punching.
- **Wrist, elbow, shoulder** — the kinematic chain that generates and
  directs a punch; wrist and elbow velocity relative to the shoulder is the
  main signal used later to detect a punch (§3.3.4 / §4.2.3).
- **Hips, torso rotation** — the core driver of punching power, via
  rotational torque transferred from hips through the shoulder into the
  strike; also the reference point used for the movement metric (§2.2.4,
  §3.3.9).
- **Footwork / movement** — repositioning via the legs and hips to control
  range, angle, and evade — distinct from the arm motion of a punch itself.

These are highlighted specifically because they correspond to the exact
MediaPipe landmarks the system tracks downstream (nose, wrist, elbow,
shoulder, hip) — this subsection is the domain justification for that
landmark subset, not just general anatomy.

### 2.2.3 Punches
Four fundamental punches are generally recognized:
- **Jab** — a straight punch thrown with the lead hand; primarily used for
  range-finding and setting up combinations rather than as a primary power
  shot.
- **Cross** (rear straight) — a straight punch thrown with the rear hand,
  generating power through hip and shoulder rotation.
- **Hook** — a circular punch thrown with a bent elbow, targeting the side
  of the head or body.
- **Uppercut** — a rising, vertical punch thrown from below, targeting the
  chin or body.

_(Matches the breakdown used in the reference thesis, §2.1.2 —
`old versions/reference_thesis_outline.md`.)_ The present system's punch
detector (§3.3.5/§4.2.4) does not currently distinguish between these punch
types — it detects punch vs. no-punch only — which is noted as a limitation
(§6.5) and a direction for future work (§6.6, and independently identified
in the article's own future-work section, `old versions/old article/05_conc_ref.md`).

### 2.2.4 Performance Metrics in Boxing: Punch Volume, Guard Height, Movement
- **Punch volume** — the number or rate of punches thrown over a given
  period. A core work-rate indicator coaches use to gauge output, and a
  practical proxy for the "effective aggression" criterion judges score
  bouts on.
- **Guard height** — how consistently a boxer keeps their guard raised to
  protect the head when not actively punching. Guard height is one of the
  first things to visibly degrade as fatigue sets in, which makes it as
  much a fatigue/form-degradation proxy as a defensive metric.
- **Movement** — general footwork and body displacement, independent of
  punches thrown; used to gauge ring activity, distance management, and
  evasive movement.

These three were chosen because each maps to a distinct, coach-recognizable
pillar of boxing performance — offensive output, defensive discipline, and
ring activity — that a boxer training alone, without a coach watching, has
no way to self-assess. That gap is exactly what the pilot study (§2.6) later
confirms users want addressed.

## 2.3 Pose Estimation Studies
*(renumbered from old §2.2 — reused verbatim, see
`old versions/old thesis/2_old_litrev.md` for the full per-study tables)*

### 2.3.1 AI in Sports Biomechanics (Souaifi et al., 2025)
Souaifi et al. (2025) conducted a broad scoping review examining the
application of AI across sports biomechanics, including wearable sensors,
markerless vision systems, and machine learning-based analytical models.
Their review highlights motion capture technologies as well as macro-level
trends, such as the shift from controlled laboratory environments toward
mobile, real-time solutions — validating the overall direction of this
project, which aims to bring pose analysis to smartphones.
A strength of their review is its extensive coverage, identifying recurring
issues: high cost of hardware-based systems, limited portability of
lab-grade setups, and the emerging preference for markerless AI pipelines.
The paper does not, however, provide detailed implementation guidance or
evaluate any specific approach experimentally. For this project, this study
confirms that affordable, accessible, AI-driven motion analysis is a
recognised need, and that markerless CNN-based methods are increasingly
feasible on consumer devices. *(Table — see old thesis Table 2.1.)*

### 2.3.2 Pose Estimation for Medical and Exercise Assessment (Patil et al., 2022)
Patil et al. (2022) presented a pose-based exercise assessment system using
a webcam or smartphone camera to automatically analyse medical
rehabilitation movements. Their work demonstrates that lightweight pose
estimation can support structured feedback without wearables — a practical
foundation for sports-oriented applications — successfully combining pose
estimation, rule-based scoring, and a dashboard interface into a full
end-to-end pipeline achievable outside laboratory settings.
This aligns with this project's requirement to run pose extraction and
analytics on mobile hardware. However, their system is deliberately designed
for slow, controlled movements such as physiotherapy exercises, which are
predictable and low-variability, rarely challenging the pose estimator with
occlusion or high-speed dynamics. This cannot be directly applied to fast
boxing movements, where rapid accelerations and torso rotation introduce
greater landmark noise. *(Table — see old thesis Table 2.2.)*

### 2.3.3 Real-Time Boxing Feedback System (Bulun & Berg, 2024)
Bulun and Berg (2024) developed an AI system targeted specifically at
boxing. Their mobile prototype detects jab punches and determines whether
the athlete's guard drops afterward, using MediaPipe BlazePose for pose
extraction and a Random Forest classifier to detect punch frames, with guard
height computed using threshold-based logic relative to the shoulder
position. This demonstrates that pose-based analysis already runs at
real-time speeds on mobile devices, validating the feasibility of
smartphone-based boxing feedback — and their desktop-prototype-then-Android
engineering path is itself a useful precedent, mirrored in this project's
own Python-prototype-then-Android-port approach (§4).
However, the system is intentionally narrow: it only identifies jabs and
only detects one specific error (guard drop immediately after a jab). It
does not measure performance over an entire session, track hooks or
uppercuts, or analyse overall movement patterns. This project builds
directly on their findings, expanding from a single-event detector to a
multi-metric performance tracker with guard trend analysis, punch volume
tracking, and full-body movement frequency. *(Table — see old thesis Table 2.3.)*

### 2.3.4 Accuracy of Markerless Motion Capture for Boxing (Magalhães et al., 2022)
Magalhães et al. (2022) evaluated the accuracy of Theia3D, a high-quality
markerless motion capture system, against a marker-based reference system
during boxing tasks, showing that markerless vision systems can achieve
strong kinematic accuracy — even for fast punch movements — when using
multi-camera, high-frame-rate setups. This is important evidence that
boxing motion can be effectively captured using only video.
Their setup uses professional-grade cameras, careful lighting, and a
controlled environment, making it impractical for casual consumer use. This
project shifts the same concept into a more accessible context: a
smartphone camera and an embedded CNN pose estimator (MediaPipe), accepting
slightly lower accuracy in exchange for usability and low cost.
*(Table — see old thesis Table 2.4.)*

### 2.3.5 2D-to-3D Pose Model Transfer for Boxing (Lin et al., 2023)
Lin et al. (2023) explored improvements in boxing pose estimation by
extending 2D pose models into 3D using transfer learning and RGB channel
patching, demonstrating that monocular RGB video contains enough implicit
depth cues for meaningful 3D reconstruction. This matters because boxing
involves occluded limbs, rapid torso rotation, and complex multi-planar
movement that 2D coordinates alone cannot capture reliably.
Their models are computationally heavy and designed for offline experiments
rather than mobile deployment, making them unsuitable for on-device
inference. This project instead adopts MediaPipe, which achieves efficient
real-time landmark estimation suitable for Android. *(Table — see old thesis
Table 2.5.)*

### 2.3.6 Summary of Literature Findings
The reviewed work collectively demonstrates that camera-based human pose
estimation is a practical technique for analysing athletic movement.
Across sports biomechanics, medical exercise assessment, and boxing-specific
pose estimation, modern markerless systems extract meaningful joint
kinematics with high accuracy even during dynamic upper-body movements —
establishing a clear foundation for accessible, low-cost motion analysis
tools without wearables or lab-grade equipment.
The literature also reveals limitations that motivate this project: most
existing systems focus on single-event analysis (e.g. counting repetitions)
rather than evaluating performance across an entire session; none of the
reviewed studies implement time-based analytics to monitor how technique
changes as fatigue accumulates; and prior work tends to rely on either
controlled laboratory environments or simplified exercises that don't
generalize to high-intensity striking performance. This motivates a system
that moves beyond single-event detection toward session-level trend
analysis. *(Consolidated comparison — see old thesis Table 2.6.)*

## 2.4 Random Forest Studies
*(renumbered from old §2.3 — reused verbatim)*

### 2.4.1 3D Human Pose Estimation Using Single Depth Images (Park et al., 2017)
Park et al. (2017) propose a three-stage random forest pipeline combining
random regression forests with verification forests to improve precision
and efficiency of 3D joint localization from depth images, achieving higher
accuracy and lower computation time than baseline forest approaches on a
golf-swing dataset. The verification stage — reducing noisy vote
contributions and improving accuracy for occluded joints while lowering
compute cost — is a key strength.
Tree-based ensembles can enhance pose estimation accuracy and robustness,
particularly for occluded or fast movements such as punches, but the
approach relies on depth camera input, unsuitable for standard RGB
smartphone video. This project instead adopts MediaPipe for RGB-only
landmark estimation, while adapting the ensemble/vote-verification concept
to filter noisy frames before feature computation. *(Table — see old thesis
Table 2.7.)*

### 2.4.2 Random Forest for Abnormal Knee Joint Movement (Prodanović et al., 2024)
Prodanović et al. (2024) applied Random Forest classifiers to joint
kinematic data to detect abnormal knee movement patterns, with strong
classification performance on clinical motion-capture datasets and detailed
discussion of preprocessing, feature selection, and labeling strategy —
clear evidence that Random Forests handle noisy joint coordinate data
effectively when features are engineered correctly.
The dataset was high-fidelity clinical motion-capture data with
domain-specific labeled anomalies, limiting direct transfer to smartphone
video. This project instead applies Random Forest to boxing guard/movement
states using engineered features from MediaPipe landmarks, adopting best
practices in preprocessing, feature selection, careful labeling, and
cross-validation. *(Table — see old thesis Table 2.8.)*

### 2.4.3 Automatic Activity Classification During a Sports Training Session (Ahmadi et al., 2014)
Ahmadi et al. (2014) developed an ambulatory motion analysis system using
wearable IMUs, extracting time-frequency features via Discrete Wavelet
Transform (DWT) and classifying training activities with a Random Forest
model, achieving up to ~98% accuracy for certain activity classes — a
lightweight, computationally efficient pipeline in unconstrained training
environments.
The input modality was wearable IMUs, requiring rethinking for
vision-based landmarks. This project instead computes temporal features
from pose landmarks (velocity, and short-window statistics of joint
trajectories) to capture punch bursts and movement patterns, feeding a
Random Forest classifier — mirroring Ahmadi et al.'s success with a
lightweight pipeline on small datasets. *(Table — see old thesis Table 2.9.)*

### 2.4.4 Summary of Literature Findings
The reviewed studies demonstrate that Random Forest classifiers are highly
effective for recognizing and classifying human movement patterns from pose
or motion-related data, consistently achieving strong accuracy with low
computational complexity — suitable for tasks requiring interpretability,
robustness to noise, and efficiency. Studies in golf swing analysis and
athletic movement recognition confirm that engineered features combined
with RF models deliver high-performance classification without relying on
deep neural networks, supporting the use of feature-based methods over
resource-heavy deep learning on small datasets. Research on gait detection
and sensor-based activity recognition further highlights RF's adaptability
to heterogeneous inputs and limited training data — aligning with this
project's constraints of mixed public and smartphone video sources.
*(Consolidated comparison — see old thesis Table 2.10.)*

## 2.5 Comparative Analysis of Existing Applications
*(renumbered from old §2.4 — reused verbatim)*

### 2.5.1 Jabbr (AI Boxing Judge)
Jabbr is a mobile application functioning as an automated "AI boxing
judge," analysing recorded footage to generate punch statistics and
classify punch types via 2D computer vision, without specialized hardware.
Its scope remains narrow — almost exclusively 2D punch-event metrics (count,
speed, category) — which misses performance issues like fatigue-related
guard drop or deteriorating stance that 2D punch-event data alone can't
detect, and it provides no time-series visualisation of body posture. This
project improves on Jabbr by adding guard-height trend tracking and
timeline-based visual analytics alongside punch statistics. *(Table — see
old thesis Table 2.11.)*

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
does not monitor. *(Table — see old thesis Table 2.12.)*

### 2.5.3 Tempo (AI Home Gym System)
Tempo is an AI-powered home fitness system using 3D sensing for
personalized strength-training guidance — rep counting, form scoring,
technique correction — via structured sensing hardware and 3D skeletal
modeling. It requires dedicated 3D sensing hardware and a subscription, and
is tailored to general fitness movements (squats, deadlifts, presses)
rather than boxing-specific patterns like rotational strikes, guard
maintenance, or footwork variability. This project targets smartphone-only
tracking with no dedicated hardware, focused specifically on boxing
biomechanics. *(Table — see old thesis Table 2.13.)*

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
devices. *(Consolidated comparison — see old thesis Table 2.14.)*

## 2.6 Data Analysis from Pilot Study
*(renumbered from old §2.5 — reused verbatim; questionnaire instrument
reproduced in `old versions/old thesis/7_old_append.md`)*

To complement the literature review and comparative app analysis, a pilot
study was conducted to understand training habits, feedback methods, and
expectations of potential end-users. An online questionnaire was
distributed across university students, recreational fitness groups, and
boxing communities; 40 responses were collected.

### 2.6.1 Training Demographics, Insights from Respondents, and Feature Interest
82.5% of respondents train regularly. Of those, 33.3% participate in boxing
or other striking sports, and an additional 42.4% engage in form-focused
disciplines such as strength training and dance — both benefit naturally
from visual feedback/motion-analysis tools. 51.5% train 3–6 times per week,
indicating routines where fatigue and form degradation are likely to occur
over extended sessions.
Despite high training frequency, feedback access is limited: only 27.3%
receive in-person coaching, 33.3% rely on subjective, time-consuming video
self-review, and 45.5% receive no feedback at all. Notably, all boxers
surveyed record their training sessions, but only 18% have those recordings
reviewed by a coach — confirming visual data exists but isn't being
effectively analyzed, and directly motivating an automated system that
extracts guard maintenance, punch volume, and movement patterns from video.
Among non-trainers, 71.4% cite lack of affordable coaching and 57.1% cite
lack of training knowledge as barriers to starting exercise, while 85.7%
indicated the proposed app could encourage them to begin training.
On feature interest specifically: 82.5% want to track fatigue/form
degradation trends, 70% want a replay of their training, and 87.5% want
visual graphs summarizing performance changes over time — a strong
preference for visual, easy-to-interpret analytics over raw numeric
statistics. Among athletes/boxers specifically, interest in form
degradation and fatigue-related monitoring reached 100%.

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

### 2.6.3 Pilot Study Summary
The pilot study confirms both frequent athletes and occasional exercisers
face major limitations in obtaining clear, objective performance feedback.
With over 90% of participants expressing strong interest in AI-driven
movement analysis and visual tracking, the findings validate the need for
the proposed system, and directly guide the design and development
priorities carried into Chapter 3.

## 2.7 Summary
*(renumbered from old §2.6)*
The literature review confirms that AI-based pose estimation and
camera-only motion analysis have advanced significantly, enabling accurate
landmark extraction and biomechanical assessment without specialized
hardware, and that Random Forest classifiers offer strong accuracy at low
computational cost — a solid foundation for accessible, mobile-based sports
analysis. Major gaps remain, however: existing research focuses on isolated
tasks (punch detection, repetition counting), and commercial systems
(Jabbr, Growl, Tempo) depend on proprietary hardware or limit analysis to
basic 2D metrics. None deliver a camera-only solution capable of
session-level trend analysis for boxing. The pilot study reinforces this
gap, with over 90% of participants interested in fatigue tracking, replay,
and automated feedback.
Section 2.2 additionally established the boxing-domain vocabulary — guard,
stance, punch types, and the punch volume / guard height / movement metric
definitions — that the methodology (Chapter 3) and implementation
(Chapter 4) build on directly. Chapter 3 next outlines the methodology and
system design developed to address the identified gaps.
