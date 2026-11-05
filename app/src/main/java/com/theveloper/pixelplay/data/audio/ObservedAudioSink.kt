package com.theveloper.pixelplay.data.audio

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink

/** Observe Media3 without replacing its listener, negotiation or fallback paths. */
@UnstableApi
internal class ObservedAudioSink(sink: AudioSink, private val outputs: AudioOutputRepository) : ForwardingAudioSink(sink) {
    private val token = Any()
    private data class Track(val config: AudioSink.AudioTrackConfig, val route: Long, val epoch: Long)
    private val pendingTracks = mutableListOf<Track>()
    private var epoch = 0L
    @Volatile private var released = false

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
    override fun reset() { invalidateTracks(finalRelease = false); super.reset() }
    override fun release() { invalidateTracks(finalRelease = true); super.release() }
}
