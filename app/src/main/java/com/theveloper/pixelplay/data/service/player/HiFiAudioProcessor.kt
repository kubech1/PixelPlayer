@file:Suppress("DEPRECATION")
package com.theveloper.pixelplay.data.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tanh

/**
 * Mastering-grade floating-point [AudioProcessor] for PixelPlayer's Hi-Fi (`PCM_FLOAT`) pipeline.
 *
 * ### Problems Solved
 * 1. **Inter-Sample Peak (ISP) Clipping in `AudioFlinger` Resampler (`44.1 kHz -> 48 kHz`):**
 *    Aggressively mastered tracks (e.g. modern electronic/dubstep with 15–20% of samples pinned at
 *    `0 dBFS` / `±1.0f`) produce inter-sample overshoots of `+1.5` to `+3.0 dBTP` (`±1.19f..1.41f`)
 *    when Android's multi-tap sinc resampler interpolates `44.1 kHz` into the `48 kHz` system mix.
 *    In `ENCODING_PCM_FLOAT`, unattenuated `±1.0f` brickwall runs saturate downstream resampler and
 *    effect stages.
 * 2. **Lossy Decoder & Surround Float Overshoots (`> 1.0f`):**
 *    Platform lossy decoders (`MP3`, `AAC`, `Opus`, `Vorbis`) and surround downmixes in
 *    `ENCODING_PCM_FLOAT` emit unclipped floating-point peaks above `±1.0f`, which Media3's
 *    `ToFloatPcmAudioProcessor` passes through untouched.
 * 3. **Equalizer / AudioEffect Headroom Overload:**
 *    Positive band boosts in `Equalizer`, `BassBoost`, or `Virtualizer` attached to the output
 *    `audioSessionId` clip `0 dBFS` masters unless compensated with pre-attenuation headroom.
 * 4. **`NaN` / `±Infinity` / Subnormal Sanitization:**
 *    Protects downstream IIR filters and mixer busses from non-finite float values and FPU
 *    denormal stalls.
 * 5. **Universal High-Precision Float Promotion:**
 *    When Hi-Fi mode is enabled, promotes `16-bit`, `24-bit`, and `32-bit` integer PCM streams
 *    directly to `C.ENCODING_PCM_FLOAT` (`32-bit IEEE 754 float`) before applying headroom and
 *    true-peak limiting, ensuring `DefaultAudioSink` always negotiates float output.
 *
 * ### Signal Path
 * - Below [SOFT_KNEE_THRESHOLD] (`0.85f`), the transfer function is **100% linear** ($y = g \cdot x$),
 *   preserving full 24-bit float mantissa resolution with zero harmonic distortion.
 * - A 4-tap Lagrange/Hermite half-sample interpolator estimates inter-sample true peaks
 *   ($\text{mid} = \frac{-s_{n-2} + 9(s_{n-1} + s_n) - s_{n+1}}{16}$) and drives a zero-latency,
 *   instant-attack, smooth-release envelope attenuator when an ISP would exceed [ISP_CEILING].
 * - Above [SOFT_KNEE_THRESHOLD], a $C^1$-continuous hyperbolic tangent soft-knee stage smoothly
 *   rounds peaks toward [HARD_CEILING] (`0.985f`), guaranteeing mathematically that no output
 *   sample ever reaches or exceeds `±1.0f`.
 */
