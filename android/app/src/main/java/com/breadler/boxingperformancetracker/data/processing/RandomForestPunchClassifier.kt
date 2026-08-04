package com.breadler.boxingperformancetracker.data.processing

/**
 * On-device punch classifier backed by [PunchForestModel] - the 300-tree
 * RandomForestClassifier from python/models/random_forest.joblib, ported to plain
 * Java via m2cgen (see python/export_random_forest_java.py) rather than a
 * separately-trained TFLite network.
 *
 * The previous approach trained a small dense NN from scratch on the same
 * training.csv for on-device use, kept independent of the RandomForest used for
 * all Python-side testing. With a video-grouped train/validation split (instead
 * of the original per-row shuffle, which let near-duplicate overlapping windows
 * from the same clip leak across both sides), that NN's held-out accuracy came in
 * at ~50% - essentially chance - on only 350 labeled rows across 111 videos. The
 * RandomForest is the model that's actually been validated throughout this
 * project's Python testing, so porting it directly (rather than training a second,
 * independent model on the same thin data) is both more accurate and removes an
 * entire class of train/inference divergence between the app and Python.
 */
internal object RandomForestPunchClassifier {
    const val windowMs: Long = 250
    const val strideMs: Long = 40
    const val threshold: Float = 0.5f
    private const val PUNCH_CLASS_INDEX = 1

    // Must match python/models/random_forest.joblib's feature_columns order exactly -
    // PunchForestModel.score() reads its input as a plain positional double[], with
    // no column-name metadata of its own to validate against.
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

    fun classify(features: Map<String, Float>): Float {
        val input = DoubleArray(featureColumns.size) { index ->
            (features[featureColumns[index]] ?: 0f).toDouble()
        }
        val classVotes = PunchForestModel.score(input)
        return classVotes.getOrElse(PUNCH_CLASS_INDEX) { 0.0 }.toFloat().coerceIn(0f, 1f)
    }
}
