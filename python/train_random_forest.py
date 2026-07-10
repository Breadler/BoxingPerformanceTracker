from __future__ import annotations

import argparse
import pickle
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier


LABEL_COLUMN = "label"
METADATA_COLUMNS = {
    "video_id",
    "start_frame",
    "end_frame",
    "center_frame",
    "start_ms",
    "end_ms",
    "center_ms",
    "duration_ms",
    "source",
}


def get_feature_columns(data: pd.DataFrame) -> list[str]:
    ignored_columns = {*METADATA_COLUMNS, LABEL_COLUMN}
    feature_columns = [
        column
        for column in data.columns
        if column not in ignored_columns and pd.api.types.is_numeric_dtype(data[column])
    ]
    if not feature_columns:
        raise ValueError("Input CSV does not contain any numeric feature columns.")
    return feature_columns


def train_model(
    dataset_path: Path,
    *,
    n_estimators: int,
    random_state: int,
) -> tuple[RandomForestClassifier, list[str], pd.Series]:
    data = pd.read_csv(dataset_path)
    if LABEL_COLUMN not in data.columns:
        raise ValueError("Input CSV must include a 'label' column.")

    feature_columns = get_feature_columns(data)
    x = data[feature_columns].fillna(0.0)
    y = data[LABEL_COLUMN]

    model = RandomForestClassifier(
        n_estimators=n_estimators,
        random_state=random_state,
        class_weight="balanced",
    )
    model.fit(x, y)
    model.feature_names_in_ = pd.Index(feature_columns).to_numpy()
    return model, feature_columns, y.value_counts()


def main() -> None:
    parser = argparse.ArgumentParser(description="Train a Random Forest punch classifier.")
    parser.add_argument("--input", type=Path, required=True, help="Path to training.csv")
    parser.add_argument("--output", type=Path, required=True, help="Path for saved model (.joblib)")
    parser.add_argument("--n-estimators", type=int, default=300, help="Number of trees")
    parser.add_argument("--random-state", type=int, default=42, help="Random seed")
    args = parser.parse_args()

    model, feature_columns, label_counts = train_model(
        args.input,
        n_estimators=args.n_estimators,
        random_state=args.random_state,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    artifact = {
        "model": model,
        "feature_columns": feature_columns,
        "label_column": LABEL_COLUMN,
        "metadata_columns": sorted(METADATA_COLUMNS),
    }
    try:
        import joblib

        joblib.dump(artifact, args.output)
    except ModuleNotFoundError:
        with args.output.open("wb") as output_file:
            pickle.dump(artifact, output_file)
    print("Model saved to:", args.output)
    print(f"Trained on {len(feature_columns)} features from {args.input}")
    print("Training label counts:")
    for label, count in label_counts.items():
        print(f"  {label}: {count}")


if __name__ == "__main__":
    main()
