package com.breadler.boxingperformancetracker.data.processing

import android.util.Log
import com.breadler.boxingperformancetracker.data.PerformancePoint
import com.breadler.boxingperformancetracker.data.PunchWindow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Local port of python/graph_metrics.py + punch_volume.py: head-relative guard
 * height and hip x/z movement speed on a uniform sliding-window grid
 * ([computeGraphMetrics]), and punch volume as sparse per-punch keyframes
 * ([computePunchVolumeKeyframes]) rather than that same grid - it's a sparse,
 * bursty signal (tens of punches per round vs. thousands of grid samples), and
 * doesn't need pose data at all, only the already-merged punch windows and the
 * session duration. That makes it both the closest match to
 * punch_volume.compute_punch_volume_keyframes() and the cheapest option on
 * device: no per-window combo lookup over the whole video, no
 * smoothing/downsampling/curve-fitting - just a straight line through a
 * couple hundred points at most, computed on demand from data already held
 * in memory (no extra Room storage, no re-processing).
 */
internal object GraphMetrics {
    private const val TAG = "GraphMetrics"
    const val DEFAULT_COMBO_GAP_MS = 500L
    const val DEFAULT_GUARD_HEIGHT_SMOOTHING_MS = 1500L
    const val DEFAULT_MOVEMENT_SMOOTHING_MS = 1500L
    const val DEFAULT_DOWNSAMPLE_BUCKET_MS = 500L

    fun computeGraphMetrics(
        observations: List<FrameObservation>,
        windowMs: Long,
        strideMs: Long,
    ): List<PerformancePoint> {
        val minMs = observations.minOfOrNull { it.timestampMs } ?: return emptyList()
        val maxMs = observations.maxOfOrNull { it.timestampMs } ?: return emptyList()
        if (maxMs - minMs < windowMs) return emptyList()

        return buildList {
            var startMs = minMs
            while (startMs <= maxMs - windowMs) {
                val endMs = startMs + windowMs
                val window = observations.filter { it.timestampMs in startMs..endMs }
                if (window.isNotEmpty()) {
                    val centerMs = (startMs + endMs) / 2
                    add(
                        PerformancePoint(
                            timestampMs = centerMs,
                            guardHeight = guardHeightForWindow(window),
                            movement = movementForWindow(window),
                        ),
                    )
                }
                startMs += strideMs
            }
        }
    }

    /** Mean of (nose_y - min(left_wrist_y, right_wrist_y)) across the window. MediaPipe
     * y is inverted (0 = top of frame), so this is positive when the higher-guarding
     * wrist sits above the nose, shrinking toward zero/negative as the guard drops to
     * chin/chest level or below. Uses whichever wrist is available/higher, since one
     * hand may be extended to punch while the other still guards. */
    private fun guardHeightForWindow(window: List<FrameObservation>): Double {
        val values = window.mapNotNull { observation ->
            val noseY = observation.landmarks["nose"]?.y ?: return@mapNotNull null
            val leftWristY = observation.landmarks["left_wrist"]?.y
            val rightWristY = observation.landmarks["right_wrist"]?.y
            val rawWristY = when {
                leftWristY != null && rightWristY != null -> minOf(leftWristY, rightWristY)
                leftWristY != null -> leftWristY
                rightWristY != null -> rightWristY
                else -> return@mapNotNull null
            }
            (noseY - rawWristY).toDouble()
        }
        return if (values.isEmpty()) 0.0 else values.average()
    }

