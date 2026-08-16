package com.breadler.boxingperformancetracker.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

// CameraX preview + video capture controller
class CameraRecordingController(
    private val context: Context,
    initialLensFacing: Int = CameraSelector.LENS_FACING_BACK,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var boundPreviewView: PreviewView? = null
    private var boundLifecycleOwner: LifecycleOwner? = null

    private val lensFacingState = mutableIntStateOf(initialLensFacing)
    val lensFacing: State<Int> get() = lensFacingState

    // Attach a preview surface and start the camera
    fun bind(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        boundPreviewView = previewView
        boundLifecycleOwner = lifecycleOwner
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                cameraProvider = providerFuture.get()
                rebind()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    // Toggle front/back camera
    fun switchCamera() {
        if (activeRecording != null) return
        lensFacingState.intValue = if (lensFacingState.intValue == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        rebind()
    }

    // Bind preview + video capture use cases to the lifecycle
    private fun rebind() {
        val provider = cameraProvider ?: return
        val previewView = boundPreviewView ?: return
        val lifecycleOwner = boundLifecycleOwner ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(Quality.FHD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)),
            )
            .build()
        val newVideoCapture = VideoCapture.withOutput(recorder)
        videoCapture = newVideoCapture

        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.Builder().requireLensFacing(lensFacingState.intValue).build(),
                preview,
                newVideoCapture,
            )
        }.onFailure { error ->
            Log.e(TAG, "rebind: failed to bind camera use cases", error)
        }
    }

    // Start recording to file
    fun startRecording(outputFile: File, onFinalized: (Uri?) -> Unit) {
        val capture = videoCapture
        if (capture == null) {
            Log.e(TAG, "startRecording: camera not bound yet")
            onFinalized(null)
            return
        }

        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        activeRecording = capture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    activeRecording = null
                    if (event.hasError()) {
                        Log.e(TAG, "startRecording: finalize error ${event.error}", event.cause)
                        onFinalized(null)
                    } else {
                        onFinalized(Uri.fromFile(outputFile))
                    }
                }
            }
    }

    // Stop the active recording
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    // Release the camera
    fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        videoCapture = null
        boundPreviewView = null
        boundLifecycleOwner = null
    }

    private companion object {
        const val TAG = "CameraRecordingController"
    }
}

// Live camera preview composable
@Composable
fun CameraPreview(
    controller: CameraRecordingController,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            PreviewView(context).apply {
                controller.bind(this, lifecycleOwner)
            }
        },
    )

    DisposableEffect(controller) {
        onDispose { controller.unbind() }
    }
}

// Remembers one controller across recompositions
@Composable
fun rememberCameraRecordingController(
    initialLensFacing: Int = CameraSelector.LENS_FACING_BACK,
): CameraRecordingController {
    val context = LocalContext.current
    return remember { CameraRecordingController(context.applicationContext, initialLensFacing) }
}
