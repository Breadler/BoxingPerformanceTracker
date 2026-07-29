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

class PoseFrameExtractor(private val context: Context) {

    /**
     * Runs MediaPipe pose detection across the video at [frameStrideMs] increments.
     *
     * When [annotatedOutputFile] is provided, this also renders a skeleton-only
     * overlay (no punch predictions) for each processed frame and muxes it into a
     * local mp4, mirroring the extractor's annotated review video. If annotated
     * video generation fails for any reason, pose extraction still completes and
     * the partially written file (if any) is discarded.
     */
    fun extract(
        videoUri: Uri,
        frameStrideMs: Long,
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
            Log.d(TAG, "extract: uri=$videoUri durationMs=$durationMs frameStrideMs=$frameStrideMs annotate=${annotatedOutputFile != null}")
            if (durationMs <= 0L) {
                Log.e(TAG, "extract: could not read a valid video duration (got $durationMs) for $videoUri")
                return emptyList()
            }

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
                    var frameIndex = 1
                    var timestampMs = 0L
                    while (timestampMs <= durationMs) {
                        val rawBitmap = retriever.getFrameAtTime(
                            timestampMs * 1000,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                        )
                        // MediaPipe's BitmapImageBuilder requires ARGB_8888. getFrameAtTime can
                        // return other configs (RGB_565, HARDWARE, RGBA_F16) depending on the
                        // device/codec, so normalize before doing anything else with the frame.
                        val bitmap = rawBitmap?.let {
                            if (it.config != Bitmap.Config.ARGB_8888) {
                                it.copy(Bitmap.Config.ARGB_8888, false).also { converted -> it.recycle() }
                            } else {
                                it
                            }
                        }
                        if (bitmap != null) {
                            val observation = detectFrame(landmarker, bitmap, frameIndex, timestampMs)
                            add(observation)

                            if (annotatedOutputFile != null && !annotationFailed) {
                                runCatching {
                                    val annotatedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                    val canvas = Canvas(annotatedBitmap)
                                    if (observation.poseDetected) {
                                        drawSkeleton(canvas, observation.landmarks, annotatedBitmap.width, annotatedBitmap.height)
                                    }
                                    drawFrameLabel(canvas, frameIndex)

                                    val encoder = videoEncoder ?: PoseVideoEncoder(
                                        outputFile = annotatedOutputFile,
                                        width = annotatedBitmap.width,
                                        height = annotatedBitmap.height,
                                    ).also { videoEncoder = it }
                                    encoder.encodeFrame(annotatedBitmap, timestampMs * 1000)
                                    annotatedBitmap.recycle()
                                }.onFailure { error ->
                                    annotationFailed = true
                                    Log.e(TAG, "extract: annotated video encoding failed at frame $frameIndex (timestampMs=$timestampMs)", error)
                                }
                            }

                            bitmap.recycle()
                        } else {
                            nullBitmapCount += 1
                        }
                        frameIndex += 1
                        timestampMs += frameStrideMs.coerceAtLeast(1L)
                        onProgress?.invoke((timestampMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
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

        val boxingLandmarkIndices = listOf(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
        )

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

        // POSE_CONNECTIONS from pose_extractor.py, restricted to the boxing landmark
        // subset (drops fingertip connections for indices 17-22, which are excluded
        // from BOXING_LANDMARK_INDICES).
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
