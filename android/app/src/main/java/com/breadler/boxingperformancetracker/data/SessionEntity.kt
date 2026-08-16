package com.breadler.boxingperformancetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room row for one processed session
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dateLabel: String,
    val durationLabel: String,
    val durationMs: Long,
    val sourceVideoName: String,
    val sourceVideoUri: String,
    val annotatedVideoUri: String,
    val thumbnailUri: String,
    val punchCount: Int,
    val processedAtMs: Long,
    val punchWindowsJson: String,
    val predictionWindowsJson: String,
    val performancePointsJson: String,
)
