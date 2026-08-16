package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoWhite

// Circular record/stop button, shrinks its inner dot while active
@Composable
fun RecordButton(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = StrykoWhite,
        shadowElevation = 0.dp,
        modifier = modifier
            .size(86.dp)
            .border(3.dp, StrykoRed, CircleShape),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(if (active) 42.dp else 52.dp)
                    .background(StrykoRed, CircleShape),
            )
        }
    }
}