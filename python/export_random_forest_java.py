from __future__ import annotations

import argparse
from pathlib import Path

import joblib


def export_random_forest_java(
    model_path: Path,
    output_java_path: Path,
    *,
    package_name: str,
    class_name: str,
) -> list[str]:
    """Export the trained RandomForestClassifier to a Java class via m2cgen."""
    try:
        import m2cgen
    except ModuleNotFoundError as exc:
        raise SystemExit(
            "m2cgen is required for export. Install it in your Python environment with: pip install m2cgen"
        ) from exc

    # Load the trained model and convert it to Java source
    artifact = joblib.load(model_path)
    model = artifact["model"]
    feature_columns = list(artifact["feature_columns"])

    code = m2cgen.export_to_java(model, class_name=class_name, package_name=package_name)
    output_java_path.parent.mkdir(parents=True, exist_ok=True)
    output_java_path.write_text(code, encoding="utf-8")

    print(f"Wrote {output_java_path} ({code.count(chr(10))} lines)")
    print(f"classes_: {list(model.classes_)}")
    print(f"feature_columns ({len(feature_columns)}), in order:")
    for column in feature_columns:
        print(f"  {column}")
    print(
        "\nIf this order or the class list changed, update the matching featureColumns "
        "list and PUNCH_CLASS_INDEX in RandomForestPunchClassifier.kt to match."
    )
    return feature_columns


# CLI entry point
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Export the trained RandomForest punch classifier to a Java class for the Android app.",
    )
    parser.add_argument("--model", type=Path, default=Path("models/random_forest.joblib"))
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(
            "../android/app/src/main/java/com/breadler/boxingperformancetracker/"
            "data/processing/PunchForestModel.java"
        ),
    )
    parser.add_argument(
        "--package-name",
        default="com.breadler.boxingperformancetracker.data.processing",
    )
    parser.add_argument("--class-name", default="PunchForestModel")
    args = parser.parse_args()

    export_random_forest_java(
        args.model,
        args.output,
        package_name=args.package_name,
        class_name=args.class_name,
    )


if __name__ == "__main__":
    main()
