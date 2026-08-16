package com.breadler.boxingperformancetracker.data.processing

// One frame's pose detection result
data class FrameObservation(
    val frameIndex: Int,
    val timestampMs: Long,
    val poseDetected: Boolean,
    val landmarks: Map<String, LandmarkPoint>,
)

// A single landmark's position and confidence
data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
)

