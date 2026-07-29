package com.breadler.boxingperformancetracker.data.processing

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TflitePunchClassifier private constructor(
    private val interpreter: Interpreter,
    private val metadata: PunchModelMetadata,
) : AutoCloseable {
    val windowMs: Long = metadata.windowMs
    val strideMs: Long = metadata.strideMs
    val threshold: Float = metadata.punchThreshold

    fun classify(features: Map<String, Float>): Float {
        val input = ByteBuffer
            .allocateDirect(metadata.featureColumns.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        metadata.featureColumns.forEach { column ->
            input.putFloat(features[column] ?: 0f)
        }
        input.rewind()

        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputSize = outputShape.fold(1) { total, value -> total * value }
        val output = Array(1) { FloatArray(outputSize.coerceAtLeast(1)) }
        interpreter.run(input, output)
        return when {
            output[0].size >= 2 -> output[0][metadata.punchOutputIndex.coerceIn(output[0].indices)]
            else -> output[0][0]
        }.coerceIn(0f, 1f)
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "TflitePunchClassifier"

        fun createOrNull(context: Context): TflitePunchClassifier? {
            return runCatching {
                val metadata = context.assets.open("punch_model_metadata.json").reader().use { reader ->
                    Gson().fromJson(reader, PunchModelMetadata::class.java)
                }
                TflitePunchClassifier(
                    interpreter = Interpreter(loadAssetModel(context, "punch_model.tflite")),
                    metadata = metadata,
                )
            }.onFailure { error ->
                Log.e(TAG, "Failed to load on-device punch model (punch_model.tflite / punch_model_metadata.json)", error)
            }.getOrNull()
        }

        private fun loadAssetModel(context: Context, assetName: String): MappedByteBuffer {
            val descriptor = context.assets.openFd(assetName)
            FileInputStream(descriptor.fileDescriptor).use { inputStream ->
                return inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            }
        }
    }
}

data class PunchModelMetadata(
    val featureColumns: List<String>,
    val windowMs: Long = 250,
    val strideMs: Long = 40,
    val punchThreshold: Float = 0.5f,
    val punchOutputIndex: Int = 0,
)

