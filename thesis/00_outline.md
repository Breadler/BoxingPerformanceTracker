# Thesis structure — working outline

Status: draft, revised 2026-08-08. This file is the source of truth for
chapter numbering. Each chapter has its own file in this folder — keep
section headers here in sync with the headers in those files.

## Source material in `old versions/`

Two things were dropped into `old versions/` to draw from:

- **`old versions/old thesis/`** — the full old thesis (pre-implementation),
  split one file per chapter (`0_old_abst.md` … `7_old_append.md`). This is
  what's being updated *from*. Anything that doesn't depend on implementation
  specifics (background/motivation, the literature review, the pilot study,
  the comparative app analysis) is reused close to verbatim. Anything that
  describes the system itself (methodology, results) is rewritten, because
  the plan in that version — Olympic Boxing Punch dataset, TensorFlow Lite
  conversion, a rigged 3D SceneView skeleton — isn't what was actually built.
- **`old versions/old article/`** — a conference-paper-style write-up of the
  labelling/feature-engineering pipeline specifically, with real numbers
  (163 videos processed, 95.4% pose-detection coverage, 175 labelled punch
  windows, audit passed, etc.). This is reused directly — it's the most
  precise, already-evaluated account of the Python-side pipeline that
  exists, and its Section III/IV map almost one-to-one onto the new Ch.
  4/5.

Reuse map (source → new home):

| Old file | Reused in |
|---|---|
| `old thesis/1_old_intro.md` | `01_introduction.md` — mostly verbatim |
| `old thesis/2_old_litrev.md` §2.2–2.6 | `02_literature_review.md` §2.3–2.7 (renumbered) |
| `old article/00_abst_intro_litrev.md` | `02_literature_review.md` (extra citations/framing for the labelling-specific literature) |
| `old article/03_meth.md` | `04_implementation.md` §4.2 (Python implementation — this is the most detailed, accurate account of stages B–F that exists) |
| `old article/04_res.md`, `05_conc_ref.md` | `05_results_and_evaluation.md` (real pipeline-output numbers + label-quality audit results + limitations) |
| `old thesis/5_old_conc.md` | `06_conclusion.md` — structure reused, content updated |
| `old thesis/6_old_refs.md` | `notes/references.md` |
| `old thesis/7_old_append.md` | pilot-study questionnaire — stays cited from `02_literature_review.md` §2.6; `notes/tester_questionnaire.md` is its counterpart for the *new* user evaluation |

## Changes made 2026-08-08

1. **§2 gets a new "Boxing Fundamentals" subsection** — boxing rules, key
   terminology and body parts (wrist, hips, guard, stance), what a punch is,
   and definitions of the three performance metrics (punch volume, guard
   height, movement) plus why they matter to boxers/competitively. This is
   background a reader needs before either the literature review or the
   methodology makes sense, so it goes early in Ch. 2, before the pose
   estimation / Random Forest literature.
2. **§3 (Methodology) — label auditing removed as a method step**, moved
   into §3.6 Evaluation Strategy instead (auditing labels is quality
   control/evaluation, not construction). The single metrics subsection is
   now three — one each for Punch Volume, Guard Height, Movement — matching
   how they're actually computed (independently, on the same window grid;
   see the mermaid diagram in `04_implementation.md`). Old §3.5
   "Implementation" is removed from this chapter entirely — it's promoted to
   a full chapter (see below), since it grew far past what a methodology
   subsection should carry. §3 is now strictly *the plan* — it should read
   the same whether or not the system had actually been built yet.
3. **§4 is now "Implementation"** — not "System Development." More detail
   than before: for both the Python prototype and the Android port, it names
   the actual technology, algorithm, calculation, and (where relevant)
   database used at each stage, and justifies the choice. This is where the
   RandomForest-vs-TensorFlow-Lite decision and the exact metric formulas
   live, alongside the two pipeline diagrams.
4. **§5 now shows outputs from every stage, and doubles as evaluation and
   testing** — not just a discussion chapter. Each pipeline stage gets its
   actual output shown (sample CSV rows, counts, plots, screenshots), and
   the evaluation/testing content (label audit results, model accuracy,
   on-device performance, metric validity, user testing) lives here as real
   results rather than a testing plan.

## Proposed table of contents

1. **Introduction** (`01_introduction.md`)
   - 1.1 Background
   - 1.2 Problem Statement
   - 1.3 Research Question
   - 1.4 Research Objective
   - 1.5 Scope of Research
   - 1.6 Thesis Outline
