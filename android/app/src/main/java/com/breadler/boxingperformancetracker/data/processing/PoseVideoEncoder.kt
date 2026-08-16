package com.breadler.boxingperformancetracker.data.processing

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

// Encodes annotated bitmaps into an H.264/MP4 file
class PoseVideoEncoder(
    outputFile: File,
    width: Int,
    height: Int,
    bitRate: Int = 6_000_000,
    frameRateHint: Int = 25,
) : AutoCloseable {
    private val width = width - (width % 2)
    private val height = height - (height % 2)
    private val bufferInfo = MediaCodec.BufferInfo()

    private val colorFormat: Int
    private val encoder: MediaCodec
    private val muxer: MediaMuxer = MediaMuxer(
        outputFile.absolutePath,
        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
    )
    private var muxerTrackIndex = -1
    private var muxerStarted = false
    private var lastPresentationTimeUs = 0L
    private var frameCount = 0

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, this.width, this.height)
        colorFormat = selectColorFormat()
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRateHint)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
    }

    // Encode a single frame
    fun encodeFrame(bitmap: Bitmap, presentationTimeUs: Long) {
        val safeTimeUs = if (frameCount == 0) {
            presentationTimeUs
        } else {
            presentationTimeUs.coerceAtLeast(lastPresentationTimeUs + 1L)
        }
        lastPresentationTimeUs = safeTimeUs
        frameCount += 1

        val scaled = if (bitmap.width != width || bitmap.height != height) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }

        val inputIndex = dequeueInputBufferBlocking()
        if (inputIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inputIndex)
            if (inputBuffer != null) {
                inputBuffer.clear()
                val yuv = bitmapToYuv420(scaled, colorFormat)
                inputBuffer.put(yuv)
                encoder.queueInputBuffer(inputIndex, 0, yuv.size, safeTimeUs, 0)
            } else {
                encoder.queueInputBuffer(inputIndex, 0, 0, safeTimeUs, 0)
            }
        }
        if (scaled !== bitmap) {
            scaled.recycle()
        }

        drainEncoder(endOfStream = false)
    }

    // Flush and finalize output file
    fun finish() {
        val inputIndex = dequeueInputBufferBlocking()
        if (inputIndex >= 0) {
            encoder.queueInputBuffer(inputIndex, 0, 0, lastPresentationTimeUs + 1L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainEncoder(endOfStream = true)
        close()
    }

    // Release encoder and muxer resources
    override fun close() {
        runCatching { encoder.stop() }
        runCatching { encoder.release() }
        if (muxerStarted) {
            runCatching { muxer.stop() }
        }
        runCatching { muxer.release() }
    }

    // Wait for a free input buffer
    private fun dequeueInputBufferBlocking(): Int {
        return encoder.dequeueInputBuffer(TIMEOUT_US)
    }

    // Pull encoded output and write it to the muxer
    private fun drainEncoder(endOfStream: Boolean) {
        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outputIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(muxerTrackIndex, outputBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return
                    }
                }
            }
        }
    }

    // Pick a YUV420 color format the encoder supports
    private fun selectColorFormat(): Int {
        val capabilities = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).let { codec ->
            try {
                codec.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
            } finally {
                codec.release()
            }
        }
        val supported = capabilities.colorFormats.toSet()
        return when {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar in supported ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar in supported ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            else -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }
    }

    // Convert an RGB bitmap to YUV420 bytes for the encoder
    private fun bitmapToYuv420(bitmap: Bitmap, colorFormat: Int): ByteArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val frameSize = w * h
        val chromaSize = frameSize / 4
        val out = ByteArray(frameSize + chromaSize * 2)

        var yIndex = 0
        val uBuffer = ByteArray(chromaSize)
        val vBuffer = ByteArray(chromaSize)
        var chromaIndex = 0

        for (row in 0 until h) {
            for (col in 0 until w) {
                val pixel = pixels[row * w + col]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                out[yIndex] = y.coerceIn(0, 255).toByte()
                yIndex += 1

                if (row % 2 == 0 && col % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    uBuffer[chromaIndex] = u.coerceIn(0, 255).toByte()
                    vBuffer[chromaIndex] = v.coerceIn(0, 255).toByte()
                    chromaIndex += 1
                }
            }
        }

        if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            var uvIndex = frameSize
            for (i in 0 until chromaSize) {
                out[uvIndex] = uBuffer[i]
                out[uvIndex + 1] = vBuffer[i]
                uvIndex += 2
            }
        } else {
            System.arraycopy(uBuffer, 0, out, frameSize, chromaSize)
            System.arraycopy(vBuffer, 0, out, frameSize + chromaSize, chromaSize)
        }

        return out
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}
