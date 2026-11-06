package com.theveloper.pixelplay.presentation.utils

import android.os.VibrationEffect
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
    fun `every event produces a valid primitive composition for SM-S921B linear motor without 300ms THUD`() {
        val supportedPrimitives = setOf(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            VibrationEffect.Composition.PRIMITIVE_SPIN
        )
        AppHapticEvent.entries.forEach { event ->
            val steps = requireNotNull(HapticPolicy.composition(event, HapticIntensity.HIGH.scale))
            assertTrue(steps.isNotEmpty() && steps.size <= 10)
            assertTrue(steps.all { it.primitiveId in supportedPrimitives })
            assertTrue(steps.none { it.primitiveId == VibrationEffect.Composition.PRIMITIVE_THUD })
            assertTrue(steps.all { it.scale in 0.05f..1f && it.delayMs in 0..1000 })
        }
    }

    @Test
    fun `toggle on and toggle off have distinct tactile trajectories`() {
        val toggleOn = requireNotNull(HapticPolicy.composition(AppHapticEvent.TOGGLE_ON, 1f))
        val toggleOff = requireNotNull(HapticPolicy.composition(AppHapticEvent.TOGGLE_OFF, 1f))
        assertTrue(toggleOn.first().scale < toggleOn.last().scale)
        assertTrue(toggleOff.first().scale > toggleOff.last().scale)
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
