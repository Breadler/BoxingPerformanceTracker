# 1. Introduction

Reused close to verbatim from `old versions/old thesis/1_old_intro.md` —
background, problem statement, research question, and research objective
don't depend on implementation specifics that later changed, so they hold.
Only §1.5 (scope) and §1.6 (outline) need real edits, flagged inline below.

## 1.1 Background
Traditionally, sports training has relied on manual observation by coaches
or athletes, which can be inconsistent, time-consuming, and prone to human
error (Stenum et al., 2021). In striking sports such as boxing, maintaining
proper form is important for both performance and injury prevention.
However, as fatigue sets in during training, athletes often experience form
degradation without realizing it.
With recent advancements in artificial intelligence (AI) and deep learning,
pose estimation models now make it possible to analyze human movement
directly from standard video input. This eliminates the need for motion
capture suits or specialized tracking hardware, making the technology
cost-effective and accessible. Although research has demonstrated the
potential integration of AI-based pose estimation into sports environments,
there are currently few mobile solutions that provide real-time, camera-only
performance tracking tailored to individual athletes.
Therefore, this project aims to develop a mobile application for athletes,
both casual and professional, that records training sessions using the
smartphone camera and applies pose estimation combined with Random Forest
classification to track body movements. The system provides insights into
punch volume, guard consistency, and movement activity, thereby supporting
effective training and performance improvement.

## 1.2 Problem Statement
Public interest in health, exercise, and sports has increased significantly
in recent years, driven by growing awareness of health and well-being
(Chern, 2025). Many individuals incorporate physical training into their
daily routines. However, effective sports training requires substantial
technical knowledge, which may not be widely available or may demand
commitments beyond the reach of casual athletes.
Currently, athletes most often rely on manual observation by coaches or
wearable sensors for performance feedback. Standard biomechanical analysis
methods typically involve laboratory-based facilities and expert personnel,
which are both time-consuming and expensive (Souaifi et al., 2025). Existing
consumer fitness applications and smart devices that track movement mainly
focus on basic metrics such as repetition counts or one-time form checks —
for example, a swing analysis app for tennis that evaluates single-movement
performance (Chan-Danisi, 2024). However, these systems lack time-based
analysis of form degradation or fatigue trends that occur over prolonged
training sessions, such as in boxing.
AI adoption in athlete training is increasing across ASEAN countries, but
its implementation in Malaysia's sports sector remains in its early stages.
Nevertheless, studies show growing trust among athletes toward AI-assisted
tools in Malaysia, with overall perceptions of AI reported as high
(mean = 3.69/5) (Nazrin Aiman Azmi, 2025).
Therefore, there is a clear need for a low-cost, mobile solution that
provides analysis of technique, fatigue indicators, and training volume,
making high-quality performance feedback accessible to a wider audience.

## 1.3 Research Question
This study is guided by the following research questions:
i) How can AI-based pose estimation be adapted to analyze boxing-specific
   movements using only a smartphone camera?
ii) What methods can be implemented to detect punch events and
   fatigue-related form degradation across a training session using Random
   Forest classification?
iii) How effective is the proposed mobile application in providing clear,
   actionable feedback compared to existing fitness apps and manual
   observation?

## 1.4 Research Objective
The main objective of this research is to develop a mobile application that
uses AI-driven pose estimation and a Random Forest classifier, ported for
on-device use, to track boxing performance and provide visual summaries.
Specifically:
i) To develop a pose-landmark extraction and labelling algorithm that
   converts boxing video into a feature-engineered, punch-labelled training
   dataset.
ii) To propose an AI-driven boxing performance tracker using a Random
   Forest classifier, ported for on-device use in a mobile application.
iii) To evaluate the system's effectiveness across three performance
   measurements (punch volume, guard height, and movement) through model
   evaluation and real boxer testing.

## 1.5 Scope of Research
_Reused from old thesis §1.5 with edits — marked inline. Most bullets held
up exactly as originally scoped; the visualization bullet is the one real
change (§4.3.5 for why)._

This project focuses on developing a mobile application for boxing
performance tracking. The scope of this research is as follows:
- Video capture and frame extraction using a smartphone camera.
- On-device pose estimation using MediaPipe Pose Landmarker. _(Originally
  scoped as "CNN-based models such as MediaPipe BlazePose" — still
  accurate; MediaPipe Tasks is the current API surface for the same
  underlying model family.)_
- Extraction of body landmarks (head, shoulders, elbows, wrists, hips,
  knees, ankles, feet).
- Analysis of boxing-specific metrics, including:
  - Guard height (hand height relative to head when not punching).
  - Punch volume (number of punches per period).
  - Movement (general body movement besides punching).
- Application of a Random Forest classifier to detect punch events, ported
  to run natively on-device (§4.3.2) — _the original scope described
  "TensorFlow Lite" conversion specifically; this changed (§4.3.2 explains
  why) but the underlying scope item — an on-device, interpretable
  classifier — held._
- Visualization of results through a skeleton-overlay video synced to
  graphical dashboards. _(Originally scoped as "a 3D skeleton viewer,
  interactive timeline, and graphical dashboards" — the 3D skeleton viewer
  specifically was dropped, §4.3.5/§6.5; timeline + graphs held.)_
- Validation using public datasets during development and a self-collected
  smartphone dataset during testing.

Out-of-scope items (unchanged): real-time multi-person tracking,
multi-camera synchronized motion-capture workflows, integration with
commercial wearable hardware or depth sensors, high-precision laboratory
motion capture, clinical/medical diagnostic use, sparring or competitive
matches, training for sports outside boxing, payment gateways, gym
management system integration, live streaming to remote coaches, and
automated medical/injury diagnosis.
The system is designed for single-user sessions captured by a single
smartphone camera under reasonable lighting with minimal occlusion, assuming
stationary boxing training (e.g. a punching bag) rather than dynamic
sparring.

## 1.6 Thesis Outline
_Rewritten — the old 5-chapter outline (old thesis §1.6) predates the
System Development / Implementation split; see `00_outline.md` for the full
rationale._
This thesis is organized into six chapters:
- **Chapter 1. Introduction.** Background, problem statement, research
  questions and objectives, and scope of the research.
- **Chapter 2. Literature Review.** Boxing fundamentals and terminology,
  followed by a review of existing pose-estimation and Random-Forest-based
  sports-analysis research, a comparative analysis of existing boxing/
  fitness apps, and pilot-study findings on user needs.
- **Chapter 3. Methodology.** The planned research design and pipeline —
  data collection, landmark extraction, labelling, feature engineering,
  model development, per-metric computation, system design, and the
  planned evaluation strategy.
- **Chapter 4. Implementation.** What was actually built: the Python
  prototype, validated end to end, then ported to run entirely on-device as
  an Android application — including the specific technologies, algorithms,
  and calculations used, and the reasoning behind key implementation
  decisions.
- **Chapter 5. Results: Outputs, Evaluation and Testing.** The actual output
  produced at every pipeline stage, and the results of evaluating the
  system against the strategy set out in Chapter 3 — label quality, model
  accuracy, on-device performance, metric validity, and user-centered
  evaluation.
- **Chapter 6. Conclusion and Recommendations for Future Research.**
  Summary of objectives achieved, research contributions, practical
  implications, limitations, and directions for future work.
