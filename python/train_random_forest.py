from __future__ import annotations

import argparse
from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report
from sklearn.model_selection import train_test_split


def train_model(dataset_path: Path) -> tuple[RandomForestClassifier, str]:
    data = pd.read_csv(dataset_path)
    if "label" not in data.columns:
        raise ValueError("Input CSV must include a 'label' column.")

    x = data.drop(columns=["label"])
    y = data["label"]

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=0.2,
        random_state=42,
        stratify=y,
    )

    model = RandomForestClassifier(n_estimators=300, random_state=42)
    model.fit(x_train, y_train)
    predictions = model.predict(x_test)
    report = classification_report(y_test, predictions)
    return model, report


def main() -> None:
    parser = argparse.ArgumentParser(description="Train Random Forest model for boxing movement labels.")
    parser.add_argument("--input", type=Path, required=True, help="Path to feature dataset CSV")
    parser.add_argument("--output", type=Path, required=True, help="Path for saved model (.joblib)")
    args = parser.parse_args()

    model, report = train_model(args.input)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, args.output)
    print("Model saved to:", args.output)
    print("\nEvaluation report:\n")
    print(report)


if __name__ == "__main__":
    main()