2. **Literature Review** (`02_literature_review.md`)
   - 2.1 Introduction
   - 2.2 Boxing Fundamentals — *new*
     - 2.2.1 Overview of Boxing and Rules
     - 2.2.2 Key Body Parts and Terminology
     - 2.2.3 Punches
     - 2.2.4 Performance Metrics in Boxing: Punch Volume, Guard Height, Movement
   - 2.3 Pose Estimation Studies *(was 2.2)*
   - 2.4 Random Forest Studies *(was 2.3)*
   - 2.5 Comparative Analysis of Existing Applications *(was 2.4)*
   - 2.6 Data Analysis from Pilot Study *(was 2.5)*
   - 2.7 Summary *(was 2.6)*
3. **Methodology — the plan** (`03_methodology.md`)
   - 3.1 Introduction
   - 3.2 Research Design
   - 3.3 Proposed Methodology
     - 3.3.1 Data Collection
     - 3.3.2 MediaPipe Landmark Extraction
     - 3.3.3 Manual Punch Labelling *(audit moved out — see 3.6.2)*
     - 3.3.4 Feature Engineering for Boxing Metrics
     - 3.3.5 Random Forest Model Development
     - 3.3.6 Python-Side Inference Validation
     - 3.3.7 Punch Volume Metric Computation — *split out*
     - 3.3.8 Guard Height Metric Computation — *split out*
     - 3.3.9 Movement Metric Computation — *split out*
     - 3.3.10 Graph Generation
   - 3.4 System Design (3.4.1 architecture, 3.4.2 data schema, 3.4.3 UX flow)
   - 3.5 Experimental Setup *(was 3.6; old 3.5 "Implementation" promoted to Ch. 4)*
     - 3.5.1 Data Sources and Recording Protocol
     - 3.5.2 Evaluation Metrics & Procedures
     - 3.5.3 Testing Conditions & Controls
   - 3.6 Evaluation Strategy *(was 3.7)*
     - 3.6.1 Model Accuracy Evaluation
     - 3.6.2 Label Quality Auditing — *moved in from 3.3.3*
     - 3.6.3 Pose Quality and Visualization Validation
     - 3.6.4 User-Centered Evaluation of Interface and Clarity
   - 3.7 Summary
4. **Implementation** (`04_implementation.md`) — *renamed from "System Development"*
   - 4.1 Development Environment & Tools
   - 4.2 Python Implementation
     - 4.2.1 Pose Extraction Pipeline
     - 4.2.2 Manual Labelling & Label-Auditing Tooling
     - 4.2.3 Feature Engineering
     - 4.2.4 Random Forest Training
     - 4.2.5 Python-Side Inference
     - 4.2.6 Punch Volume / Guard Height / Movement Computation
     - 4.2.7 Graph Generation
   - 4.3 Android Implementation
     - 4.3.1 On-Device Pose Extraction
     - 4.3.2 On-Device Classification: RandomForest (m2cgen) vs. TensorFlow Lite
     - 4.3.3 Window Feature Aggregation & On-Device Metric Computation
     - 4.3.4 Data Persistence (Room)
     - 4.3.5 UI & Visualization
   - 4.4 Differences Between Prototype and Android App
   - 4.5 Summary
5. **Results: Outputs, Evaluation and Testing** (`05_results_and_evaluation.md`)
   *(replaces "Results and Discussion")*
   - 5.1 Introduction
   - 5.2 Python Pipeline Outputs *(per-stage: sample CSV rows, counts, plots)*
   - 5.3 Android App Outputs *(screenshots: overlay video, graphs, session list)*
   - 5.4 Label Quality Audit Results
   - 5.5 Model Accuracy & Evaluation
   - 5.6 On-Device Performance
   - 5.7 Metric Validity
   - 5.8 User-Centered Evaluation
   - 5.9 Summary
6. **Conclusion and Recommendations for Future Research** (`06_conclusion.md`)
   - 6.1 Introduction
   - 6.2 Summary of the Research Objectives
   - 6.3 Research Contributions
   - 6.4 Practical Implications and Beneficiaries
   - 6.5 Limitations of the Present Study
   - 6.6 Future Works
   - 6.7 Summary

## Open questions

- Does the university/supervisor require Ch. 4 to be folded back into
  Methodology instead of standing alone?
- §2.2 Boxing Fundamentals needs real citations for the rules/terminology
  content (currently drafted from general knowledge) — worth pulling from an
  official boxing body (e.g. AIBA/World Boxing rules) or a sports-science
  textbook rather than leaving uncited.
- Confirm chapter/section numbering once content is drafted — this is a
  proposal, not final.
