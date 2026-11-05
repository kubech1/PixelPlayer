package com.theveloper.pixelplay.data.audio

import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.jupiter.api.Test

@androidx.annotation.OptIn(UnstableApi::class)
class ObservedAudioSinkTest {
    @Test fun `copied release configs remove old tracks without hiding the newly initialized track`() {
        val source = mockk<AudioRouteSource>(relaxed = true)
        every { source.query() } returns QueriedAudioRoute(emptyList(),emptyList())
        val outputs = AudioOutputRepository(source)
        val sink = mockk<AudioSink>(relaxed = true)
        val forwarded = mockk<AudioSink.Listener>(relaxed = true)
        val callbacks = slot<AudioSink.Listener>()
        every { sink.setListener(capture(callbacks)) } just Runs
        val observed = ObservedAudioSink(sink,outputs)
        observed.setListener(forwarded)
        callbacks.captured.onAudioSessionIdChanged(10)
        outputs.selectSession(10)
        callbacks.captured.onAudioTrackInitialized(config(44100))
        callbacks.captured.onAudioTrackInitialized(config(48000))
        callbacks.captured.onAudioTrackReleased(config(44100))
        assertThat(outputs.state.value.sink?.sampleRate).isEqualTo(48000)
        callbacks.captured.onAudioTrackReleased(config(48000))
        assertThat(outputs.state.value.sink).isNull()
        verify(exactly = 2) { forwarded.onAudioTrackReleased(any()) }
        verify(exactly = 2) { forwarded.onAudioTrackInitialized(any()) }
    }
    @Test fun `identical configurations remain observed until the last matching track is released`() {
        val source = mockk<AudioRouteSource>(relaxed = true)
        every { source.query() } returns QueriedAudioRoute(emptyList(),emptyList())
        val outputs = AudioOutputRepository(source)
        val sink = mockk<AudioSink>(relaxed = true)
        val callbacks = slot<AudioSink.Listener>()
        every { sink.setListener(capture(callbacks)) } just Runs
        val observed = ObservedAudioSink(sink,outputs)
        observed.setListener(mockk(relaxed = true))
        callbacks.captured.onAudioSessionIdChanged(10);outputs.selectSession(10)
        repeat(2) { callbacks.captured.onAudioTrackInitialized(config(48000)) }
        callbacks.captured.onAudioTrackReleased(config(48000))
        assertThat(outputs.state.value.sink).isNotNull()
        callbacks.captured.onAudioTrackReleased(config(48000))
        assertThat(outputs.state.value.sink).isNull()
    }
    @Test fun `late releases after reset cannot erase the next track`() {
        val source = mockk<AudioRouteSource>(relaxed = true)
        every { source.query() } returns QueriedAudioRoute(emptyList(),emptyList())
        val outputs = AudioOutputRepository(source)
        val sink = mockk<AudioSink>(relaxed = true)
        val callbacks = slot<AudioSink.Listener>()
        every { sink.setListener(capture(callbacks)) } just Runs
        val observed = ObservedAudioSink(sink,outputs)
        observed.setListener(mockk(relaxed = true))
        callbacks.captured.onAudioSessionIdChanged(10);outputs.selectSession(10)
        callbacks.captured.onAudioTrackInitialized(config(48000))
        observed.reset()
        assertThat(outputs.state.value.sink).isNull()
        callbacks.captured.onAudioSessionIdChanged(10)
        callbacks.captured.onAudioTrackInitialized(config(48000))
        callbacks.captured.onAudioTrackReleased(config(48000))
        assertThat(outputs.state.value.sink).isNotNull()
        observed.release()
        callbacks.captured.onAudioTrackInitialized(config(48000))
        callbacks.captured.onAudioTrackReleased(config(48000))
        assertThat(outputs.state.value.sink).isNull()
    }
    @Test fun `a release from the previous route cannot republish stale configuration`() {
        val source = mockk<AudioRouteSource>(relaxed = true)
        var route = QueriedAudioRoute(emptyList(),emptyList())
        every { source.query() } answers { route }
        val outputs = AudioOutputRepository(source)
        val sink = mockk<AudioSink>(relaxed = true)
        val callbacks = slot<AudioSink.Listener>()
        every { sink.setListener(capture(callbacks)) } just Runs
        ObservedAudioSink(sink,outputs).setListener(mockk(relaxed = true))
        callbacks.captured.onAudioSessionIdChanged(10);outputs.selectSession(10)
        callbacks.captured.onAudioTrackInitialized(config(44100))
        callbacks.captured.onAudioTrackInitialized(config(48000))
        val device = AudioRouteDevice(1,8,"Unknown receiver")
        route = QueriedAudioRoute(listOf(device),listOf(device));outputs.refresh()
        callbacks.captured.onAudioTrackReleased(config(44100))
        assertThat(outputs.state.value.sink).isNull()
        callbacks.captured.onAudioTrackInitialized(config(48000))
        callbacks.captured.onAudioTrackReleased(config(48000))
        assertThat(outputs.state.value.sink).isNotNull()
    }
    private fun config(rate: Int) = AudioSink.AudioTrackConfig(2,rate,12,false,false,4096)
}
