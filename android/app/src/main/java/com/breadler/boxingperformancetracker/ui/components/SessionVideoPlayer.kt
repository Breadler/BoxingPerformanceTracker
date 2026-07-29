package com.breadler.boxingperformancetracker.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun SessionVideoPlayer(
    videoUri: Uri?,
    isPlaying: Boolean,
    seekToMs: Long?,
    onPositionUpdate: (Long) -> Unit,
    onDurationLoaded: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    var playbackError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoUri) {
        if (videoUri == null) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            onDurationLoaded(0L)
            playbackError = null
            return@LaunchedEffect
        }

        if (videoUri.scheme == "file") {
            val file = java.io.File(videoUri.path ?: "")
            if (!file.exists()) {
                playbackError = "Video file not found at local path."
                return@LaunchedEffect
            }
        }

        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        playbackError = null
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying, videoUri) {
        if (videoUri == null) {
            return@LaunchedEffect
        }
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(seekToMs) {
        if (videoUri == null || seekToMs == null) {
            return@LaunchedEffect
        }
        exoPlayer.seekTo(seekToMs)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                if (isPlayingNow) {
                    onPositionUpdate(exoPlayer.currentPosition)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onDurationLoaded(exoPlayer.duration.coerceAtLeast(0L))
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "This video could not be played."
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(exoPlayer, isPlaying, videoUri) {
        if (videoUri == null || !isPlaying) {
            return@LaunchedEffect
        }
        while (isPlaying) {
            onPositionUpdate(exoPlayer.currentPosition.coerceAtLeast(0L))
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.pause()
                break
            }
            kotlinx.coroutines.delay(100)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (playbackError != null) {
            Text(
                text = playbackError ?: "This video could not be played.",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (videoUri == null) {
            Text(
                text = "Annotated video replay",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                },
            )
        }
    }
}
