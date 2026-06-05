# BoxingPerformanceTracker

BoxingPerformanceTracker is a starter project for:

- an **Android app written in Kotlin** for capturing or loading boxing training videos
- a **Python ML pipeline** that uses **MediaPipe** pose landmarks and trains a **Random Forest** classifier

## Repository layout

- `android/` – minimal Android Kotlin app scaffold
- `python/` – MediaPipe feature extraction and Random Forest training scripts

## Python setup

```bash
cd python
pip install -r requirements.txt
```

## Train a model

```bash
cd python
python train_random_forest.py --input data/training.csv --output models/random_forest.joblib
```