    /** Mean frame-to-frame speed of the hip midpoint on x/z only (y excluded - vertical
     * bob isn't footwork). Same shape as the existing wrist/elbow velocity features
     * used for punch classification, applied to the hip center instead. */
    private fun movementForWindow(window: List<FrameObservation>): Double {
        val speeds = window.zipWithNext().mapNotNull { (previous, current) ->
            val previousCenter = hipCenter(previous) ?: return@mapNotNull null
            val currentCenter = hipCenter(current) ?: return@mapNotNull null
            val deltaSeconds = (current.timestampMs - previous.timestampMs) / 1000.0
            if (deltaSeconds <= 0.0) return@mapNotNull null
            val dx = (currentCenter.first - previousCenter.first).toDouble()
            val dz = (currentCenter.second - previousCenter.second).toDouble()
            sqrt(dx * dx + dz * dz) / deltaSeconds
        }
        return if (speeds.isEmpty()) 0.0 else speeds.average()
    }

    private fun hipCenter(observation: FrameObservation): Pair<Float, Float>? {
        val leftHip = observation.landmarks["left_hip"] ?: return null
        val rightHip = observation.landmarks["right_hip"] ?: return null
        return ((leftHip.x + rightHip.x) / 2f) to ((leftHip.z + rightHip.z) / 2f)
    }

    /** Centered rolling mean per column, independently windowed, mirroring
     * smooth_graph_metrics(). [points] must already be sorted by timestampMs and
     * evenly spaced - true for anything produced by [computeGraphMetrics]. */
    fun smoothPerformancePoints(
        points: List<PerformancePoint>,
        strideMs: Long,
        guardHeightSmoothingMs: Long = DEFAULT_GUARD_HEIGHT_SMOOTHING_MS,
        movementSmoothingMs: Long = DEFAULT_MOVEMENT_SMOOTHING_MS,
    ): List<PerformancePoint> {
        if (points.size < 2 || strideMs <= 0) return points

        val guardHeight = smoothSeries(points.map { it.guardHeight }, sampleWindow(guardHeightSmoothingMs, strideMs))
        val movement = smoothSeries(points.map { it.movement }, sampleWindow(movementSmoothingMs, strideMs))

        return points.mapIndexed { index, point ->
            point.copy(
                guardHeight = guardHeight[index],
                movement = movement[index],
            )
        }
    }

    private fun sampleWindow(smoothingMs: Long, strideMs: Long): Int {
        return (smoothingMs.toDouble() / strideMs.toDouble()).roundToInt().coerceAtLeast(1)
    }

    /** Same semantics as pandas' rolling(window, center=True, min_periods=1).mean() -
     * the window shrinks near the edges instead of producing a missing value there. */
    private fun smoothSeries(values: List<Double>, windowSamples: Int): List<Double> {
        if (windowSamples <= 1) return values
        val half = windowSamples / 2
        return values.indices.map { index ->
            val from = (index - half).coerceAtLeast(0)
            val to = (index + (windowSamples - 1 - half)).coerceAtMost(values.lastIndex)
            values.subList(from, to + 1).average()
        }
    }

    /** Collapses [points] into bucketMs-wide buckets (mean per bucket), mirroring
     * downsample_graph_metrics(). Meant to run after [smoothPerformancePoints]: smooth
     * first for a stable trend, then downsample so each remaining point still reflects
     * that trend rather than one raw sample. */
    fun downsamplePerformancePoints(
        points: List<PerformancePoint>,
        bucketMs: Long = DEFAULT_DOWNSAMPLE_BUCKET_MS,
    ): List<PerformancePoint> {
        if (points.isEmpty() || bucketMs <= 0) return points

        return points
            .groupBy { it.timestampMs / bucketMs }
            .toSortedMap()
            .map { (_, bucketPoints) ->
                PerformancePoint(
                    timestampMs = bucketPoints.map { it.timestampMs }.average().roundToLong(),
                    guardHeight = bucketPoints.map { it.guardHeight }.average(),
                    movement = bucketPoints.map { it.movement }.average(),
                )
            }
    }

    data class PunchVolumeKeyframe(val timestampMs: Long, val punchVolume: Int)

    private data class PunchCombo(val startMs: Long, val endMs: Long, val punchEndTimesMs: List<Long>)

