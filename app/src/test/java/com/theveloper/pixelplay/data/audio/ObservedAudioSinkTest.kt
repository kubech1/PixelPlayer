package com.theveloper.pixelplay.data.audio

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import com.google.common.truth.Truth.assertThat
import com.theveloper.pixelplay.data.service.player.HiFiAudioProcessor
import com.theveloper.pixelplay.data.service.player.HiResSampleRateCapAudioProcessor
import com.theveloper.pixelplay.data.service.player.SurroundDownmixProcessor
import io.mockk.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    @Test fun `pcmProcessors pipeline transforms 384kHz 6ch 24bit format into 192kHz stereo float and scales encoder delay`() {
        val source = mockk<AudioRouteSource>(relaxed = true)
        every { source.query() } returns QueriedAudioRoute(emptyList(), emptyList())
        val outputs = AudioOutputRepository(source)
        val sink = mockk<AudioSink>(relaxed = true)
        val configuredFormat = slot<Format>()
        every { sink.configure(capture(configuredFormat), any(), any()) } just Runs
        every { sink.supportsFormat(any()) } answers {
            val f = firstArg<Format>()
            f.sampleRate <= 192_000 && f.channelCount == 2 && f.pcmEncoding == C.ENCODING_PCM_FLOAT
        }
        every { sink.getFormatSupport(any()) } answers {
            val f = firstArg<Format>()
            if (f.sampleRate <= 192_000 && f.channelCount == 2 && f.pcmEncoding == C.ENCODING_PCM_FLOAT) {
                AudioSink.SINK_FORMAT_SUPPORTED_WITH_TRANSCODING
            } else {
                AudioSink.SINK_FORMAT_UNSUPPORTED
            }
        }

        val processors: List<AudioProcessor> = listOf(
            HiResSampleRateCapAudioProcessor(),
            SurroundDownmixProcessor(),
            HiFiAudioProcessor()
        )
        val observed = ObservedAudioSink(sink, outputs, processors)

        val inputFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_RAW)
            .setSampleRate(384_000)
            .setChannelCount(6)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .setEncoderDelay(1_024)
            .setEncoderPadding(512)
            .build()

        assertThat(observed.supportsFormat(inputFormat)).isTrue()
        assertThat(observed.getFormatSupport(inputFormat))
            .isEqualTo(AudioSink.SINK_FORMAT_SUPPORTED_WITH_TRANSCODING)

        observed.configure(inputFormat, 0, null)

        assertThat(configuredFormat.captured.sampleRate).isEqualTo(192_000)
        assertThat(configuredFormat.captured.channelCount).isEqualTo(2)
        assertThat(configuredFormat.captured.pcmEncoding).isEqualTo(C.ENCODING_PCM_FLOAT)
        assertThat(configuredFormat.captured.encoderDelay).isEqualTo(512)
        assertThat(configuredFormat.captured.encoderPadding).isEqualTo(256)
    }

    @Test fun `pcmProcessors pipeline preserves pending downstream buffer across handleBuffer retries`() {
        val source = mockk<AudioRouteSource>(relaxed = true)
        every { source.query() } returns QueriedAudioRoute(emptyList(), emptyList())
        val outputs = AudioOutputRepository(source)
        val sink = mockk<AudioSink>(relaxed = true)
        val receivedBuffers = mutableListOf<ByteBuffer>()
        var acceptDownstream = false
        every { sink.handleBuffer(any(), any(), any()) } answers {
            val buf = firstArg<ByteBuffer>()
            receivedBuffers += buf
            if (acceptDownstream) {
                buf.position(buf.limit())
                true
            } else {
                false
            }
        }

        val observed = ObservedAudioSink(
            sink = sink,
            outputs = outputs,
            pcmProcessors = listOf(HiFiAudioProcessor())
        )

        val inputFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_RAW)
            .setSampleRate(44_100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()
        observed.configure(inputFormat, 0, null)

        val raw16 = ByteBuffer.allocateDirect(4 * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        raw16.putShort(16000)
        raw16.putShort(-16000)
        raw16.putShort(8000)
        raw16.putShort(-8000)
        raw16.flip()

        // First call: processor consumes raw16 and creates downstream float buffer, but sink returns false
        val firstAccepted = observed.handleBuffer(raw16, 123_456L, 1)
        assertThat(firstAccepted).isFalse()
        assertThat(receivedBuffers).hasSize(1)

        // Second call (retry from renderer with same input buffer): sink now accepts the exact same pending buffer instance
        acceptDownstream = true
        val secondAccepted = observed.handleBuffer(raw16, 123_456L, 1)
        assertThat(secondAccepted).isTrue()
        assertThat(receivedBuffers).hasSize(2)
        assertThat(receivedBuffers[1]).isSameInstanceAs(receivedBuffers[0])
        assertThat(receivedBuffers[1].order()).isEqualTo(ByteOrder.LITTLE_ENDIAN)
    }

    private fun config(rate: Int) = AudioSink.AudioTrackConfig(2,rate,12,false,false,4096)
}
