package com.theveloper.pixelplay.presentation.utils

import android.os.VibrationEffect

enum class HapticIntensity(val scale: Float) {
    LOW(0.60f), MEDIUM(0.88f), HIGH(1f);

    companion object {
        fun fromStorage(value: String?): HapticIntensity = entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

enum class AppHapticEvent {
    SELECTION,
    NAVIGATION_STEP,
    ACTION,
    TOGGLE_ON,
    TOGGLE_OFF,
    CONFIRMATION,
    REJECTION,
    DRAG_START,
    DRAG_END,
    LIMIT
}

internal data class HapticPoint(val intensity: Float, val sharpness: Float, val durationMs: Long)
internal data class EnvelopeLimits(val minPointMs: Long, val maxPointMs: Long, val maxPoints: Int, val maxDurationMs: Long)
internal data class PrimitiveStep(val primitiveId: Int, val scale: Float, val delayMs: Int = 0)

/**
 * Pure policy: validates envelopes and multi-primitive compositions before any hardware request is made.
 * Tuned for linear actuators such as the SM-S921B (`MOTOR_LINEAR_INDEX = 5`, `SEM_SUPPORTED_VIBRATION_TYPE_D`)
 * where `CLICK`, `TICK`, and `LOW_TICK` have 20ms HAL durations and `THUD` is a long 300ms buzz.
 */
internal object HapticPolicy {
    fun envelope(event: AppHapticEvent, scale: Float, limits: EnvelopeLimits): List<HapticPoint>? {
        if (limits.minPointMs <= 0 || limits.maxPointMs < limits.minPointMs) return null
        val strength = scale.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: HapticIntensity.MEDIUM.scale
        val points = when (event) {
            AppHapticEvent.SELECTION -> listOf(HapticPoint(0.45f, 0.55f, 20), HapticPoint(0f, 0.55f, 20))
            AppHapticEvent.NAVIGATION_STEP -> listOf(HapticPoint(0.65f, 0.75f, 20), HapticPoint(0f, 0.75f, 20))
            AppHapticEvent.ACTION -> listOf(
                HapticPoint(0.55f, 0.7f, 20),
                HapticPoint(0.9f, 0.85f, 22),
                HapticPoint(0f, 0.85f, 25)
            )
            AppHapticEvent.TOGGLE_ON -> listOf(
                HapticPoint(0.5f, 0.6f, 20),
                HapticPoint(0.95f, 0.88f, 22),
                HapticPoint(0f, 0.88f, 24)
            )
            AppHapticEvent.TOGGLE_OFF -> listOf(
                HapticPoint(0.75f, 0.75f, 20),
                HapticPoint(0.4f, 0.5f, 22),
                HapticPoint(0f, 0.5f, 24)
            )
            AppHapticEvent.CONFIRMATION -> listOf(
                HapticPoint(0.7f, 0.7f, 20),
                HapticPoint(0f, 0.7f, 20),
                HapticPoint(0f, 0.8f, 25),
                HapticPoint(0.95f, 0.85f, 20),
                HapticPoint(0f, 0.85f, 30)
            )
            AppHapticEvent.REJECTION -> listOf(
                HapticPoint(1f, 0.45f, 20),
                HapticPoint(0f, 0.45f, 35),
                HapticPoint(0f, 0.45f, 35),
                HapticPoint(0.9f, 0.65f, 20),
                HapticPoint(0f, 0.65f, 35)
            )
            AppHapticEvent.DRAG_START -> listOf(HapticPoint(0.7f, 0.5f, 20), HapticPoint(0f, 0.7f, 35))
            AppHapticEvent.DRAG_END -> listOf(HapticPoint(0.75f, 0.75f, 20), HapticPoint(0f, 0.75f, 25))
            AppHapticEvent.LIMIT -> listOf(
                HapticPoint(1f, 0.9f, 20),
                HapticPoint(0.85f, 0.85f, 20),
                HapticPoint(0f, 0.9f, 30)
            )
        }.map { it.copy(intensity = it.intensity * strength, durationMs = it.durationMs.coerceAtLeast(limits.minPointMs)) }
        return points.takeIf {
            it.size <= limits.maxPoints && it.all { point -> point.durationMs <= limits.maxPointMs } &&
                it.sumOf { point -> point.durationMs } <= limits.maxDurationMs && it.last().intensity == 0f
        }
    }

    fun composition(
        event: AppHapticEvent,
        scale: Float,
        maxSize: Int = 10,
        maxDelayMs: Int = 1000
    ): List<PrimitiveStep>? {
        if (maxSize <= 0 || maxDelayMs < 0) return null
        val strength = scale.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: HapticIntensity.MEDIUM.scale
        if (strength <= 0f) return null

        val rawSteps = when (event) {
            AppHapticEvent.SELECTION -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.78f)
            )
            AppHapticEvent.NAVIGATION_STEP -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.78f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.92f, delayMs = 14)
            )
            AppHapticEvent.ACTION -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.84f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, delayMs = 20)
            )
            AppHapticEvent.TOGGLE_ON -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.82f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, delayMs = 18)
            )
            AppHapticEvent.TOGGLE_OFF -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.96f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.72f, delayMs = 22)
            )
            AppHapticEvent.CONFIRMATION -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.88f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, delayMs = 42)
            )
            AppHapticEvent.REJECTION -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.92f, delayMs = 55)
            )
            AppHapticEvent.DRAG_START -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.78f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, delayMs = 16)
            )
            AppHapticEvent.DRAG_END -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.90f, delayMs = 0)
            )
            AppHapticEvent.LIMIT -> listOf(
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, delayMs = 0),
                PrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.96f, delayMs = 14)
            )
        }.map { step ->
            step.copy(scale = (step.scale * strength).coerceIn(0.05f, 1f))
        }

        return rawSteps.takeIf { steps ->
            steps.size <= maxSize && steps.all { it.delayMs in 0..maxDelayMs && it.scale in 0f..1f }
        }
    }
}

/** Input-event rate limiting only; no timer, coroutine or vibration runs in the background. */
internal class HapticEventLimiter {
    private var lastEvent: AppHapticEvent? = null
    private var lastAtMs: Long? = null
    private var lastControlAtMs: Long? = null

    fun accept(event: AppHapticEvent, nowMs: Long): Boolean {
        val isControl = event == AppHapticEvent.SELECTION || event == AppHapticEvent.LIMIT
        if (isControl && lastControlAtMs?.let { nowMs >= it && nowMs - it < 50 } == true) return false
        val previous = lastAtMs
        if (previous != null && nowMs >= previous) {
            val elapsed = nowMs - previous
            if (elapsed < 8 || (lastEvent == event && elapsed < 50)) return false
        }
        lastAtMs = nowMs
        if (isControl) lastControlAtMs = nowMs
        lastEvent = event
        return true
    }
}