    /** Sparse points for the punch volume graph: one point at each individual punch's
     * own end time, holding the running count within its combo (1, 2, 3...), plus a 0
     * right before each combo starts and right after it ends. Connecting these
     * directly with straight lines gives a diagonal ramp up through each punch and
     * back down - never a flat hold for a punch's duration, and no smoothing/curve
     * fitting on top. Mirrors punch_volume.compute_punch_volume_keyframes(), using
     * [durationMs] in place of Python's pose-frame timestamp bounds. */
    fun computePunchVolumeKeyframes(
        punchWindows: List<PunchWindow>,
        durationMs: Long,
        comboGapMs: Long = DEFAULT_COMBO_GAP_MS,
    ): List<PunchVolumeKeyframe> {
        val combos = buildPunchCombos(punchWindows, comboGapMs)
        if (combos.isEmpty()) {
            return listOf(PunchVolumeKeyframe(0L, 0), PunchVolumeKeyframe(durationMs, 0))
        }

        val byTimestamp = LinkedHashMap<Long, Int>()
        fun mark(timestampMs: Long, value: Int) {
            byTimestamp[timestampMs.coerceIn(0L, durationMs)] = value
        }

        if (combos.first().startMs > 0L) {
            mark(0L, 0)
        }
        combos.forEach { combo ->
            mark(combo.startMs, 0)
            combo.punchEndTimesMs.forEachIndexed { index, endMs -> mark(endMs, index + 1) }
            if (combo.endMs < durationMs) {
                mark(minOf(combo.endMs + 1, durationMs), 0)
            }
        }
        if (durationMs > combos.last().endMs) {
            mark(durationMs, 0)
        }

        return byTimestamp.entries.sortedBy { it.key }.map { PunchVolumeKeyframe(it.key, it.value) }
    }

    /** Groups individual predicted punches into combos when the gap between one punch
     * ending and the next starting is at most comboGapMs. Mirrors the overlap-merge
     * that produces [punchWindows] in the first place, one level up. */
    private fun buildPunchCombos(punchWindows: List<PunchWindow>, comboGapMs: Long): List<PunchCombo> {
        if (punchWindows.isEmpty()) return emptyList()
        val sorted = punchWindows.sortedBy { it.startMs }
        Log.d(TAG, "buildPunchCombos: ${sorted.size} punches, comboGapMs=$comboGapMs")
        Log.d(TAG, "  punch 0: ${sorted.first().startMs}ms-${sorted.first().endMs}ms")

        val combos = mutableListOf<PunchCombo>()
        var comboStart = sorted.first().startMs
        var comboEnd = sorted.first().endMs
        var endTimesMs = mutableListOf(sorted.first().endMs)

        sorted.drop(1).forEachIndexed { index, window ->
            val gapMs = window.startMs - comboEnd
            if (gapMs <= comboGapMs) {
                Log.d(TAG, "  punch ${index + 1}: ${window.startMs}ms-${window.endMs}ms gap=${gapMs}ms <= $comboGapMs -> merged into combo")
                comboEnd = maxOf(comboEnd, window.endMs)
                endTimesMs.add(window.endMs)
            } else {
                Log.d(TAG, "  punch ${index + 1}: ${window.startMs}ms-${window.endMs}ms gap=${gapMs}ms > $comboGapMs -> new combo")
                combos += PunchCombo(comboStart, comboEnd, endTimesMs)
                comboStart = window.startMs
                comboEnd = window.endMs
                endTimesMs = mutableListOf(window.endMs)
            }
        }
        combos += PunchCombo(comboStart, comboEnd, endTimesMs)
        combos.forEachIndexed { index, combo ->
            Log.d(
                TAG,
                "  combo $index: ${combo.startMs}ms-${combo.endMs}ms " +
                    "durationMs=${combo.endMs - combo.startMs} punches=${combo.punchEndTimesMs.size}",
            )
        }
        return combos
    }
}
