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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.data.SessionProcessingState
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
    importState: SessionProcessingState,
    onImportVideo: (Uri) -> Unit,
    onImportFinished: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoRed, navigationBarColor = StrykoRed)

    var isRecording by remember { mutableStateOf(false) }
    var prepareSeconds by remember { mutableStateOf(30) }
    var workSeconds by remember { mutableStateOf(180) }

    LaunchedEffect(importState.completedSessionId) {
        importState.completedSessionId?.let(onImportFinished)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            onImportVideo(uri)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = StrykoBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    text = "exit",
                    backgroundColor = StrykoBlue,
                    iconResId = R.drawable.ic_back,
                    onClick = onExit,
                )
                SmallActionChip(
                    text = "import",
                    backgroundColor = StrykoRed,
                    iconResId = R.drawable.ic_import,
                    onClick = { openDocumentLauncher.launch(arrayOf("video/*")) },
                )
            }

            if (importState.statusMessage.isNotBlank()) {
                Text(
                    text = importState.statusMessage,
                    color = if (importState.errorMessage == null) StrykoTextMuted else StrykoRed,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            importState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = StrykoRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
            ) {
                Surface(
                    color = StrykoBlack,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {}

                RecordButton(
                    active = isRecording || importState.isProcessing,
                    onClick = { if (!importState.isProcessing) isRecording = !isRecording },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 43.dp),
                )
            }

            Spacer(modifier = Modifier.height(38.dp))

            Text(
                text = "make sure your full body is in frame",
                color = StrykoTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            TimerSection(
                title = "PREPARE",
                time = formatDuration(prepareSeconds),
                onMinus = { prepareSeconds = (prepareSeconds - 5).coerceAtLeast(0) },
                onPlus = { prepareSeconds += 5 },
            )
            TimerSection(
                title = "WORK",
                time = formatDuration(workSeconds),
                onMinus = { workSeconds = (workSeconds - 15).coerceAtLeast(0) },
                onPlus = { workSeconds += 15 },
            )
        }

        if (importState.isProcessing) {
            ProcessingOverlay(importState = importState)
        }
        }
    }
}

@Composable
private fun ProcessingOverlay(importState: SessionProcessingState) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StrykoBlack.copy(alpha = 0.88f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val progress = importState.progress
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = StrykoRed,
                    trackColor = StrykoCard,
                    modifier = Modifier.size(64.dp),
                )
            } else {
                CircularProgressIndicator(color = StrykoRed, trackColor = StrykoCard, modifier = Modifier.size(64.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Analyzing on this device",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = importState.statusMessage.ifBlank { "Processing..." },
                color = StrykoTextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = StrykoRed,
                    trackColor = StrykoCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This can take a few minutes for longer videos — MediaPipe pose extraction, the annotated skeleton video, and the punch model all run locally on your phone.",
                color = StrykoTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
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
            Text(text = text, color = StrykoCard, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
