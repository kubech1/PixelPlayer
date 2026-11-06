@file:Suppress("DEPRECATION")
package com.theveloper.pixelplay.data.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An [AudioProcessor] that downmixes 5.1 (6-channel) and 7.1 (8-channel) surround audio
 * to stereo (2-channel) PCM using the standard Dolby downmix matrix coefficients.
 *
 * Downmix matrix:
 * ```
 *   L = FL + 0.707·FC + 0.707·SL [+ 0.707·SBL] + 0.707·LFE
 *   R = FR + 0.707·FC + 0.707·SR [+ 0.707·SBR] + 0.707·LFE
 * ```
 *
 * FFmpeg output channel order assumed:
 * - 5.1: FL, FR, FC, LFE, SL, SR
 * - 7.1: FL, FR, FC, LFE, SL, SR, SBL, SBR
 *
 * This processor is active for 6-channel or 8-channel 16-bit, 24-bit, 32-bit, or float PCM input.
 * All other formats are passed through without modification.
 */
@UnstableApi
class SurroundDownmixProcessor : AudioProcessor {

    companion object {
        /** Dolby standard surround downmix coefficient: 1/√2 ≈ 0.707 */
        private const val COEFF_SURROUND = 0.707f

        /** LFE (subwoofer) mix coefficient */
        private const val COEFF_LFE = 0.707f

        /** Largest supported surround layout (7.1). */
        private const val MAX_SUPPORTED_CHANNELS = 8

        private const val MIN_PCM_24 = -8_388_608
        private const val MAX_PCM_24 = 8_388_607

        // 5.1 channel indices (FFmpeg order)
        private const val FL_51  = 0
        private const val FR_51  = 1
        private const val FC_51  = 2
        private const val LFE_51 = 3
        private const val SL_51  = 4
        private const val SR_51  = 5

        // 7.1 channel indices (FFmpeg order)
        private const val FL_71  = 0
        private const val FR_71  = 1
        private const val FC_71  = 2
        private const val LFE_71 = 3
        private const val SL_71  = 4
        private const val SR_71  = 5
        private const val SBL_71 = 6
        private const val SBR_71 = 7
    }

    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    private var reusableBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private val floatScratch = FloatArray(MAX_SUPPORTED_CHANNELS)
    private val shortScratch = ShortArray(MAX_SUPPORTED_CHANNELS)
    private val intScratch = IntArray(MAX_SUPPORTED_CHANNELS)

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        val isSupportedEncoding = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_24BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_32BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val isSupported = (inputAudioFormat.channelCount == 6 || inputAudioFormat.channelCount == 8) &&
            isSupportedEncoding

