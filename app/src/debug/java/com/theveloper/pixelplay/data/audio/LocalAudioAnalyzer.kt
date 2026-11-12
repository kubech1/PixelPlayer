package com.theveloper.pixelplay.data.audio

import android.content.ContentResolver
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** On-demand decoder and compact feature extractor. Does not retain decoded PCM or mutate playback. */
class LocalAudioAnalyzer(private val resolver: ContentResolver) {
    fun analyze(uri: Uri): AudioSimilarityProfile {
        return analyzeSource { extractor ->
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                extractor.setDataSource(descriptor.fileDescriptor)
            } ?: throw AudioAnalysisException("Cannot open audio URI")
        }
    }

    /** File entry point exists for generated diagnostics fixtures; product callers should use content URIs. */
    fun analyze(file: File): AudioSimilarityProfile = analyzeSource { it.setDataSource(file.absolutePath) }

    private fun analyzeSource(setDataSource: (MediaExtractor) -> Unit): AudioSimilarityProfile {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            setDataSource(extractor)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw AudioAnalysisException("No audio track")
            extractor.selectTrack(track)
            val inputFormat = extractor.getTrackFormat(track)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw AudioAnalysisException("Missing audio MIME type")
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()
            val durationMs = inputFormat.getLongOrNull(MediaFormat.KEY_DURATION)?.div(1000L)?.coerceAtLeast(1L) ?: 1L
            val accumulator = Accumulator(durationMs)
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var outputSampleRate = inputFormat.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
            var outputChannels = inputFormat.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 0
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            var decodedFrameIndex = 0L
            var nextOutputPosition = 0.0
            var previousMono = 0f
            var hasPreviousMono = false
            var resamplingStep = 0.0
            var iterations = 0
            var idlePolls = 0
            while (!outputEnded && iterations++ < MAX_CODEC_ITERATIONS) {
                var madeProgress = false
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val buffer = codec.getInputBuffer(inputIndex) ?: throw AudioAnalysisException("Decoder input unavailable")
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                            madeProgress = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime.coerceAtLeast(0L), 0)
                            extractor.advance()
                            madeProgress = true
                        }
                    }
                }
                val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = codec.outputFormat
                        outputSampleRate = format.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: outputSampleRate
                        outputChannels = format.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannels
                        pcmEncoding = format.getIntegerOrNull(MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
                        madeProgress = true
                    }
                    outputIndex >= 0 -> {
                        val output = codec.getOutputBuffer(outputIndex)
                        if (output != null && info.size > 0) {
                            if (outputSampleRate <= 0 || outputChannels <= 0) throw AudioAnalysisException("Invalid decoded PCM format")
                            output.position(info.offset); output.limit(info.offset + info.size)
                            output.order(ByteOrder.LITTLE_ENDIAN)
                            val bytesPerSample = bytesPerSample(pcmEncoding)
                            val frameBytes = bytesPerSample * outputChannels
                            val frames = output.remaining() / frameBytes
                            if (resamplingStep == 0.0) resamplingStep = outputSampleRate.toDouble() / ANALYSIS_RATE
                            repeat(frames) { frame ->
                                var mono = 0f
                                repeat(outputChannels) { channel ->
                                    mono += readPcm(output, frame * frameBytes + channel * bytesPerSample, pcmEncoding)
                                }
                                mono = (mono / outputChannels).coerceIn(-1f, 1f)
                                val currentPosition = decodedFrameIndex.toDouble()
                                if (!hasPreviousMono) {
                                    accumulator.accept(mono)
                                    nextOutputPosition = resamplingStep
                                    hasPreviousMono = true
                                } else {
                                    val previousPosition = currentPosition - 1.0
                                    while (nextOutputPosition <= currentPosition) {
                                        val fraction = (nextOutputPosition - previousPosition).coerceIn(0.0, 1.0).toFloat()
                                        accumulator.accept(previousMono + (mono - previousMono) * fraction)
                                        nextOutputPosition += resamplingStep
                                    }
                                }
                                previousMono = mono
                                decodedFrameIndex++
                            }
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                        madeProgress = true
                    }
                }
                if (madeProgress) idlePolls = 0 else if (++idlePolls >= MAX_IDLE_POLLS) {
                    throw AudioAnalysisException("Decoder made no progress")
                }
            }
            if (!outputEnded) throw AudioAnalysisException("Decoder exceeded safety limit")
            return accumulator.finish(durationMs)
        } catch (e: AudioAnalysisException) {
            throw e
        } catch (e: Exception) {
            throw AudioAnalysisException("Audio decoding failed (${e.javaClass.simpleName})", e)
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            try { codec?.release() } catch (_: Exception) { }
            extractor.release()
        }
    }

    private inner class Accumulator(private val durationMs: Long) {
        private val energySums = DoubleArray(AudioSimilarity.ENERGY_BINS)
        private val energyCounts = IntArray(AudioSimilarity.ENERGY_BINS)
        private val bandSums = DoubleArray(AudioSimilarity.TIMBRE_BANDS)
        private val energyWindowSampleCount = WINDOW_SIZE
        private var energySumSquares = 0.0
        private var energySamples = 0
        private val fftWindow = FloatArray(FFT_SIZE)
        private val fftReal = FloatArray(FFT_SIZE)
        private val fftImag = FloatArray(FFT_SIZE)
        private val currentBands = FloatArray(AudioSimilarity.TIMBRE_BANDS)
        private val onset = ArrayList<Float>()
        private var previousBands = FloatArray(AudioSimilarity.TIMBRE_BANDS)
        private var fftFill = 0
        private var samplesSinceSpectrum = 0
        private var acceptedSamples = 0L
        private var sampleIndex = 0L

        fun accept(sample: Float) {
            acceptedSamples++
            energySumSquares += sample.toDouble() * sample
            energySamples++
            sampleIndex++
            if (energySamples == energyWindowSampleCount) finishEnergyWindow()

            fftWindow[fftFill++] = sample
            samplesSinceSpectrum++
            if (fftFill == FFT_SIZE) {
                finishSpectrum()
                System.arraycopy(fftWindow, HOP_SIZE, fftWindow, 0, FFT_SIZE - HOP_SIZE)
                fftFill = FFT_SIZE - HOP_SIZE
                samplesSinceSpectrum = 0
            }
        }

        private fun finishEnergyWindow() {
            val rms = sqrt(energySumSquares / energySamples).toFloat()
            val timeMs = sampleIndex * 1000L / ANALYSIS_RATE
            val energyBin = ((timeMs * AudioSimilarity.ENERGY_BINS) / durationMs).toInt()
                .coerceIn(0, AudioSimilarity.ENERGY_BINS - 1)
            energySums[energyBin] += rms
            energyCounts[energyBin]++
            energySumSquares = 0.0
            energySamples = 0
        }

        fun finish(durationMs: Long): AudioSimilarityProfile {
            if (energySamples > 0) finishEnergyWindow()
            if (acceptedSamples == 0L) throw AudioAnalysisException("Decoded stream is empty")
            if (samplesSinceSpectrum > 0 || onset.isEmpty()) {
                java.util.Arrays.fill(fftWindow, fftFill, FFT_SIZE, 0f)
                finishSpectrum()
            }
            val raw = FloatArray(AudioSimilarity.ENERGY_BINS) { i ->
                if (energyCounts[i] == 0) 0f else (energySums[i] / energyCounts[i]).toFloat()
            }
            val logEnergy = raw.map { 20f * log10(max(it, 1e-5f)) }
            val median = logEnergy.sorted()[logEnergy.size / 2]
            val spread = max(1f, logEnergy.map { abs(it - median) }.sorted()[logEnergy.size / 2] * 1.4826f)
            val normalizedEnergy = FloatArray(raw.size) { i -> ((logEnergy[i] - median) / (3f * spread)).coerceIn(-1f, 1f) }
            val bands = FloatArray(AudioSimilarity.TIMBRE_BANDS) { i -> (bandSums[i] / onset.size.coerceAtLeast(1)).toFloat() }
            val norm = sqrt(bands.sumOf { (it * it).toDouble() }).toFloat()
            if (norm > 0f) for (i in bands.indices) bands[i] /= norm
            val tempo = estimateTempo(onset)
            return AudioSimilarityProfile(AudioSimilarity.PROFILE_VERSION, durationMs, normalizedEnergy, bands, tempo.first, tempo.second)
        }

        private fun finishSpectrum() {
            java.util.Arrays.fill(fftReal, 0f)
            java.util.Arrays.fill(fftImag, 0f)
            for (i in 0 until FFT_SIZE) {
                fftReal[i] = fftWindow[i] * (0.5 - 0.5 * cos(2.0 * PI * i / (FFT_SIZE - 1))).toFloat()
            }
            fft(fftReal, fftImag)
            java.util.Arrays.fill(currentBands, 0f)
            for (bin in 1 until FFT_SIZE / 2) {
                val hz = bin.toFloat() * ANALYSIS_RATE / FFT_SIZE
                val band = when {
                    hz < 80f -> 0
                    hz < 160f -> 1
                    hz < 315f -> 2
                    hz < 630f -> 3
                    hz < 1250f -> 4
                    hz < 2500f -> 5
                    hz < 5000f -> 6
                    else -> 7
                }
                currentBands[band] += sqrt(fftReal[bin] * fftReal[bin] + fftImag[bin] * fftImag[bin])
            }
            val total = currentBands.sum().coerceAtLeast(1e-12f)
            var positiveFlux = 0f
            for (i in currentBands.indices) {
                currentBands[i] /= total
                bandSums[i] += currentBands[i]
                positiveFlux += max(0f, currentBands[i] - previousBands[i])
            }
            onset += positiveFlux
            for (i in previousBands.indices) previousBands[i] = currentBands[i]
        }
    }

    private fun estimateTempo(flux: List<Float>): Pair<Float?, Float> {
        if (flux.size < 12) return null to 0f
        val mean = flux.average().toFloat()
        val centered = flux.map { it - mean }
        val hopMs = HOP_SIZE * 1000f / ANALYSIS_RATE
        val minLag = max(1, kotlin.math.ceil(60_000f / MAX_BPM / hopMs).toInt())
        val maxLag = min(centered.size / 2, (60_000f / MIN_BPM / hopMs).toInt())
        if (maxLag <= minLag) return null to 0f
        var bestLag = -1; var best = 0f; var second = 0f
        for (lag in minLag..maxLag) {
            var corr = 0f; var norm = 0f
            for (i in lag until centered.size) { corr += centered[i] * centered[i - lag]; norm += centered[i] * centered[i] }
            val score = if (norm > 1e-9f) corr / norm else 0f
            if (score > best) { second = best; best = score; bestLag = lag } else if (score > second) second = score
        }
        if (bestLag < 0 || best < 0.08f) return null to 0f
        val bpm = (60_000f / (bestLag * hopMs)).coerceIn(MIN_BPM, MAX_BPM)
        val confidence = ((best - max(0f, second)) / max(best, 0.001f)).coerceIn(0f, 1f)
        return bpm to confidence
    }
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val tr = real[i]; real[i] = real[j]; real[j] = tr; val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wrStep = cos(angle); val wiStep = sin(angle)
            for (start in 0 until n step len) {
                var wr = 1.0; var wi = 0.0
                for (k in 0 until len / 2) {
                    val even = start + k; val odd = even + len / 2
                    val tr = (wr * real[odd] - wi * imag[odd]).toFloat()
                    val ti = (wr * imag[odd] + wi * real[odd]).toFloat()
                    real[odd] = real[even] - tr; imag[odd] = imag[even] - ti
                    real[even] += tr; imag[even] += ti
                    val nextWr = wr * wrStep - wi * wiStep; wi = wr * wiStep + wi * wrStep; wr = nextWr
                }
            }
            len = len shl 1
        }
    }
    private fun readPcm(buffer: java.nio.ByteBuffer, offset: Int, encoding: Int): Float = when (encoding) {
        AudioFormat.ENCODING_PCM_FLOAT -> buffer.getFloat(offset).coerceIn(-1f, 1f)
        AudioFormat.ENCODING_PCM_8BIT -> ((buffer.get(offset).toInt() and 0xff) - 128) / 128f
        AudioFormat.ENCODING_PCM_16BIT -> buffer.getShort(offset) / 32768f
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
            val value = (buffer.get(offset).toInt() and 0xff) or ((buffer.get(offset + 1).toInt() and 0xff) shl 8) or (buffer.get(offset + 2).toInt() shl 16)
            (value / 8388608f).coerceIn(-1f, 1f)
        }
        AudioFormat.ENCODING_PCM_32BIT -> buffer.getInt(offset) / 2147483648f
        else -> throw AudioAnalysisException("Unsupported decoded PCM encoding")
    }
    private fun bytesPerSample(encoding: Int): Int = when (encoding) {
        AudioFormat.ENCODING_PCM_8BIT -> 1
        AudioFormat.ENCODING_PCM_16BIT -> 2
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
        AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
        else -> throw AudioAnalysisException("Unsupported decoded PCM encoding")
    }
    private fun MediaFormat.getIntegerOrNull(key: String): Int? = try { getInteger(key) } catch (_: Exception) { null }
    private fun MediaFormat.getLongOrNull(key: String): Long? = try { getLong(key) } catch (_: Exception) { null }

    companion object {
        private const val ANALYSIS_RATE = 11_025
        private const val FFT_SIZE = 1024
        private const val HOP_SIZE = FFT_SIZE / 2
        private const val WINDOW_SIZE = 5_512 // 500 ms
        private const val MIN_BPM = 60f
        private const val MAX_BPM = 180f
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val MAX_IDLE_POLLS = 500
        private const val MAX_CODEC_ITERATIONS = 2_000_000
    }
}

class AudioAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)
