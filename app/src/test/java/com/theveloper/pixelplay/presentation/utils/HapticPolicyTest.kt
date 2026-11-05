package com.theveloper.pixelplay.presentation.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HapticPolicyTest {
    private val supported = EnvelopeLimits(20, 100, 16, 500)

    @Test
    fun `alternating events cannot bypass the control rate limit`() {
        val limiter = HapticEventLimiter()
        assertTrue(limiter.accept(AppHapticEvent.SELECTION, 100))
        assertTrue(limiter.accept(AppHapticEvent.ACTION, 125))
        assertFalse(limiter.accept(AppHapticEvent.LIMIT, 140))
        assertTrue(limiter.accept(AppHapticEvent.SELECTION, 150))
    }

    @Test
    fun `every event ends in silence and obeys the hardware limits`() {
        AppHapticEvent.entries.forEach { event ->
            val points = requireNotNull(HapticPolicy.envelope(event, 0.7f, supported))
            assertEquals(0f, points.last().intensity)
            assertTrue(points.all { it.intensity in 0f..1f && it.sharpness in 0f..1f && it.durationMs in 20..100 })
            assertTrue(points.sumOf { it.durationMs } <= 500)
        }
    }

    @Test
    fun `unsupported point count or total duration falls back instead of truncating a signal`() {
        assertNull(HapticPolicy.envelope(AppHapticEvent.CONFIRMATION, 1f, supported.copy(maxPoints = 2)))
        assertNull(HapticPolicy.envelope(AppHapticEvent.REJECTION, 1f, supported.copy(maxDurationMs = 50)))
        assertNull(HapticPolicy.envelope(AppHapticEvent.ACTION, 1f, supported.copy(minPointMs = 120)))
    }

    @Test
    fun `minimum transition duration and selected intensity are honored`() {
        val low = requireNotNull(HapticPolicy.envelope(AppHapticEvent.ACTION, HapticIntensity.LOW.scale, supported.copy(minPointMs = 40)))
        val high = requireNotNull(HapticPolicy.envelope(AppHapticEvent.ACTION, HapticIntensity.HIGH.scale, supported.copy(minPointMs = 40)))
        assertTrue(low.all { it.durationMs >= 40 })
        assertTrue(low.maxOf { it.intensity } < high.maxOf { it.intensity })
        assertEquals(HapticIntensity.MEDIUM, HapticIntensity.fromStorage("unrecognized"))
    }

    @Test
    fun `repetitive input is limited to one event per fifty milliseconds`() {
        val limiter = HapticEventLimiter()
        assertTrue(limiter.accept(AppHapticEvent.SELECTION, 100))
        assertFalse(limiter.accept(AppHapticEvent.SELECTION, 120))
        assertFalse(limiter.accept(AppHapticEvent.SELECTION, 149))
        assertTrue(limiter.accept(AppHapticEvent.SELECTION, 150))
        assertFalse(limiter.accept(AppHapticEvent.ACTION, 151))
        assertTrue(limiter.accept(AppHapticEvent.CONFIRMATION, 200))
    }
}