        return if (isSupported) {
            inputFormat = inputAudioFormat
            outputFormat = AudioFormat(
                inputAudioFormat.sampleRate,
                /* channelCount = */ 2,
                inputAudioFormat.encoding
            )
            outputFormat
        } else {
            inputFormat = AudioFormat.NOT_SET
            outputFormat = AudioFormat.NOT_SET
            inputAudioFormat // pass-through
        }
    }

    override fun isActive(): Boolean = outputFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) return

        val channelCount = inputFormat.channelCount

        when (inputFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> {
                val bytesPerFrame = channelCount * Float.SIZE_BYTES
                val frameCount = inputBuffer.remaining() / bytesPerFrame
                outputBuffer = ensureOutputBuffer(frameCount * 2 * Float.SIZE_BYTES)

                val floatInput = inputBuffer.duplicate().order(ByteOrder.nativeOrder()).asFloatBuffer()
                repeat(frameCount) {
                    floatInput.get(floatScratch, 0, channelCount)
                    for (ch in 0 until channelCount) {
                        val s = floatScratch[ch]
                        if (!s.isFinite()) floatScratch[ch] = 0f
                    }
                    val left: Float
                    val right: Float
                    if (channelCount == 6) {
                        left = downmix51Left(floatScratch)
                        right = downmix51Right(floatScratch)
                    } else {
                        left = downmix71Left(floatScratch)
                        right = downmix71Right(floatScratch)
                    }
                    outputBuffer.putFloat(if (left.isFinite()) left.coerceIn(-1f, 1f) else 0f)
                    outputBuffer.putFloat(if (right.isFinite()) right.coerceIn(-1f, 1f) else 0f)
                }
                inputBuffer.position(inputBuffer.limit())
            }
            C.ENCODING_PCM_24BIT -> {
                val bytesPerFrame = channelCount * 3
                val frameCount = inputBuffer.remaining() / bytesPerFrame
                outputBuffer = ensureOutputBuffer(frameCount * 2 * 3)

                val source = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
                repeat(frameCount) {
                    for (ch in 0 until channelCount) {
                        val b0 = source.get().toInt() and 0xFF
                        val b1 = source.get().toInt() and 0xFF
                        val b2 = source.get().toInt()
                        intScratch[ch] = (b2 shl 16) or (b1 shl 8) or b0
                    }
                    val left = if (channelCount == 6) downmix51Left(intScratch) else downmix71Left(intScratch)
                    val right = if (channelCount == 6) downmix51Right(intScratch) else downmix71Right(intScratch)
                    putPcm24Sample(outputBuffer, left.toInt().coerceIn(MIN_PCM_24, MAX_PCM_24))
                    putPcm24Sample(outputBuffer, right.toInt().coerceIn(MIN_PCM_24, MAX_PCM_24))
                }
                inputBuffer.position(inputBuffer.limit())
            }
            C.ENCODING_PCM_32BIT -> {
                val bytesPerFrame = channelCount * Int.SIZE_BYTES
                val frameCount = inputBuffer.remaining() / bytesPerFrame
                outputBuffer = ensureOutputBuffer(frameCount * 2 * Int.SIZE_BYTES)

                val intInput = inputBuffer.duplicate().order(ByteOrder.nativeOrder()).asIntBuffer()
                repeat(frameCount) {
                    intInput.get(intScratch, 0, channelCount)
                    val left = if (channelCount == 6) downmix51LeftDouble(intScratch) else downmix71LeftDouble(intScratch)
                    val right = if (channelCount == 6) downmix51RightDouble(intScratch) else downmix71RightDouble(intScratch)
                    outputBuffer.putInt(left.toLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())
                    outputBuffer.putInt(right.toLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())
                }
                inputBuffer.position(inputBuffer.limit())
            }
            else -> {
                val bytesPerFrame = channelCount * Short.SIZE_BYTES
                val frameCount = inputBuffer.remaining() / bytesPerFrame
                outputBuffer = ensureOutputBuffer(frameCount * 2 * Short.SIZE_BYTES)

                val shortInput = inputBuffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
                repeat(frameCount) {
                    shortInput.get(shortScratch, 0, channelCount)
                    val left: Float
                    val right: Float
                    if (channelCount == 6) {
                        left = downmix51Left(shortScratch)
                        right = downmix51Right(shortScratch)
                    } else {
                        left = downmix71Left(shortScratch)
                        right = downmix71Right(shortScratch)
                    }
                    outputBuffer.putShort(left.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                    outputBuffer.putShort(right.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                }
                inputBuffer.position(inputBuffer.limit())
            }
        }
        outputBuffer.flip()
    }

    private fun putPcm24Sample(buffer: ByteBuffer, sample: Int) {
        buffer.put((sample and 0xFF).toByte())
        buffer.put(((sample shr 8) and 0xFF).toByte())
        buffer.put(((sample shr 16) and 0xFF).toByte())
    }

    private fun ensureOutputBuffer(requiredCapacity: Int): ByteBuffer {
        if (reusableBuffer.capacity() < requiredCapacity) {
            reusableBuffer = ByteBuffer.allocateDirect(requiredCapacity).order(ByteOrder.nativeOrder())
        } else {
            reusableBuffer.clear()
        }
        outputBuffer = reusableBuffer
        return reusableBuffer
    }

    override fun getOutput(): ByteBuffer {
        val pending = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return pending
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun queueEndOfStream() { inputEnded = true }

    @Deprecated("Media3 AudioProcessor now prefers flush(StreamMetadata); kept for interface compatibility")
    @Suppress("DEPRECATION")
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
        reusableBuffer = AudioProcessor.EMPTY_BUFFER
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
    }

    /** Left channel for a 5.1 surround frame (FL, FR, FC, LFE, SL, SR). */
    private fun downmix51Left(s: ShortArray): Float {
        return s[FL_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SL_51] + COEFF_LFE * s[LFE_51]
    }

    /** Right channel for a 5.1 surround frame (FL, FR, FC, LFE, SL, SR). */
    private fun downmix51Right(s: ShortArray): Float {
        return s[FR_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SR_51] + COEFF_LFE * s[LFE_51]
    }

    /** Left channel for a 7.1 surround frame (FL, FR, FC, LFE, SL, SR, SBL, SBR). */
    private fun downmix71Left(s: ShortArray): Float {
        return s[FL_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SL_71] + COEFF_SURROUND * s[SBL_71] + COEFF_LFE * s[LFE_71]
    }

    /** Right channel for a 7.1 surround frame (FL, FR, FC, LFE, SL, SR, SBL, SBR). */
    private fun downmix71Right(s: ShortArray): Float {
        return s[FR_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SR_71] + COEFF_SURROUND * s[SBR_71] + COEFF_LFE * s[LFE_71]
    }

    private fun downmix51Left(s: FloatArray): Float {
        return s[FL_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SL_51] + COEFF_LFE * s[LFE_51]
    }

    private fun downmix51Right(s: FloatArray): Float {
        return s[FR_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SR_51] + COEFF_LFE * s[LFE_51]
    }

    private fun downmix71Left(s: FloatArray): Float {
        return s[FL_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SL_71] + COEFF_SURROUND * s[SBL_71] + COEFF_LFE * s[LFE_71]
    }

    private fun downmix71Right(s: FloatArray): Float {
        return s[FR_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SR_71] + COEFF_SURROUND * s[SBR_71] + COEFF_LFE * s[LFE_71]
    }

    private fun downmix51Left(s: IntArray): Float {
        return s[FL_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SL_51] + COEFF_LFE * s[LFE_51]
    }

    private fun downmix51Right(s: IntArray): Float {
        return s[FR_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SR_51] + COEFF_LFE * s[LFE_51]
    }

    private fun downmix71Left(s: IntArray): Float {
        return s[FL_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SL_71] + COEFF_SURROUND * s[SBL_71] + COEFF_LFE * s[LFE_71]
    }

    private fun downmix71Right(s: IntArray): Float {
        return s[FR_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SR_71] + COEFF_SURROUND * s[SBR_71] + COEFF_LFE * s[LFE_71]
    }

    private fun downmix51LeftDouble(s: IntArray): Double {
        return s[FL_51].toDouble() + COEFF_SURROUND * s[FC_51].toDouble() + COEFF_SURROUND * s[SL_51].toDouble() + COEFF_LFE * s[LFE_51].toDouble()
    }

    private fun downmix51RightDouble(s: IntArray): Double {
        return s[FR_51].toDouble() + COEFF_SURROUND * s[FC_51].toDouble() + COEFF_SURROUND * s[SR_51].toDouble() + COEFF_LFE * s[LFE_51].toDouble()
    }

    private fun downmix71LeftDouble(s: IntArray): Double {
        return s[FL_71].toDouble() + COEFF_SURROUND * s[FC_71].toDouble() + COEFF_SURROUND * s[SL_71].toDouble() + COEFF_SURROUND * s[SBL_71].toDouble() + COEFF_LFE * s[LFE_71].toDouble()
    }

    private fun downmix71RightDouble(s: IntArray): Double {
        return s[FR_71].toDouble() + COEFF_SURROUND * s[FC_71].toDouble() + COEFF_SURROUND * s[SR_71].toDouble() + COEFF_SURROUND * s[SBR_71].toDouble() + COEFF_LFE * s[LFE_71].toDouble()
    }
}
