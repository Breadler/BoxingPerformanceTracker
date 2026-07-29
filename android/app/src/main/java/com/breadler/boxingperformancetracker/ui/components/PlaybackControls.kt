package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.ui.theme.PunchHighlight
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted

@Composable
fun PlaybackControls(
    currentPositionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val sliderValue = currentPositionMs.toFloat() / safeDuration.toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            Text(text = formatTimeLabel(currentPositionMs), color = StrykoTextMuted, fontWeight = FontWeight.Medium)
            Text(text = formatTimeLabel(durationMs), color = StrykoTextMuted, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatTimeLabel(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}