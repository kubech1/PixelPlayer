package com.theveloper.pixelplay.data.service.player

import androidx.media3.common.MimeTypes
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AudioDecoderPolicyTest {

    @Test
    fun selectPlatformDecoders_routesAlacToExtensionRenderer() {
        val decoders = listOf("c2.qti.alac.decoder", "c2.android.alac.decoder")

        val selected = AudioDecoderPolicy.selectPlatformDecoders(MimeTypes.AUDIO_ALAC, decoders)

        assertThat(selected).isEmpty()
    }

    @Test
    fun selectPlatformDecoders_routesMidiToExtensionRenderer() {
        val decoders = listOf("platform-midi-decoder")

        val selected = AudioDecoderPolicy.selectPlatformDecoders(MimeTypes.AUDIO_EXOPLAYER_MIDI, decoders)

        assertThat(selected).isEmpty()
    }

    @Test
    fun selectPlatformDecoders_preservesMedia3OrderForCoreFormats() {
        val decoders = listOf("c2.android.aac.decoder", "c2.qti.aac.decoder")

        val selected = AudioDecoderPolicy.selectPlatformDecoders(MimeTypes.AUDIO_AAC, decoders)

        assertThat(selected).containsExactlyElementsIn(decoders).inOrder()
    }

    @Test
    fun selectPlatformDecoders_deprioritizesSamsungVendorCodecsBehindAosp() {
        val flacDecoders = listOf("c2.sec.flac.decoder", "c2.android.flac.decoder")
        val mp3Decoders = listOf("c2.sec.mp3.decoder.mpeg", "c2.android.mp3.decoder")

        assertThat(AudioDecoderPolicy.selectPlatformDecoders(MimeTypes.AUDIO_FLAC, flacDecoders))
            .containsExactly("c2.android.flac.decoder", "c2.sec.flac.decoder")
            .inOrder()
        assertThat(AudioDecoderPolicy.selectPlatformDecoders(MimeTypes.AUDIO_MPEG, mp3Decoders))
            .containsExactly("c2.android.mp3.decoder", "c2.sec.mp3.decoder.mpeg")
            .inOrder()
    }

    @Test
    fun selectPlatformDecoders_retainsSamsungVendorCodecWhenOnlyOption() {
        val apeDecoders = listOf("c2.sec.ape.decoder")

        assertThat(AudioDecoderPolicy.selectPlatformDecoders("audio/x-ape", apeDecoders))
            .containsExactly("c2.sec.ape.decoder")
    }

    @Test
    fun isLikelyHardwareDecoder_marksSoftwareRenderersAsSoftware() {
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("OMX.google.aac.decoder")).isFalse()
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("c2.android.aac.decoder")).isFalse()
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("ffmpegAudioRenderer")).isFalse()
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("MidiRenderer(JSyn)")).isFalse()
    }

    @Test
    fun isLikelyHardwareDecoder_marksVendorCodecsAsHardware() {
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("c2.qti.aac.decoder")).isTrue()
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("c2.qti.flac.decoder")).isTrue()
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("OMX.qcom.audio.decoder.aac")).isTrue()
        assertThat(AudioDecoderPolicy.isLikelyHardwareDecoder("c2.sec.aac.decoder")).isTrue()
    }
}
