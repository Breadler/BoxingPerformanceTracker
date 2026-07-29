package com.breadler.boxingperformancetracker.model

import com.breadler.boxingperformancetracker.data.PunchWindow

data class BoxingSession(
    val id: String,
    val title: String,
    val dateLabel: String,
    val durationLabel: String,
    val durationMs: Long,
    val videoPath: String?,
    val punchWindowsCsvPath: String?,
    val fallbackPunchWindows: List<PunchWindow> = emptyList(),
)

object SampleBoxingSessions {
    private val sessions = listOf(
        BoxingSession(
            id = "round-01",
            title = "Round 01",
            dateLabel = "22/07/2026",
            durationLabel = "3:26",
            durationMs = 206_010L,
            videoPath = "/sdk_gphone16k_x86_64/Stryko/testvideo_annotated.mp4",
            punchWindowsCsvPath = "/sdk_gphone16k_x86_64/Stryko/predicted_punch_windows.csv",
            fallbackPunchWindows = listOf(
                PunchWindow(280L, 570L),
                PunchWindow(1000L, 1250L),
                PunchWindow(2400L, 2690L),
            ),
        ),
        BoxingSession(
            id = "round-02",
            title = "Round 02",
            dateLabel = "23/07/2026",
            durationLabel = "3:26",
            durationMs = 206_010L,
            videoPath = "/sdk_gphone16k_x86_64/Stryko/testvideo_annotated.mp4",
            punchWindowsCsvPath = "/sdk_gphone16k_x86_64/Stryko/predicted_punch_windows.csv",
            fallbackPunchWindows = listOf(
                PunchWindow(2920L, 4210L),
                PunchWindow(5720L, 6130L),
                PunchWindow(7640L, 7890L),
            ),
        ),
        BoxingSession(
            id = "round-03",
            title = "Round 03",
            dateLabel = "24/07/2026",
            durationLabel = "3:26",
            durationMs = 206_010L,
            videoPath = "/sdk_gphone16k_x86_64/Stryko/testvideo_annotated.mp4",
            punchWindowsCsvPath = "/sdk_gphone16k_x86_64/Stryko/predicted_punch_windows.csv",
            fallbackPunchWindows = listOf(
                PunchWindow(8080L, 8410L),
                PunchWindow(9400L, 9730L),
                PunchWindow(10960L, 11330L),
            ),
        ),
    )

    fun all(): List<BoxingSession> = sessions

    fun findById(sessionId: String): BoxingSession? = sessions.firstOrNull { it.id == sessionId }
}