package com.breadler.boxingperformancetracker.data.processing

import android.content.Context
import android.net.Uri
import android.util.Log
import com.breadler.boxingperformancetracker.data.PerformancePoint
import com.breadler.boxingperformancetracker.data.PunchPrediction
import com.breadler.boxingperformancetracker.data.PunchWindow
import java.io.File

class OnDeviceSessionProcessor(private val context: Context) {
    /**
     * Runs the full local user-inference workflow for one imported video: MediaPipe
     * pose extraction (+ annotated skeleton video), TFLite sliding-window punch
     * classification, and predicted punch window merging.
     *
     * [annotatedOutputFile] is a session-specific path so each imported video keeps
     * its own independent annotated video, pose data, and predictions.
     *
     * Throws with a descriptive message if the on-device model can't load or no pose
     * data could be extracted at all, instead of silently returning an empty result
     * that would look like a successful-but-blank session.
     */
    fun process(
        videoUri: Uri,
        annotatedOutputFile: File? = null,
        onProgress: ((fraction: Float) -> Unit)? = null,
    ): OnDeviceProcessingResult {
        val classifier = TflitePunchClassifier.createOrNull(context)
            ?: throw IllegalStateException(
                "The on-device punch model failed to load (see logcat tag TflitePunchClassifier).",
            )
        classifier.use {
            val observations = PoseFrameExtractor(context).extract(
                videoUri,
                classifier.strideMs,
                annotatedOutputFile,
                onProgress,
            )
            if (observations.isEmpty()) {
                throw IllegalStateException(
                    "No video frames could be analyzed on this device (see logcat tag PoseFrameExtractor).",
                )
            }

            val predictions = buildPredictions(observations, classifier)
            if (predictions.isEmpty()) {
                Log.w(
                    TAG,
                    "No prediction windows were generated - video may be shorter than the " +
                        "${classifier.windowMs}ms classification window.",
                )
            }
            val punchWindows = mergePunchWindows(predictions)
            val annotatedVideoPath = annotatedOutputFile
                ?.takeIf { it.exists() && it.length() > 0L }
                ?.absolutePath
            Log.d(
                TAG,
                "process: ${observations.size} pose frames, ${predictions.size} prediction windows, " +
                    "${punchWindows.size} punch windows, annotatedVideo=${annotatedVideoPath != null}",
            )
            return OnDeviceProcessingResult(
                punchWindows = punchWindows,
                punchPredictions = predictions,
                performancePoints = buildPerformancePoints(predictions, punchWindows),
                annotatedVideoPath = annotatedVideoPath,
            )
        }
    }

    private fun buildPredictions(
        observations: List<FrameObservation>,
        classifier: TflitePunchClassifier,
    ): List<PunchPrediction> {
        val minMs = observations.minOfOrNull { it.timestampMs } ?: return emptyList()
        val maxMs = observations.maxOfOrNull { it.timestampMs } ?: return emptyList()
        if (maxMs - minMs < classifier.windowMs) return emptyList()

        return buildList {
            var startMs = minMs
            while (startMs <= maxMs - classifier.windowMs) {
                val endMs = startMs + classifier.windowMs
                val features = WindowFeatures.aggregate(observations, startMs, endMs)
                if (features != null) {
                    val probability = classifier.classify(features).toDouble()
                    add(
                        PunchPrediction(
                            startMs = startMs,
                            endMs = endMs,
                            prediction = if (probability >= classifier.threshold) "punch" else "no_punch",
                            punchProbability = probability,
                        ),
                    )
                }
                startMs += classifier.strideMs
            }
        }
    }

    private fun mergePunchWindows(predictions: List<PunchPrediction>): List<PunchWindow> {
        val punchRows = predictions.filter { it.prediction == "punch" }.sortedBy { it.startMs }
        if (punchRows.isEmpty()) return emptyList()

        val merged = mutableListOf<PunchWindow>()
        var currentStart = punchRows.first().startMs
        var currentEnd = punchRows.first().endMs

        punchRows.drop(1).forEach { prediction ->
            if (prediction.startMs <= currentEnd) {
                currentEnd = maxOf(currentEnd, prediction.endMs)
            } else {
                merged += PunchWindow(currentStart, currentEnd)
                currentStart = prediction.startMs
                currentEnd = prediction.endMs
            }
        }
        merged += PunchWindow(currentStart, currentEnd)
        return merged
    }

    /**
     * Punch Volume is a binary 0/1 signal sourced from the merged
     * [punchWindows] (predicted_punch_windows), not the raw per-window
     * classifier probability, matching the Python inference contract.
     *
     * guardScore/movementScore have no model behind them yet, so they're kept flat
     * at 0 rather than faked from punch probability.
     */
    private fun buildPerformancePoints(
        predictions: List<PunchPrediction>,
        punchWindows: List<PunchWindow>,
    ): List<PerformancePoint> {
        return predictions.map { prediction ->
            val timestampMs = prediction.endMs
            val punchDetected = punchWindows.any { timestampMs in it.startMs..it.endMs }
            PerformancePoint(
                timestampMs = timestampMs,
                punchProbability = prediction.punchProbability.coerceIn(0.0, 1.0),
                punchVolume = if (punchDetected) 1.0 else 0.0,
                guardScore = 0.0,
                movementScore = 0.0,
            )
        }
    }

    private companion object {
        const val TAG = "OnDeviceSessionProcessor"
    }
}

data class OnDeviceProcessingResult(
    val punchWindows: List<PunchWindow> = emptyList(),
    val punchPredictions: List<PunchPrediction> = emptyList(),
    val performancePoints: List<PerformancePoint> = emptyList(),
    val annotatedVideoPath: String? = null,
)

