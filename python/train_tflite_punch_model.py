from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd

from build_training_csv import LABEL_COLUMN


def train_tflite_punch_model(
    training_csv_path: Path,
    output_model_path: Path,
    output_metadata_path: Path,
    *,
    window_ms: int,
    stride_ms: int,
    punch_threshold: float,
    epochs: int,
) -> None:
    try:
        import tensorflow as tf
    except ModuleNotFoundError as exc:
        raise SystemExit(
            "TensorFlow is required for export. Install it in your Python environment with: "
            "pip install tensorflow"
        ) from exc

    training = pd.read_csv(training_csv_path)
    metadata_columns = {
        "video_id",
        "start_frame",
        "end_frame",
        "center_frame",
        "start_ms",
        "end_ms",
        "center_ms",
        "duration_ms",
        "source",
        LABEL_COLUMN,
    }
    feature_columns = [
        column
        for column in training.columns
        if column not in metadata_columns and pd.api.types.is_numeric_dtype(training[column])
    ]
    if not feature_columns:
        raise ValueError(f"No numeric training features found in {training_csv_path}")

    x = training[feature_columns].fillna(0.0).astype("float32").to_numpy()
    y = (training[LABEL_COLUMN].astype(str) == "punch").astype("float32").to_numpy()

    rng = np.random.default_rng(42)
    indices = np.arange(len(x))
    rng.shuffle(indices)
    split = max(1, int(len(indices) * 0.85))
    train_indices = indices[:split]
    validation_indices = indices[split:] if split < len(indices) else indices[:split]

    normalizer = tf.keras.layers.Normalization(axis=-1)
    normalizer.adapt(x[train_indices])

    model = tf.keras.Sequential(
        [
            tf.keras.Input(shape=(len(feature_columns),), name="features"),
            normalizer,
            tf.keras.layers.Dense(64, activation="relu"),
            tf.keras.layers.Dropout(0.15),
            tf.keras.layers.Dense(32, activation="relu"),
            tf.keras.layers.Dense(1, activation="sigmoid", name="punch_probability"),
        ]
    )
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss="binary_crossentropy",
        metrics=[tf.keras.metrics.BinaryAccuracy(name="accuracy")],
    )
    model.fit(
        x[train_indices],
        y[train_indices],
        validation_data=(x[validation_indices], y[validation_indices]),
        epochs=epochs,
        batch_size=min(32, max(1, len(train_indices))),
        verbose=2,
    )

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    output_model_path.parent.mkdir(parents=True, exist_ok=True)
    output_metadata_path.parent.mkdir(parents=True, exist_ok=True)
    output_model_path.write_bytes(tflite_model)
    output_metadata_path.write_text(
        json.dumps(
            {
                "featureColumns": feature_columns,
                "windowMs": window_ms,
                "strideMs": stride_ms,
                "punchThreshold": punch_threshold,
                "punchOutputIndex": 0,
            },
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"Wrote TFLite model: {output_model_path}")
    print(f"Wrote model metadata: {output_metadata_path}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Train and export the punch classifier as TensorFlow Lite assets for the Android app.",
    )
    parser.add_argument("--training-csv", type=Path, default=Path("data/training.csv"))
    parser.add_argument(
        "--output-model",
        type=Path,
        default=Path("../android/app/src/main/assets/punch_model.tflite"),
    )
    parser.add_argument(
        "--output-metadata",
        type=Path,
        default=Path("../android/app/src/main/assets/punch_model_metadata.json"),
    )
    parser.add_argument("--window-ms", type=int, default=250)
    parser.add_argument("--stride-ms", type=int, default=40)
    parser.add_argument("--punch-threshold", type=float, default=0.5)
    parser.add_argument("--epochs", type=int, default=80)
    args = parser.parse_args()

    train_tflite_punch_model(
        args.training_csv,
        args.output_model,
        args.output_metadata,
        window_ms=args.window_ms,
        stride_ms=args.stride_ms,
        punch_threshold=args.punch_threshold,
        epochs=args.epochs,
    )


if __name__ == "__main__":
    main()

