package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard

// Small circular icon button used for the +/- timer steppers
@Composable
fun TimerButton(
    iconResId: Int,
    onClick: () -> Unit,
    backgroundColor: Color = StrykoBlue,
    iconTint: Color = StrykoCard,
) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(backgroundColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}