package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.data.SessionProcessingState
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted

// Docked processing status bar
@Composable
fun ProcessingStatusBar(
    state: SessionProcessingState,
    onTapProgress: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = StrykoRed,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.isProcessing, onClick = onTapProgress)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.sessionName,
                    color = StrykoCard,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                val subtitle = when {
                    state.errorMessage != null -> state.errorMessage
                    state.completedSessionId != null -> "Ready to view"
                    else -> state.phase?.label ?: "Processing..."
                }
                Text(
                    text = subtitle,
                    color = if (state.errorMessage != null) Color.White else StrykoTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (state.errorMessage != null) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (state.isProcessing) {
                    val progress = state.progress
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = Color.White,
                            trackColor = StrykoTextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                        )
                    } else {
                        LinearProgressIndicator(
                            color = Color.White,
                            trackColor = StrykoTextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                        )
                    }
                }
            }

            if (!state.isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.completedSessionId != null) {
                        IconButton(onClick = onOpen) {
                            Icon(
                                painter = painterResource(R.drawable.ic_view),
                                contentDescription = "Open session",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
