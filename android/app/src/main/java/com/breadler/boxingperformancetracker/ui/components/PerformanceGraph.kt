package com.breadler.boxingperformancetracker.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.data.PunchWindow
import com.breadler.boxingperformancetracker.ui.theme.GraphBlue
import com.breadler.boxingperformancetracker.ui.theme.GraphGreen
import com.breadler.boxingperformancetracker.ui.theme.PunchHighlight
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PerformanceGraph(
    durationMs: Long,
    currentPositionMs: Long,
    punchWindows: List<PunchWindow>,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val series = remember(safeDuration) { buildSeries(points = 120) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StrykoCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendSwatch(color = PunchHighlight, label = "punch volume")
                LegendSwatch(color = GraphBlue, label = "guard height")
                LegendSwatch(color = GraphGreen, label = "movement")
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp),
            ) {
                val chartLeft = 12f
                val chartRight = size.width - 12f
                val chartTop = 12f
                val chartBottom = size.height - 28f
                val chartWidth = chartRight - chartLeft
                val chartHeight = chartBottom - chartTop

                fun xForTime(timeMs: Long): Float {
                    val fraction = (timeMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    return chartLeft + chartWidth * fraction
                }

                fun yForValue(value: Float): Float {
                    val normalized = value.coerceIn(0f, 1f)
                    return chartBottom - (chartHeight * normalized)
                }

                drawSeries(
                    values = series.guardHeight,
                    fillColor = GraphBlue.copy(alpha = 0.12f),
                    lineColor = GraphBlue,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartWidth = chartWidth,
                    yForValue = ::yForValue,
                )
                drawSeries(
                    values = series.movement,
                    fillColor = GraphGreen.copy(alpha = 0.12f),
                    lineColor = GraphGreen,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartWidth = chartWidth,
                    yForValue = ::yForValue,
                )
                drawSeries(
                    values = series.punchVolume,
                    fillColor = PunchHighlight.copy(alpha = 0.10f),
                    lineColor = PunchHighlight,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartWidth = chartWidth,
                    yForValue = ::yForValue,
                )

                punchWindows.forEach { window ->
                    val x = xForTime(window.endMs.coerceIn(0L, safeDuration))
                    drawRect(
                        color = PunchHighlight.copy(alpha = 0.78f),
                        topLeft = Offset(x - 2f, chartTop),
                        size = Size(width = 4f, height = chartBottom - chartTop),
                    )
                }

                val playheadX = xForTime(currentPositionMs.coerceIn(0L, safeDuration))
                drawLine(
                    color = PunchHighlight,
                    start = Offset(playheadX, chartTop),
                    end = Offset(playheadX, chartBottom),
                    strokeWidth = 2.5f,
                )

                drawTimeLabel(
                    label = formatTimeLabel(currentPositionMs),
                    x = playheadX,
                    y = 6f,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("0:00", "0:10", "0:20", "0:30", "0:40", "1:00").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = StrykoTextMuted,
                    )
                }
            }

            Surface(
                onClick = onTogglePlayback,
                shape = RoundedCornerShape(50.dp),
                color = StrykoCard,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, StrykoBlue),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                BoxedPlaybackIcon(isPlaying = isPlaying)
            }
        }
    }
}

@Composable
private fun LegendSwatch(
    color: Color,
    label: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), color = color, shape = RoundedCornerShape(2.dp)) {}
        Text(text = label, color = StrykoTextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BoxedPlaybackIcon(isPlaying: Boolean) {
    androidx.compose.foundation.layout.Box(
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

private fun DrawScope.drawSeries(
    values: List<Float>,
    fillColor: Color,
    lineColor: Color,
    chartLeft: Float,
    chartBottom: Float,
    chartWidth: Float,
    yForValue: (Float) -> Float,
) {
    if (values.size < 2) return

    val linePath = Path()
    val fillPath = Path()

    values.forEachIndexed { index, value ->
        val x = chartLeft + (chartWidth * index / values.lastIndex.coerceAtLeast(1))
        val y = yForValue(value)
        if (index == 0) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, chartBottom)
            fillPath.lineTo(x, y)
        } else {
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
        if (index == values.lastIndex) {
            fillPath.lineTo(x, chartBottom)
            fillPath.close()
        }
    }

    drawPath(path = fillPath, color = fillColor)
    drawPath(path = linePath, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
}

private fun DrawScope.drawTimeLabel(
    label: String,
    x: Float,
    y: Float,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PunchHighlight.toArgb()
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val textWidth = paint.measureText(label)
    val bubbleWidth = textWidth + 24f
    val bubbleHeight = 38f
    val left = (x - bubbleWidth / 2f).coerceAtLeast(0f)
    val right = left + bubbleWidth
    drawRoundRect(
        color = PunchHighlight.copy(alpha = 0.95f),
        topLeft = Offset(left, y),
        size = Size(bubbleWidth, bubbleHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
    )
    drawContext.canvas.nativeCanvas.drawText(
        label,
        left + bubbleWidth / 2f,
        y + 24f,
        paint,
    )
}

private data class GraphSeries(
    val punchVolume: List<Float>,
    val guardHeight: List<Float>,
    val movement: List<Float>,
)

private fun buildSeries(points: Int): GraphSeries {
    return GraphSeries(
        punchVolume = List(points) { index ->
            val phase = index.toFloat() / points.toFloat()
            (0.26f + 0.12f * sin(phase * 10f * PI.toFloat()) + 0.05f * sin(phase * 25f * PI.toFloat())).coerceIn(0f, 1f)
        },
        guardHeight = List(points) { index ->
            val phase = index.toFloat() / points.toFloat()
            (0.58f + 0.08f * sin(phase * 5f * PI.toFloat() + 0.2f)).coerceIn(0f, 1f)
        },
        movement = List(points) { index ->
            val phase = index.toFloat() / points.toFloat()
            (0.40f + 0.10f * sin(phase * 6f * PI.toFloat() + 0.8f)).coerceIn(0f, 1f)
        },
    )
}

private fun formatTimeLabel(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}