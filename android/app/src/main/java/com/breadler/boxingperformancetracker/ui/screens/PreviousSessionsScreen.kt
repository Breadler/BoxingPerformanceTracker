package com.breadler.boxingperformancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.data.SessionSummary
import com.breadler.boxingperformancetracker.ui.components.SessionCard
import com.breadler.boxingperformancetracker.ui.components.SmallRoundButton
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoSystemBars

// Scrollable list of processed sessions, or an empty-state message
@Composable
fun PreviousSessionsScreen(
    sessions: List<SessionSummary>,
    onExit: () -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoBlue, navigationBarColor = StrykoBlue)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = StrykoBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            RowHeader(onBack = onExit)

            Spacer(modifier = Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                Text(
                    text = "Processed sessions will appear here after import completes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StrykoBlue,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onPlay = { onOpenSession(session.id) },
                            onDelete = { onDeleteSession(session.id) },
                        )
                    }
                }
            }
        }
    }
}

// Top bar with an exit button
@Composable
private fun RowHeader(
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallRoundButton(
            text = "exit",
            background = StrykoBlue,
            iconResId = R.drawable.ic_back,
            onClick = onBack,
        )
    }
}