package com.breadler.boxingperformancetracker.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.data.PerformancePoint
import com.breadler.boxingperformancetracker.data.PunchWindow
import com.breadler.boxingperformancetracker.ui.theme.GraphBlue
import com.breadler.boxingperformancetracker.ui.theme.GraphGreen
import com.breadler.boxingperformancetracker.ui.theme.PunchHighlight
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted

@Composable
fun PerformanceGraph(
    durationMs: Long,
    currentPositionMs: Long,
    punchWindows: List<PunchWindow>,
    performancePoints: List<PerformancePoint>,
    onScrub: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val series = remember(performancePoints, safeDuration) {
        if (performancePoints.isEmpty()) {
            // No fabricated data: a flat zero line across the real duration is an honest
            // "no predictions were produced" state, not a fake-looking curve.
            GraphSeries(
                punchVolume = listOf(0f, 0f),
                guardHeight = listOf(0f, 0f),
                movement = listOf(0f, 0f),
                timestamps = listOf(0L, safeDuration),
            )
        } else {
            GraphSeries(
                punchVolume = performancePoints.map { it.punchVolume.toFloat() },
                guardHeight = performancePoints.map { it.guardScore.toFloat() },
                movement = performancePoints.map { it.movementScore.toFloat() },
                timestamps = performancePoints.map { it.timestampMs.coerceIn(0L, safeDuration) },
            )
        }
    }

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

            if (performancePoints.isEmpty()) {
                Text(
                    text = "No punch data available for this session",
                    color = StrykoBlue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .pointerInput(safeDuration) {
                        val chartLeft = 12f
                        fun scrubTo(x: Float) {
                            val chartWidth = (size.width - 2 * chartLeft).coerceAtLeast(1f)
                            val fraction = ((x - chartLeft) / chartWidth).coerceIn(0f, 1f)
                            onScrub((fraction * safeDuration).toLong())
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            scrubTo(down.position.x)
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) break
                                scrubTo(change.position.x)
                                change.consume()
                            }
                        }
                    },
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

                // Punch volume is drawn compressed into the middle half of the chart:
                // 0 sits at the 1/4-height mark, 1 sits at the 3/4-height mark.
                fun yForPunchVolume(value: Float): Float {
                    val normalized = value.coerceIn(0f, 1f)
                    val zeroY = chartBottom - chartHeight * 0.25f
                    val oneY = chartBottom - chartHeight * 0.75f
                    return zeroY + (oneY - zeroY) * normalized
                }

                drawSeries(
                    points = series.timestamps.zip(series.guardHeight),
                    fillColor = GraphBlue.copy(alpha = 0.12f),
                    lineColor = GraphBlue,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartWidth = chartWidth,
                    durationMs = safeDuration,
                    yForValue = ::yForValue,
                )
                drawSeries(
                    points = series.timestamps.zip(series.movement),
                    fillColor = GraphGreen.copy(alpha = 0.12f),
                    lineColor = GraphGreen,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartWidth = chartWidth,
                    durationMs = safeDuration,
                    yForValue = ::yForValue,
                )
                drawSeries(
                    points = series.timestamps.zip(series.punchVolume),
                    fillColor = PunchHighlight.copy(alpha = 0.10f),
                    lineColor = PunchHighlight,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartWidth = chartWidth,
                    durationMs = safeDuration,
                    yForValue = ::yForPunchVolume,
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

                val punchesSoFar = punchWindows.count { it.startMs <= currentPositionMs.coerceIn(0L, safeDuration) }
                drawCountLabel(
                    count = punchesSoFar,
                    x = playheadX,
                    y = 6f,
                )
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

private fun DrawScope.drawSeries(
    points: List<Pair<Long, Float>>,
    fillColor: Color,
    lineColor: Color,
    chartLeft: Float,
    chartBottom: Float,
    chartWidth: Float,
    durationMs: Long,
    yForValue: (Float) -> Float,
) {
    if (points.size < 2) return

    val pixelPoints = points.map { (timeMs, value) ->
        val fraction = (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        Offset(chartLeft + (chartWidth * fraction), yForValue(value))
    }

    val linePath = buildSmoothPath(pixelPoints)
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(pixelPoints.last().x, chartBottom)
        lineTo(pixelPoints.first().x, chartBottom)
        close()
    }

    drawPath(path = fillPath, color = fillColor)
    drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Catmull-Rom spline through [pixelPoints], converted to cubic Bezier segments, so
 * even sharp punch on/off transitions render as the smooth curve seen in the design
 * rather than a jagged straight-line zigzag. Still passes exactly through every point. */
private fun buildSmoothPath(pixelPoints: List<Offset>): Path {
    val path = Path()
    if (pixelPoints.isEmpty()) return path
    path.moveTo(pixelPoints.first().x, pixelPoints.first().y)
    if (pixelPoints.size < 3) {
        pixelPoints.drop(1).forEach { path.lineTo(it.x, it.y) }
        return path
    }

    for (i in 0 until pixelPoints.size - 1) {
        val p0 = pixelPoints[(i - 1).coerceAtLeast(0)]
        val p1 = pixelPoints[i]
        val p2 = pixelPoints[i + 1]
        val p3 = pixelPoints[(i + 2).coerceAtMost(pixelPoints.size - 1)]
        val control1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val control2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
        path.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
    }
    return path
}

/** Circular badge on the playhead showing the punch count reached by [x]'s timestamp. */
private fun DrawScope.drawCountLabel(
    count: Int,
    x: Float,
    y: Float,
) {
    val label = count.toString()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val radius = 22f
    val center = Offset(x.coerceAtLeast(radius), y + radius)
    drawCircle(
        color = PunchHighlight.copy(alpha = 0.95f),
        radius = radius,
        center = center,
    )
    drawContext.canvas.nativeCanvas.drawText(
        label,
        center.x,
        center.y + 10f,
        paint,
    )
}

private data class GraphSeries(
    val punchVolume: List<Float>,
    val guardHeight: List<Float>,
    val movement: List<Float>,
    val timestamps: List<Long>,
)