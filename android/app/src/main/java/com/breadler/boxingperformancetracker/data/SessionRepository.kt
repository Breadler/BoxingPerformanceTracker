package com.breadler.boxingperformancetracker.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.breadler.boxingperformancetracker.data.processing.OnDeviceSessionProcessor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SessionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = SessionDatabase.getInstance(appContext).sessionDao()
    private val gson = Gson()

    val sessions: Flow<List<SessionSummary>> = dao.observeSessions().map { entities ->
        entities.map { it.toDomain() }
    }

    // Single session lookup
    suspend fun getSession(sessionId: String): SessionSummary? {
        return dao.getSession(sessionId)?.toDomain()
    }

    // Delete session row and its local files
    suspend fun deleteSession(sessionId: String): Unit = withContext(Dispatchers.IO) {
        val entity = dao.getSession(sessionId) ?: return@withContext
        dao.delete(sessionId)
        deleteLocalFile(entity.sourceVideoUri)
        deleteLocalFile(entity.annotatedVideoUri)
        deleteLocalFile(entity.thumbnailUri)
    }

    // Delete one file on disk, ignoring missing/invalid paths
    private fun deleteLocalFile(uriString: String) {
        if (uriString.isBlank()) return
        runCatching {
            val path = Uri.parse(uriString).path ?: return
            File(path).takeIf { it.exists() }?.delete()
        }.onFailure { error ->
            Log.e(TAG, "deleteLocalFile: failed to delete $uriString", error)
        }
    }

    // Copy, process, and save one imported video as a new session
    suspend fun importVideo(
        videoUri: Uri,
        sessionName: String,
        onProgress: (phase: ProcessingPhase, fraction: Float?) -> Unit = { _, _ -> },
    ): Result<SessionSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val sessionId = UUID.randomUUID().toString()
            onProgress(ProcessingPhase.COPYING, 0f)
            val sourceName = queryDisplayName(videoUri) ?: "Imported video"
            val storedVideo = copyUriToSessionStorage(videoUri, sourceName)
            val storedVideoUri = Uri.fromFile(storedVideo)
            val durationMs = readVideoDurationMs(videoUri)

            onProgress(ProcessingPhase.LOADING_MODELS, 0.05f)
            val annotatedOutputFile = annotatedVideoFile(sessionId)
            Log.d(TAG, "importVideo: sessionId=$sessionId source=$sourceName durationMs=$durationMs")
            val processingResult = OnDeviceSessionProcessor(appContext).process(
                storedVideoUri,
                annotatedOutputFile,
            ) { phase, fraction ->
                val overall = when (phase) {
                    ProcessingPhase.EXTRACTING -> 0.05f + fraction * 0.8f
                    ProcessingPhase.DETECTING -> 0.85f
                    else -> null
                }
                onProgress(phase, overall)
            }
            onProgress(ProcessingPhase.SAVING, 0.95f)

            val annotatedVideoUri = processingResult.annotatedVideoPath?.let { Uri.fromFile(File(it)).toString() }
            val thumbnailSourceFile = processingResult.annotatedVideoPath?.let(::File) ?: storedVideo
            val thumbnailUri = generateThumbnail(sessionId, thumbnailSourceFile)
            val importedAt = Date()
            val session = SessionSummary(
                id = sessionId,
                title = sessionName,
                dateLabel = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(importedAt),
                durationLabel = formatDurationLabel(durationMs),
                durationMs = durationMs,
                sourceVideoUri = storedVideoUri.toString(),
                annotatedVideoUri = annotatedVideoUri,
                thumbnailUri = thumbnailUri,
                sourceVideoName = sourceName,
                punchWindows = processingResult.punchWindows,
                punchPredictions = processingResult.punchPredictions,
                performancePoints = processingResult.performancePoints,
                punchCount = processingResult.punchWindows.size,
            )
            dao.upsert(session.toEntity())
            Log.d(
                TAG,
                "importVideo: sessionId=$sessionId saved with punchCount=${session.punchCount}, " +
                    "annotatedVideo=${annotatedVideoUri != null}, thumbnail=${thumbnailUri != null}",
            )
            session
        }.onFailure { error ->
            Log.e(TAG, "importVideo: failed", error)
        }
    }

    // Path for a session's annotated video file
    private fun annotatedVideoFile(sessionId: String): File {
        val annotatedDir = File(appContext.filesDir, "session_videos/annotated")
        annotatedDir.mkdirs()
        return File(annotatedDir, "$sessionId.mp4")
    }

    // Generate scaled-down JPEG thumbnail from video
    private fun generateThumbnail(sessionId: String, videoFile: File): String? {
        if (!videoFile.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frameTimeUs = (durationMs * 1000L / 4).coerceAtLeast(0L)
            val frame = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: return null

            val scale = THUMBNAIL_MAX_DIMENSION_PX.toFloat() / maxOf(frame.width, frame.height)
            val scaledFrame = if (scale < 1f) {
                Bitmap.createScaledBitmap(frame, (frame.width * scale).toInt(), (frame.height * scale).toInt(), true)
            } else {
                frame
            }

            val thumbnailDir = File(appContext.filesDir, "session_videos/thumbnails").apply { mkdirs() }
            val thumbnailFile = File(thumbnailDir, "$sessionId.jpg")
            thumbnailFile.outputStream().use { output ->
                scaledFrame.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_JPEG_QUALITY, output)
            }
            Uri.fromFile(thumbnailFile).toString()
        } catch (error: Exception) {
            Log.e(TAG, "generateThumbnail: failed for sessionId=$sessionId", error)
            null
        } finally {
            retriever.release()
        }
    }

    // Copy a content Uri into this app's private storage
    private fun copyUriToSessionStorage(videoUri: Uri, sourceName: String): File {
        val sessionVideoDir = File(appContext.filesDir, "session_videos")
        sessionVideoDir.mkdirs()
        val extension = sourceName.substringAfterLast('.', missingDelimiterValue = "mp4").ifBlank { "mp4" }
        val targetFile = File(sessionVideoDir, "${UUID.randomUUID()}.$extension")
        appContext.contentResolver.openInputStream(videoUri).use { inputStream ->
            if (inputStream == null) {
                throw IOException("Could not open the selected video.")
            }
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return targetFile
    }

    // Read a video's duration via MediaMetadataRetriever
    private fun readVideoDurationMs(videoUri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, videoUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    // Look up a content Uri's display name
    private fun queryDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }

    // Room entity -> domain model
    private fun SessionEntity.toDomain(): SessionSummary {
        return SessionSummary(
            id = id,
            title = title,
            dateLabel = dateLabel,
            durationLabel = durationLabel,
            durationMs = durationMs,
            sourceVideoUri = sourceVideoUri.ifBlank { null },
            annotatedVideoUri = annotatedVideoUri.ifBlank { null },
            thumbnailUri = thumbnailUri.ifBlank { null },
            sourceVideoName = sourceVideoName,
            punchCount = punchCount,
            punchWindows = gson.fromJson(punchWindowsJson, punchWindowListType()),
            punchPredictions = gson.fromJson(predictionWindowsJson, punchPredictionListType()),
            performancePoints = gson.fromJson(performancePointsJson, performancePointListType()),
        )
    }

    // Domain model -> Room entity
    private fun SessionSummary.toEntity(): SessionEntity {
        return SessionEntity(
            id = id,
            title = title,
            dateLabel = dateLabel,
            durationLabel = durationLabel,
            durationMs = durationMs,
            sourceVideoName = sourceVideoName ?: title,
            sourceVideoUri = sourceVideoUri.orEmpty(),
            annotatedVideoUri = annotatedVideoUri.orEmpty(),
            thumbnailUri = thumbnailUri.orEmpty(),
            punchCount = punchCount,
            processedAtMs = System.currentTimeMillis(),
            punchWindowsJson = gson.toJson(punchWindows),
            predictionWindowsJson = gson.toJson(punchPredictions),
            performancePointsJson = gson.toJson(performancePoints),
        )
    }

    // Gson type tokens for JSON list columns
    private fun punchWindowListType() = object : TypeToken<List<PunchWindow>>() {}.type
    private fun punchPredictionListType() = object : TypeToken<List<PunchPrediction>>() {}.type
    private fun performancePointListType() = object : TypeToken<List<PerformancePoint>>() {}.type

    // Format milliseconds as m:ss
    private fun formatDurationLabel(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private companion object {
        const val TAG = "SessionRepository"
        const val THUMBNAIL_MAX_DIMENSION_PX = 480
        const val THUMBNAIL_JPEG_QUALITY = 85
    }
}
