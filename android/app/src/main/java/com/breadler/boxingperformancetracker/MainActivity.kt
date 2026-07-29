package com.breadler.boxingperformancetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.breadler.boxingperformancetracker.navigation.AppNavigation
import com.breadler.boxingperformancetracker.ui.theme.BoxingPerformanceTrackerTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            BoxingPerformanceTrackerTheme {
                AppNavigation()
            }
        }
    }
}
