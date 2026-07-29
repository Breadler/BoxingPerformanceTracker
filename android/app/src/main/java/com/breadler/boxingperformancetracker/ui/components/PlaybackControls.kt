package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.ui.theme.PunchHighlight
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted

@Composable
fun PlaybackControls(
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onScrub: (Long) -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val sliderValue = currentPositionMs.toFloat() / safeDuration.toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = { value ->
                onScrub((value * safeDuration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = PunchHighlight,
                activeTrackColor = PunchHighlight,
                inactiveTrackColor = PunchHighlight.copy(alpha = 0.25f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            timeDivisions(safeDuration).forEach { tickMs ->
                Text(text = formatTimeLabel(tickMs), color = StrykoTextMuted)
            }
        }

        Surface(
            onClick = onTogglePlayback,
            shape = RoundedCornerShape(50.dp),
            color = StrykoCard,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = StrykoBlue,
                )
            }
        }
    }
}

private fun timeDivisions(durationMs: Long, tickCount: Int = 6): List<Long> {
    if (tickCount < 2) return listOf(0L, durationMs)
    return (0 until tickCount).map { index -> durationMs * index / (tickCount - 1) }
}

private fun formatTimeLabel(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
