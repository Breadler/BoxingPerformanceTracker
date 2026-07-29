package com.breadler.boxingperformancetracker.data.processing

internal object WindowFeatures {
    private val velocityLandmarks = listOf("left_wrist", "right_wrist", "left_elbow", "right_elbow")
    private val armLandmarks = listOf("left_wrist", "right_wrist", "left_elbow", "right_elbow")
    private val shoulderWristPairs = listOf(
        "left_shoulder" to "left_wrist",
        "right_shoulder" to "right_wrist",
    )
    private val bodyCenterLandmarks = listOf("left_shoulder", "right_shoulder", "left_hip", "right_hip")

    fun aggregate(
        observations: List<FrameObservation>,
        startMs: Long,
        endMs: Long,
    ): Map<String, Float>? {
        val window = observations.filter { it.timestampMs in startMs..endMs }
        if (window.isEmpty()) return null

        val features = mutableMapOf<String, Float>()
        addVelocityFeatures(features, window)
        addBodyRelativeArmFeatures(features, window)
        addShoulderRelativeWristFeatures(features, window)
        addExtensionFeatures(features, window)
        return features
    }

    private fun addVelocityFeatures(features: MutableMap<String, Float>, window: List<FrameObservation>) {
        velocityLandmarks.forEach { landmark ->
            val speeds = window.zipWithNext().mapNotNull { (previous, current) ->
                val previousPoint = previous.landmarks[landmark] ?: return@mapNotNull null
                val currentPoint = current.landmarks[landmark] ?: return@mapNotNull null
                val deltaSeconds = (current.timestampMs - previous.timestampMs).toFloat() / 1000f
                if (deltaSeconds <= 0f) return@mapNotNull null
                distance(previousPoint, currentPoint) / deltaSeconds
            }
            features["${landmark}_velocity_mean"] = speeds.meanOrZero()
            features["${landmark}_velocity_std"] = speeds.stdOrZero()
            features["${landmark}_velocity_max"] = speeds.maxOrNull() ?: 0f
        }
    }

    private fun addBodyRelativeArmFeatures(features: MutableMap<String, Float>, window: List<FrameObservation>) {
        armLandmarks.forEach { landmark ->
            val relativePoints = window.mapNotNull { observation ->
                val point = observation.landmarks[landmark] ?: return@mapNotNull null
                val center = bodyCenter(observation) ?: return@mapNotNull null
                LandmarkPoint(
                    x = point.x - center.x,
                    y = point.y - center.y,
                    z = point.z - center.z,
                    visibility = point.visibility,
                )
            }
            addAxisStats(features, "${landmark}_body_relative", relativePoints)
        }
    }

    private fun addShoulderRelativeWristFeatures(features: MutableMap<String, Float>, window: List<FrameObservation>) {
        shoulderWristPairs.forEach { (shoulder, wrist) ->
            val side = shoulder.removeSuffix("_shoulder")
            val relativePoints = window.mapNotNull { observation ->
                val shoulderPoint = observation.landmarks[shoulder] ?: return@mapNotNull null
                val wristPoint = observation.landmarks[wrist] ?: return@mapNotNull null
                LandmarkPoint(
                    x = wristPoint.x - shoulderPoint.x,
                    y = wristPoint.y - shoulderPoint.y,
                    z = wristPoint.z - shoulderPoint.z,
                    visibility = wristPoint.visibility,
                )
            }
            addAxisStats(features, "${side}_wrist_shoulder_relative", relativePoints)
        }
    }

    private fun addExtensionFeatures(features: MutableMap<String, Float>, window: List<FrameObservation>) {
        shoulderWristPairs.forEach { (shoulder, wrist) ->
            val side = shoulder.removeSuffix("_shoulder")
            val distances = window.mapNotNull { observation ->
                val shoulderPoint = observation.landmarks[shoulder] ?: return@mapNotNull null
                val wristPoint = observation.landmarks[wrist] ?: return@mapNotNull null
                distance(shoulderPoint, wristPoint)
            }
            features["${side}_shoulder_to_wrist_distance_change"] = distances.changeOrZero()

            val relativeDepth = window.mapNotNull { observation ->
                val shoulderPoint = observation.landmarks[shoulder] ?: return@mapNotNull null
                val wristPoint = observation.landmarks[wrist] ?: return@mapNotNull null
                wristPoint.z - shoulderPoint.z
            }
            features["${side}_wrist_forward_extension_change"] = if (relativeDepth.isEmpty()) {
                0f
            } else {
                relativeDepth.first() - relativeDepth.last()
            }
        }
    }

    private fun addAxisStats(features: MutableMap<String, Float>, prefix: String, points: List<LandmarkPoint>) {
        val axes = mapOf(
            "x" to points.map { it.x },
            "y" to points.map { it.y },
            "z" to points.map { it.z },
        )
        axes.forEach { (axis, values) ->
            features["${prefix}_${axis}_mean"] = values.meanOrZero()
            features["${prefix}_${axis}_std"] = values.stdOrZero()
            features["${prefix}_${axis}_change"] = values.changeOrZero()
        }
    }

    private fun bodyCenter(observation: FrameObservation): LandmarkPoint? {
        val points = bodyCenterLandmarks.mapNotNull { observation.landmarks[it] }
        if (points.isEmpty()) return null
        return LandmarkPoint(
            x = points.map { it.x }.meanOrZero(),
            y = points.map { it.y }.meanOrZero(),
            z = points.map { it.z }.meanOrZero(),
            visibility = points.map { it.visibility }.meanOrZero(),
        )
    }

    private fun distance(first: LandmarkPoint, second: LandmarkPoint): Float {
        val dx = second.x - first.x
        val dy = second.y - first.y
        val dz = second.z - first.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun List<Float>.meanOrZero(): Float = if (isEmpty()) 0f else sum() / size

    private fun List<Float>.stdOrZero(): Float {
        if (isEmpty()) return 0f
        val mean = meanOrZero()
        return kotlin.math.sqrt(sumOf { value -> ((value - mean) * (value - mean)).toDouble() }.toFloat() / size)
    }

    private fun List<Float>.changeOrZero(): Float = if (isEmpty()) 0f else last() - first()
}

