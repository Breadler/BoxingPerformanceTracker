package com.breadler.boxingperformancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.data.SessionProcessingState
import com.breadler.boxingperformancetracker.ui.components.SmallRoundButton
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoSystemBars
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted
import kotlinx.coroutines.delay

// Progress spinner + status for the active/finished import, plus the queue
@Composable
fun ProcessingScreen(
    importState: SessionProcessingState,
    queuedSessionNames: List<String>,
    onExit: () -> Unit,
    onNewRound: () -> Unit,
    onOpenSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoRed, navigationBarColor = StrykoRed)

    // Tick the elapsed-time counter while processing
    var elapsedSeconds by remember(importState.startTimeMs) { mutableLongStateOf(0L) }
    LaunchedEffect(importState.startTimeMs, importState.isProcessing) {
        if (!importState.isProcessing) return@LaunchedEffect
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - importState.startTimeMs) / 1000
            delay(1000)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = StrykoBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    text = "new round",
                    background = StrykoRed,
                    iconResId = R.drawable.ic_next,
                    onClick = onNewRound,
                    iconAtEnd = true,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val progress = importState.progress
                Box(contentAlignment = Alignment.Center) {
                    if (progress != null) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = StrykoRed,
                            trackColor = StrykoCard,
                            modifier = Modifier.size(88.dp),
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = StrykoRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    } else {
                        CircularProgressIndicator(color = StrykoRed, trackColor = StrykoCard, modifier = Modifier.size(88.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = formatElapsed(elapsedSeconds),
                    color = StrykoTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Analyzing on this device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    importState.errorMessage != null -> {
                        Text(
                            text = importState.errorMessage,
                            color = StrykoRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    importState.completedSessionId != null -> {
                        Text(
                            text = "Finished!",
                            color = StrykoTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SmallRoundButton(
                            text = "open session",
                            background = StrykoRed,
                            iconResId = R.drawable.ic_view,
                            onClick = onOpenSession,
                        )
                    }
                    else -> {
                        Text(
                            text = importState.phase?.label ?: "Processing...",
                            color = StrykoTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (importState.isProcessing) {
                    Text(
                        text = "This can take a few minutes.",
                        color = StrykoTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (queuedSessionNames.isNotEmpty()) {
                Text(
                    text = "Up next",
                    color = StrykoBlue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    queuedSessionNames.forEach { sessionName ->
                        QueuedSessionCard(sessionName = sessionName)
                    }
                }
            }
        }
    }
}

// Row showing one video waiting in the processing queue
@Composable
private fun QueuedSessionCard(sessionName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StrykoCard,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sessionName,
                color = StrykoBlue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Queued",
                color = StrykoTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// Format seconds as mm:ss
private fun formatElapsed(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
