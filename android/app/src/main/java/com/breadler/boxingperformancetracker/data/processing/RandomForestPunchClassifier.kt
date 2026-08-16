package com.breadler.boxingperformancetracker.data.processing

// On-device punch classifier backed by PunchForestModel
internal object RandomForestPunchClassifier {
    // Config constants
    const val windowMs: Long = 250
    const val strideMs: Long = 40
    const val threshold: Float = 0.5f
    private const val PUNCH_CLASS_INDEX = 1

    // Feature order (must match random_forest.joblib)
    private val featureColumns = listOf(
        "left_wrist_velocity_mean",
        "left_wrist_velocity_std",
        "left_wrist_velocity_max",
        "right_wrist_velocity_mean",
        "right_wrist_velocity_std",
        "right_wrist_velocity_max",
        "left_elbow_velocity_mean",
        "left_elbow_velocity_std",
        "left_elbow_velocity_max",
        "right_elbow_velocity_mean",
        "right_elbow_velocity_std",
        "right_elbow_velocity_max",
        "left_wrist_body_relative_x_mean",
        "left_wrist_body_relative_x_std",
        "left_wrist_body_relative_x_change",
        "left_wrist_body_relative_y_mean",
        "left_wrist_body_relative_y_std",
        "left_wrist_body_relative_y_change",
        "left_wrist_body_relative_z_mean",
        "left_wrist_body_relative_z_std",
        "left_wrist_body_relative_z_change",
        "right_wrist_body_relative_x_mean",
        "right_wrist_body_relative_x_std",
        "right_wrist_body_relative_x_change",
        "right_wrist_body_relative_y_mean",
        "right_wrist_body_relative_y_std",
        "right_wrist_body_relative_y_change",
        "right_wrist_body_relative_z_mean",
        "right_wrist_body_relative_z_std",
        "right_wrist_body_relative_z_change",
        "left_elbow_body_relative_x_mean",
        "left_elbow_body_relative_x_std",
        "left_elbow_body_relative_x_change",
        "left_elbow_body_relative_y_mean",
        "left_elbow_body_relative_y_std",
        "left_elbow_body_relative_y_change",
        "left_elbow_body_relative_z_mean",
        "left_elbow_body_relative_z_std",
        "left_elbow_body_relative_z_change",
        "right_elbow_body_relative_x_mean",
        "right_elbow_body_relative_x_std",
        "right_elbow_body_relative_x_change",
        "right_elbow_body_relative_y_mean",
        "right_elbow_body_relative_y_std",
        "right_elbow_body_relative_y_change",
        "right_elbow_body_relative_z_mean",
        "right_elbow_body_relative_z_std",
        "right_elbow_body_relative_z_change",
        "left_wrist_shoulder_relative_x_mean",
        "left_wrist_shoulder_relative_x_std",
        "left_wrist_shoulder_relative_x_change",
        "left_wrist_shoulder_relative_y_mean",
        "left_wrist_shoulder_relative_y_std",
        "left_wrist_shoulder_relative_y_change",
        "left_wrist_shoulder_relative_z_mean",
        "left_wrist_shoulder_relative_z_std",
        "left_wrist_shoulder_relative_z_change",
        "right_wrist_shoulder_relative_x_mean",
        "right_wrist_shoulder_relative_x_std",
        "right_wrist_shoulder_relative_x_change",
        "right_wrist_shoulder_relative_y_mean",
        "right_wrist_shoulder_relative_y_std",
        "right_wrist_shoulder_relative_y_change",
        "right_wrist_shoulder_relative_z_mean",
        "right_wrist_shoulder_relative_z_std",
        "right_wrist_shoulder_relative_z_change",
        "left_shoulder_to_wrist_distance_change",
        "left_wrist_forward_extension_change",
        "right_shoulder_to_wrist_distance_change",
        "right_wrist_forward_extension_change",
    )

    // Classification entry point
    fun classify(features: Map<String, Float>): Float {
        val input = DoubleArray(featureColumns.size) { index ->
            (features[featureColumns[index]] ?: 0f).toDouble()
        }
        val classVotes = PunchForestModel.score(input)
        return classVotes.getOrElse(PUNCH_CLASS_INDEX) { 0.0 }.toFloat().coerceIn(0f, 1f)
    }
}
