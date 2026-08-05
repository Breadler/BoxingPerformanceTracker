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

/**
 * Owns the CameraX preview + video-capture use cases for one recording session.
 * Video-only (no audio track/permission) - the on-device pipeline only ever looks
 * at pose landmarks, so there's nothing to gain from asking for microphone access.
 */
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

    /** Switches between front/back and rebinds. No-ops while a recording is in
     * progress - CameraX doesn't support swapping the camera under an active
     * VideoCapture cleanly, and the caller should be disabling this during
     * recording anyway. */
    fun switchCamera() {
        if (activeRecording != null) return
        lensFacingState.intValue = if (lensFacingState.intValue == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        rebind()
    }

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

    /** Starts recording to [outputFile]. [onFinalized] fires exactly once, on the main
     * executor, with the resulting file's Uri on success or null on failure. */
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

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

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

/** Live camera preview, bound/unbound to [controller] across this composable's lifetime.
 * The preview starts as soon as this enters composition - callers don't need to do
 * anything else to "open" the camera. */
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

@Composable
fun rememberCameraRecordingController(
    initialLensFacing: Int = CameraSelector.LENS_FACING_BACK,
): CameraRecordingController {
    val context = LocalContext.current
    return remember { CameraRecordingController(context.applicationContext, initialLensFacing) }
}
