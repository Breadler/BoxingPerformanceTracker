package com.breadler.boxingperformancetracker.data

data class PunchWindow(
    val startMs: Long,
    val endMs: Long,
)

data class SessionSummary(
    val id: String,
    val title: String,
    val dateLabel: String,
    val durationLabel: String,
    val durationMs: Long,
    val videoPath: String?,
    val punchWindowsCsvPath: String?,
    val punchWindows: List<PunchWindow> = emptyList(),
)

object SampleSessions {
    private val sessions = listOf(
        SessionSummary(
            id = "testvideo",
            title = "Annotated Test Video",
            dateLabel = "Stryko emulator session",
            durationLabel = "3 mins 26s",
            durationMs = 206_010L,
            videoPath = "/sdk_gphone16k_x86_64/Stryko/testvideo_annotated.mp4",
            punchWindowsCsvPath = "/sdk_gphone16k_x86_64/Stryko/predicted_punch_windows.csv",
        ),
    )

    fun all(): List<SessionSummary> = sessions

    fun findById(sessionId: String): SessionSummary? = sessions.firstOrNull { it.id == sessionId }
}
