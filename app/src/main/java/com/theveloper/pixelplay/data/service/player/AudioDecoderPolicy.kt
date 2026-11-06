package com.theveloper.pixelplay.data.service.player

import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import java.util.Locale

@UnstableApi
internal object AudioDecoderPolicy {
    private const val AUDIO_MIDI = "audio/midi"
    private val extensionOnlyMimeTypes = setOf(
        MimeTypes.AUDIO_ALAC,
        MimeTypes.AUDIO_EXOPLAYER_MIDI,
        AUDIO_MIDI
    )

    fun shouldUseExtensionRenderer(mimeType: String): Boolean {
        return extensionOnlyMimeTypes.any { it.equals(mimeType, ignoreCase = true) }
    }

    fun <T> selectPlatformDecoders(
        mimeType: String,
        decoderInfos: List<T>,
        decoderNameSelector: (T) -> String = { info ->
            when (info) {
                is MediaCodecInfo -> info.name
                else -> info.toString()
            }
        }
    ): List<T> {
        if (shouldUseExtensionRenderer(mimeType)) {
            return emptyList()
        }
        if (decoderInfos.size <= 1) {
            return decoderInfos
        }
        // Samsung's proprietary software C2 audio decoders (c2.sec.*, omx.sec.*)
        // fail CCodecConfig when KEY_PCM_ENCODING = ENCODING_PCM_FLOAT (4) is requested
        // ("config failed => CORRUPTED"), leaving their internal C2 output at 16-bit integer
        // while CCodec reports pcm-encoding = 4 (float), which causes severe full-scale
        // digital distortion. Prefer AOSP/standard decoders ahead of Samsung vendor audio
        // codecs while retaining them as a fallback if no other decoder is registered.
        val (deprioritizedVendorDecoders, preferredDecoders) = decoderInfos.partition { info ->
            isBuggyFloatPcmVendorDecoder(decoderNameSelector(info))
        }
        return if (preferredDecoders.isNotEmpty()) {
            preferredDecoders + deprioritizedVendorDecoders
        } else {
            decoderInfos
        }
    }

    internal fun isBuggyFloatPcmVendorDecoder(decoderName: String): Boolean {
        val normalized = decoderName.lowercase(Locale.US)
        return normalized.startsWith("c2.sec.") ||
            normalized.startsWith("omx.sec.") ||
            normalized.contains(".sec.flac.") ||
            normalized.contains(".sec.mp3.") ||
            normalized.contains(".sec.amr") ||
            normalized.contains(".sec.aac.") ||
            normalized.contains(".sec.ape.") ||
            normalized.contains(".sec.ima.")
    }

    fun isLikelyHardwareDecoder(decoderName: String): Boolean {
        val normalized = decoderName.lowercase(Locale.US)
        val knownSoftwareTokens = listOf(
            "omx.google.",
            "c2.android.",
            "ffmpeg",
            "midi",
            "jsyn",
            "libgav1",
            "dav1d"
        )
        if (knownSoftwareTokens.any(normalized::contains)) return false

        return normalized.startsWith("omx.") ||
            normalized.startsWith("c2.") ||
            normalized.contains(".qti.") ||
            normalized.contains(".qcom.") ||
            normalized.contains(".sec.") ||
            normalized.contains(".mtk.") ||
            normalized.contains(".exynos.") ||
            normalized.contains(".dolby.")
    }
}
