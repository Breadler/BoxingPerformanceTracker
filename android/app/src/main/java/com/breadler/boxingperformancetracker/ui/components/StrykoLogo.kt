package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed

@Composable
fun StrykoLogo(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "StryKO",
        color = StrykoRed,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}