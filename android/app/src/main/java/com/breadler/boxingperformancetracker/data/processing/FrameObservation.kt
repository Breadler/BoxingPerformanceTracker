package com.breadler.boxingperformancetracker.data.processing

data class FrameObservation(
    val frameIndex: Int,
    val timestampMs: Long,
    val poseDetected: Boolean,
    val landmarks: Map<String, LandmarkPoint>,
)

data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
)

