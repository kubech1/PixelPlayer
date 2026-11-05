package com.theveloper.pixelplay.data.audio

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AudioOutputRepositoryTest {
    private class Source : AudioRouteSource {
        var current = QueriedAudioRoute(emptyList(), emptyList())
        var fail = false
        var watches = 0
        var unwatches = 0
        var queries = 0
        override fun query(): QueriedAudioRoute { queries++; if(fail) throw SecurityException("private diagnostic detail"); return current }
        override fun watch(onChange: () -> Unit) { watches++ }
        override fun unwatch() { unwatches++ }
    }
    private val device = AudioRouteDevice(7,8,"User-editable headset",listOf(48000),listOf(2),listOf(2))
    @Test fun queryFailureClearsStaleRoutesAndRecoversWithoutInventingBluetoothCodec() {
        val source=Source(); source.current=QueriedAudioRoute(listOf(device),listOf(device))
        val repo=AudioOutputRepository(source);repo.acquire()
        assertThat(repo.state.value.anticipated).containsExactly(device)
        val revision=repo.state.value.routeRevision
        repo.refresh();assertThat(repo.state.value.routeRevision).isEqualTo(revision)
        source.fail=true;repo.refresh()
        assertThat(repo.state.value.anticipated).isEmpty()
        assertThat(repo.state.value.queryFailure).isEqualTo("SecurityException")
        assertThat(repo.state.value.bluetoothCodec).isNull()
        source.fail=false;repo.refresh()
        assertThat(repo.state.value.anticipated).containsExactly(device)
        assertThat(repo.state.value.queryFailure).isNull()
    }
    @Test fun twoConsumersShareOneDeviceListenerAndReleaseIt() {
        val source=Source();val repo=AudioOutputRepository(source)
        repo.acquire();repo.acquire();assertThat(source.watches).isEqualTo(1)
        repo.release();assertThat(source.unwatches).isEqualTo(0)
        repo.release();repo.release();assertThat(source.unwatches).isEqualTo(1)
    }
    @Test fun auxiliarySinkCannotOverwriteMasterAndRouteChangeInvalidatesObservations() {
        val source=Source();source.current=QueriedAudioRoute(listOf(device),listOf(device))
        val repo=AudioOutputRepository(source);repo.acquire()
        val master=Any();val auxiliary=Any()
        val pcm=SinkConfiguration(2,48000,12,false)
        val offload=SinkConfiguration(9,44100,12,true)
        repo.sinkSession(master,10);repo.sinkConfigured(master,pcm);repo.selectSession(10)
        repo.sinkSession(auxiliary,11);repo.sinkConfigured(auxiliary,offload)
        assertThat(repo.state.value.sink).isEqualTo(pcm)
        repo.requested(float=true,offload=true)
        assertThat(repo.state.value.sink?.offload).isFalse()
        assertThat(repo.state.value.sleepingForOffload).isNull()
        repo.selectSession(11);assertThat(repo.state.value.sink).isEqualTo(offload)
        source.current=QueriedAudioRoute(emptyList(),emptyList());repo.refresh()
        assertThat(repo.state.value.sink).isNull()
        assertThat(repo.state.value.floatRequested).isTrue()
        repo.selectSession(11);assertThat(repo.state.value.sink).isNull()
    }
    @Test fun renamedDeviceDoesNotChangeRouteIdentityOrInferCapabilities() {
        val before=AudioOutputSnapshot().withRoute(QueriedAudioRoute(listOf(device),listOf(device)),null)
        val renamed=device.copy(label="Sony WH-1000XM5")
        val after=before.withRoute(QueriedAudioRoute(listOf(renamed),listOf(renamed)),null)
        assertThat(after.routeRevision).isEqualTo(before.routeRevision)
        assertThat(after.bluetoothCodec).isNull()
    }
    @Test fun effectRecoveryIsBoundedPerRouteAndSession() {
        val gate=EffectRecoveryGate()
        assertThat(gate.canAttempt(0)).isFalse()
        assertThat(gate.canAttempt(10)).isTrue()
        gate.failed(10);repeat(20){assertThat(gate.canAttempt(10)).isFalse()}
        assertThat(gate.canAttempt(11)).isTrue()
        gate.failed(11);assertThat(gate.canAttempt(10)).isFalse()
        gate.routeChanged(1);assertThat(gate.canAttempt(10)).isTrue()
        gate.failed(10);gate.routeChanged(1);assertThat(gate.canAttempt(10)).isFalse()
    }
}
