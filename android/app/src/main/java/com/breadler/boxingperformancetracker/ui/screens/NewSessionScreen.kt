package com.breadler.boxingperformancetracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.ui.components.CameraPreview
import com.breadler.boxingperformancetracker.ui.components.RecordButton
import com.breadler.boxingperformancetracker.ui.components.SmallRoundButton
import com.breadler.boxingperformancetracker.ui.components.TimerSection
import com.breadler.boxingperformancetracker.ui.components.rememberCameraRecordingController
import com.breadler.boxingperformancetracker.ui.components.rememberCountdownBeeper
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlack
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoSystemBars
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private enum class RecordingPhase { IDLE, PREPARING, RECORDING }

@Composable
fun NewSessionScreen(
    onImportVideo: (Uri) -> Unit,
    onExit: () -> Unit,
    initialPrepareSeconds: Int,
    initialWorkSeconds: Int,
    initialUseFrontCamera: Boolean,
    onPrepareSecondsChanged: (Int) -> Unit,
    onWorkSecondsChanged: (Int) -> Unit,
    onUseFrontCameraChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoRed, navigationBarColor = StrykoRed)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraController = rememberCameraRecordingController(
        initialLensFacing = if (initialUseFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK,
    )
    val beeper = rememberCountdownBeeper()

    var prepareSeconds by remember { mutableIntStateOf(initialPrepareSeconds) }
    var workSeconds by remember { mutableIntStateOf(initialWorkSeconds) }
    var phase by remember { mutableStateOf(RecordingPhase.IDLE) }
    var prepareRemainingSeconds by remember { mutableIntStateOf(prepareSeconds) }
    var workRemainingSeconds by remember { mutableIntStateOf(workSeconds) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose { recordingJob?.cancel() }
    }

    fun cancelActiveRecording() {
        recordingJob?.cancel()
        recordingJob = null
        cameraController.stopRecording()
        phase = RecordingPhase.IDLE
    }

    fun startRecordingSession() {
        recordingJob = scope.launch {
            phase = RecordingPhase.PREPARING
            prepareRemainingSeconds = prepareSeconds
            while (prepareRemainingSeconds > 0) {
                delay(1000)
                prepareRemainingSeconds -= 1
                if (prepareRemainingSeconds in 1..3) beeper.tick()
            }

            phase = RecordingPhase.RECORDING
            beeper.playStart()
            workRemainingSeconds = workSeconds
            val outputDir = File(context.cacheDir, "camera_recordings").apply { mkdirs() }
            val outputFile = File(outputDir, "${UUID.randomUUID()}.mp4")

            val finalizedUri = CompletableDeferred<Uri?>()
            cameraController.startRecording(outputFile) { uri -> finalizedUri.complete(uri) }

            while (workRemainingSeconds > 0) {
                delay(1000)
                workRemainingSeconds -= 1
                if (workRemainingSeconds in 1..3) beeper.tick()
            }
            cameraController.stopRecording()

            val resultUri = finalizedUri.await()
            beeper.playEnd()
            phase = RecordingPhase.IDLE
            recordingJob = null
            if (resultUri != null) {
                onImportVideo(resultUri)
            } else {
                Log.e("NewSessionScreen", "startRecordingSession: recording failed, nothing to import")
            }
        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                    text = "import",
                    background = StrykoRed,
                    iconResId = R.drawable.ic_import,
                    onClick = { openDocumentLauncher.launch(arrayOf("video/*")) },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
            ) {
                if (hasCameraPermission) {
                    CameraPreview(
                        controller = cameraController,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(15.dp)),
                    )
                } else {
                    Surface(
                        color = StrykoBlack,
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Camera access is needed to record a session",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = StrykoRed),
                            ) {
                                Text(text = "Grant camera access", color = StrykoCard, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                RecordButton(
                    active = phase != RecordingPhase.IDLE,
                    onClick = {
                        if (!hasCameraPermission) return@RecordButton
                        if (phase == RecordingPhase.IDLE) startRecordingSession() else cancelActiveRecording()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 43.dp),
                )

                // Switching cameras mid-recording isn't supported cleanly by CameraX's
                // VideoCapture, so this is only offered before a session starts.
                if (hasCameraPermission && phase == RecordingPhase.IDLE) {
                    IconButton(
                        onClick = {
                            cameraController.switchCamera()
                            onUseFrontCameraChanged(cameraController.lensFacing.value == CameraSelector.LENS_FACING_FRONT)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch camera",
                            tint = Color.White,
                        )
                    }
                }
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
                time = formatDuration(if (phase == RecordingPhase.IDLE) prepareSeconds else prepareRemainingSeconds),
                onMinus = {
                    prepareSeconds = (prepareSeconds - 5).coerceAtLeast(0)
                    onPrepareSecondsChanged(prepareSeconds)
                },
                onPlus = {
                    prepareSeconds += 5
                    onPrepareSecondsChanged(prepareSeconds)
                },
                adjustable = phase == RecordingPhase.IDLE,
            )
            TimerSection(
                title = "WORK",
                time = formatDuration(if (phase == RecordingPhase.RECORDING) workRemainingSeconds else workSeconds),
                onMinus = {
                    workSeconds = (workSeconds - 15).coerceAtLeast(0)
                    onWorkSecondsChanged(workSeconds)
                },
                onPlus = {
                    workSeconds += 15
                    onWorkSecondsChanged(workSeconds)
                },
                adjustable = phase == RecordingPhase.IDLE,
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
