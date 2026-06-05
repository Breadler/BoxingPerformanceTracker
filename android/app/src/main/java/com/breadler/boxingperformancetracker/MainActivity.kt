package com.breadler.boxingperformancetracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = "Boxing Performance Tracker\nKotlin Android app scaffold"
                textSize = 18f
                setPadding(32, 64, 32, 32)
            },
        )
    }
}
