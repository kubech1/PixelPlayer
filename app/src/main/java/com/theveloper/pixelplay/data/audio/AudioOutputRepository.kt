package com.theveloper.pixelplay.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.theveloper.pixelplay.data.service.player.HiFiCapabilityChecker

interface AudioRouteSource {
    fun query(): QueriedAudioRoute
    fun watch(onChange: () -> Unit)
    fun unwatch()
}

class AndroidAudioRouteSource @Inject constructor(@ApplicationContext context: Context) : AudioRouteSource {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private var callback: AudioDeviceCallback? = null
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null
    override fun query(): QueriedAudioRoute = QueriedAudioRoute(
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.toRouteDevice() },
        manager.getAudioDevicesForAttributes(attributes).map { it.toRouteDevice() }
    )
    override fun watch(onChange: () -> Unit) {
        if (callback != null) return
        val listener = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = onChange()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = onChange()
        }
        val playback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>) {
                // A routing change can happen between already connected receivers. Do not inspect or persist other apps' configs.
                if (callback != null) onChange()
            }
        }
        try {
            manager.registerAudioDeviceCallback(listener, Handler(Looper.getMainLooper()))
            callback = listener
            manager.registerAudioPlaybackCallback(playback, Handler(Looper.getMainLooper()))
            playbackCallback = playback
        } catch (failure: Exception) {
            runCatching { manager.unregisterAudioDeviceCallback(listener) }
            runCatching { manager.unregisterAudioPlaybackCallback(playback) }
            callback = null
            throw failure
        }
    }
    override fun unwatch() {
        callback?.let { runCatching { manager.unregisterAudioDeviceCallback(it) } }
        playbackCallback?.let { runCatching { manager.unregisterAudioPlaybackCallback(it) } }
        callback = null
        playbackCallback = null
    }
    private fun AudioDeviceInfo.toRouteDevice() = AudioRouteDevice(id, type, productName.toString(),
        sampleRates.sorted(), encodings.sorted(), channelCounts.sorted())
}

@Singleton
class AudioOutputRepository @Inject constructor(private val source: AudioRouteSource) {
    private val mutable = MutableStateFlow(AudioOutputSnapshot())
    val state = mutable.asStateFlow()
    private var owners = 0
    private data class SinkRecord(var session: Int = 0, var configuration: SinkConfiguration? = null, var capabilities: String? = null)
    private val sinks = mutableMapOf<Any, SinkRecord>()

    @Synchronized fun acquire() {
        owners++
        if (owners == 1) runCatching { source.watch(::refresh) }
        refresh()
    }
    @Synchronized fun release() {
        if (owners == 0) return
        owners--
        if (owners == 0) runCatching { source.unwatch() }
    }
    @Synchronized fun refresh() {
        val result = runCatching { source.query() }
        val previous = mutable.value.routeRevision
        mutable.update { it.withRoute(result.getOrNull(), result.exceptionOrNull()?.javaClass?.simpleName) }
        if (mutable.value.routeRevision != previous) {
            HiFiCapabilityChecker.invalidate()
            // A configuration created before the route change is not evidence for the new sink.
            sinks.values.forEach { it.configuration = null; it.capabilities = null }
        }
    }
    fun requested(float: Boolean, offload: Boolean) { mutable.update { it.copy(floatRequested = float, offloadRequested = offload) } }
    @Synchronized fun selectSession(session: Int, sleeping: Boolean? = null) {
        val record = sinks.values.firstOrNull { it.session == session && session != 0 }
        mutable.update { it.copy(sessionId = session, sink = record?.configuration,
            sinkCapabilities = record?.capabilities, sleepingForOffload = sleeping.takeIf { record?.configuration != null }) }
    }
    @Synchronized fun sinkSession(token: Any, session: Int) {
        sinks.getOrPut(token) { SinkRecord() }.session = session
        publish(token)
    }
    @Synchronized fun sinkConfigured(token: Any, configuration: SinkConfiguration?, routeRevision: Long = mutable.value.routeRevision) {
        if (routeRevision != mutable.value.routeRevision) return
        sinks.getOrPut(token) { SinkRecord() }.configuration = configuration
        publish(token)
    }
    @Synchronized fun sinkCapabilities(token: Any, capabilities: String?) {
        refresh()
        sinks.getOrPut(token) { SinkRecord() }.capabilities = capabilities
        publish(token)
    }
    @Synchronized fun removeSink(token: Any) { sinks.remove(token); selectSession(mutable.value.sessionId, mutable.value.sleepingForOffload) }
    private fun publish(token: Any) {
        val record = sinks[token] ?: return
        if (record.session != 0 && record.session == mutable.value.sessionId) {
            mutable.update { it.copy(sink = record.configuration, sinkCapabilities = record.capabilities,
                sleepingForOffload = it.sleepingForOffload.takeIf { record.configuration != null }) }
        }
    }
}
