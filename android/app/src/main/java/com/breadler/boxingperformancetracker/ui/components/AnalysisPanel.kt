package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.data.PunchWindow
import com.breadler.boxingperformancetracker.ui.theme.GraphBlue
import com.breadler.boxingperformancetracker.ui.theme.GraphGreen
import com.breadler.boxingperformancetracker.ui.theme.PunchHighlight
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoTextMuted
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AnalysisPanel(
    durationMs: Long,
    currentPositionMs: Long,
    punchWindows: List<PunchWindow>,
    modifier: Modifier = Modifier,
) {
    val safeDurationMs = durationMs.coerceAtLeast(1L)
    val chartSeries = remember(safeDurationMs) {
        buildPlaceholderSeries(points = 100)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = StrykoCard,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendSwatch(color = PunchHighlight, label = "punch volume")
                LegendSwatch(color = GraphBlue, label = "guard height")
                LegendSwatch(color = GraphGreen, label = "movement")
            }

            AnalysisChart(
                durationMs = safeDurationMs,
                currentPositionMs = currentPositionMs.coerceIn(0L, safeDurationMs),
                punchWindows = punchWindows,
                guardSeries = chartSeries.guardHeight,
                movementSeries = chartSeries.movement,
                balanceSeries = chartSeries.balance,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

@Composable
private fun LegendSwatch(
    color: Color,
    label: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier.size(12.dp),
            color = color,
            shape = RoundedCornerShape(2.dp),
        ) {}
        Text(
            text = label,
            color = StrykoTextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AnalysisChart(
    durationMs: Long,
    currentPositionMs: Long,
    punchWindows: List<PunchWindow>,
    guardSeries: List<Float>,
    movementSeries: List<Float>,
    balanceSeries: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val chartLeft = 8f
        val chartRight = size.width - 8f
        val chartTop = 8f
        val chartBottom = size.height - 8f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        fun xForTime(timeMs: Long): Float {
            val fraction = (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            return chartLeft + (chartWidth * fraction)
        }

        fun yForValue(value: Float): Float {
            val normalized = value.coerceIn(0f, 1f)
            return chartBottom - (chartHeight * normalized)
        }

        drawSeries(
            values = guardSeries,
            fillColor = GraphBlue.copy(alpha = 0.2f),
            lineColor = GraphBlue.copy(alpha = 0.85f),
            chartLeft = chartLeft,
            chartBottom = chartBottom,
            chartWidth = chartWidth,
            yForValue = ::yForValue,
        )
        drawSeries(
            values = movementSeries,
            fillColor = GraphGreen.copy(alpha = 0.2f),
            lineColor = GraphGreen.copy(alpha = 0.85f),
            chartLeft = chartLeft,
            chartBottom = chartBottom,
            chartWidth = chartWidth,
            yForValue = ::yForValue,
        )
        drawSeries(
            values = balanceSeries,
            fillColor = Color(0xFF6B728E).copy(alpha = 0.1f),
            lineColor = Color(0xFF6B728E),
            chartLeft = chartLeft,
            chartBottom = chartBottom,
            chartWidth = chartWidth,
            yForValue = ::yForValue,
        )

        drawPunchBars(
            punchWindows = punchWindows,
            durationMs = durationMs,
            chartTop = chartTop,
            chartBottom = chartBottom,
            xForTime = ::xForTime,
        )

        val playheadX = xForTime(currentPositionMs)
        drawLine(
            color = Color.Gray.copy(alpha = 0.8f),
            start = Offset(playheadX, chartTop),
            end = Offset(playheadX, chartBottom),
            strokeWidth = 2f,
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
        val x = chartLeft + (chartWidth * index / (values.lastIndex.coerceAtLeast(1)))
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
    drawPath(path = linePath, color = lineColor, style = Stroke(width = 2.dp.toPx()))
}

private fun DrawScope.drawPunchBars(
    punchWindows: List<PunchWindow>,
    durationMs: Long,
    chartTop: Float,
    chartBottom: Float,
    xForTime: (Long) -> Float,
) {
    punchWindows.forEach { window ->
        val punchX = xForTime(window.endMs.coerceIn(0L, durationMs))
        drawRect(
            color = PunchHighlight.copy(alpha = 0.6f),
            topLeft = Offset(punchX - 4.dp.toPx(), chartTop),
            size = Size(
                width = 8.dp.toPx(),
                height = chartBottom - chartTop,
            ),
        )
    }
}

private data class ChartSeries(
    val guardHeight: List<Float>,
    val movement: List<Float>,
    val balance: List<Float>,
)

private fun buildPlaceholderSeries(points: Int): ChartSeries {
    return ChartSeries(
        guardHeight = List(points) { index ->
            val phase = index.toFloat() / points.toFloat()
            (0.55f + 0.08f * sin(phase * 5f * PI.toFloat())).coerceIn(0f, 1f)
        },
        movement = List(points) { index ->
            val phase = index.toFloat() / points.toFloat()
            (0.35f + 0.1f * sin(phase * 6f * PI.toFloat() + 0.8f)).coerceIn(0f, 1f)
        },
        balance = List(points) { index ->
            val phase = index.toFloat() / points.toFloat()
            (0.42f + 0.10f * sin(phase * 4f * PI.toFloat()) + 0.06f * sin(phase * 17f * PI.toFloat()))
                .coerceIn(0f, 1f)
        }
    )
}