@UnstableApi
class HiFiAudioProcessor(
    private val hiFiEnabledProvider: () -> Boolean = { true },
    private val eqHeadroomDbProvider: () -> Float = { 0f }
) : AudioProcessor {

    companion object {
        /**
         * Headroom (dB) applied to sample rates that require asynchronous resampling against a
         * 48 kHz system mixer clock (e.g. 44.1 kHz, 88.2 kHz, 176.4 kHz).
         */
        // Full-track reconstruction reached +4.8 dBTP on the 44.1 kHz validation master.
        const val ASYNC_RESAMPLE_ISP_HEADROOM_DB = -5.5f

        /**
         * Subtle headroom (dB) applied to 48 kHz-family rates (48 / 96 / 192 kHz) to protect
         * DAC reconstruction filters from 0 dBFS square-wave overshoots.
         */
        const val NATIVE_RATE_ISP_HEADROOM_DB = -0.6f

        /** Maximum dynamic EQ pre-cut (dB) applied when positive EQ/BassBoost bands are active. */
        const val MAX_EQ_HEADROOM_DB = 12.0f

        /** Linear threshold below which the soft-knee stage is a 100% bit-linear identity. */
        const val SOFT_KNEE_THRESHOLD = 0.85f

        /** Asymptotic ceiling (~-0.13 dBFS) of the C1-continuous tanh soft-knee stage. */
        const val HARD_CEILING = 0.985f

        /** True-peak (inter-sample polyphase) threshold (~-0.91 dBTP) for envelope protection. */
        const val ISP_CEILING = 0.90f

        private const val KNEE_WIDTH = HARD_CEILING - SOFT_KNEE_THRESHOLD
        private const val INV_KNEE_WIDTH = 1.0f / KNEE_WIDTH
        private const val DENORMAL_FLOOR = 1.0e-20f

        private const val PCM_16_SCALE = 1.0f / 32768.0f
        private const val PCM_24_SCALE = 1.0f / 8388608.0f
        private const val PCM_32_SCALE = (1.0 / 2147483648.0).toFloat()

        // Smooth parameter ramping (~10 ms time constant at 44.1 kHz) to avoid zipper noise.
        private const val GAIN_SMOOTHING_COEFF = 0.002f
        // Smooth envelope release (~45 ms time constant at 44.1 kHz) after an ISP transient.
        private const val ISP_ENVELOPE_RELEASE_COEFF = 0.0005f

        /**
         * Pure, stateless $C^1$-continuous mastering soft-knee transfer function.
         *
         * - For $|x| \le$ [SOFT_KNEE_THRESHOLD], returns $x$ with zero nonlinear alteration.
         * - For $|x| >$ [SOFT_KNEE_THRESHOLD], smoothly compresses into [HARD_CEILING] using $\tanh$.
         */
        fun softLimitSample(sample: Float): Float {
            if (!sample.isFinite()) return 0f
            val absX = abs(sample)
            if (absX < DENORMAL_FLOOR) return 0f
            if (absX <= SOFT_KNEE_THRESHOLD) return sample

            val excess = (absX - SOFT_KNEE_THRESHOLD) * INV_KNEE_WIDTH
            val compressed = SOFT_KNEE_THRESHOLD + KNEE_WIDTH * tanh(excess.toDouble()).toFloat()
            val bounded = min(compressed, HARD_CEILING)
            return if (sample >= 0f) bounded else -bounded
        }

        /**
         * 4-tap Lagrange/Hermite cubic 4x-oversampled polyphase filter estimating the
         * reconstructed inter-sample peak between [s1] and [s2] at $t \in \{0.25, 0.50, 0.75\}$.
         */
        fun estimateInterSamplePeak(s0: Float, s1: Float, s2: Float, s3: Float): Float {
            val p25 = (-9f * s0 + 111f * s1 + 29f * s2 - 3f * s3) * 0.0078125f
            val p50 = (-s0 + 9f * (s1 + s2) - s3) * 0.0625f
            val p75 = (-3f * s0 + 29f * s1 + 111f * s2 - 9f * s3) * 0.0078125f
            val maxSample = max(abs(s1), abs(s2))
            val maxInterp = max(max(abs(p25), abs(p50)), abs(p75))
            return max(maxSample, maxInterp)
        }
    }

    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    private var reusableBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var gainSmoothing = GAIN_SMOOTHING_COEFF
    private var envelopeRelease = ISP_ENVELOPE_RELEASE_COEFF

    private var baseIspGain: Float = 1.0f
    private var smoothedPreGain: Float = 1.0f
    private var ispEnvelopeGain: Float = 1.0f
    private var loggedFirstBufferAfterConfigure = false

    // Per-channel 2-sample history for 4-tap inter-sample peak detection across buffer boundaries
    private var historyPrev2 = FloatArray(0)
    private var historyPrev1 = FloatArray(0)
    private var currentFrameSamples = FloatArray(0)
    private var nextFrameSamples = FloatArray(0)

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.channelCount <= 0 || inputAudioFormat.sampleRate <= 0) {
            inputFormat = AudioFormat.NOT_SET
            outputFormat = AudioFormat.NOT_SET
            return inputAudioFormat
        }

        val hiFiEnabled = hiFiEnabledProvider()
        val isSupportedEncoding = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> true
            C.ENCODING_PCM_16BIT,
            C.ENCODING_PCM_24BIT,
            C.ENCODING_PCM_24BIT_BIG_ENDIAN,
            C.ENCODING_PCM_32BIT,
            C.ENCODING_PCM_32BIT_BIG_ENDIAN -> hiFiEnabled
            else -> false
        }

        if (!isSupportedEncoding) {
            inputFormat = AudioFormat.NOT_SET
            outputFormat = AudioFormat.NOT_SET
            return inputAudioFormat
        }

        inputFormat = inputAudioFormat
        outputFormat = AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_FLOAT
        )

        val baseHeadroomDb = if (requiresAsyncResamplingTo48kFamily(inputAudioFormat.sampleRate)) {
            ASYNC_RESAMPLE_ISP_HEADROOM_DB
        } else {
            NATIVE_RATE_ISP_HEADROOM_DB
        }
        // Preserve the 44.1 kHz tuning in elapsed time at every source rate.
        val referenceFramesPerFrame = 44_100.0 / inputAudioFormat.sampleRate
        gainSmoothing = (1.0 - (1.0 - GAIN_SMOOTHING_COEFF).pow(referenceFramesPerFrame)).toFloat()
        envelopeRelease = (1.0 - (1.0 - ISP_ENVELOPE_RELEASE_COEFF).pow(referenceFramesPerFrame)).toFloat()
        baseIspGain = dbToLinear(baseHeadroomDb)
        smoothedPreGain = computeTargetPreGain()
        ispEnvelopeGain = 1.0f
        loggedFirstBufferAfterConfigure = false

        ensureChannelScratchCapacity(inputAudioFormat.channelCount)
        return outputFormat
    }

    override fun isActive(): Boolean = outputFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) return
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        val channelCount = inputFormat.channelCount
        val bytesPerInputSample = bytesPerSample(inputFormat.encoding)
        val bytesPerInputFrame = channelCount * bytesPerInputSample
        val frameCount = remainingBytes / bytesPerInputFrame
        if (frameCount <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val outputByteCount = frameCount * channelCount * Float.SIZE_BYTES
        val out = ensureOutputBuffer(outputByteCount)
        val source = inputBuffer.duplicate().order(ByteOrder.nativeOrder())

        val targetPreGain = computeTargetPreGain()
        var currentPreGain = smoothedPreGain
        var currentIspGain = ispEnvelopeGain
        val prev2 = historyPrev2
        val prev1 = historyPrev1
        val curr = currentFrameSamples
        val next = nextFrameSamples

        // Prime first frame into `curr`
        readFrameAsFloat(source, inputFormat.encoding, channelCount, curr)

        for (frameIndex in 0 until frameCount) {
            // Smoothly ramp pre-gain toward target (ISP + EQ headroom)
            currentPreGain += (targetPreGain - currentPreGain) * gainSmoothing

            // Read lookahead frame (or repeat current frame at buffer boundary)
            if (frameIndex + 1 < frameCount) {
                readFrameAsFloat(source, inputFormat.encoding, channelCount, next)
            } else {
                for (ch in 0 until channelCount) {
                    next[ch] = curr[ch]
                }
            }

            // Estimate worst-case inter-sample peak across all channels for this frame,
            // including the 1-frame lookahead span [s2..s3] so `curr` is attenuated BEFORE
            // a brickwall discontinuity into `next`.
            var maxEstimatedTruePeak = 0f
            for (ch in 0 until channelCount) {
                val s0 = prev2[ch]
                val s1 = prev1[ch]
                val s2 = curr[ch] * currentPreGain
                val s3 = next[ch] * currentPreGain
                val spanPrevToCurr = estimateInterSamplePeak(s0, s1, s2, s3)
                val spanCurrToNext = estimateInterSamplePeak(s1, s2, s3, -s2)
                val channelTruePeak = max(spanPrevToCurr, spanCurrToNext)
                if (channelTruePeak > maxEstimatedTruePeak) {
                    maxEstimatedTruePeak = channelTruePeak
                }
            }

            // Instant attack if estimated true peak exceeds ISP_CEILING; smooth release otherwise
            if (maxEstimatedTruePeak * currentIspGain > ISP_CEILING && maxEstimatedTruePeak > 1e-6f) {
                currentIspGain = (ISP_CEILING / maxEstimatedTruePeak).coerceIn(0.25f, 1.0f)
            } else if (currentIspGain < 1.0f) {
                currentIspGain = min(1.0f, currentIspGain + (1.0f - currentIspGain) * envelopeRelease)
            }

            val totalFrameGain = currentPreGain * currentIspGain

            for (ch in 0 until channelCount) {
                val scaled = curr[ch] * totalFrameGain
                val limited = softLimitSample(scaled)
                out.putFloat(limited)

                // Advance history state using the pre-scaled signal
                prev2[ch] = prev1[ch]
                prev1[ch] = curr[ch] * currentPreGain
                curr[ch] = next[ch]
            }
        }

        smoothedPreGain = currentPreGain
        ispEnvelopeGain = currentIspGain
        inputBuffer.position(inputBuffer.position() + frameCount * bytesPerInputFrame)
        out.flip()
        if (!loggedFirstBufferAfterConfigure) {
            loggedFirstBufferAfterConfigure = true
            var outPeak = 0f
            val inspect = out.duplicate().order(ByteOrder.nativeOrder())
            while (inspect.remaining() >= Float.SIZE_BYTES) {
                val v = abs(inspect.float)
                if (v > outPeak) outPeak = v
            }
            Timber.tag("HiFiAudio").d(
                "HiFiAudioProcessor firstBuffer: frames=%d, inEnc=%d, sr=%d, ch=%d, preGain=%.4f, ispGain=%.4f, outPeak=%.4f",
                frameCount,
                inputFormat.encoding,
                inputFormat.sampleRate,
                channelCount,
                currentPreGain,
                currentIspGain,
                outPeak
            )
        }
    }

    override fun getOutput(): ByteBuffer {
        val pending = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return pending
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun queueEndOfStream() {
        inputEnded = true
    }

    @Deprecated("Media3 AudioProcessor now prefers flush(StreamMetadata); kept for interface compatibility")
    @Suppress("DEPRECATION")
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        ispEnvelopeGain = 1.0f
        smoothedPreGain = computeTargetPreGain()
        if (historyPrev2.isNotEmpty()) {
            historyPrev2.fill(0f)
            historyPrev1.fill(0f)
        }
    }

    override fun reset() {
        flush()
        reusableBuffer = AudioProcessor.EMPTY_BUFFER
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
        baseIspGain = 1.0f
        smoothedPreGain = 1.0f
    }

    private fun computeTargetPreGain(): Float {
        if (inputFormat == AudioFormat.NOT_SET) return 1.0f
        val rawEqHeadroomDb = eqHeadroomDbProvider().let { if (it.isFinite()) it else 0f }
        val clampedEqCutDb = -rawEqHeadroomDb.coerceIn(0f, MAX_EQ_HEADROOM_DB)
        val eqGain = if (clampedEqCutDb == 0f) 1.0f else dbToLinear(clampedEqCutDb)
        return (baseIspGain * eqGain).coerceIn(0.15f, 1.0f)
    }

    private fun requiresAsyncResamplingTo48kFamily(sampleRate: Int): Boolean {
        if (sampleRate <= 0) return false
        return sampleRate % 48_000 != 0 && 48_000 % sampleRate != 0
    }

    private fun dbToLinear(db: Float): Float = 10.0.pow(db.toDouble() / 20.0).toFloat()

    private fun bytesPerSample(encoding: Int): Int = when (encoding) {
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN, C.ENCODING_PCM_FLOAT -> 4
        else -> throw IllegalArgumentException("Unsupported PCM encoding: $encoding")
    }

    private fun readFrameAsFloat(
        source: ByteBuffer,
        encoding: Int,
        channelCount: Int,
        dest: FloatArray
    ) {
        when (encoding) {
            C.ENCODING_PCM_FLOAT -> {
                for (ch in 0 until channelCount) {
                    val sample = source.float
                    dest[ch] = sanitizeInputSample(sample)
                }
            }
            C.ENCODING_PCM_16BIT -> {
                for (ch in 0 until channelCount) {
                    dest[ch] = source.short.toInt() * PCM_16_SCALE
                }
            }
            C.ENCODING_PCM_24BIT -> {
                for (ch in 0 until channelCount) {
                    val b0 = source.get().toInt() and 0xFF
                    val b1 = source.get().toInt() and 0xFF
                    val b2 = source.get().toInt() // sign-extended MSB
                    val sample24 = (b2 shl 16) or (b1 shl 8) or b0
                    dest[ch] = sample24 * PCM_24_SCALE
                }
            }
            C.ENCODING_PCM_24BIT_BIG_ENDIAN -> {
                for (ch in 0 until channelCount) {
                    val b2 = source.get().toInt() // sign-extended MSB
                    val b1 = source.get().toInt() and 0xFF
                    val b0 = source.get().toInt() and 0xFF
                    val sample24 = (b2 shl 16) or (b1 shl 8) or b0
                    dest[ch] = sample24 * PCM_24_SCALE
                }
            }
            C.ENCODING_PCM_32BIT -> {
                for (ch in 0 until channelCount) {
                    val b0 = source.get().toInt() and 0xFF
                    val b1 = source.get().toInt() and 0xFF
                    val b2 = source.get().toInt() and 0xFF
                    val b3 = source.get().toInt()
                    val sample32 = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
                    dest[ch] = sample32 * PCM_32_SCALE
                }
            }
            C.ENCODING_PCM_32BIT_BIG_ENDIAN -> {
                for (ch in 0 until channelCount) {
                    val b3 = source.get().toInt()
                    val b2 = source.get().toInt() and 0xFF
                    val b1 = source.get().toInt() and 0xFF
                    val b0 = source.get().toInt() and 0xFF
                    val sample32 = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
                    dest[ch] = sample32 * PCM_32_SCALE
                }
            }
            else -> {
                for (ch in 0 until channelCount) {
                    dest[ch] = 0f
                }
            }
        }
    }

    private fun sanitizeInputSample(sample: Float): Float {
        if (!sample.isFinite()) return 0f
        if (abs(sample) < DENORMAL_FLOOR) return 0f
        // Bound pathological corrupted float spikes (> +24 dBFS) before ISP/soft-knee math
        return sample.coerceIn(-16.0f, 16.0f)
    }

    private fun ensureChannelScratchCapacity(channelCount: Int) {
        if (historyPrev2.size < channelCount) {
            historyPrev2 = FloatArray(channelCount)
            historyPrev1 = FloatArray(channelCount)
            currentFrameSamples = FloatArray(channelCount)
            nextFrameSamples = FloatArray(channelCount)
        } else {
            historyPrev2.fill(0f)
            historyPrev1.fill(0f)
            currentFrameSamples.fill(0f)
            nextFrameSamples.fill(0f)
        }
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
}
