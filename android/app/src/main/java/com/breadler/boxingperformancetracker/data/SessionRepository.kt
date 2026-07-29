package com.breadler.boxingperformancetracker.data

import android.content.Context
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

    suspend fun getSession(sessionId: String): SessionSummary? {
        return dao.getSession(sessionId)?.toDomain()
    }

    suspend fun importVideo(
        videoUri: Uri,
        onProgress: (status: String, fraction: Float?) -> Unit = { _, _ -> },
    ): Result<SessionSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val sessionId = UUID.randomUUID().toString()
            onProgress("Copying video to device storage...", 0f)
            val sourceName = queryDisplayName(videoUri) ?: "Imported video"
            val storedVideo = copyUriToSessionStorage(videoUri, sourceName)
            val storedVideoUri = Uri.fromFile(storedVideo)
            val durationMs = readVideoDurationMs(videoUri)

            onProgress("Loading on-device pose and punch models...", 0.05f)
            val annotatedOutputFile = annotatedVideoFile(sessionId)
            Log.d(TAG, "importVideo: sessionId=$sessionId source=$sourceName durationMs=$durationMs")
            val processingResult = OnDeviceSessionProcessor(appContext).process(
                storedVideoUri,
                annotatedOutputFile,
            ) { fraction ->
                onProgress("Analyzing pose and punches (${(fraction * 100).toInt()}%)...", 0.05f + fraction * 0.9f)
            }
            onProgress("Saving session...", 0.97f)

            val annotatedVideoUri = processingResult.annotatedVideoPath?.let { Uri.fromFile(File(it)).toString() }
            val session = SessionSummary(
                id = sessionId,
                title = sourceName.substringBeforeLast(".", sourceName),
                dateLabel = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                durationLabel = formatDurationLabel(durationMs),
                durationMs = durationMs,
                sourceVideoUri = storedVideoUri.toString(),
                annotatedVideoUri = annotatedVideoUri,
                sourceVideoName = sourceName,
                punchWindows = processingResult.punchWindows,
                punchPredictions = processingResult.punchPredictions,
                performancePoints = processingResult.performancePoints,
                punchCount = processingResult.punchWindows.size,
            )
            dao.upsert(session.toEntity())
            Log.d(TAG, "importVideo: sessionId=$sessionId saved with punchCount=${session.punchCount}, annotatedVideo=${annotatedVideoUri != null}")
            session
        }.onFailure { error ->
            Log.e(TAG, "importVideo: failed", error)
        }
    }

    private fun annotatedVideoFile(sessionId: String): File {
        val annotatedDir = File(appContext.filesDir, "session_videos/annotated")
        annotatedDir.mkdirs()
        return File(annotatedDir, "$sessionId.mp4")
    }

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

    private fun readVideoDurationMs(videoUri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, videoUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

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

    private fun SessionEntity.toDomain(): SessionSummary {
        return SessionSummary(
            id = id,
            title = title,
            dateLabel = dateLabel,
            durationLabel = durationLabel,
            durationMs = durationMs,
            sourceVideoUri = sourceVideoUri.ifBlank { null },
            annotatedVideoUri = annotatedVideoUri.ifBlank { null },
            sourceVideoName = sourceVideoName,
            punchCount = punchCount,
            punchWindows = gson.fromJson(punchWindowsJson, punchWindowListType()),
            punchPredictions = gson.fromJson(predictionWindowsJson, punchPredictionListType()),
            performancePoints = gson.fromJson(performancePointsJson, performancePointListType()),
        )
    }

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
            punchCount = punchCount,
            processedAtMs = System.currentTimeMillis(),
            punchWindowsJson = gson.toJson(punchWindows),
            predictionWindowsJson = gson.toJson(punchPredictions),
            performancePointsJson = gson.toJson(performancePoints),
        )
    }

    private fun punchWindowListType() = object : TypeToken<List<PunchWindow>>() {}.type
    private fun punchPredictionListType() = object : TypeToken<List<PunchPrediction>>() {}.type
    private fun performancePointListType() = object : TypeToken<List<PerformancePoint>>() {}.type

    private fun formatDurationLabel(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private companion object {
        const val TAG = "SessionRepository"
    }
}
