package com.breadler.boxingperformancetracker.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.data.SessionCardItem
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOLD_TO_DELETE_MS = 900

@Composable
fun SessionCard(
    session: SessionCardItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteModeActive by remember(session.id) { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StrykoCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { deleteModeActive = !deleteModeActive }) {
                Icon(
                    imageVector = if (deleteModeActive) Icons.Default.Close else Icons.Default.MoreVert,
                    contentDescription = if (deleteModeActive) "Cancel delete" else "Session options",
                    tint = StrykoBlue,
                )
            }

            if (deleteModeActive) {
                HoldToDeleteBar(
                    onDeleteConfirmed = {
                        deleteModeActive = false
                        onDelete()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
            } else {
                SessionThumbnail(
                    thumbnailUri = session.thumbnailUri,
                    modifier = Modifier.size(width = 100.dp, height = 74.dp),
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = session.dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StrykoBlue,
                    )
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = StrykoBlue,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = session.durationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StrykoTextMuted,
                    )
                }

                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(width = 52.dp, height = 74.dp)
                        .background(StrykoBlue, RoundedCornerShape(26.dp)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_view),
                        contentDescription = "Open session",
                        tint = StrykoCard,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
        }
    }
}

/** In-memory only - thumbnails are already small (max 480px) local JPEGs, this just
 * avoids re-decoding one on every scroll pass through the list. */
private object SessionThumbnailCache {
    private val cache = mutableMapOf<String, Bitmap?>()

    fun get(uri: String): Bitmap? = cache[uri]
    fun put(uri: String, bitmap: Bitmap?) {
        cache[uri] = bitmap
    }
}

@Composable
private fun SessionThumbnail(
    thumbnailUri: String?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(thumbnailUri) {
        mutableStateOf(thumbnailUri?.let(SessionThumbnailCache::get))
    }

    LaunchedEffect(thumbnailUri) {
        if (thumbnailUri != null && bitmap == null) {
            val decoded = withContext(Dispatchers.IO) {
                runCatching {
                    val path = Uri.parse(thumbnailUri).path ?: return@runCatching null
                    BitmapFactory.decodeFile(path)
                }.getOrNull()
            }
            SessionThumbnailCache.put(thumbnailUri, decoded)
            bitmap = decoded
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Press-and-hold confirmation: a red fill grows from the left over
 * [HOLD_TO_DELETE_MS] while held, triggering [onDeleteConfirmed] if the hold
 * completes. Releasing early resets the fill back to empty instead of deleting -
 * a plain tap can't trigger it, only a sustained hold. */
@Composable
private fun HoldToDeleteBar(
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StrykoRed.copy(alpha = 0.25f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val holdJob = scope.launch {
                        progress.animateTo(1f, animationSpec = tween(HOLD_TO_DELETE_MS, easing = LinearEasing))
                        onDeleteConfirmed()
                    }
                    waitForUpOrCancellation()
                    holdJob.cancel()
                    scope.launch { progress.animateTo(0f, animationSpec = tween(200)) }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(StrykoRed, RoundedCornerShape(14.dp)),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Hold to delete", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}