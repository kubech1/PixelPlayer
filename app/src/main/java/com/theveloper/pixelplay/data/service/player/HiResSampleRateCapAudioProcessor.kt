package com.theveloper.pixelplay.data.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

/**
 * Caps ultra-high-rate PCM to a rate Android devices handle more reliably.
 *
 * Media3/AudioTrack can end up in unstable device-specific paths for sample rates above 192 kHz.
 * Reducing 352.8/384 kHz streams before they reach the sink avoids the "loading audio" hang while
 * preserving normal playback for standard and regular hi-res files.
 */
@UnstableApi
class HiResSampleRateCapAudioProcessor(
    private val maxOutputSampleRateHz: Int = 192_000
) : AudioProcessor {

    private companion object {
        private val NATIVE_ORDER = ByteOrder.nativeOrder()
        private val NATIVE_ORDER_IS_BIG_ENDIAN = NATIVE_ORDER == ByteOrder.BIG_ENDIAN
        private const val MIN_PCM_24 = -8_388_608L
        private const val MAX_PCM_24 = 8_388_607L
    }

    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    private var downsampleFactor: Int = 1
    private var reusableBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var pendingBytes = ByteArray(0)
    private var pendingSize = 0
    private var shortAccumulators = IntArray(0)
    private var longAccumulators = LongArray(0)
    private var floatAccumulators = FloatArray(0)
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        val isSupportedEncoding = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_24BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_32BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val shouldDownsample = isSupportedEncoding &&
            inputAudioFormat.channelCount > 0 &&
            inputAudioFormat.sampleRate > maxOutputSampleRateHz

        if (!shouldDownsample) {
            inputFormat = AudioFormat.NOT_SET
            outputFormat = AudioFormat.NOT_SET
            downsampleFactor = 1
            pendingSize = 0
            return inputAudioFormat
        }

        downsampleFactor = ceil(
            inputAudioFormat.sampleRate.toDouble() / maxOutputSampleRateHz.toDouble()
        ).toInt().coerceAtLeast(2)

        inputFormat = inputAudioFormat
        outputFormat = AudioFormat(
            inputAudioFormat.sampleRate / downsampleFactor,
            inputAudioFormat.channelCount,
            inputAudioFormat.encoding
        )
        pendingSize = 0
        ensurePendingCapacity(maxPendingBytes())
        ensureAccumulatorCapacity(inputAudioFormat.channelCount)
        return outputFormat
    }

    override fun isActive(): Boolean = outputFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) return

        when (inputFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer)
            C.ENCODING_PCM_24BIT -> process24Bit(inputBuffer)
            C.ENCODING_PCM_32BIT -> process32Bit(inputBuffer)
            else -> process16Bit(inputBuffer)
        }
        inputBuffer.position(inputBuffer.limit())
    }

    private fun process16Bit(inputBuffer: ByteBuffer) {
        val bytesPerFrame = inputFormat.channelCount * Short.SIZE_BYTES
        val processableFrameCount = processableFrameCount(bytesPerFrame, inputBuffer.remaining())
        if (processableFrameCount == 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            stashPendingBytes(inputBuffer)
            return
        }

        outputBuffer = ensureOutputBuffer(processableFrameCount * bytesPerFrame)
        val shortInput = inputBuffer.duplicate().order(NATIVE_ORDER)
        var pendingReadOffset = 0

        repeat(processableFrameCount) {
            java.util.Arrays.fill(shortAccumulators, 0)
            repeat(downsampleFactor) {
                if (pendingReadOffset < pendingSize) {
                    for (ch in 0 until inputFormat.channelCount) {
                        shortAccumulators[ch] += readShortFromPending(pendingReadOffset).toInt()
                        pendingReadOffset += Short.SIZE_BYTES
                    }
                } else {
                    for (ch in 0 until inputFormat.channelCount) {
                        shortAccumulators[ch] += shortInput.short.toInt()
                    }
                }
            }
            for (ch in 0 until inputFormat.channelCount) {
                outputBuffer.putShort(
                    (shortAccumulators[ch] / downsampleFactor)
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                )
            }
        }
        storePendingRemainder(pendingReadOffset, shortInput)
        outputBuffer.flip()
    }

    private fun process24Bit(inputBuffer: ByteBuffer) {
        val bytesPerSample = 3
        val bytesPerFrame = inputFormat.channelCount * bytesPerSample
        val processableFrameCount = processableFrameCount(bytesPerFrame, inputBuffer.remaining())
        if (processableFrameCount == 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            stashPendingBytes(inputBuffer)
            return
        }

        outputBuffer = ensureOutputBuffer(processableFrameCount * bytesPerFrame)
        val source = inputBuffer.duplicate().order(NATIVE_ORDER)
        var pendingReadOffset = 0

        repeat(processableFrameCount) {
            java.util.Arrays.fill(longAccumulators, 0L)
            repeat(downsampleFactor) {
                if (pendingReadOffset < pendingSize) {
                    for (ch in 0 until inputFormat.channelCount) {
                        longAccumulators[ch] += readPcm24FromPending(pendingReadOffset).toLong()
                        pendingReadOffset += 3
                    }
                } else {
                    for (ch in 0 until inputFormat.channelCount) {
                        val b0 = source.get().toInt() and 0xFF
                        val b1 = source.get().toInt() and 0xFF
                        val b2 = source.get().toInt()
                        longAccumulators[ch] += ((b2 shl 16) or (b1 shl 8) or b0).toLong()
                    }
                }
            }
            for (ch in 0 until inputFormat.channelCount) {
                val sample = (longAccumulators[ch] / downsampleFactor).coerceIn(MIN_PCM_24, MAX_PCM_24).toInt()
                outputBuffer.put((sample and 0xFF).toByte())
                outputBuffer.put(((sample shr 8) and 0xFF).toByte())
                outputBuffer.put(((sample shr 16) and 0xFF).toByte())
            }
        }
        storePendingRemainder(pendingReadOffset, source)
        outputBuffer.flip()
    }

    private fun process32Bit(inputBuffer: ByteBuffer) {
        val bytesPerSample = Int.SIZE_BYTES
        val bytesPerFrame = inputFormat.channelCount * bytesPerSample
        val processableFrameCount = processableFrameCount(bytesPerFrame, inputBuffer.remaining())
        if (processableFrameCount == 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            stashPendingBytes(inputBuffer)
            return
        }

        outputBuffer = ensureOutputBuffer(processableFrameCount * bytesPerFrame)
        val intInput = inputBuffer.duplicate().order(NATIVE_ORDER)
        var pendingReadOffset = 0

        repeat(processableFrameCount) {
            java.util.Arrays.fill(longAccumulators, 0L)
            repeat(downsampleFactor) {
                if (pendingReadOffset < pendingSize) {
                    for (ch in 0 until inputFormat.channelCount) {
                        longAccumulators[ch] += readIntFromPending(pendingReadOffset).toLong()
                        pendingReadOffset += Int.SIZE_BYTES
                    }
                } else {
                    for (ch in 0 until inputFormat.channelCount) {
                        longAccumulators[ch] += intInput.int.toLong()
                    }
                }
            }
            for (ch in 0 until inputFormat.channelCount) {
                outputBuffer.putInt(
                    (longAccumulators[ch] / downsampleFactor)
                        .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                        .toInt()
                )
            }
        }
        storePendingRemainder(pendingReadOffset, intInput)
        outputBuffer.flip()
    }

    private fun processFloat(inputBuffer: ByteBuffer) {
        val bytesPerSample = Float.SIZE_BYTES
        val bytesPerFrame = inputFormat.channelCount * bytesPerSample
        val processableFrameCount = processableFrameCount(bytesPerFrame, inputBuffer.remaining())
        if (processableFrameCount == 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            stashPendingBytes(inputBuffer)
            return
        }

        outputBuffer = ensureOutputBuffer(processableFrameCount * bytesPerFrame)
        val floatInput = inputBuffer.duplicate().order(NATIVE_ORDER)
        var pendingReadOffset = 0

        repeat(processableFrameCount) {
            java.util.Arrays.fill(floatAccumulators, 0f)
            repeat(downsampleFactor) {
                if (pendingReadOffset < pendingSize) {
                    for (ch in 0 until inputFormat.channelCount) {
                        val s = readFloatFromPending(pendingReadOffset)
                        floatAccumulators[ch] += if (s.isFinite()) s else 0f
                        pendingReadOffset += Float.SIZE_BYTES
                    }
                } else {
                    for (ch in 0 until inputFormat.channelCount) {
                        val s = floatInput.float
                        floatAccumulators[ch] += if (s.isFinite()) s else 0f
                    }
                }
            }
            for (ch in 0 until inputFormat.channelCount) {
                val avg = floatAccumulators[ch] / downsampleFactor
                outputBuffer.putFloat(if (avg.isFinite()) avg.coerceIn(-1f, 1f) else 0f)
            }
        }
        storePendingRemainder(pendingReadOffset, floatInput)
        outputBuffer.flip()
    }

    private fun processableFrameCount(bytesPerFrame: Int, incomingBytes: Int): Int {
        val totalFrames = (pendingSize + incomingBytes) / bytesPerFrame
        return totalFrames / downsampleFactor
    }

    private fun maxPendingBytes(): Int {
        val sampleBytes = when (inputFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> Float.SIZE_BYTES
            C.ENCODING_PCM_32BIT -> Int.SIZE_BYTES
            C.ENCODING_PCM_24BIT -> 3
            else -> Short.SIZE_BYTES
        }
        return inputFormat.channelCount * sampleBytes * downsampleFactor
    }

    private fun ensurePendingCapacity(requiredCapacity: Int) {
        if (pendingBytes.size >= requiredCapacity) return
        pendingBytes = pendingBytes.copyOf(requiredCapacity.coerceAtLeast(pendingBytes.size * 2).coerceAtLeast(1))
    }

    private fun ensureAccumulatorCapacity(channelCount: Int) {
        if (shortAccumulators.size < channelCount) {
            shortAccumulators = IntArray(channelCount)
        }
        if (longAccumulators.size < channelCount) {
            longAccumulators = LongArray(channelCount)
        }
        if (floatAccumulators.size < channelCount) {
            floatAccumulators = FloatArray(channelCount)
        }
    }

    private fun ensureOutputBuffer(requiredCapacity: Int): ByteBuffer {
        if (reusableBuffer.capacity() < requiredCapacity) {
            reusableBuffer = ByteBuffer.allocateDirect(requiredCapacity).order(NATIVE_ORDER)
        } else {
            reusableBuffer.clear()
        }
        outputBuffer = reusableBuffer
        return reusableBuffer
    }

    private fun stashPendingBytes(inputBuffer: ByteBuffer) {
        ensurePendingCapacity(pendingSize + inputBuffer.remaining())
        val source = inputBuffer.duplicate()
        source.get(pendingBytes, pendingSize, source.remaining())
        pendingSize += inputBuffer.remaining()
    }

    private fun storePendingRemainder(pendingReadOffset: Int, inputBuffer: ByteBuffer) {
        val unreadPendingBytes = pendingSize - pendingReadOffset
        val remainingInputBytes = inputBuffer.remaining()
        ensurePendingCapacity(unreadPendingBytes + remainingInputBytes)
        if (unreadPendingBytes > 0 && pendingReadOffset > 0) {
            pendingBytes.copyInto(
                destination = pendingBytes,
                destinationOffset = 0,
                startIndex = pendingReadOffset,
                endIndex = pendingSize
            )
        }
        if (remainingInputBytes > 0) {
            inputBuffer.get(pendingBytes, unreadPendingBytes, remainingInputBytes)
        }
        pendingSize = unreadPendingBytes + remainingInputBytes
    }

    private fun readShortFromPending(offset: Int): Short {
        val byte0 = pendingBytes[offset].toInt() and 0xFF
        val byte1 = pendingBytes[offset + 1].toInt() and 0xFF
        val bits = if (NATIVE_ORDER_IS_BIG_ENDIAN) {
            (byte0 shl 8) or byte1
        } else {
            (byte1 shl 8) or byte0
        }
        return bits.toShort()
    }

    private fun readPcm24FromPending(offset: Int): Int {
        val byte0 = pendingBytes[offset].toInt() and 0xFF
        val byte1 = pendingBytes[offset + 1].toInt() and 0xFF
        val byte2 = pendingBytes[offset + 2].toInt() // sign-extended MSB in little-endian
        return (byte2 shl 16) or (byte1 shl 8) or byte0
    }

    private fun readFloatFromPending(offset: Int): Float {
        val intBits = readIntFromPending(offset)
        return Float.fromBits(intBits)
    }

    private fun readIntFromPending(offset: Int): Int {
        val byte0 = pendingBytes[offset].toInt() and 0xFF
        val byte1 = pendingBytes[offset + 1].toInt() and 0xFF
        val byte2 = pendingBytes[offset + 2].toInt() and 0xFF
        val byte3 = pendingBytes[offset + 3].toInt() and 0xFF
        return if (NATIVE_ORDER_IS_BIG_ENDIAN) {
            (byte0 shl 24) or (byte1 shl 16) or (byte2 shl 8) or byte3
        } else {
            (byte3 shl 24) or (byte2 shl 16) or (byte1 shl 8) or byte0
        }
    }

    override fun getOutput(): ByteBuffer {
        val pendingOutput = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return pendingOutput
    }

    override fun isEnded(): Boolean = inputEnded && pendingSize == 0 && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun queueEndOfStream() {
        pendingSize = 0
        inputEnded = true
    }

    @Deprecated("Media3 AudioProcessor now prefers flush(StreamMetadata); kept for interface compatibility")
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        pendingSize = 0
        inputEnded = false
    }

    override fun reset() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        pendingSize = 0
        inputEnded = false
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
        downsampleFactor = 1
    }
}
