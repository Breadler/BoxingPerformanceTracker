package com.breadler.boxingperformancetracker.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
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
import com.breadler.boxingperformancetracker.data.processing.GraphMetrics
import com.breadler.boxingperformancetracker.ui.theme.GraphBlue
import com.breadler.boxingperformancetracker.ui.theme.GraphGreen
import com.breadler.boxingperformancetracker.ui.theme.PunchHighlight
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted
import kotlin.math.roundToInt

private const val DIMMED_ALPHA = 0.18f
private const val LEGEND_DIMMED_ALPHA = 0.4f

private enum class GraphMetric { PUNCH_VOLUME, GUARD_HEIGHT, MOVEMENT }

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
    var selectedMetric by remember(performancePoints) { mutableStateOf<GraphMetric?>(null) }

    val guardMovementSeries = remember(performancePoints, safeDuration) {
        if (performancePoints.isEmpty()) {
            // No fabricated data: a flat zero line across the real duration is an honest
            // "no predictions were produced" state, not a fake-looking curve.
            GuardMovementSeries(
                guardHeight = listOf(0f, 0f),
                movement = listOf(0f, 0f),
                timestamps = listOf(0L, safeDuration),
            )
        } else {
            // Mirrors graph_metrics.py's compute -> smooth -> downsample pipeline: the
            // stored points are raw/exact, this is purely a display-time pass. Each series
            // is min-max normalized to 0..1 here; the Canvas draw step below maps that
            // into its own staggered band rather than one shared full-height axis.
            val sorted = performancePoints.sortedBy { it.timestampMs }
            val inferredStrideMs = sorted.zipWithNext()
                .map { (a, b) -> b.timestampMs - a.timestampMs }
                .filter { it > 0 }
                .minOrNull() ?: 40L

            val smoothed = GraphMetrics.smoothPerformancePoints(sorted, strideMs = inferredStrideMs)
            val displayPoints = GraphMetrics.downsamplePerformancePoints(smoothed)

            GuardMovementSeries(
                guardHeight = normalizeToUnitRange(displayPoints.map { it.guardHeight }),
                movement = normalizeToUnitRange(displayPoints.map { it.movement }),
                timestamps = displayPoints.map { it.timestampMs.coerceIn(0L, safeDuration) },
            )
        }
    }

    val punchVolumeSeries = remember(punchWindows, safeDuration) {
        // Sparse, one point per punch (running count within its combo) plus a 0
        // right before/after each combo, not smoothed/downsampled on this data
        // itself - the curve drawn through these points is purely a display-time
        // rounding, not averaging. Mirrors plot_graph_metrics.py's punch volume
        // view, not the uniform grid.
        val keyframes = GraphMetrics.computePunchVolumeKeyframes(punchWindows, safeDuration)
        PunchVolumeSeries(
            punchVolume = normalizeToUnitRange(keyframes.map { it.punchVolume.toDouble() }),
            rawPunchVolume = keyframes.map { it.punchVolume },
            timestamps = keyframes.map { it.timestampMs.coerceIn(0L, safeDuration) },
        )
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
                LegendSwatch(
                    color = PunchHighlight,
                    label = "punch volume",
                    isDimmed = selectedMetric != null && selectedMetric != GraphMetric.PUNCH_VOLUME,
                    onClick = {
                        selectedMetric = if (selectedMetric == GraphMetric.PUNCH_VOLUME) null else GraphMetric.PUNCH_VOLUME
                    },
                )
                LegendSwatch(
                    color = GraphBlue,
                    label = "guard height",
                    isDimmed = selectedMetric != null && selectedMetric != GraphMetric.GUARD_HEIGHT,
                    onClick = {
                        selectedMetric = if (selectedMetric == GraphMetric.GUARD_HEIGHT) null else GraphMetric.GUARD_HEIGHT
                    },
                )
                LegendSwatch(
                    color = GraphGreen,
                    label = "movement",
                    isDimmed = selectedMetric != null && selectedMetric != GraphMetric.MOVEMENT,
                    onClick = {
                        selectedMetric = if (selectedMetric == GraphMetric.MOVEMENT) null else GraphMetric.MOVEMENT
                    },
                )
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

                // Guard height and movement are staggered into their own half-height
                // bands so they overlap in a cascade: guard height in the middle half
                // (1/4-3/4), movement in the bottom half (2/4-4/4), both floored at
                // their own band's bottom edge (0.75 and 1.0 respectively). Punch volume
                // keeps its peak at the very top (0/4, unchanged) but its floor sits at
                // 0.875 - halfway between guard height's floor and movement's floor.
                fun yForBand(value: Float, bandTopFraction: Float, bandBottomFraction: Float): Float {
                    val normalized = value.coerceIn(0f, 1f)
                    val topY = chartTop + chartHeight * bandTopFraction
                    val bottomY = chartTop + chartHeight * bandBottomFraction
                    return bottomY + (topY - bottomY) * normalized
                }

                fun yForPunchVolume(value: Float) = yForBand(value, bandTopFraction = 0f, bandBottomFraction = 0.875f)
                fun yForGuardHeight(value: Float) = yForBand(value, bandTopFraction = 0.25f, bandBottomFraction = 0.75f)
                fun yForMovement(value: Float) = yForBand(value, bandTopFraction = 0.5f, bandBottomFraction = 1f)

                val punchAlpha = seriesAlpha(selectedMetric, GraphMetric.PUNCH_VOLUME)
                val guardAlpha = seriesAlpha(selectedMetric, GraphMetric.GUARD_HEIGHT)
                val movementAlpha = seriesAlpha(selectedMetric, GraphMetric.MOVEMENT)

                drawSeries(
                    points = punchVolumeSeries.timestamps.zip(punchVolumeSeries.punchVolume),
                    fillColor = PunchHighlight.copy(alpha = 0.10f),
                    lineColor = PunchHighlight,
                    chartLeft = chartLeft,
                    baselineY = chartBottom,
                    chartWidth = chartWidth,
                    durationMs = safeDuration,
                    yForValue = ::yForPunchVolume,
                    alpha = punchAlpha,
                )
                drawSeries(
                    points = guardMovementSeries.timestamps.zip(guardMovementSeries.guardHeight),
                    fillColor = GraphBlue.copy(alpha = 0.12f),
                    lineColor = GraphBlue,
                    chartLeft = chartLeft,
                    baselineY = chartBottom,
                    chartWidth = chartWidth,
                    durationMs = safeDuration,
                    yForValue = ::yForGuardHeight,
                    alpha = guardAlpha,
                )
                drawSeries(
                    points = guardMovementSeries.timestamps.zip(guardMovementSeries.movement),
                    fillColor = GraphGreen.copy(alpha = 0.12f),
                    lineColor = GraphGreen,
                    chartLeft = chartLeft,
                    baselineY = chartBottom,
                    chartWidth = chartWidth,
                    durationMs = safeDuration,
                    yForValue = ::yForMovement,
                    alpha = movementAlpha,
                )

                // One dot per individual predicted punch, sitting exactly on the punch
                // volume line at that punch's own end-time keyframe (see
                // GraphMetrics.computePunchVolumeKeyframes) rather than a full-height
                // bar, so a fast combo reads as a run of beads along the curve.
                val punchVolumeByTimestamp = punchVolumeSeries.timestamps.zip(punchVolumeSeries.punchVolume).toMap()
                punchWindows.forEach { window ->
                    val endMs = window.endMs.coerceIn(0L, safeDuration)
                    val normalizedVolume = punchVolumeByTimestamp[endMs] ?: return@forEach
                    val center = Offset(xForTime(endMs), yForPunchVolume(normalizedVolume))
                    drawCircle(color = Color.White.copy(alpha = 0.9f * punchAlpha), radius = 5f, center = center)
                    drawCircle(color = PunchHighlight.copy(alpha = punchAlpha), radius = 3f, center = center)
                }

                val playheadX = xForTime(currentPositionMs.coerceIn(0L, safeDuration))
                drawLine(
                    color = PunchHighlight,
                    start = Offset(playheadX, chartTop),
                    end = Offset(playheadX, chartBottom),
                    strokeWidth = 2.5f,
                )

                val clampedPositionMs = currentPositionMs.coerceIn(0L, safeDuration)
                val badgeLabel = when (selectedMetric) {
                    GraphMetric.GUARD_HEIGHT ->
                        "${(valueAtOrBefore(guardMovementSeries.timestamps, guardMovementSeries.guardHeight, clampedPositionMs) * 100).roundToInt()}%"
                    GraphMetric.MOVEMENT ->
                        "${(valueAtOrBefore(guardMovementSeries.timestamps, guardMovementSeries.movement, clampedPositionMs) * 100).roundToInt()}%"
                    GraphMetric.PUNCH_VOLUME, null ->
                        rawValueAtOrBefore(punchVolumeSeries.timestamps, punchVolumeSeries.rawPunchVolume, clampedPositionMs).toString()
                }
                drawCountLabel(
                    label = badgeLabel,
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
    isDimmed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .alpha(if (isDimmed) LEGEND_DIMMED_ALPHA else 1f)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(modifier = Modifier.size(10.dp), color = color, shape = RoundedCornerShape(2.dp)) {}
        Text(text = label, color = StrykoTextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

/** 1f when nothing is selected or [metric] is the selected one, [DIMMED_ALPHA]
 * otherwise - the "focus" effect for tapping a legend item. */
private fun seriesAlpha(selected: GraphMetric?, metric: GraphMetric): Float {
    return if (selected == null || selected == metric) 1f else DIMMED_ALPHA
}

/** Last series value at or before [atMs], for the playhead badge to track whichever
 * metric is currently focused. Falls back to the first point if [atMs] is earlier
 * than every sample. */
private fun valueAtOrBefore(timestamps: List<Long>, values: List<Float>, atMs: Long): Float {
    if (values.isEmpty()) return 0f
    val index = timestamps.indexOfLast { it <= atMs }
    return if (index >= 0) values[index] else values.first()
}

/** Same lookup as [valueAtOrBefore] but over the punch volume keyframes' raw integer
 * counts, so the playhead badge shows the running count within whichever combo covers
 * [atMs] (0 between combos) instead of a lifetime total that never resets. */
private fun rawValueAtOrBefore(timestamps: List<Long>, values: List<Int>, atMs: Long): Int {
    if (values.isEmpty()) return 0
    val index = timestamps.indexOfLast { it <= atMs }
    return if (index >= 0) values[index] else values.first()
}

private fun DrawScope.drawSeries(
    points: List<Pair<Long, Float>>,
    fillColor: Color,
    lineColor: Color,
    chartLeft: Float,
    baselineY: Float,
    chartWidth: Float,
    durationMs: Long,
    yForValue: (Float) -> Float,
    alpha: Float = 1f,
) {
    if (points.size < 2) return

    val pixelPoints = points.map { (timeMs, value) ->
        val fraction = (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        Offset(chartLeft + (chartWidth * fraction), yForValue(value))
    }

    // Every series curves through its points now (including punch volume, previously
    // straight point-to-point) for a softer, more polished look across the board.
    val linePath = buildSmoothPath(pixelPoints)
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(pixelPoints.last().x, baselineY)
        lineTo(pixelPoints.first().x, baselineY)
        close()
    }

    drawPath(path = fillPath, color = fillColor.copy(alpha = fillColor.alpha * alpha))
    drawPath(
        path = linePath,
        color = lineColor.copy(alpha = lineColor.alpha * alpha),
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

/** Circular badge on the playhead. Shows the punch count by default, or the
 * currently focused metric's value (as a percentage) when one is selected. */
private fun DrawScope.drawCountLabel(
    label: String,
    x: Float,
    y: Float,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 26f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Radius grows to fit longer labels (e.g. "100%") while staying circular for
    // short ones (e.g. "5").
    val radius = (paint.measureText(label) / 2f + 12f).coerceAtLeast(22f)
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

private data class GuardMovementSeries(
    val guardHeight: List<Float>,
    val movement: List<Float>,
    val timestamps: List<Long>,
)

private data class PunchVolumeSeries(
    val punchVolume: List<Float>,
    val rawPunchVolume: List<Int>,
    val timestamps: List<Long>,
)

/** Min-max scales [values] to 0..1 for a shared y-axis. A constant series maps to a
 * flat 0 if that constant is ~zero (e.g. "no punches at all"), else a flat 0.5 -
 * a constant non-zero value has no natural place on a relative 0..1 scale. */
private fun normalizeToUnitRange(values: List<Double>): List<Float> {
    if (values.isEmpty()) return emptyList()
    val minValue = values.min()
    val maxValue = values.max()
    val range = maxValue - minValue
    if (range <= 1e-9) {
        val flat = if (maxValue > 1e-9) 0.5f else 0f
        return values.map { flat }
    }
    return values.map { ((it - minValue) / range).toFloat() }
}