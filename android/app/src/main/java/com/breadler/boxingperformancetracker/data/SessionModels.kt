package com.breadler.boxingperformancetracker.data

// One merged punch event
data class PunchWindow(
    val startMs: Long,
    val endMs: Long,
)

// One sliding-window classifier prediction
data class PunchPrediction(
    val startMs: Long,
    val endMs: Long,
    val prediction: String,
    val punchProbability: Double,
)

// One graph metrics sample point
data class PerformancePoint(
    val timestampMs: Long,
    val guardHeight: Double, // nose-to-wrist vertical gap
    val movement: Double, // hip center speed (x/z)
)

// Common fields shown on a session list card
interface SessionCardItem {
    val id: String
    val title: String
    val dateLabel: String
    val durationLabel: String
    val punchCount: Int
    val thumbnailUri: String?
}

// Full domain model for one processed session
data class SessionSummary(
    override val id: String,
    override val title: String,
    override val dateLabel: String,
    override val durationLabel: String,
    val durationMs: Long,
    val videoPath: String? = null, // legacy, prefer sourceVideoUri/annotatedVideoUri
    val punchWindowsCsvPath: String? = null,
    val punchWindows: List<PunchWindow> = emptyList(),
    val sourceVideoUri: String? = null, // raw imported video
    val annotatedVideoUri: String? = null, // processed video with overlays
    override val thumbnailUri: String? = null, // still-frame thumbnail
    val sourceVideoName: String? = null,
    val punchPredictions: List<PunchPrediction> = emptyList(),
    val performancePoints: List<PerformancePoint> = emptyList(),
    override val punchCount: Int = punchWindows.size,
) : SessionCardItem

// Stages of the on-device processing pipeline, with UI labels
enum class ProcessingPhase(val label: String) {
    COPYING("Copying video..."),
    LOADING_MODELS("Loading models..."),
    EXTRACTING("Extracting..."),
    DETECTING("Detecting..."),
    SAVING("Saving..."),
}

// Current state of the active/queued session import
data class SessionProcessingState(
    val isProcessing: Boolean = false,
    val sessionName: String = "",
    val phase: ProcessingPhase? = null,
    val progress: Float? = null, // 0f..1f, null while indeterminate
    val startTimeMs: Long = 0L,
    val errorMessage: String? = null,
    val completedSessionId: String? = null,
) {
    val isActive: Boolean get() = isProcessing || completedSessionId != null || errorMessage != null
}
