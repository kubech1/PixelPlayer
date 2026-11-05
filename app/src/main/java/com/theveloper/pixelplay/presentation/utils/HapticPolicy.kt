package com.theveloper.pixelplay.presentation.utils

enum class HapticIntensity(val scale: Float) {
    LOW(0.4f), MEDIUM(0.7f), HIGH(1f);

    companion object {
        fun fromStorage(value: String?): HapticIntensity = entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

enum class AppHapticEvent { SELECTION, ACTION, CONFIRMATION, REJECTION, DRAG_START, DRAG_END, LIMIT }

internal data class HapticPoint(val intensity: Float, val sharpness: Float, val durationMs: Long)
internal data class EnvelopeLimits(val minPointMs: Long, val maxPointMs: Long, val maxPoints: Int, val maxDurationMs: Long)

/** Pure policy: validates the whole envelope before any hardware request is made. */
internal object HapticPolicy {
    fun envelope(event: AppHapticEvent, scale: Float, limits: EnvelopeLimits): List<HapticPoint>? {
        if (limits.minPointMs <= 0 || limits.maxPointMs < limits.minPointMs) return null
        val strength = scale.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: HapticIntensity.MEDIUM.scale
        val points = when (event) {
            AppHapticEvent.SELECTION -> listOf(HapticPoint(0.45f, 0.6f, 20), HapticPoint(0f, 0.6f, 20))
            AppHapticEvent.ACTION -> listOf(HapticPoint(0.85f, 0.8f, 20), HapticPoint(0f, 0.8f, 30))
            AppHapticEvent.CONFIRMATION -> listOf(HapticPoint(0.7f, 0.7f, 20), HapticPoint(0f, 0.7f, 20), HapticPoint(0f, 0.8f, 30), HapticPoint(0.9f, 0.8f, 20), HapticPoint(0f, 0.8f, 30))
            AppHapticEvent.REJECTION -> listOf(HapticPoint(1f, 0.4f, 20), HapticPoint(0f, 0.4f, 40), HapticPoint(0f, 0.4f, 40), HapticPoint(1f, 0.4f, 20), HapticPoint(0f, 0.4f, 40))
            AppHapticEvent.DRAG_START -> listOf(HapticPoint(0.7f, 0.45f, 20), HapticPoint(0f, 0.7f, 40))
            AppHapticEvent.DRAG_END -> listOf(HapticPoint(0.8f, 0.7f, 20), HapticPoint(0f, 0.7f, 30))
            AppHapticEvent.LIMIT -> listOf(HapticPoint(1f, 0.9f, 20), HapticPoint(0f, 0.9f, 40))
        }.map { it.copy(intensity = it.intensity * strength, durationMs = it.durationMs.coerceAtLeast(limits.minPointMs)) }
        return points.takeIf {
            it.size <= limits.maxPoints && it.all { point -> point.durationMs <= limits.maxPointMs } &&
                it.sumOf { point -> point.durationMs } <= limits.maxDurationMs && it.last().intensity == 0f
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
