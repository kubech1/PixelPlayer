package com.theveloper.pixelplay.data.audio

import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Observe Media3 without replacing its listener, negotiation or fallback paths, while
 * guaranteeing that custom PCM [AudioProcessor]s (hi-res sample-rate cap, surround downmix,
 * and Hi-Fi float headroom/limiter) execute even when `DefaultAudioSink`'s `shouldUseFloatOutput`
 * branch bypasses `audioProcessorChain`.
 */
@UnstableApi
internal class ObservedAudioSink(
    sink: AudioSink,
    private val outputs: AudioOutputRepository,
    private val pcmProcessors: List<AudioProcessor> = emptyList()
) : ForwardingAudioSink(sink) {
    private val token = Any()
    private data class Track(val config: AudioSink.AudioTrackConfig, val route: Long, val epoch: Long)
    private val pendingTracks = mutableListOf<Track>()
    private var epoch = 0L
    @Volatile private var released = false

    private var activeProcessors: List<AudioProcessor> = emptyList()
    private var pendingDownstreamBuffer: ByteBuffer? = null
    private var pendingSourceBuffer: ByteBuffer? = null
    private var pendingSourceLimit: Int = 0
    private var pendingPresentationTimeUs: Long = 0L
    private var pendingAccessUnitCount: Int = 0

    override fun setListener(listener: AudioSink.Listener) {
        super.setListener(object : AudioSink.Listener by listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (!released) outputs.sinkSession(token, audioSessionId)
                listener.onAudioSessionIdChanged(audioSessionId)
            }
            override fun onAudioCapabilitiesChanged() {
                if (!released) outputs.sinkCapabilities(token, this@ObservedAudioSink.audioCapabilities?.toString())
                listener.onAudioCapabilitiesChanged()
            }
            override fun onAudioTrackInitialized(audioTrackConfig: AudioSink.AudioTrackConfig) {
                recordTrack(audioTrackConfig)
                listener.onAudioTrackInitialized(audioTrackConfig)
            }
            override fun onAudioTrackReleased(audioTrackConfig: AudioSink.AudioTrackConfig) {
                releaseTrack(audioTrackConfig)
                listener.onAudioTrackReleased(audioTrackConfig)
            }
        })
    }

    override fun supportsFormat(format: Format): Boolean {
        if (super.supportsFormat(format)) return true
        if (pcmProcessors.isEmpty() || !isLinearPcmFormat(format)) return false
        val transformed = previewProcessedFormat(format)
        return transformed != format && super.supportsFormat(transformed)
    }

    override fun getFormatSupport(format: Format): Int {
        val directSupport = super.getFormatSupport(format)
        if (directSupport != AudioSink.SINK_FORMAT_UNSUPPORTED ||
            pcmProcessors.isEmpty() ||
            !isLinearPcmFormat(format)
        ) {
            return directSupport
        }
        val transformed = previewProcessedFormat(format)
        return if (transformed != format) super.getFormatSupport(transformed) else directSupport
    }

    override fun configure(
        inputFormat: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?
    ) {
        clearPendingDownstream()
        if (pcmProcessors.isEmpty() || !isLinearPcmFormat(inputFormat)) {
            activeProcessors = emptyList()
            super.configure(inputFormat, specifiedBufferSize, outputChannels)
            return
        }

        var currentAudioFormat = AudioFormat(
            inputFormat.sampleRate,
            inputFormat.channelCount,
            inputFormat.pcmEncoding
        )
        val configuredActive = ArrayList<AudioProcessor>(pcmProcessors.size)
        for (processor in pcmProcessors) {
            val nextFormat = processor.configure(currentAudioFormat)
            @Suppress("DEPRECATION")
            processor.flush()
            if (processor.isActive()) {
                configuredActive.add(processor)
                currentAudioFormat = nextFormat
            }
        }
        activeProcessors = configuredActive

        if (configuredActive.isEmpty()) {
            super.configure(inputFormat, specifiedBufferSize, outputChannels)
            return
        }

        val scaledEncoderDelay = if (
            inputFormat.encoderDelay > 0 &&
            inputFormat.sampleRate > 0 &&
            currentAudioFormat.sampleRate != inputFormat.sampleRate
        ) {
            ((inputFormat.encoderDelay.toLong() * currentAudioFormat.sampleRate) / inputFormat.sampleRate).toInt()
        } else {
            inputFormat.encoderDelay
        }
        val scaledEncoderPadding = if (
            inputFormat.encoderPadding > 0 &&
            inputFormat.sampleRate > 0 &&
            currentAudioFormat.sampleRate != inputFormat.sampleRate
        ) {
            ((inputFormat.encoderPadding.toLong() * currentAudioFormat.sampleRate) / inputFormat.sampleRate).toInt()
        } else {
            inputFormat.encoderPadding
        }

        val downstreamFormat = inputFormat.buildUpon()
            .setSampleRate(currentAudioFormat.sampleRate)
            .setChannelCount(currentAudioFormat.channelCount)
            .setPcmEncoding(currentAudioFormat.encoding)
            .setEncoderDelay(scaledEncoderDelay)
            .setEncoderPadding(scaledEncoderPadding)
            .build()
        val downstreamOutputChannels = if (currentAudioFormat.channelCount != inputFormat.channelCount) {
            null
        } else {
            outputChannels
        }
        Timber.tag("HiFiAudio").d(
            "ObservedAudioSink.configure: in(enc=%d, sr=%d, ch=%d) -> out(enc=%d, sr=%d, ch=%d), activeProcessors=%s",
            inputFormat.pcmEncoding,
            inputFormat.sampleRate,
            inputFormat.channelCount,
            downstreamFormat.pcmEncoding,
            downstreamFormat.sampleRate,
            downstreamFormat.channelCount,
            configuredActive.map { it.javaClass.simpleName }
        )
        super.configure(downstreamFormat, specifiedBufferSize, downstreamOutputChannels)
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int
    ): Boolean {
        val processors = activeProcessors
        if (processors.isEmpty()) {
            return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        }

        val existingDownstream = pendingDownstreamBuffer
        if (existingDownstream != null) {
            val consumed = super.handleBuffer(
                existingDownstream,
                pendingPresentationTimeUs,
                pendingAccessUnitCount
            )
            if (!consumed) {
                return false
            }
            val matchedSource = pendingSourceBuffer === buffer
            val savedLimit = pendingSourceLimit
            clearPendingDownstream()
            if (matchedSource) {
                buffer.position(savedLimit)
                return true
            }
        }

        if (!buffer.hasRemaining()) {
            return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        }

        val sourceLimit = buffer.limit()
        var currentBuffer: ByteBuffer = buffer.duplicate().order(ByteOrder.nativeOrder())
        for (processor in processors) {
            processor.queueInput(currentBuffer)
            currentBuffer = processor.getOutput()
            if (!currentBuffer.hasRemaining()) {
                break
            }
        }

        if (!currentBuffer.hasRemaining()) {
            // Input bytes were stashed inside a stateful processor (e.g. partial hi-res frame).
            buffer.position(sourceLimit)
            return true
        }

        val downstreamBuffer = currentBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val consumed = super.handleBuffer(
            downstreamBuffer,
            presentationTimeUs,
            encodedAccessUnitCount
        )
        return if (consumed) {
            buffer.position(sourceLimit)
            true
        } else {
            pendingDownstreamBuffer = downstreamBuffer
            pendingSourceBuffer = buffer
            pendingSourceLimit = sourceLimit
            pendingPresentationTimeUs = presentationTimeUs
            pendingAccessUnitCount = encodedAccessUnitCount
            false
        }
    }

    override fun playToEndOfStream() {
        for (processor in activeProcessors) {
            processor.queueEndOfStream()
        }
        super.playToEndOfStream()
    }

    private fun isLinearPcmFormat(format: Format): Boolean {
        return format.sampleRate > 0 &&
            format.channelCount > 0 &&
            format.pcmEncoding != Format.NO_VALUE &&
            Util.isEncodingLinearPcm(format.pcmEncoding)
    }

    private fun previewProcessedFormat(format: Format): Format {
        var sampleRate = format.sampleRate
        var channelCount = format.channelCount
        var encoding = format.pcmEncoding
        for (processor in pcmProcessors) {
            when (processor) {
                is com.theveloper.pixelplay.data.service.player.HiResSampleRateCapAudioProcessor -> {
                    if (sampleRate > 192_000) {
                        val factor = kotlin.math.ceil(sampleRate.toDouble() / 192_000.0).toInt().coerceAtLeast(2)
                        sampleRate /= factor
                    }
                }
                is com.theveloper.pixelplay.data.service.player.SurroundDownmixProcessor -> {
                    if (channelCount == 6 || channelCount == 8) {
                        channelCount = 2
                    }
                }
                is com.theveloper.pixelplay.data.service.player.HiFiAudioProcessor -> {
                    encoding = androidx.media3.common.C.ENCODING_PCM_FLOAT
                }
            }
        }
        if (sampleRate == format.sampleRate &&
            channelCount == format.channelCount &&
            encoding == format.pcmEncoding
        ) {
            return format
        }
        return format.buildUpon()
            .setSampleRate(sampleRate)
            .setChannelCount(channelCount)
            .setPcmEncoding(encoding)
            .build()
    }

    private fun clearPendingDownstream() {
        pendingDownstreamBuffer = null
        pendingSourceBuffer = null
        pendingSourceLimit = 0
        pendingPresentationTimeUs = 0L
        pendingAccessUnitCount = 0
    }

    @Synchronized private fun recordTrack(config: AudioSink.AudioTrackConfig) {
        if (released) return
        val route = outputs.state.value.routeRevision
        // Native release callbacks can arrive after the next initialization. Bound the pending metadata.
        if (pendingTracks.size == 32) pendingTracks.removeAt(0)
        pendingTracks += Track(config, route, epoch)
        outputs.sinkConfigured(token, config.snapshot(), route)
    }

    @Synchronized private fun releaseTrack(config: AudioSink.AudioTrackConfig) {
        if (released) return
        // Configs are copied by Media3: compare values, retaining older entries across reset for late callbacks.
        val index = pendingTracks.indexOfFirst { sameConfiguration(it.config, config) }
        if (index >= 0) pendingTracks.removeAt(index)
        val route = outputs.state.value.routeRevision
        val latest = pendingTracks.lastOrNull { it.epoch == epoch && it.route == route }
        outputs.sinkConfigured(token, latest?.config?.snapshot(), route)
    }

    private fun AudioSink.AudioTrackConfig.snapshot() = SinkConfiguration(encoding, sampleRate, channelConfig, offload)
    private fun sameConfiguration(first: AudioSink.AudioTrackConfig, second: AudioSink.AudioTrackConfig): Boolean =
        first.encoding == second.encoding && first.sampleRate == second.sampleRate &&
            first.channelConfig == second.channelConfig && first.tunneling == second.tunneling &&
            first.offload == second.offload && first.bufferSize == second.bufferSize

    @Synchronized private fun invalidateTracks(finalRelease: Boolean) {
        epoch++
        released = finalRelease
        if (finalRelease) pendingTracks.clear()
        outputs.removeSink(token)
    }

    override fun flush() {
        clearPendingDownstream()
        for (processor in activeProcessors) {
            @Suppress("DEPRECATION")
            processor.flush()
        }
        super.flush()
    }

    override fun reset() {
        clearPendingDownstream()
        for (processor in pcmProcessors) {
            processor.reset()
        }
        activeProcessors = emptyList()
        invalidateTracks(finalRelease = false)
        super.reset()
    }

    override fun release() {
        clearPendingDownstream()
        for (processor in pcmProcessors) {
            processor.reset()
        }
        activeProcessors = emptyList()
        invalidateTracks(finalRelease = true)
        super.release()
    }
}
