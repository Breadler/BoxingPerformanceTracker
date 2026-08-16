package com.breadler.boxingperformancetracker.data.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.io.File
import kotlin.math.ceil

class PoseFrameExtractor(private val context: Context) {

    // Pose extraction entry point
    fun extract(
        videoUri: Uri,
        frameStride: Int = 1,
        annotatedOutputFile: File? = null,
        onProgress: ((fraction: Float) -> Unit)? = null,
    ): List<FrameObservation> {
        val retriever = MediaMetadataRetriever()
        var videoEncoder: PoseVideoEncoder? = null
        var annotationFailed = false
        var nullBitmapCount = 0

        return try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            if (durationMs <= 0L) {
                Log.e(TAG, "extract: could not read a valid video duration (got $durationMs) for $videoUri")
                return emptyList()
            }

            // Frame count / fps fallback
            val reportedFrameCount = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            val fps = reportedFrameCount?.let { it * 1000.0 / durationMs } ?: DEFAULT_FPS
            val totalFrames = reportedFrameCount ?: ceil(durationMs / 1000.0 * DEFAULT_FPS).toInt()
            Log.d(
                TAG,
                "extract: uri=$videoUri durationMs=$durationMs fps=$fps totalFrames=$totalFrames " +
                    "frameStride=$frameStride annotate=${annotatedOutputFile != null}",
            )

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("pose_landmarker_lite.task")
                        .build(),
                )
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .build()

