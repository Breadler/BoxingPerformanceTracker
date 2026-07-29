package com.breadler.boxingperformancetracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.ui.components.RecordButton
import com.breadler.boxingperformancetracker.ui.components.TimerSection
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlack
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoSystemBars
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted

@Composable
fun NewSessionScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoRed, navigationBarColor = StrykoRed)

    var selectedVideoName by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var prepareSeconds by remember { mutableStateOf(30) }
    var workSeconds by remember { mutableStateOf(180) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        selectedVideoName = uri?.lastPathSegment ?: "Imported video"
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
                SmallActionChip(
                    text = "Exit",
                    backgroundColor = StrykoBlue,
                    iconResId = R.drawable.ic_back,
                    onClick = onExit,
                )
                SmallActionChip(
                    text = "Import",
                    backgroundColor = StrykoRed,
                    iconResId = R.drawable.ic_import,
                    onClick = { openDocumentLauncher.launch(arrayOf("video/*")) },
                )
            }

            Text(
                text = "New Session",
                style = MaterialTheme.typography.titleLarge,
                color = StrykoBlue,
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
            ) {
                Surface(
                    color = StrykoBlack,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = selectedVideoName ?: "Camera preview",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRecording) "Recording..." else "Ready to record",
                            color = StrykoTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                RecordButton(
                    active = isRecording,
                    onClick = { isRecording = !isRecording },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimerSection(
                    title = "PREPARE",
                    time = formatDuration(prepareSeconds),
                    onMinus = { prepareSeconds = (prepareSeconds - 5).coerceAtLeast(0) },
                    onPlus = { prepareSeconds += 5 },
                    modifier = Modifier.weight(1f),
                )
                TimerSection(
                    title = "WORK",
                    time = formatDuration(workSeconds),
                    onMinus = { workSeconds = (workSeconds - 15).coerceAtLeast(0) },
                    onPlus = { workSeconds += 15 },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "Use the record control over the preview to begin a round.",
                color = StrykoTextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SmallActionChip(
    text: String,
    backgroundColor: Color,
    iconResId: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painter = painterResource(iconResId), contentDescription = null, tint = StrykoCard, modifier = Modifier.size(18.dp))
            Text(text = text, color = StrykoCard, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}