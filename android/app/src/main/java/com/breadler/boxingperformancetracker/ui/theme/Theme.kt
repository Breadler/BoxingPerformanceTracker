package com.breadler.boxingperformancetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = StrykoRed,
    secondary = StrykoBlue,
    background = StrykoBackground,
    surface = StrykoCard,
    onPrimary = StrykoCard,
    onSecondary = StrykoCard,
    onBackground = StrykoBlue,
    onSurface = StrykoBlue,
)

@Composable
fun BoxingPerformanceTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            titleMedium = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            bodyLarge = TextStyle(
                fontSize = 16.sp,
            ),
            bodyMedium = TextStyle(
                fontSize = 14.sp,
            ),
        ),
        content = content,
    )
}
