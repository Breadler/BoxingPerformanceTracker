from __future__ import annotations

import shutil
from pathlib import Path

import pandas as pd
from fastapi import FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from session_processing import process_video


ROOT_DIR = Path(__file__).resolve().parent
STORAGE_DIR = ROOT_DIR / "api_storage"
UPLOAD_DIR = STORAGE_DIR / "uploads"
SESSION_DIR = STORAGE_DIR / "sessions"
MODEL_PATH = ROOT_DIR / "models" / "random_forest.joblib"

STORAGE_DIR.mkdir(parents=True, exist_ok=True)


app = FastAPI(title="StryKO Processing API")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.mount("/files", StaticFiles(directory=str(STORAGE_DIR)), name="files")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/sessions/process")
async def process_session(
    request: Request,
    video: UploadFile = File(...),
    window_ms: int = Form(250),
    stride_ms: int = Form(40),
) -> dict[str, object]:
    STORAGE_DIR.mkdir(parents=True, exist_ok=True)
    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    SESSION_DIR.mkdir(parents=True, exist_ok=True)

    upload_path = UPLOAD_DIR / Path(video.filename).name
    try:
        with upload_path.open("wb") as buffer:
            shutil.copyfileobj(video.file, buffer)
    except OSError as exc:
        raise HTTPException(status_code=500, detail=f"Failed to save uploaded video: {exc}") from exc
    finally:
        await video.close()

    try:
        result = process_video(
            upload_path,
            SESSION_DIR,
            model_path=MODEL_PATH,
            write_annotated=True,
            window_ms=window_ms,
            stride_ms=stride_ms,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"Video processing failed: {exc}") from exc

    response = result.to_response(base_url=str(request.base_url), storage_root=STORAGE_DIR)
    prediction_windows = pd.read_csv(result.prediction_windows_path)
    response["predictionWindows"] = [
        {
            "startMs": int(row.start_ms),
            "endMs": int(row.end_ms),
            "prediction": str(row.prediction),
            "punchProbability": float(getattr(row, "punch_probability", 0.0)),
        }
        for row in prediction_windows.itertuples(index=False)
    ]
    # performancePoints (punchVolume/guardHeight/movement) already comes from
    # result.to_response() via asdict() on the current PerformancePoint fields.
    return response


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("api_server:app", host="0.0.0.0", port=8000, reload=True)
