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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.data.PunchWindow
import com.breadler.boxingperformancetracker.model.BoxingSession
import com.breadler.boxingperformancetracker.ui.components.PerformanceGraph
import com.breadler.boxingperformancetracker.ui.components.PlaybackControls
import com.breadler.boxingperformancetracker.ui.components.SessionVideoPlayer
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoSystemBars
import com.breadler.boxingperformancetracker.R
import java.io.File

@Composable
fun SessionPlaybackScreen(
    session: BoxingSession,
    onExit: () -> Unit,
    onStartNewRound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoRed, navigationBarColor = StrykoRed)

    val videoUri = remember(session.videoPath) {
        session.videoPath?.let { path ->
            val file = File(path)
            if (file.exists()) Uri.fromFile(file) else null
        }
    }
    val punchWindows = remember(session.punchWindowsCsvPath, session.fallbackPunchWindows) {
        loadPunchWindows(session.punchWindowsCsvPath).ifEmpty { session.fallbackPunchWindows }
    }

    var durationMs by remember(session.id) { mutableLongStateOf(session.durationMs) }
    var currentPositionMs by remember(session.id) { mutableLongStateOf(0L) }
    var isPlaying by remember(session.id) { mutableStateOf(false) }
    var seekRequestMs by remember(session.id) { mutableStateOf<Long?>(null) }
    var isUserScrubbing by remember(session.id) { mutableStateOf(false) }

    LaunchedEffect(isPlaying, videoUri) {
        if (!isPlaying || videoUri == null) return@LaunchedEffect
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallRoundButton(
                    text = "Exit",
                    background = StrykoBlue,
                    iconResId = R.drawable.ic_back,
                    onClick = onExit,
                )
                SmallRoundButton(
                    text = "Start New Round",
                    background = StrykoRed,
                    iconResId = R.drawable.ic_next,
                    onClick = onStartNewRound,
                )
            }

            Text(
                text = session.title,
                style = MaterialTheme.typography.titleLarge,
                color = StrykoBlue,
                fontWeight = FontWeight.Bold,
            )

            SessionVideoPlayer(
                videoUri = videoUri,
                isPlaying = isPlaying,
                seekToMs = seekRequestMs,
                onPositionUpdate = { positionMs ->
                    if (!isUserScrubbing) {
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
                    .height(410.dp),
            )

            PerformanceGraph(
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                punchWindows = punchWindows,
                isPlaying = isPlaying,
                onTogglePlayback = {
                    if (currentPositionMs >= durationMs) {
                        currentPositionMs = 0L
                        seekRequestMs = 0L
                    }
                    isPlaying = !isPlaying
                },
            )

            PlaybackControls(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onScrub = { newPositionMs ->
                    currentPositionMs = newPositionMs
                    seekRequestMs = newPositionMs
                    isUserScrubbing = false
                },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun SmallRoundButton(
    text: String,
    background: Color,
    iconResId: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painter = painterResource(iconResId), contentDescription = null, tint = StrykoCard, modifier = Modifier.size(18.dp))
            Text(text = text, color = StrykoCard, fontWeight = FontWeight.Bold)
        }
    }
}

private fun loadPunchWindows(csvPath: String?): List<PunchWindow> {
    if (csvPath.isNullOrBlank()) return emptyList()

    val file = File(csvPath)
    if (!file.exists()) return emptyList()

    val windows = mutableListOf<PunchWindow>()
    file.useLines { lines ->
        lines.drop(1).forEach { line ->
            val parts = line.split(',')
            if (parts.size < 3) return@forEach
            val startMs = parts[1].trim().toLongOrNull()
            val endMs = parts[2].trim().toLongOrNull()
            if (startMs != null && endMs != null && endMs >= startMs) {
                windows.add(PunchWindow(startMs = startMs, endMs = endMs))
            }
        }
    }
    return windows
}