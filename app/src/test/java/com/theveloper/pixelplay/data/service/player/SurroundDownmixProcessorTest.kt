package com.theveloper.pixelplay.data.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SurroundDownmixProcessorTest {

    @Test
    fun queueInput_downmixes51Pcm16BitToStereo() {
        val processor = SurroundDownmixProcessor()
        val outputFormat = processor.configure(AudioFormat(48_000, 6, C.ENCODING_PCM_16BIT))

        processor.queueInput(
            shortBufferOf(
                1_000, 2_000, 3_000, 4_000, 5_000, 6_000
            )
        )

        assertThat(processor.isActive()).isTrue()
        assertThat(outputFormat.channelCount).isEqualTo(2)
        assertThat(readShorts(processor.getOutput()))
            .containsExactly(9_484, 11_191)
            .inOrder()
    }

    @Test
    fun queueInput_downmixes71FloatToStereo() {
        val processor = SurroundDownmixProcessor()
        val outputFormat = processor.configure(AudioFormat(48_000, 8, C.ENCODING_PCM_FLOAT))

        processor.queueInput(
            floatBufferOf(
                0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f
            )
        )

        assertThat(processor.isActive()).isTrue()
        assertThat(outputFormat.channelCount).isEqualTo(2)

        val output = readFloats(processor.getOutput())
        assertThat(output).hasSize(2)
        assertThat(output[0]).isWithin(1e-6f).of(1f.coerceAtMost(0.1f + 0.707f * 0.3f + 0.707f * 0.5f + 0.707f * 0.7f + 0.707f * 0.4f))
        assertThat(output[1]).isWithin(1e-6f).of(1f.coerceAtMost(0.2f + 0.707f * 0.3f + 0.707f * 0.6f + 0.707f * 0.8f + 0.707f * 0.4f))
    }

    @Test
    fun queueInput_downmixes51Pcm24BitAnd32BitToStereo() {
        val processor24 = SurroundDownmixProcessor()
        val format24 = processor24.configure(AudioFormat(48_000, 6, C.ENCODING_PCM_24BIT))
        assertThat(processor24.isActive()).isTrue()
        assertThat(format24.channelCount).isEqualTo(2)
        assertThat(format24.encoding).isEqualTo(C.ENCODING_PCM_24BIT)

        val buf24 = ByteBuffer.allocateDirect(6 * 3).order(ByteOrder.LITTLE_ENDIAN)
        val samples24 = intArrayOf(100_000, 200_000, 300_000, 400_000, 500_000, 600_000)
        for (s in samples24) {
            buf24.put((s and 0xFF).toByte())
            buf24.put(((s shr 8) and 0xFF).toByte())
            buf24.put(((s shr 16) and 0xFF).toByte())
        }
        buf24.flip()
        processor24.queueInput(buf24)
        val out24 = processor24.getOutput()
        assertThat(out24.remaining()).isEqualTo(2 * 3)

        val processor32 = SurroundDownmixProcessor()
        val format32 = processor32.configure(AudioFormat(48_000, 6, C.ENCODING_PCM_32BIT))
        assertThat(processor32.isActive()).isTrue()
        assertThat(format32.channelCount).isEqualTo(2)
        assertThat(format32.encoding).isEqualTo(C.ENCODING_PCM_32BIT)

        val buf32 = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples24) {
            buf32.putInt(s * 64)
        }
        buf32.flip()
        processor32.queueInput(buf32)
        val out32 = processor32.getOutput()
        assertThat(out32.remaining()).isEqualTo(2 * 4)
    }

    @Test
    fun queueInput_sanitizesNonFiniteFloatsDuringDownmix() {
        val processor = SurroundDownmixProcessor()
        processor.configure(AudioFormat(48_000, 6, C.ENCODING_PCM_FLOAT))
        processor.queueInput(
            floatBufferOf(
                Float.NaN, Float.POSITIVE_INFINITY, 0.2f, Float.NEGATIVE_INFINITY, 0.1f, 0.1f
            )
        )
        val output = readFloats(processor.getOutput())
        assertThat(output).hasSize(2)
        assertThat(output[0].isFinite()).isTrue()
        assertThat(output[1].isFinite()).isTrue()
    }

    private fun shortBufferOf(vararg samples: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        for (sample in samples) {
            buffer.putShort(sample.toShort())
        }
        buffer.flip()
        return buffer
    }

    private fun floatBufferOf(vararg samples: Float): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        for (sample in samples) {
            buffer.putFloat(sample)
        }
        buffer.flip()
        return buffer
    }

    private fun readShorts(buffer: ByteBuffer): List<Int> {
        val duplicate = buffer.duplicate().order(ByteOrder.nativeOrder())
        val samples = mutableListOf<Int>()
        while (duplicate.remaining() >= Short.SIZE_BYTES) {
            samples += duplicate.short.toInt()
        }
        return samples
    }

    private fun readFloats(buffer: ByteBuffer): List<Float> {
        val duplicate = buffer.duplicate().order(ByteOrder.nativeOrder())
        val samples = mutableListOf<Float>()
        while (duplicate.remaining() >= Float.SIZE_BYTES) {
            samples += duplicate.float
        }
        return samples
    }
}
