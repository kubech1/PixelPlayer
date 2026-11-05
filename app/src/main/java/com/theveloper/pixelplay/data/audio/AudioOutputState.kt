package com.theveloper.pixelplay.data.audio

/** Platform prediction and sink observations have different meanings; neither exposes a reliable public BT codec. */
data class AudioRouteDevice(
    val id: Int, val type: Int, val label: String,
    val sampleRates: List<Int> = emptyList(), val encodings: List<Int> = emptyList(), val channels: List<Int> = emptyList()
)
data class SinkConfiguration(val encoding: Int, val sampleRate: Int, val channelMask: Int, val offload: Boolean)
data class AudioOutputSnapshot(
    val available: List<AudioRouteDevice> = emptyList(),
    val anticipated: List<AudioRouteDevice> = emptyList(),
    val routeRevision: Long = 0,
    val queryFailure: String? = null,
    val sessionId: Int = 0,
    val sink: SinkConfiguration? = null,
    val sinkCapabilities: String? = null,
    val floatRequested: Boolean = false,
    val offloadRequested: Boolean = false,
    val sleepingForOffload: Boolean? = null,
    val bluetoothCodec: String? = null
)

data class QueriedAudioRoute(val available: List<AudioRouteDevice>, val anticipated: List<AudioRouteDevice>)

internal fun AudioOutputSnapshot.withRoute(route: QueriedAudioRoute?, failure: String?): AudioOutputSnapshot {
    fun List<AudioRouteDevice>.fingerprint() = sortedBy { it.id }.map { it.copy(label = "") }
    val changed = available.fingerprint() != route?.available.orEmpty().fingerprint() ||
        anticipated.fingerprint() != route?.anticipated.orEmpty().fingerprint() || queryFailure != failure
    return copy(available = route?.available.orEmpty(), anticipated = route?.anticipated.orEmpty(),
        routeRevision = routeRevision + if (changed) 1 else 0, queryFailure = failure,
        sink = if (changed) null else sink, sinkCapabilities = if (changed) null else sinkCapabilities,
        sleepingForOffload = if (changed) null else sleepingForOffload)
}

/** One failed initialization per route/session; a new route or session permits a new attempt. */
internal class EffectRecoveryGate {
    private var route: Long = 0
    private val failedSessions = mutableSetOf<Int>()
    fun routeChanged(revision: Long): Boolean {
        if (route == revision) return false
        route = revision
        failedSessions.clear()
        return true
    }
    fun canAttempt(session: Int): Boolean = session > 0 && session !in failedSessions
    fun failed(session: Int) { failedSessions += session }
}
