# Decision log

Running log of design decisions worth citing in the thesis, with dates, so
the reasoning doesn't have to be reconstructed later. Add an entry whenever a
"we tried X, it didn't work, we did Y instead" moment happens — those are
some of the most citable material in Ch. 4/5.

## 2026-08-07 — RandomForest ported via m2cgen, not retrained as TFLite
An on-device TensorFlow Lite network was trained from scratch on the same
`training.csv` used for the Python RandomForest. Evaluated with a proper
**video-grouped** train/validation split (grouping by `video_id` — a naive
per-row shuffle lets near-duplicate overlapping windows from the same clip
leak across train and validation and inflate the score), the TFLite model's
held-out accuracy came in at ~50% — essentially chance — on 350 labeled rows
across 111 videos. Decision: port the already-validated Python RandomForest
directly to a plain Java class via m2cgen instead of shipping a second,
independently-trained, unvalidated model.
Sources: `python/export_random_forest_java.py`,
`RandomForestPunchClassifier.kt` (both have first-hand docstring/comment
explanations — worth quoting directly in Ch. 4).

**Correction (2026-08-10):** re-checked the actual deleted script
(`git show 193db24:python/train_tflite_punch_model.py` — removed in
1429ee7) while writing the RF evaluation below. It does **not** group by
`video_id`; it's a plain `rng.shuffle()` over row indices with an 85/15
cut. So the "proper video-grouped split" description above doesn't match
the committed code — don't cite it as a grouped split without re-verifying
how that specific number was actually produced (may have been done outside
the script, or the description above may just be wrong). Doesn't
necessarily overturn the ~50%/chance-level conclusion itself, since a leaky
per-row split would be expected to *inflate* accuracy via near-duplicate
leakage, not suppress it — scoring at chance despite the more favorable
split is if anything a point in favor of "TFLite genuinely didn't learn
anything," just not for the reason originally stated. If this comparison
needs to hold up in the defense, re-run `train_tflite_punch_model.py` (or
its logic) with proper `GroupShuffleSplit`/`GroupKFold` before citing it
next to the RF numbers in `evaluate_random_forest.py`, which *do* use a
verified grouped methodology (see 2026-08-10 entry below).

## 2026-08-10 — RandomForest video-grouped evaluation results
`python/evaluate_random_forest.py` added as a read-only evaluation script —
loads `training.csv`, trains throwaway in-memory models, never writes to
`models/random_forest.joblib` (the artifact actually exported to Android),
so it can't affect the shipped app. Uses the same hyperparameters as
production training (`n_estimators=300`, `class_weight="balanced"`) and the
same `get_feature_columns` selection, imported directly from
`train_random_forest.py` rather than re-implemented, so feature selection
can't silently drift from what's actually shipped.

Three runs, all grouped by `video_id`:
1. **Single 85/15 held-out split** (seed 42): 45.2% accuracy — looks close
   to chance in isolation.
2. **5-fold grouped cross-validation, pooled** across all 111 videos:
   **69.4% accuracy, 0.692 macro F1** (punch: precision 0.736/recall
   0.606/F1 0.665; no_punch: precision 0.665/recall 0.783/F1 0.719).
3. **Sensitivity check** — repeated the single-split with 15 different
   random seeds: accuracy ranged **32.9%–91.3%** (mean 72.5%, std 0.160).

Decision: report (2), the pooled cross-validation figure, as the headline
number for §5.2.6 — not (1). Run (3) exists specifically to show why: on a
dataset this small (111 videos, uneven rows per video), which videos land
in the held-out 15% swings a single split's accuracy by ~60 points, so any
single split is not a stable, citable number on its own. This also means
the original TFLite ~50% single-split-style number (correction above) may
have simply been an unlucky (or lucky) draw rather than a true chance-level
result — another reason to re-run it with cross-validation before treating
the RF-vs-TFLite comparison as fully apples-to-apples.
Full results: `python/data/rf_eval_results.json`.

## 2026-08-07 — 3D skeleton visualization dropped
Original methodology (old flowchart, and old thesis §1.5/§3.3.5) planned a
rigged 3D skeleton viewer (SceneView + Filament, with a Maya-authored .glb
model) as the primary visualization. The shipped Android app instead draws
the skeleton overlay directly onto the 2D session video, synced to line
graphs (guard height / movement / punch volume) during playback. No
SceneView (or equivalent) dependency exists anywhere in `android/app/src`.
Reason: dropped due to time constraint — lower priority than getting the
detection/metrics pipeline working end to end. Worth a line in limitations
(§6.5) / future work (§6.6), framed as a scope decision rather than a
failure.

## 2026-08-07 — On-device punch volume ships the keyframe representation, refined from the uniform-grid version
Python-side punch volume was first implemented on the same uniform
sliding-window grid as guard height/movement (`punch_volume.
compute_punch_volume()`), feeding `graph_metrics.py`'s merge per §3.3.7/
§4.2.6. Within that same Python implementation pass, `compute_punch_volume_
keyframes()` was then built on top of the same combo-detection logic
(`build_punch_combos`) as a refinement: sparse points at each punch's own
end time instead of a value sampled onto the shared grid, so the signal's
sparse/bursty shape isn't flattened the way sampling onto a uniform grid
would flatten it. It originated as a display option for
`plot_graph_metrics.py`.
The Android port (`GraphMetrics.kt`) ports this keyframe representation
directly — not a divergence from Python, but a faithful port of a function
that already existed in `punch_volume.py`. The punch/combo detection itself
(`build_punch_combos` ↔ `buildPunchCombos`) is identical either way; only
the choice of which existing Python representation to port differs from
guard height/movement (which port the grid version). Chosen because it's
the closer match to how punch volume should read on a line graph, and
cheaper on-device: no per-window combo lookup across the whole video, no
smoothing/downsampling, just a line through data already held in memory.
Both functions remain in `punch_volume.py` — the grid version stays wired
into `graph_metrics.py`'s merge for the training/analysis pipeline; the
keyframe version is what Android ports. Not listed in §4.4's differences
table since it isn't one — it's part of the full Python implementation,
same as everything else the app ports. Real implementation detail, fine
for §4.3.3; not a limitation for §6.5.