package com.breadler.boxingperformancetracker.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.breadler.boxingperformancetracker.R

private const val TICK_DURATION_MS = 150

// Countdown/cue sounds singleton
object CountdownBeeper {
    private var appContext: Context? = null
    private val toneGenerator by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME) }
    private val activePlayers = mutableSetOf<MediaPlayer>()

    // Capture the application context once
    private fun bind(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    // Short countdown beep
    fun tick() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, TICK_DURATION_MS)
    }

    fun playStart() = playRaw(R.raw.audio_bell_start)

    fun playEnd() = playRaw(R.raw.audio_bell_end)

    // Play a raw audio resource once
    private fun playRaw(resId: Int) {
        val context = appContext ?: return
        val player = MediaPlayer.create(context, resId) ?: return
        activePlayers += player
        player.setOnCompletionListener {
            activePlayers -= it
            it.release()
        }
        player.start()
    }

    @Composable
    fun rememberBound(): CountdownBeeper {
        bind(LocalContext.current)
        return this
    }
}

@Composable
fun rememberCountdownBeeper(): CountdownBeeper = CountdownBeeper.rememberBound()
