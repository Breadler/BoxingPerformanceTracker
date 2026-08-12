# Poster outline — StryKO (draft text for you to lay out)

Modeled on the two INNOVERSE reference posters (MicroAlert, PawPlanner),
which both follow the same seven-block structure. Text below is drafted to
drop into that structure; adjust length to fit whatever box sizes you land
on. Sourced from `01_introduction.md` (objectives), `02_literature_review.md`
§2.5–2.6 (comparative study, pilot survey), and `05_results_and_evaluation.md`
/ `06_conclusion.md` (all real numbers — nothing here is invented).

## Reference structure (both posters share this)
1. Header — institution branding, project icon/logo, title
2. Abstract — short paragraph + 3 icon highlights
3. Objectives — 3 numbered items (develop → propose → evaluate)
4. Comparative Study — table: Feature | Lit. Review | Existing App | Survey Findings | *This app's* advantage
5. Methodology — circular/cyclical diagram
6. Design/Result — app screenshot flow with arrows
7. Result & Conclusion — bar chart + stat callouts + bolded takeaway bullets
8. Footer — name, supervisor name

---

## 1. Header
**MICROALERT**/**PAWPLANNER**-equivalent title. Suggested:

> **STRYKO**
> AI-DRIVEN BOXING PERFORMANCE TRACKER

(Or whatever the app's actual display name is — check `android/app` branding
assets, e.g. `StrykoLogo.kt`, for the name/icon actually used in the shipped
app, and use that rather than inventing a new one for the poster.)

## 2. Abstract
**Paragraph** (~70 words, same length as the reference posters):

> StryKO is a mobile application that helps boxers track punch volume,
> guard height, and movement using only a smartphone camera. Video is
> processed through MediaPipe pose estimation and a Random Forest
> classifier — validated in Python (69.4% cross-validated accuracy, grouped
> by video) before being ported to run entirely on-device, with no server
> or wearable sensors required. A field test with three boxers found guard
> height and movement tracking fully accurate, zero false positives on
> punch detection, and no crashes across every test session.

**3 icon highlights** (short phrase each, matching the reference posters'
3-icon row):
- Uses smartphone camera for pose and punch tracking
- Detects punches, guard drops, and movement — fully on-device
- Built for solo training review — no coach or wearables needed

## 3. Objectives
Reused directly from `01_introduction.md` §1.4, tightened to poster length:
1. To **develop** a pose-landmark extraction and labelling algorithm that
   turns boxing video into a feature-engineered, punch-labelled dataset.
2. To **propose** an AI-driven boxing performance tracker using a Random
   Forest classifier, ported for on-device use in a mobile app.
3. To **evaluate** system effectiveness across punch volume, guard height,
   and movement through model evaluation and real boxer testing.

## 4. Comparative Study
Pulled from `02_literature_review.md` §2.5 (Jabbr/Growl/Tempo) and §2.6
(pilot survey numbers). Suggested 4 rows — trim to fit:

| Feature | Literature Review | Existing App | Survey Findings | StryKO's Advantage |
|---|---|---|---|---|
| Punch Detection | RF classifiers effectively map pose/motion features to movement states (Bulun & Berg, 2024) | Jabbr: 2D vision, punch stats only, no posture tracking | 100% of boxers surveyed record sessions, but only 18% get them reviewed | On-device RF punch detection, validated by cross-validation *and* a real boxer field test |
| Guard / Form Tracking | Guard drop is an early, measurable fatigue indicator | None of Jabbr, Growl, or Tempo track guard height specifically | 82.5% of respondents want to track form/fatigue degradation over time | Dedicated guard-height metric — rated fully accurate by all 3 field-test boxers |
| Hardware Requirement | Markerless vision matches marker-based accuracy for boxing kinematics (Magalhães et al., 2022) | Growl and Tempo require proprietary sensors/pads/3D rigs | 71.4% of non-trainers cite cost as a barrier to starting | Camera-only — phone hardware only, zero extra equipment |
| Accessibility & Feedback | Existing mobile work lacks session-level, time-based trend analysis | — | 45.5% of boxers currently get **no** feedback at all | Skeleton-overlay video + synced graphs, no coach required |

## 5. Methodology
Both reference posters use a circular "Agile" diagram (Plan → Design →
Develop → Test → Deploy/Display → Review). Two options:

**Option A — match the template convention**, using StryKO's real stages
mapped onto those labels:
- **Review** — collect findings from literature review + pilot survey
- **Plan** — data collection (UCF101 + self-recorded), define punch
  volume / guard height / movement metrics
- **Design** — pipeline architecture: MediaPipe landmarks → engineered
  features → Random Forest
- **Develop** — build and validate the Python prototype, then port to
  on-device Android
- **Test** — model evaluation (cross-validation) + 3-boxer field test
- **Deploy** — ship on-device, no server dependency

**Option B — show the real two-phase pipeline** instead of a generic Agile
wheel, since it's more informative for a technical judge and you already
have both diagrams built: the Python pipeline (A→J) and Android port (A→K)
from `04_implementation.md`. A simplified single-row version of each
(6–7 boxes, not the full mermaid detail) would fit a poster column.

Recommend Option A for template consistency with the other posters at the
same event, Option B if the judges are technical and diagram literacy is
valued over convention. Your call — you know the audience.

## 6. Design / Result (screenshot flow)
Suggested screen order, matching the actual app screens
(`android/app/src/main/java/.../ui/screens/`):

**Home → New Session (record/import) → Processing → Session Playback
(skeleton overlay + synced graphs) → Previous Sessions (history)**

Same visual pattern as the reference posters: phone mockups in a row
connected by arrows. If space allows, a callout on the Session Playback
screenshot pointing at the three graphs (guard height / movement / punch
volume) ties directly back to the Objectives and Comparative Study rows
above.

## 7. Result & Conclusion

**Bar chart** — from `05_results_and_evaluation.md` §5.7 (n=3 boxers, 1–5
Likert scale). Reference posters use ~4 bars; suggested subset:

| Statement | Mean score |
|---|---|
| App is easy to use | 5.00 / 5 |
| Skeleton overlay is easy to follow | 5.00 / 5 |
| Graphs are easy to understand | 5.00 / 5 |
| Punch count matched what was thrown | 4.00 / 5 |

*(Deliberately keeping the lowest-scoring item on the chart rather than
only the perfect 5.00s — it's honest, and it directly sets up the
"clear path forward" bullet below instead of looking cherry-picked.)*

**Stat callouts** (">X%"-style line, matching both reference posters):
- **4.83 / 5** average usability rating across 3 boxers, 10 categories
- **69.4%** cross-validated punch-detection accuracy (grouped by video —
  not an inflated single-split number)
- **0** false positives across every field-test session
- **95.4%** pose-detection coverage across 31,420 extracted frames
- **100%** of test sessions completed with no crashes

**Bolded takeaway bullets** (icon + bold statement, matching the
MicroAlert/PawPlanner style):
- 📷 **Camera-only, fully on-device punch, guard, and movement tracking** —
  no coach, wearables, or server required
- ✅ **Validated end-to-end** — Python-side cross-validated model evaluation
  *and* a real boxer field test, not just simulated accuracy
- 🛡️ **Zero false positives** — guard-height and movement tracking are
  fully reliable even where punch counting undercounts
- 🎯 **Clear, evidence-backed path forward** — southpaw stance and
  punch-type coverage identified as the next data-collection priority

**Optional closing line**, quote-styled like PawPlanner's:
> "StryKO gives boxers training alone a way to see their guard, movement,
> and punch output — without a coach, a wearable, or a lab."

## 8. Footer
Name: _[your name]_
Supervisor Name: _[supervisor's name]_

---

## Notes on what NOT to put on the poster
- The southpaw recall figure (32.6%) and combo-collapsing pattern are real
  and worth keeping *somewhere* (the takeaway bullet above frames it
  honestly as "next priority" rather than hiding it) — but the full
  per-boxer breakdown table in §5.5 is thesis-defense detail, not poster
  detail. One line is enough here.
- Don't cite the ~50% TFLite comparison number on the poster without the
  caveat that's now in `notes/decisions.md` (its split methodology couldn't
  be confirmed as grouped) — if you want the RF-vs-TFLite story on the
  poster at all, say "meaningfully outperformed an on-device neural network
  trained from scratch" rather than citing the exact ~50% figure as a
  rigorous apples-to-apples number.
