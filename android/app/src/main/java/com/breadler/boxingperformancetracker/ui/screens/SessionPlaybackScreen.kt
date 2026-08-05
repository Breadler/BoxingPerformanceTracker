package com.breadler.boxingperformancetracker.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.data.SessionSummary
import com.breadler.boxingperformancetracker.ui.components.PerformanceGraph
import com.breadler.boxingperformancetracker.ui.components.PlaybackControls
import com.breadler.boxingperformancetracker.ui.components.SessionVideoPlayer
import com.breadler.boxingperformancetracker.ui.components.SmallRoundButton
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoSystemBars

@Composable
fun SessionPlaybackScreen(
    session: SessionSummary,
    onExit: () -> Unit,
    onStartNewRound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoRed, navigationBarColor = StrykoRed)

    val videoUri = remember(session.annotatedVideoUri, session.sourceVideoUri, session.videoPath) {
        val uriString = session.annotatedVideoUri.takeIf { !it.isNullOrBlank() }
            ?: session.sourceVideoUri.takeIf { !it.isNullOrBlank() }
            ?: session.videoPath.takeIf { !it.isNullOrBlank() }

        uriString?.let { str ->
            if (str.startsWith("/") || !str.contains("://")) {
                Uri.fromFile(java.io.File(str))
            } else {
                Uri.parse(str)
            }
        }
    }

    var durationMs by remember(session.id) { mutableLongStateOf(session.durationMs) }
    var currentPositionMs by remember(session.id) { mutableLongStateOf(0L) }
    var isPlaying by remember(session.id) { mutableStateOf(false) }
    var seekRequestMs by remember(session.id) { mutableStateOf<Long?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = StrykoBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallRoundButton(
                    text = "exit",
                    background = StrykoBlue,
                    iconResId = R.drawable.ic_back,
                    onClick = onExit,
                )
                SmallRoundButton(
                    text = "start a new round",
                    background = StrykoRed,
                    iconResId = R.drawable.ic_next,
                    onClick = onStartNewRound,
                )
            }

            SessionVideoPlayer(
                videoUri = videoUri,
                isPlaying = isPlaying,
                seekToMs = seekRequestMs,
                onPositionUpdate = { positionMs ->
                    currentPositionMs = positionMs.coerceIn(0L, durationMs)
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
                punchWindows = session.punchWindows,
                performancePoints = session.performancePoints,
                onScrub = { newPositionMs ->
                    currentPositionMs = newPositionMs
                    seekRequestMs = newPositionMs
                    isPlaying = false
                },
            )

            PlaybackControls(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onScrub = { newPositionMs ->
                    currentPositionMs = newPositionMs
                    seekRequestMs = newPositionMs
                    isPlaying = false
                },
                onTogglePlayback = {
                    if (currentPositionMs >= durationMs) {
                        currentPositionMs = 0L
                        seekRequestMs = 0L
                    }
                    isPlaying = !isPlaying
                },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}
