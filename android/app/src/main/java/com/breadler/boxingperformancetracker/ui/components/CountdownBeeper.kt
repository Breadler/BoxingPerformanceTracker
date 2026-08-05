package com.breadler.boxingperformancetracker.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.breadler.boxingperformancetracker.R

private const val TICK_DURATION_MS = 150

/**
 * Countdown/cue sounds for a recording session: [tick] is a short synthesized beep for
 * the last-3-seconds countdown warning, [playStart]/[playEnd] each play their bell audio
 * file once for "recording started"/"recording finished".
 *
 * This is a process-lifetime singleton, not tied to any screen's composition. [playEnd] in
 * particular fires right before New Session navigates away to the processing screen - if the
 * MediaPlayer were only reachable through that screen's remembered state, it became eligible
 * for garbage collection the instant the screen was disposed, which was cutting the sound off
 * mid-playback (a native MediaPlayer keeps playing regardless of Java-level reachability, but
 * only until GC actually collects it).
 */
object CountdownBeeper {
    private var appContext: Context? = null
    private val toneGenerator by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME) }
    private val activePlayers = mutableSetOf<MediaPlayer>()

    private fun bind(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun tick() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, TICK_DURATION_MS)
    }

    fun playStart() = playRaw(R.raw.audio_bell_start)

    fun playEnd() = playRaw(R.raw.audio_bell_end)

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