            val landmarker = try {
                PoseLandmarker.createFromOptions(context, options)
            } catch (error: Exception) {
                Log.e(TAG, "extract: failed to load MediaPipe pose_landmarker_lite.task", error)
                throw error
            }
            try {
                val observations = buildList {
                    for (frameIndex in 0 until totalFrames step frameStride.coerceAtLeast(1)) {
                        val timestampMs = (frameIndex * 1000.0 / fps).toLong()
                        val rawBitmap = retriever.getFrameAtIndex(frameIndex)
                        // Normalize bitmap config to ARGB_8888
                        val bitmap = rawBitmap?.let {
                            if (it.config != Bitmap.Config.ARGB_8888) {
                                it.copy(Bitmap.Config.ARGB_8888, false).also { converted -> it.recycle() }
                            } else {
                                it
                            }
                        }
                        if (bitmap != null) {
                            val observation = detectFrame(landmarker, bitmap, frameIndex + 1, timestampMs)
                            add(observation)

                            if (annotatedOutputFile != null && !annotationFailed) {
                                runCatching {
                                    val annotatedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                    val canvas = Canvas(annotatedBitmap)
                                    if (observation.poseDetected) {
                                        drawSkeleton(canvas, observation.landmarks, annotatedBitmap.width, annotatedBitmap.height)
                                    }
                                    drawFrameLabel(canvas, frameIndex + 1)

                                    val encoder = videoEncoder ?: PoseVideoEncoder(
                                        outputFile = annotatedOutputFile,
                                        width = annotatedBitmap.width,
                                        height = annotatedBitmap.height,
                                    ).also { videoEncoder = it }
                                    encoder.encodeFrame(annotatedBitmap, timestampMs * 1000)
                                    annotatedBitmap.recycle()
                                }.onFailure { error ->
                                    annotationFailed = true
                                    Log.e(TAG, "extract: annotated video encoding failed at frame ${frameIndex + 1} (timestampMs=$timestampMs)", error)
                                }
                            }

                            bitmap.recycle()
                        } else {
                            nullBitmapCount += 1
                        }
                        onProgress?.invoke(((frameIndex + 1).toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f))
                    }
                }
                val poseDetectedCount = observations.count { it.poseDetected }
                Log.d(
                    TAG,
                    "extract: finished with ${observations.size} observations " +
                        "($poseDetectedCount pose-detected, $nullBitmapCount frames could not be decoded), " +
                        "annotatedVideoOk=${annotatedOutputFile != null && !annotationFailed}",
                )
                observations
            } finally {
                landmarker.close()
                if (annotationFailed) {
                    runCatching { videoEncoder?.close() }
                    runCatching { annotatedOutputFile?.takeIf { it.exists() }?.delete() }
                } else {
                    runCatching { videoEncoder?.finish() }
                        .onFailure { error -> Log.e(TAG, "extract: failed to finalize annotated video", error) }
                }
            }
        } finally {
            retriever.release()
        }
    }

    // Run pose detection on one frame
    private fun detectFrame(
        landmarker: PoseLandmarker,
        bitmap: Bitmap,
        frameIndex: Int,
        timestampMs: Long,
    ): FrameObservation {
        val result = landmarker.detectForVideo(BitmapImageBuilder(bitmap).build(), timestampMs)
        val poseLandmarks = result.landmarks().firstOrNull()
        if (poseLandmarks == null) {
            return FrameObservation(
                frameIndex = frameIndex,
                timestampMs = timestampMs,
                poseDetected = false,
                landmarks = emptyMap(),
            )
        }

        val landmarks = boxingLandmarkIndices.associate { index ->
            val landmark = poseLandmarks[index]
            poseLandmarkNames[index] to LandmarkPoint(
                x = landmark.x(),
                y = landmark.y(),
                z = landmark.z(),
                visibility = landmark.visibility().orElse(0f),
            )
        }
        return FrameObservation(
            frameIndex = frameIndex,
            timestampMs = timestampMs,
            poseDetected = true,
            landmarks = landmarks,
        )
    }

    // Draw skeleton points and connecting lines on a frame
    private fun drawSkeleton(
        canvas: Canvas,
        landmarks: Map<String, LandmarkPoint>,
        width: Int,
        height: Int,
    ) {
        val linePaint = Paint().apply {
            color = Color.rgb(0, 200, 0)
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val pointPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        poseConnections.forEach { (startName, endName) ->
            val start = landmarks[startName] ?: return@forEach
            val end = landmarks[endName] ?: return@forEach
            canvas.drawLine(start.x * width, start.y * height, end.x * width, end.y * height, linePaint)
        }
        landmarks.values.forEach { point ->
            canvas.drawCircle(point.x * width, point.y * height, 6f, pointPaint)
        }
    }

    // Draw the frame number in the corner
    private fun drawFrameLabel(canvas: Canvas, frameIndex: Int) {
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        canvas.drawText("Frame $frameIndex", 20f, 44f, textPaint)
    }

    private companion object {
        const val TAG = "PoseFrameExtractor"

        const val DEFAULT_FPS = 30.0

        // Landmark indices used for punch classification
        val boxingLandmarkIndices = listOf(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
        )

        // MediaPipe landmark name order
        val poseLandmarkNames = listOf(
            "nose",
            "left_eye_inner",
            "left_eye",
            "left_eye_outer",
            "right_eye_inner",
            "right_eye",
            "right_eye_outer",
            "left_ear",
            "right_ear",
            "mouth_left",
            "mouth_right",
            "left_shoulder",
            "right_shoulder",
            "left_elbow",
            "right_elbow",
            "left_wrist",
            "right_wrist",
            "left_pinky",
            "right_pinky",
            "left_index",
            "right_index",
            "left_thumb",
            "right_thumb",
            "left_hip",
            "right_hip",
            "left_knee",
            "right_knee",
            "left_ankle",
            "right_ankle",
            "left_heel",
            "right_heel",
            "left_foot_index",
            "right_foot_index",
        )

        // Skeleton line connections for overlay drawing
        val poseConnections = listOf(
            "nose" to "left_eye_inner",
            "left_eye_inner" to "left_eye",
            "left_eye" to "left_eye_outer",
            "left_eye_outer" to "left_ear",
            "nose" to "right_eye_inner",
            "right_eye_inner" to "right_eye",
            "right_eye" to "right_eye_outer",
            "right_eye_outer" to "right_ear",
            "mouth_left" to "mouth_right",
            "left_shoulder" to "right_shoulder",
            "left_shoulder" to "left_elbow",
            "left_elbow" to "left_wrist",
            "right_shoulder" to "right_elbow",
            "right_elbow" to "right_wrist",
            "left_shoulder" to "left_hip",
            "right_shoulder" to "right_hip",
            "left_hip" to "right_hip",
            "left_hip" to "left_knee",
            "right_hip" to "right_knee",
            "left_knee" to "left_ankle",
            "right_knee" to "right_ankle",
            "left_ankle" to "left_heel",
            "right_ankle" to "right_heel",
            "left_heel" to "left_foot_index",
            "right_heel" to "right_foot_index",
            "left_ankle" to "left_foot_index",
            "right_ankle" to "right_foot_index",
        )
    }
}
