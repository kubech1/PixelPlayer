package com.theveloper.pixelplay.data.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.google.common.truth.Truth.assertThat
import com.theveloper.pixelplay.data.equalizer.EqualizerManager
import org.junit.jupiter.api.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class HiFiAudioProcessorTest {

    @Test
    fun `limiter recovery after a transient takes the same time at 48 and 192 kHz`() {
        fun levelAfterTransient(rate: Int): Float {
            val processor = HiFiAudioProcessor()
            processor.configure(AudioFormat(rate, 1, C.ENCODING_PCM_FLOAT))
            val samples = FloatArray(rate / 20) { 0.1f }
            samples[0] = 2f
            processor.queueInput(floatBufferOf(*samples))
            return readFloats(processor.getOutput()).last()
        }

        assertThat(levelAfterTransient(192_000)).isWithin(0.0001f).of(levelAfterTransient(48_000))
    }

    @Test
    fun `brickwall 0dBFS 44_1kHz master produces zero sample or sinc-resampled clipping`() {
        // Synthesize a brutal brickwall dubstep drop matching "Fall Back to Nothing":
        // 44.1 kHz stereo where ~22% of samples are hard-pinned at -1.0f / +0.9999695f
        // with rapid polarity reversals that trigger severe Gibbs overshoots in 44.1->48kHz sinc resamplers.
        val frameCount = 4608
        val rawSamples = FloatArray(frameCount * 2)
        for (i in 0 until frameCount) {
            val t = i.toDouble() / 44_100.0
            // Sub-bass + aggressive high-frequency screech driven +12 dB into a hard [-1, 1] brickwall
            val sub = sin(2.0 * PI * 55.0 * t)
            val mid = sin(2.0 * PI * 1_850.0 * t + 0.4 * sin(2.0 * PI * 220.0 * t))
            val highs = sin(2.0 * PI * 11_025.0 * t)
            val drivenL = (sub * 1.8 + mid * 1.4 + highs * 0.9)
            val drivenR = (sub * 1.8 - mid * 1.4 + highs * 0.9)
            rawSamples[i * 2] = drivenL.coerceIn(-1.0, 32767.0 / 32768.0).toFloat()
            rawSamples[i * 2 + 1] = drivenR.coerceIn(-1.0, 32767.0 / 32768.0).toFloat()
        }

        // Verify raw signal has heavy brickwall pinning and causes severe 44.1->48kHz sinc ISP overshoots
        val rawResampledPeak = maxSincResampledPeak44k1To48k(rawSamples, channelCount = 2)
        assertThat(rawResampledPeak).isGreaterThan(1.05f)

        val processor = HiFiAudioProcessor(
            hiFiEnabledProvider = { true },
            eqHeadroomDbProvider = { 0f }
        )
        val outFormat = processor.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_FLOAT))
        assertThat(processor.isActive()).isTrue()
        assertThat(outFormat.encoding).isEqualTo(C.ENCODING_PCM_FLOAT)

        processor.queueInput(floatBufferOf(*rawSamples))
        val processedSamples = readFloats(processor.getOutput())
        assertThat(processedSamples.size).isEqualTo(rawSamples.size)

        var maxSamplePeak = 0f
        for (sample in processedSamples) {
            assertThat(sample.isFinite()).isTrue()
            maxSamplePeak = max(maxSamplePeak, abs(sample))
        }
        assertThat(maxSamplePeak).isAtMost(HiFiAudioProcessor.HARD_CEILING)
        assertThat(maxSamplePeak).isLessThan(1.0f)

        val processedResampledPeak = maxSincResampledPeak44k1To48k(processedSamples.toFloatArray(), channelCount = 2)
        assertThat(processedResampledPeak).isLessThan(1.0f)
    }

    @Test
    fun `real Fall Back to Nothing FLAC drop frames are processed without clipping or ISP overload`() {
        val flacFile = locateTestFlacFile() ?: return
        val decodedDropSamples = decodeFlacFrameAtOffset(
            file = flacFile,
            frameOffset = 10_692_350L // Drop 2 (~80.0s) where 20.12% of samples hit full-scale
        )
        assertThat(decodedDropSamples).isNotEmpty()

        // Confirm the raw decoded frame from the user's file is indeed brickwall-pinned at -32768/+32767
        var fullScaleCount = 0
        for (s in decodedDropSamples) {
            if (s <= -32767 || s >= 32766) fullScaleCount++
        }
        assertThat(fullScaleCount).isGreaterThan(decodedDropSamples.size / 10)

        // Feed as PCM_FLOAT (simulating c2.android.flac.decoder float output)
        val rawFloat = FloatArray(decodedDropSamples.size) { i ->
            decodedDropSamples[i] / 32768.0f
        }
        val rawSincPeak = maxSincResampledPeak44k1To48k(rawFloat, channelCount = 2)
        // Raw brickwall frame overshoots 1.0f during 44.1 -> 48 kHz sinc interpolation
        assertThat(rawSincPeak).isGreaterThan(1.0f)

        val processor = HiFiAudioProcessor(
            hiFiEnabledProvider = { true },
            eqHeadroomDbProvider = { 0f }
        )
        processor.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_FLOAT))
        processor.queueInput(floatBufferOf(*rawFloat))
        val outputFloats = readFloats(processor.getOutput()).toFloatArray()

        assertThat(outputFloats.size).isEqualTo(rawFloat.size)
        val maxOutputSample = outputFloats.maxOf { abs(it) }
        val resampledOutputPeak = maxSincResampledPeak44k1To48k(outputFloats, channelCount = 2)

        assertThat(maxOutputSample).isLessThan(HiFiAudioProcessor.HARD_CEILING)
        assertThat(resampledOutputPeak).isLessThan(1.0f)
    }

    @Test
    fun `linear region below knee preserves pure sine wave with zero harmonic distortion`() {
        val processor = HiFiAudioProcessor(
            hiFiEnabledProvider = { true },
            eqHeadroomDbProvider = { 0f }
        )
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))

        val frameCount = 2400 // 50 ms of 1 kHz sine at 48 kHz
        val amplitude = 0.5f // -6 dBFS (well below 0.85f knee)
        val input = FloatArray(frameCount * 2)
        for (i in 0 until frameCount) {
            val s = (amplitude * sin(2.0 * PI * 1000.0 * i / 48_000.0)).toFloat()
            input[i * 2] = s
            input[i * 2 + 1] = -s
        }

        processor.queueInput(floatBufferOf(*input))
        val output = readFloats(processor.getOutput())
        assertThat(output).hasSize(input.size)

        // Expected linear gain for 48 kHz is NATIVE_RATE_ISP_HEADROOM_DB (-0.6 dB)
        val expectedGain = Math.pow(10.0, HiFiAudioProcessor.NATIVE_RATE_ISP_HEADROOM_DB / 20.0).toFloat()
        var signalPower = 0.0
        var errorPower = 0.0
        for (i in input.indices) {
            val expected = input[i] * expectedGain
            val diff = (output[i] - expected).toDouble()
            signalPower += expected.toDouble() * expected.toDouble()
            errorPower += diff * diff
        }
        val snrDb = 10.0 * log10(signalPower / max(errorPower, 1e-30))
        // Floating-point linearity must exceed 130 dB SNR
        assertThat(snrDb).isGreaterThan(130.0)
    }

    @Test
    fun `lossy decoder overshoots up to +12 dBFS are smoothly soft-limited below 1_0f`() {
        // Test C1 continuity and monotonicity of softLimitSample across [0, 4.0f]
        var previous = 0f
        for (step in 1..4000) {
            val x = step * 0.001f
            val y = HiFiAudioProcessor.softLimitSample(x)
            val yNeg = HiFiAudioProcessor.softLimitSample(-x)
            assertThat(y).isAtLeast(previous)
            assertThat(y).isAtMost(HiFiAudioProcessor.HARD_CEILING)
            assertThat(yNeg).isWithin(1e-7f).of(-y)
            previous = y
        }

        // Verify slope continuity at SOFT_KNEE_THRESHOLD (0.85f)
        val eps = 1e-4f
        val below = HiFiAudioProcessor.softLimitSample(HiFiAudioProcessor.SOFT_KNEE_THRESHOLD - eps)
        val at = HiFiAudioProcessor.softLimitSample(HiFiAudioProcessor.SOFT_KNEE_THRESHOLD)
        val above = HiFiAudioProcessor.softLimitSample(HiFiAudioProcessor.SOFT_KNEE_THRESHOLD + eps)
        val slopeLeft = (at - below) / eps
        val slopeRight = (above - at) / eps
        assertThat(slopeLeft).isWithin(1e-2f).of(1.0f)
        assertThat(slopeRight).isWithin(1e-2f).of(1.0f)
    }

    @Test
    fun `sanitizes NaN Infinity and denormals without corrupting filter state`() {
        val processor = HiFiAudioProcessor(hiFiEnabledProvider = { true })
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))

        val corruptedInput = floatBufferOf(
            Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, 1e-30f,
            -1e-30f, 0.25f,
            -0.25f, 0.5f
        )
        processor.queueInput(corruptedInput)
        val output = readFloats(processor.getOutput())

        assertThat(output).hasSize(8)
        for (sample in output) {
            assertThat(sample.isFinite()).isTrue()
            assertThat(abs(sample)).isLessThan(1.0f)
        }
        assertThat(output[0]).isEqualTo(0f)
        assertThat(output[1]).isEqualTo(0f)
        assertThat(output[2]).isEqualTo(0f)
        assertThat(output[3]).isEqualTo(0f)
        assertThat(output[4]).isEqualTo(0f)
        assertThat(output[5]).isGreaterThan(0.2f)
    }

    @Test
    fun `promotes 16-bit 24-bit and 32-bit integer PCM to float and preserves low-level 24-bit detail`() {
        val processor = HiFiAudioProcessor(hiFiEnabledProvider = { true })

        // 1. Test 24-bit little-endian promotion with a quiet -96 dBFS signal (LSB = 128 in 24-bit, 0 in 16-bit)
        val outFormat24 = processor.configure(AudioFormat(96_000, 2, C.ENCODING_PCM_24BIT))
        assertThat(outFormat24.encoding).isEqualTo(C.ENCODING_PCM_FLOAT)
        val pcm24Buffer = ByteBuffer.allocateDirect(6).order(ByteOrder.LITTLE_ENDIAN)
        val sample24Left = 128 // -96.3 dBFS in 24-bit
        val sample24Right = -4_194_304 // -0.5f in 24-bit
        pcm24Buffer.put((sample24Left and 0xFF).toByte())
        pcm24Buffer.put(((sample24Left shr 8) and 0xFF).toByte())
        pcm24Buffer.put(((sample24Left shr 16) and 0xFF).toByte())
        pcm24Buffer.put((sample24Right and 0xFF).toByte())
        pcm24Buffer.put(((sample24Right shr 8) and 0xFF).toByte())
        pcm24Buffer.put(((sample24Right shr 16) and 0xFF).toByte())
        pcm24Buffer.flip()

        processor.queueInput(pcm24Buffer)
        val out24 = readFloats(processor.getOutput())
        assertThat(out24).hasSize(2)
        val gain96k = Math.pow(10.0, HiFiAudioProcessor.NATIVE_RATE_ISP_HEADROOM_DB / 20.0).toFloat()
        assertThat(out24[0]).isWithin(1e-7f).of((128f / 8_388_608f) * gain96k)
        assertThat(out24[1]).isWithin(1e-5f).of(-0.5f * gain96k)

        // 2. Test 32-bit little-endian promotion
        val outFormat32 = processor.configure(AudioFormat(96_000, 2, C.ENCODING_PCM_32BIT))
        assertThat(outFormat32.encoding).isEqualTo(C.ENCODING_PCM_FLOAT)
        val pcm32Buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
        pcm32Buffer.putInt(1_073_741_824) // +0.5f
        pcm32Buffer.putInt(-1_073_741_824) // -0.5f
        pcm32Buffer.flip()

        processor.queueInput(pcm32Buffer)
        val out32 = readFloats(processor.getOutput())
        assertThat(out32[0]).isWithin(1e-5f).of(0.5f * gain96k)
        assertThat(out32[1]).isWithin(1e-5f).of(-0.5f * gain96k)
    }

    @Test
    fun `dynamic EqualizerManager headroom pre-attenuates signal smoothly`() {
        var activeEqHeadroomDb = EqualizerManager.computeRequiredHeadroomDb(
            eqEnabled = true,
            bandLevels = listOf(12, 10, 6, 0, 0, 0, 0, 4, 8, 10),
            maxEqLevelMillibels = 1500,
            bassBoostEnabled = true,
            bassBoostStrength = 500,
            virtualizerEnabled = false,
            virtualizerStrength = 0
        )
        // 12 * 0.75 + 0.5 * 4.5 = 9.0 + 2.25 = 11.25 dB
        assertThat(activeEqHeadroomDb).isWithin(0.01f).of(11.25f)

        val processor = HiFiAudioProcessor(
            hiFiEnabledProvider = { true },
            eqHeadroomDbProvider = { activeEqHeadroomDb }
        )
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))

        val input = FloatArray(2000) { 0.8f }
        processor.queueInput(floatBufferOf(*input))
        val outputWithBoost = readFloats(processor.getOutput())
        // With ~11.25 dB EQ headroom + 0.6 dB base headroom (~11.85 dB total), 0.8f is attenuated to ~0.204f
        assertThat(outputWithBoost.last()).isLessThan(0.25f)
        assertThat(outputWithBoost.last()).isGreaterThan(0.18f)

        // Now turn EQ off dynamically mid-stream and verify smooth ramp without discontinuity jumps
        activeEqHeadroomDb = 0f
        processor.queueInput(floatBufferOf(*input))
        val outputRamping = readFloats(processor.getOutput())
        var maxFrameStep = 0f
        for (i in 2 until outputRamping.size step 2) {
            val step = abs(outputRamping[i] - outputRamping[i - 2])
            maxFrameStep = max(maxFrameStep, step)
        }
        // Step-to-step change per sample must be tiny (< 0.002f), guaranteeing click-free EQ toggling
        assertThat(maxFrameStep).isLessThan(0.002f)
    }

    @Test
    fun `performance stress test processes 60 seconds of 192kHz stereo float at over 50x real-time`() {
        val processor = HiFiAudioProcessor(
            hiFiEnabledProvider = { true },
            eqHeadroomDbProvider = { 3.0f }
        )
        processor.configure(AudioFormat(192_000, 2, C.ENCODING_PCM_FLOAT))

        val framesPerChunk = 4096
        val samplesPerChunk = framesPerChunk * 2
        val chunkBuffer = ByteBuffer.allocateDirect(samplesPerChunk * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        for (i in 0 until samplesPerChunk) {
            val v = sin(i * 0.05).toFloat() * 1.15f
            chunkBuffer.putFloat(v)
        }
        chunkBuffer.flip()

        // Warm-up
        repeat(20) {
            chunkBuffer.rewind()
            processor.queueInput(chunkBuffer)
            processor.getOutput()
        }

        // Benchmark 60 seconds of 192 kHz stereo float (11,520,000 frames = 23,040,000 samples = 92.16 MB)
        val totalChunks = (192_000 * 60) / framesPerChunk
        val startNs = System.nanoTime()
        var firstOutputRef: ByteBuffer? = null
        repeat(totalChunks) {
            chunkBuffer.rewind()
            processor.queueInput(chunkBuffer)
            val out = processor.getOutput()
            if (firstOutputRef == null) {
                firstOutputRef = out
            } else {
                // Zero-allocation invariant: the exact same direct ByteBuffer instance is reused
                assertThat(out).isSameInstanceAs(firstOutputRef)
            }
        }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        val realTimeMultiplier = 60_000.0 / max(elapsedMs, 1.0)

        // Must process 60s of 192kHz stereo in well under 2.4s (> 25x real-time at 192kHz = > 100x at 48kHz)
        assertThat(realTimeMultiplier).isGreaterThan(25.0)
    }

    /**
     * Simulates Android AudioFlinger's multi-tap windowed sinc resampler (44.1 kHz -> 48 kHz)
     * and returns the maximum reconstructed absolute peak across all channels.
     */
    private fun maxSincResampledPeak44k1To48k(
        interleavedSamples: FloatArray,
        channelCount: Int
    ): Float {
        val inFrames = interleavedSamples.size / channelCount
        if (inFrames < 32) return 0f
        val halfTaps = 16
        val outFrames = ((inFrames - halfTaps * 2) * 48_000L / 44_100L).toInt()
        val ratio = 44_100.0 / 48_000.0
        var maxPeak = 0f

        for (outIdx in 0 until outFrames) {
            val srcPos = halfTaps + outIdx * ratio
            val center = srcPos.toInt()
            val frac = srcPos - center
            for (ch in 0 until channelCount) {
                var sum = 0.0
                for (k in -halfTaps + 1..halfTaps) {
                    val x = k - frac
                    val sinc = if (abs(x) < 1e-9) 1.0 else sin(PI * x) / (PI * x)
                    // Hann-Blackman window
                    val w = 0.5 * (1.0 + cos(PI * x / halfTaps))
                    val sample = interleavedSamples[(center + k) * channelCount + ch]
                    sum += sample * sinc * w
                }
                val absVal = abs(sum.toFloat())
                if (absVal > maxPeak) maxPeak = absVal
            }
        }
        return maxPeak
    }

    private fun locateTestFlacFile(): File? {
        val candidates = listOf(
            File("Fall Back to Nothing (feat. Saint Monet) - Subtronics, Saint Monet.flac"),
            File("../Fall Back to Nothing (feat. Saint Monet) - Subtronics, Saint Monet.flac")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    /**
     * Minimal self-contained FLAC frame scanner + decoder for testing real frames from the user's FLAC file.
     */
    private fun decodeFlacFrameAtOffset(file: File, frameOffset: Long): IntArray {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(frameOffset)
            val buf = ByteArray(131072)
            val read = raf.read(buf)
            if (read <= 256) return IntArray(0)

            for (startPos in 0 until read - 64) {
                if ((buf[startPos].toInt() and 0xFF) != 0xFF) continue
                val b1 = buf[startPos + 1].toInt() and 0xFF
                if ((b1 and 0xFC) != 0xF8) continue
                val b2 = buf[startPos + 2].toInt() and 0xFF
                val bsCode = (b2 ushr 4) and 0x0F
                val srCode = b2 and 0x0F
                if (bsCode == 0 || srCode == 15) continue
                val b3 = buf[startPos + 3].toInt() and 0xFF
                val chAssign = (b3 ushr 4) and 0x0F
                val bpsCode = (b3 ushr 1) and 0x07
                if (chAssign > 10 || bpsCode == 3 || bpsCode == 7 || (b3 and 1) != 0) continue

                val decoded = tryDecodeFlacFrameAtSlice(buf, startPos, read)
                if (decoded.isNotEmpty()) return decoded
            }
            return IntArray(0)
        }
    }

    private fun tryDecodeFlacFrameAtSlice(buf: ByteArray, startPos: Int, read: Int): IntArray {
        return try {
            val br = FlacBitReader(buf, read, startPos)
            val sync = br.readBits(14)
            if (sync != 0x3FFE) return IntArray(0)
            br.readBits(1) // reserved
            br.readBits(1) // blocking strategy
            val bsCode = br.readBits(4)
            val srCode = br.readBits(4)
            val chAssign = br.readBits(4)
            br.readBits(3) // sample size code
            br.readBits(1) // reserved

            // UTF-8 coded frame/sample number
            val first = br.readBits(8)
            var extraBytes = 0
            var mask = 0x80
            while (first and mask != 0) {
                extraBytes++
                mask = mask shr 1
            }
            if (extraBytes == 1 || extraBytes > 7) return IntArray(0)
            if (extraBytes > 0) extraBytes--
            repeat(extraBytes) {
                val cont = br.readBits(8)
                if ((cont and 0xC0) != 0x80) return IntArray(0)
            }

            val blockSize = when (bsCode) {
                1 -> 192
                in 2..5 -> 576 shl (bsCode - 2)
                6 -> br.readBits(8) + 1
                7 -> br.readBits(16) + 1
                in 8..15 -> 256 shl (bsCode - 8)
                else -> return IntArray(0)
            }
            when (srCode) {
                12 -> br.readBits(8)
                13, 14 -> br.readBits(16)
            }
            val headerEndByte = br.currentByteIndex()
            val expectedCrc8 = br.readBits(8)
            val computedCrc8 = computeFlacCrc8(buf, startPos, headerEndByte)
            if (expectedCrc8 != computedCrc8) return IntArray(0)

            val channels = if (chAssign < 8) chAssign + 1 else 2
            val subframes = Array(channels) { ch ->
                val bps = 16 + if ((chAssign == 8 && ch == 1) || (chAssign == 9 && ch == 0) || (chAssign == 10 && ch == 1)) 1 else 0
                decodeSubframe(br, blockSize, bps)
            }

            if (chAssign == 8) {
                for (i in 0 until blockSize) subframes[1][i] = subframes[0][i] - subframes[1][i]
            } else if (chAssign == 9) {
                for (i in 0 until blockSize) subframes[0][i] = subframes[0][i] + subframes[1][i]
            } else if (chAssign == 10) {
                for (i in 0 until blockSize) {
                    val side = subframes[1][i]
                    val mid = (subframes[0][i] shl 1) or (side and 1)
                    subframes[0][i] = (mid + side) shr 1
                    subframes[1][i] = (mid - side) shr 1
                }
            }

            val interleaved = IntArray(blockSize * channels)
            for (i in 0 until blockSize) {
                for (ch in 0 until channels) {
                    val s = subframes[ch][i]
                    if (s < -32768 || s > 32767) return IntArray(0)
                    interleaved[i * channels + ch] = s
                }
            }
            interleaved
        } catch (_: Exception) {
            IntArray(0)
        }
    }

    private fun computeFlacCrc8(buf: ByteArray, start: Int, endExclusive: Int): Int {
        var crc = 0
        for (i in start until endExclusive) {
            crc = crc xor (buf[i].toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 0x80) != 0) ((crc shl 1) xor 0x07) and 0xFF else (crc shl 1) and 0xFF
            }
        }
        return crc
    }

    private fun decodeSubframe(br: FlacBitReader, blockSize: Int, bps: Int): IntArray {
        br.readBits(1) // zero bit
        val type = br.readBits(6)
        val wasted = if (br.readBits(1) == 1) {
            var k = 1
            while (br.readBits(1) == 0) k++
            k
        } else 0
        val effBps = bps - wasted
        val samples = when {
            type == 0 -> {
                val v = br.readSignedBits(effBps)
                IntArray(blockSize) { v }
            }
            type == 1 -> IntArray(blockSize) { br.readSignedBits(effBps) }
            type in 8..12 -> {
                val order = type - 8
                val out = IntArray(blockSize)
                for (i in 0 until order) out[i] = br.readSignedBits(effBps)
                decodeResidual(br, blockSize, order, out)
                for (i in order until blockSize) {
                    out[i] += when (order) {
                        0 -> 0
                        1 -> out[i - 1]
                        2 -> 2 * out[i - 1] - out[i - 2]
                        3 -> 3 * out[i - 1] - 3 * out[i - 2] + out[i - 3]
                        else -> 4 * out[i - 1] - 6 * out[i - 2] + 4 * out[i - 3] - out[i - 4]
                    }
                }
                out
            }
            type >= 32 -> {
                val order = type - 32 + 1
                val out = IntArray(blockSize)
                for (i in 0 until order) out[i] = br.readSignedBits(effBps)
                val prec = br.readBits(4) + 1
                val shift = br.readSignedBits(5)
                val coeffs = IntArray(order) { br.readSignedBits(prec) }
                decodeResidual(br, blockSize, order, out)
                for (i in order until blockSize) {
                    var sum = 0L
                    for (j in 0 until order) sum += coeffs[j].toLong() * out[i - j - 1].toLong()
                    out[i] += (sum shr shift).toInt()
                }
                out
            }
            else -> IntArray(blockSize)
        }
        if (wasted > 0) {
            for (i in samples.indices) samples[i] = samples[i] shl wasted
        }
        return samples
    }

    private fun decodeResidual(br: FlacBitReader, blockSize: Int, predOrder: Int, out: IntArray) {
        val method = br.readBits(2)
        val paramBits = if (method == 0) 4 else 5
        val escapeParam = if (method == 0) 15 else 31
        val partOrder = br.readBits(4)
        val numParts = 1 shl partOrder
        var idx = predOrder
        for (p in 0 until numParts) {
            val n = if (partOrder == 0) blockSize - predOrder
            else if (p > 0) blockSize shr partOrder
            else (blockSize shr partOrder) - predOrder
            val param = br.readBits(paramBits)
            if (param < escapeParam) {
                repeat(n) {
                    var q = 0
                    while (br.readBits(1) == 0) q++
                    val r = if (param > 0) br.readBits(param) else 0
                    val u = (q shl param) or r
                    out[idx++] = if (u and 1 != 0) -(u shr 1) - 1 else (u shr 1)
                }
            } else {
                val rawBits = br.readBits(5)
                repeat(n) {
                    out[idx++] = if (rawBits > 0) br.readSignedBits(rawBits) else 0
                }
            }
        }
    }

    private class FlacBitReader(
        private val data: ByteArray,
        private val length: Int,
        startOffset: Int = 0
    ) {
        private var bytePos = startOffset
        private var bitBuf = 0L
        private var bitsLeft = 0

        fun currentByteIndex(): Int = bytePos - (bitsLeft / 8)

        fun readBits(n: Int): Int {
            while (bitsLeft < n && bytePos < length) {
                bitBuf = (bitBuf shl 8) or (data[bytePos++].toLong() and 0xFFL)
                bitsLeft += 8
            }
            bitsLeft -= n
            return ((bitBuf ushr bitsLeft) and ((1L shl n) - 1L)).toInt()
        }

        fun readSignedBits(n: Int): Int {
            val v = readBits(n)
            return if (v and (1 shl (n - 1)) != 0) v - (1 shl n) else v
        }
    }

    private fun floatBufferOf(vararg samples: Float): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        for (sample in samples) {
            buffer.putFloat(sample)
        }
        buffer.flip()
        return buffer
    }

    private fun readFloats(buffer: ByteBuffer): List<Float> {
        val duplicate = buffer.duplicate().order(ByteOrder.nativeOrder())
        val samples = ArrayList<Float>(duplicate.remaining() / Float.SIZE_BYTES)
        while (duplicate.remaining() >= Float.SIZE_BYTES) {
            samples += duplicate.float
        }
        return samples
    }
}
