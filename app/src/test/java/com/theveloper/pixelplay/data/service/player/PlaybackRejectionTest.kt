package com.theveloper.pixelplay.data.service.player

import androidx.media3.exoplayer.ExoPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackRejectionTest {
    @Test
    fun `rejection pauses both players without replacing the volume outside a transition`() {
        // Avoid creating Android players: exercise the real rejection and cancellation methods
        // against the same player interfaces used by the service.
        val engine = spyk(mockk<DualPlayerEngine>(relaxed = true))
        val master = mockk<ExoPlayer>(relaxed = true)
        val auxiliary = mockk<ExoPlayer>(relaxed = true)
        every { auxiliary.mediaItemCount } returns 1
        DualPlayerEngine::class.java.getDeclaredField("playerA").apply {
            isAccessible = true
            set(engine, master)
        }
        DualPlayerEngine::class.java.getDeclaredField("playerB").apply {
            isAccessible = true
            set(engine, auxiliary)
        }
        engine.pauseAfterPlaybackRejection()

        verify(exactly = 1) { master.pause() }
        verify(exactly = 1) { auxiliary.pause() }
        verify(exactly = 1) { auxiliary.stop() }
        verify(exactly = 1) { auxiliary.clearMediaItems() }
        verify(exactly = 0) { master.volume = any() }
        verify(exactly = 0) { auxiliary.volume = any() }
    }
}
