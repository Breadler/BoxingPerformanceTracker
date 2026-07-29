package com.breadler.boxingperformancetracker.data

data class PunchWindow(
    val startMs: Long,
    val endMs: Long,
)

data class PunchPrediction(
    val startMs: Long,
    val endMs: Long,
    val prediction: String,
    val punchProbability: Double,
)

data class PerformancePoint(
    val timestampMs: Long,
    val punchProbability: Double,
    /** 1.0 when [timestampMs] falls inside a predicted punch window, else 0.0. Drives the Punch Volume graph. */
    val punchVolume: Double,
    val guardScore: Double,
    val movementScore: Double,
)

interface SessionCardItem {
    val id: String
    val title: String
    val dateLabel: String
    val durationLabel: String
    val punchCount: Int
}

data class SessionSummary(
    override val id: String,
    override val title: String,
    override val dateLabel: String,
    override val durationLabel: String,
    val durationMs: Long,
    /** Legacy field for file paths. Prefer [sourceVideoUri] or [annotatedVideoUri]. */
    val videoPath: String? = null,
    val punchWindowsCsvPath: String? = null,
    val punchWindows: List<PunchWindow> = emptyList(),
    /** URI for the raw imported video. */
    val sourceVideoUri: String? = null,
    /** URI for the processed video with overlays. */
    val annotatedVideoUri: String? = null,
    val sourceVideoName: String? = null,
    val punchPredictions: List<PunchPrediction> = emptyList(),
    val performancePoints: List<PerformancePoint> = emptyList(),
    override val punchCount: Int = punchWindows.size,
) : SessionCardItem

data class SessionProcessingState(
    val isProcessing: Boolean = false,
    val statusMessage: String = "",
    /** 0f..1f progress through the local pose+punch analysis pipeline, or null while indeterminate. */
    val progress: Float? = null,
    val errorMessage: String? = null,
    val completedSessionId: String? = null,
)

object SampleBoxingSessions {
    private val sessions = listOf(
        SessionSummary(
            id = "round-01",
            title = "Round 01",
            dateLabel = "22/07/2026",
            durationLabel = "3:26",
            durationMs = 206_010L,
            videoPath = "/sdk_gphone16k_x86_64/Stryko/testvideo_annotated.mp4",
            punchWindowsCsvPath = "/sdk_gphone16k_x86_64/Stryko/predicted_punch_windows.csv",
            punchWindows = listOf(
                PunchWindow(280L, 570L),
                PunchWindow(1000L, 1250L),
                PunchWindow(2400L, 2690L),
            ),
        ),
    )

    fun all(): List<SessionSummary> = sessions

    fun findById(sessionId: String): SessionSummary? = sessions.firstOrNull { it.id == sessionId }
}
