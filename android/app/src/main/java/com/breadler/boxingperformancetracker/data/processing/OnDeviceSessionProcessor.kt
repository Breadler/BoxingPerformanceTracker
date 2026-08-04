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
     * pose extraction (+ annotated skeleton video), RandomForest sliding-window
     * punch classification, and predicted punch window merging.
     *
     * [annotatedOutputFile] is a session-specific path so each imported video keeps
     * its own independent annotated video, pose data, and predictions.
     *
     * Throws with a descriptive message if no pose data could be extracted at all,
     * instead of silently returning an empty result that would look like a
     * successful-but-blank session.
     */
    fun process(
        videoUri: Uri,
        annotatedOutputFile: File? = null,
        onProgress: ((fraction: Float) -> Unit)? = null,
    ): OnDeviceProcessingResult {
        val observations = PoseFrameExtractor(context).extract(
            videoUri,
            annotatedOutputFile = annotatedOutputFile,
            onProgress = onProgress,
        )
        if (observations.isEmpty()) {
            throw IllegalStateException(
                "No video frames could be analyzed on this device (see logcat tag PoseFrameExtractor).",
            )
        }

        val predictions = buildPredictions(observations)
        if (predictions.isEmpty()) {
            Log.w(
                TAG,
                "No prediction windows were generated - video may be shorter than the " +
                    "${RandomForestPunchClassifier.windowMs}ms classification window.",
            )
        }
        val punchWindows = mergePunchWindows(predictions)
        val annotatedVideoPath = annotatedOutputFile
            ?.takeIf { it.exists() && it.length() > 0L }
            ?.absolutePath
        val performancePoints = GraphMetrics.computeGraphMetrics(
            observations,
            windowMs = RandomForestPunchClassifier.windowMs,
            strideMs = RandomForestPunchClassifier.strideMs,
        )
        Log.d(
            TAG,
            "process: ${observations.size} pose frames, ${predictions.size} prediction windows, " +
                "${punchWindows.size} punch windows, ${performancePoints.size} graph metric points, " +
                "annotatedVideo=${annotatedVideoPath != null}",
        )
        return OnDeviceProcessingResult(
            punchWindows = punchWindows,
            punchPredictions = predictions,
            performancePoints = performancePoints,
            annotatedVideoPath = annotatedVideoPath,
        )
    }

    private fun buildPredictions(observations: List<FrameObservation>): List<PunchPrediction> {
        val minMs = observations.minOfOrNull { it.timestampMs } ?: return emptyList()
        val maxMs = observations.maxOfOrNull { it.timestampMs } ?: return emptyList()
        if (maxMs - minMs < RandomForestPunchClassifier.windowMs) return emptyList()

        return buildList {
            var startMs = minMs
            while (startMs <= maxMs - RandomForestPunchClassifier.windowMs) {
                val endMs = startMs + RandomForestPunchClassifier.windowMs
                val features = WindowFeatures.aggregate(observations, startMs, endMs)
                if (features != null) {
                    val probability = RandomForestPunchClassifier.classify(features).toDouble()
                    val prediction = if (probability >= RandomForestPunchClassifier.threshold) "punch" else "no_punch"
                    add(
                        PunchPrediction(
                            startMs = startMs,
                            endMs = endMs,
                            prediction = prediction,
                            punchProbability = probability,
                        ),
                    )
                }
                startMs += RandomForestPunchClassifier.strideMs
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

