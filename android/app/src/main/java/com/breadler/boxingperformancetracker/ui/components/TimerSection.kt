package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed

/** Pixels of vertical drag needed to trigger one +/- step, besides the buttons themselves. */
private const val DRAG_PX_PER_STEP = 70f

@Composable
fun TimerSection(
    title: String,
    time: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    adjustable: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = StrykoRed,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = if (adjustable) {
                Modifier.pointerInput(Unit) {
                    var accumulatedDragPx = 0f
                    detectVerticalDragGestures(
                        onDragStart = { accumulatedDragPx = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Dragging up increases the value, dragging down decreases it.
                            accumulatedDragPx += dragAmount
                            while (accumulatedDragPx <= -DRAG_PX_PER_STEP) {
                                onPlus()
                                accumulatedDragPx += DRAG_PX_PER_STEP
                            }
                            while (accumulatedDragPx >= DRAG_PX_PER_STEP) {
                                onMinus()
                                accumulatedDragPx -= DRAG_PX_PER_STEP
                            }
                        },
                    )
                }
            } else {
                Modifier
            },
        ) {
            if (adjustable) {
                TimerButton(
                    iconResId = R.drawable.ic_minus,
                    onClick = onMinus,
                    backgroundColor = StrykoCard,
                    iconTint = StrykoRed,
                )
            }
            Text(
                text = time,
                color = StrykoRed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
            )
            if (adjustable) {
                TimerButton(
                    iconResId = R.drawable.ic_plus,
                    onClick = onPlus,
                    backgroundColor = StrykoCard,
                    iconTint = StrykoRed,
                )
            }
        }
    }
}
