"""Video-grouped evaluation of the RandomForest punch classifier.

Standalone evaluation only — never writes to models/random_forest.joblib
(the artifact exported to Android via export_random_forest_java.py). Trains
throwaway models in memory purely to score them, so it cannot affect the
shipped app.

Grouped by video_id (not a per-row shuffle) so that overlapping windows from
the same clip can't leak across train/test and inflate the score — see
notes/decisions.md's 2026-08-07 entry, which this script exists to properly
substantiate with a real run instead of a described-but-uncommitted method.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_recall_fscore_support,
)
from sklearn.model_selection import GroupKFold, GroupShuffleSplit

from train_random_forest import LABEL_COLUMN, get_feature_columns

import pandas as pd

RANDOM_STATE = 42
N_ESTIMATORS = 300
LABELS = ["punch", "no_punch"]


def _new_model() -> RandomForestClassifier:
    return RandomForestClassifier(
        n_estimators=N_ESTIMATORS,
        random_state=RANDOM_STATE,
        class_weight="balanced",
    )


def _score(y_true, y_pred) -> dict:
    precision, recall, f1, _ = precision_recall_fscore_support(
        y_true, y_pred, labels=LABELS, zero_division=0,
    )
    return {
        "accuracy": accuracy_score(y_true, y_pred),
        "f1_macro": f1_score(y_true, y_pred, average="macro"),
        "per_class": {
            label: {"precision": p, "recall": r, "f1": f}
            for label, p, r, f in zip(LABELS, precision, recall, f1)
        },
        "confusion_matrix": {
            "labels": LABELS,
            "matrix": confusion_matrix(y_true, y_pred, labels=LABELS).tolist(),
        },
        "n_test_rows": len(y_true),
    }


def _holdout_accuracy(x, y, groups, *, train_size: float, seed: int) -> float:
    splitter = GroupShuffleSplit(n_splits=1, train_size=train_size, random_state=seed)
    train_idx, test_idx = next(splitter.split(x, y, groups))
    model = _new_model()
    model.fit(x.iloc[train_idx], y.iloc[train_idx])
    pred = model.predict(x.iloc[test_idx])
    return accuracy_score(y.iloc[test_idx], pred)


def evaluate(training_csv: Path, *, holdout_train_size: float, n_folds: int, n_seeds: int) -> dict:
    data = pd.read_csv(training_csv)
    if LABEL_COLUMN not in data.columns:
        raise ValueError("Input CSV must include a 'label' column.")
    if "video_id" not in data.columns:
        raise ValueError("Input CSV must include a 'video_id' column for grouping.")

    feature_columns = get_feature_columns(data)
    x = data[feature_columns].fillna(0.0)
    y = data[LABEL_COLUMN]
    groups = data["video_id"]

    # 1) Single video-grouped held-out split.
    splitter = GroupShuffleSplit(n_splits=1, train_size=holdout_train_size, random_state=RANDOM_STATE)
    train_idx, test_idx = next(splitter.split(x, y, groups))
    holdout_model = _new_model()
    holdout_model.fit(x.iloc[train_idx], y.iloc[train_idx])
    holdout_pred = holdout_model.predict(x.iloc[test_idx])
    holdout_result = _score(y.iloc[test_idx], holdout_pred)
    holdout_result["n_train_rows"] = len(train_idx)
    holdout_result["n_train_videos"] = int(groups.iloc[train_idx].nunique())
    holdout_result["n_test_videos"] = int(groups.iloc[test_idx].nunique())

    # 2) Grouped k-fold cross-validation, pooled — more stable than one split
    #    on a dataset this small (350 rows / 111 videos).
    gkf = GroupKFold(n_splits=n_folds)
    pooled_true: list[str] = []
    pooled_pred: list[str] = []
    fold_accuracies: list[float] = []
    for fold_train_idx, fold_test_idx in gkf.split(x, y, groups):
        fold_model = _new_model()
        fold_model.fit(x.iloc[fold_train_idx], y.iloc[fold_train_idx])
        fold_pred = fold_model.predict(x.iloc[fold_test_idx])
        fold_true = y.iloc[fold_test_idx]
        fold_accuracies.append(accuracy_score(fold_true, fold_pred))
        pooled_true.extend(fold_true.tolist())
        pooled_pred.extend(fold_pred.tolist())

    cv_result = _score(pooled_true, pooled_pred)
    cv_result["n_folds"] = n_folds
    cv_result["fold_accuracies"] = fold_accuracies
    cv_result["fold_accuracy_mean"] = sum(fold_accuracies) / len(fold_accuracies)
    cv_result["fold_accuracy_std"] = pd.Series(fold_accuracies).std()

    # 3) Repeated single-split sensitivity check. A dataset this small (111
    #    videos, uneven rows per video) makes ONE video-grouped split noisy
    #    depending purely on which videos land in the held-out set — this
    #    quantifies that noise so a single split isn't cited as if it were
    #    stable. See it as justification for reporting (2) as the headline
    #    number instead of (1).
    seed_accuracies = [
        _holdout_accuracy(x, y, groups, train_size=holdout_train_size, seed=seed)
        for seed in range(n_seeds)
    ]
    sensitivity_result = {
        "n_seeds": n_seeds,
        "accuracies": seed_accuracies,
        "mean": sum(seed_accuracies) / len(seed_accuracies),
        "std": pd.Series(seed_accuracies).std(),
        "min": min(seed_accuracies),
        "max": max(seed_accuracies),
    }

    return {
        "dataset": {
            "path": str(training_csv),
            "n_rows": len(data),
            "n_videos": int(groups.nunique()),
            "n_features": len(feature_columns),
            "label_counts": y.value_counts().to_dict(),
        },
        "model_params": {"n_estimators": N_ESTIMATORS, "random_state": RANDOM_STATE, "class_weight": "balanced"},
        "holdout_split": holdout_result,
        "grouped_cross_validation": cv_result,
        "holdout_split_sensitivity": sensitivity_result,
    }


def _print_summary(results: dict) -> None:
    ds = results["dataset"]
    print(f"Dataset: {ds['n_rows']} rows / {ds['n_videos']} videos / {ds['n_features']} features")
    print(f"Label counts: {ds['label_counts']}")

    h = results["holdout_split"]
    print("\n--- Video-grouped held-out split ---")
    print(f"Train: {h['n_train_rows']} rows / {h['n_train_videos']} videos")
    print(f"Test:  {h['n_test_rows']} rows / {h['n_test_videos']} videos")
    print(f"Accuracy: {h['accuracy']:.3f}  |  Macro F1: {h['f1_macro']:.3f}")
    for label, m in h["per_class"].items():
        print(f"  {label:9s} precision={m['precision']:.3f} recall={m['recall']:.3f} f1={m['f1']:.3f}")
    print(f"Confusion matrix {h['confusion_matrix']['labels']}:")
    for row in h["confusion_matrix"]["matrix"]:
        print(f"  {row}")

    cv = results["grouped_cross_validation"]
    print(f"\n--- {cv['n_folds']}-fold grouped cross-validation (pooled) ---")
    print(f"Per-fold accuracy: {[f'{a:.3f}' for a in cv['fold_accuracies']]}")
    print(f"Mean accuracy: {cv['fold_accuracy_mean']:.3f} (std {cv['fold_accuracy_std']:.3f})")
    print(f"Pooled accuracy: {cv['accuracy']:.3f}  |  Pooled macro F1: {cv['f1_macro']:.3f}")
    for label, m in cv["per_class"].items():
        print(f"  {label:9s} precision={m['precision']:.3f} recall={m['recall']:.3f} f1={m['f1']:.3f}")
    print(f"Pooled confusion matrix {cv['confusion_matrix']['labels']}:")
    for row in cv["confusion_matrix"]["matrix"]:
        print(f"  {row}")

    s = results["holdout_split_sensitivity"]
    print(f"\n--- Held-out split sensitivity ({s['n_seeds']} random splits) ---")
    print(f"Accuracy range: {s['min']:.3f} - {s['max']:.3f}  |  mean {s['mean']:.3f} (std {s['std']:.3f})")
    print("A single held-out split on this dataset is not a stable number on its own — "
          "use the pooled cross-validation result above as the headline figure.")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Evaluate the RandomForest punch classifier on a video-grouped split (read-only; "
        "does not touch models/random_forest.joblib).",
    )
    parser.add_argument("--input", type=Path, default=Path("data/training.csv"))
    parser.add_argument("--output", type=Path, default=Path("data/rf_eval_results.json"))
    parser.add_argument("--holdout-train-size", type=float, default=0.85)
    parser.add_argument("--n-folds", type=int, default=5)
    parser.add_argument("--n-seeds", type=int, default=15, help="Random splits for the sensitivity check")
    args = parser.parse_args()

    results = evaluate(
        args.input,
        holdout_train_size=args.holdout_train_size,
        n_folds=args.n_folds,
        n_seeds=args.n_seeds,
    )
    _print_summary(results)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(results, indent=2, default=str), encoding="utf-8")
    print(f"\nWrote full results to: {args.output}")


if __name__ == "__main__":
    main()
