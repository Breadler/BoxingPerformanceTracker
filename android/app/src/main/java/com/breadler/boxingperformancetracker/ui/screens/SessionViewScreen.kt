package com.breadler.boxingperformancetracker.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.data.SessionSummary
import com.breadler.boxingperformancetracker.ui.components.AnalysisPanel
import com.breadler.boxingperformancetracker.ui.components.SessionVideoPlayer
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SessionViewScreen(
    session: SessionSummary,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoUri = remember(session.videoPath) {
        session.videoPath?.let { videoPath ->
            val file = File(videoPath)
            if (file.exists()) Uri.fromFile(file) else null
        }
    }

    val punchWindows = remember(session.punchWindowsCsvPath, session.punchWindows) {
        session.punchWindowsCsvPath
            ?.let { csvPath -> loadPunchWindowsFromCsv(csvPath) }
            ?.ifEmpty { session.punchWindows }
            ?: session.punchWindows
    }

    var durationMs by remember(session.id) { mutableLongStateOf(session.durationMs) }
    var currentPositionMs by remember(session.id) { mutableLongStateOf(0L) }
    var isPlaying by remember(session.id) { mutableStateOf(false) }
    var seekRequestMs by remember(session.id) { mutableStateOf<Long?>(null) }
    var isUserScrubbing by remember(session.id) { mutableStateOf(false) }

    LaunchedEffect(isPlaying, videoUri) {
        if (!isPlaying || videoUri == null) {
            return@LaunchedEffect
        }
        while (isPlaying) {
            kotlinx.coroutines.delay(100)
            if (!isUserScrubbing) {
                currentPositionMs = (currentPositionMs + 100).coerceAtMost(durationMs)
                if (currentPositionMs >= durationMs) {
                    isPlaying = false
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = StrykoBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = StrykoBlue,
                    )
                }
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = StrykoRed,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            SessionVideoPlayer(
                videoUri = videoUri,
                isPlaying = isPlaying,
                seekToMs = seekRequestMs,
                onPositionUpdate = { positionMs ->
                    if (!isUserScrubbing && videoUri != null) {
                        currentPositionMs = positionMs.coerceIn(0L, durationMs)
                    }
                },
                onDurationLoaded = { loadedDurationMs ->
                    if (loadedDurationMs > 0L) {
                        durationMs = loadedDurationMs
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            AnalysisPanel(
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                punchWindows = punchWindows,
            )

            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleMedium,
                color = StrykoBlue,
                fontWeight = FontWeight.Bold,
            )

            TimelineControls(
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                isPlaying = isPlaying,
                onPositionChange = { newPositionMs ->
                    currentPositionMs = newPositionMs
                    seekRequestMs = newPositionMs
                },
                onScrubbingChanged = { scrubbing ->
                    isUserScrubbing = scrubbing
                    if (!scrubbing) {
                        seekRequestMs = currentPositionMs
                    }
                },
                onTogglePlayback = {
                    if (currentPositionMs >= durationMs) {
                        currentPositionMs = 0L
                        seekRequestMs = 0L
                    }
                    isPlaying = !isPlaying
                },
            )
        }
    }
}

@Composable
private fun TimelineControls(
    durationMs: Long,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onPositionChange: (Long) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit,
    onTogglePlayback: () -> Unit,
) {
    val sliderPosition = currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            value = sliderPosition.coerceIn(0f, 1f),
            onValueChange = { value ->
                onScrubbingChanged(true)
                onPositionChange((value * durationMs).roundToInt().toLong())
            },
            onValueChangeFinished = {
                onScrubbingChanged(false)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = StrykoRed,
                activeTrackColor = StrykoRed,
                inactiveTrackColor = StrykoRed.copy(alpha = 0.25f),
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTimeLabel(currentPositionMs), color = StrykoTextMuted)
            Text(formatTimeLabel(durationMs), color = StrykoTextMuted)
        }

        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier
                .size(56.dp)
                .padding(top = 4.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = StrykoRed,
                modifier = Modifier.size(56.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = StrykoCard,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

private fun formatTimeLabel(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun loadPunchWindowsFromCsv(csvPath: String): List<com.breadler.boxingperformancetracker.data.PunchWindow> {
    val csvFile = File(csvPath)
    if (!csvFile.exists()) {
        return emptyList()
    }

    val windows = mutableListOf<com.breadler.boxingperformancetracker.data.PunchWindow>()
    csvFile.useLines { lines ->
        lines.drop(1).forEach { line ->
            val columns = line.split(',')
            if (columns.size < 3) {
                return@forEach
            }

            val startMs = columns[1].trim().toLongOrNull()
            val endMs = columns[2].trim().toLongOrNull()
            if (startMs != null && endMs != null && endMs >= startMs) {
                windows.add(
                    com.breadler.boxingperformancetracker.data.PunchWindow(
                        startMs = startMs,
                        endMs = endMs,
                    ),
                )
            }
        }
    }
    return windows
}
