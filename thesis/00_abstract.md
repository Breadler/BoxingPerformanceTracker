# Abstract

Rewritten from `old versions/old thesis/0_old_abst.md`, which described the
pre-implementation plan (TensorFlow Lite deployment, a rigged 3D skeleton
viewer, 33 raw landmarks). This version reports what was actually built
and evaluated in Chapters 4 to 6, replacing every plan-stage claim with a
delivered, measured one.

_Status: NEW. Drafted last, once the real Chapter 4 to 6 numbers were
final._

This thesis presents the design, implementation, and evaluation of a
mobile application for boxing performance analysis using AI-driven pose
estimation and Random Forest classification, addressing limitations in
traditional training methods that rely on manual observation or costly
wearable hardware. A review of pose estimation and machine learning
applications in sports biomechanics identified gaps in existing
camera-based boxing trackers, including limited session-level trend
tracking and a reliance on hardware beyond a smartphone camera. A pilot
study of 40 respondents confirmed strong demand for posture tracking and
session-level trend analytics, validating the feasibility of a
camera-only approach. Footage from the boxing subset of a public
action-recognition dataset (UCF101) was processed using MediaPipe to
extract pose landmarks per frame, which were reduced to 27 retained
landmarks and transformed into 70 engineered features across punch,
guard, and movement families. Of 163 pose-extracted videos, 15 were
manually labelled and audited for quality, producing a balanced 350-row
training set (175 punch and 175 no_punch windows) used to train a Random
Forest classifier, which reached 69.4% held-out accuracy and 0.692 macro
F1 under video-grouped cross-validation, well above the roughly
chance-level result observed when the same task was attempted with a
TensorFlow Lite network. The classifier was ported to the Android app by
transpiling it directly to native Java code with m2cgen, and runs
entirely on-device at four to eight times real-time with no network
dependency. The completed app records a training session, extracts pose
landmarks and classifies punches on-device, and presents punch volume,
guard height, and movement as time-series graphs alongside a 2D
skeleton-overlay video for the boxer or coach to review. A field test
with three boxers found guard height and movement tracking closely with
the source video, punch-count recall averaging 58.8% with
stance-dependent variation, and an overall usability rating of 4.83 out
of 5.
These results show that a lightweight, camera-only system can deliver
session-level boxing feedback without specialized hardware, while
identifying dataset scale and southpaw coverage as the clearest paths to
a more accurate classifier. Future work includes growing the labelled
training set, extending the same pose-estimation foundation to punch
types and other boxing-specific metrics, and moving from assisted review
toward automatic trend detection.
