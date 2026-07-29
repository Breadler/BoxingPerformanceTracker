package com.breadler.boxingperformancetracker.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun StrykoSystemBars(
    statusBarColor: Color,
    navigationBarColor: Color,
) {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return

    SideEffect {
        val window = activity.window
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navigationBarColor.toArgb()

        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = statusBarColor.luminance() > 0.5f
        controller.isAppearanceLightNavigationBars = navigationBarColor.luminance() > 0.5f
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    255,
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)