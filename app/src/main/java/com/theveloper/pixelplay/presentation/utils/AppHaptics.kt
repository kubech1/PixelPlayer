package com.theveloper.pixelplay.presentation.utils

import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.theveloper.pixelplay.R

@Immutable
data class AppHapticsConfig(val enabled: Boolean = true, val intensityScale: Float = HapticIntensity.MEDIUM.scale)

val LocalAppHapticsConfig = staticCompositionLocalOf { AppHapticsConfig() }

val NoOpHapticFeedback: HapticFeedback = object : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/** All Compose and View entry points share the same engine and limiter for their window. */
class ExpressiveHapticFeedback(private val view: View, private val config: AppHapticsConfig) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val constant = when (hapticFeedbackType) {
            HapticFeedbackType.Confirm -> HapticFeedbackConstants.CONFIRM
            HapticFeedbackType.Reject -> HapticFeedbackConstants.REJECT
            HapticFeedbackType.GestureEnd -> HapticFeedbackConstants.GESTURE_END
            HapticFeedbackType.GestureThresholdActivate -> HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            HapticFeedbackType.LongPress -> HapticFeedbackConstants.LONG_PRESS
            HapticFeedbackType.SegmentFrequentTick -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            HapticFeedbackType.SegmentTick -> HapticFeedbackConstants.SEGMENT_TICK
            HapticFeedbackType.TextHandleMove -> HapticFeedbackConstants.TEXT_HANDLE_MOVE
            HapticFeedbackType.ToggleOn -> HapticFeedbackConstants.TOGGLE_ON
            HapticFeedbackType.ToggleOff -> HapticFeedbackConstants.TOGGLE_OFF
            HapticFeedbackType.ContextClick -> HapticFeedbackConstants.CONTEXT_CLICK
            else -> HapticFeedbackConstants.CLOCK_TICK
        }
        view.performAppHapticFeedback(config, constant)
    }
}

fun View.performAppHapticFeedback(config: AppHapticsConfig, event: AppHapticEvent): Boolean =
    performAppHapticFeedback(config, when (event) {
        AppHapticEvent.SELECTION -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        AppHapticEvent.NAVIGATION_STEP -> HapticFeedbackConstants.SEGMENT_TICK
        AppHapticEvent.ACTION -> HapticFeedbackConstants.CONTEXT_CLICK
        AppHapticEvent.TOGGLE_ON -> HapticFeedbackConstants.TOGGLE_ON
        AppHapticEvent.TOGGLE_OFF -> HapticFeedbackConstants.TOGGLE_OFF
        AppHapticEvent.CONFIRMATION -> HapticFeedbackConstants.CONFIRM
        AppHapticEvent.REJECTION -> HapticFeedbackConstants.REJECT
        AppHapticEvent.DRAG_START -> HapticFeedbackConstants.DRAG_START
        AppHapticEvent.DRAG_END -> HapticFeedbackConstants.GESTURE_END
        AppHapticEvent.LIMIT -> HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
    })

fun View.performAppHapticFeedback(appHapticsConfig: AppHapticsConfig, feedbackConstant: Int, flags: Int = 0): Boolean {
    if (!appHapticsConfig.enabled || !isHapticFeedbackEnabled || !isAttachedToWindow) return false
    val window = rootView
    val engine = (window.getTag(R.id.app_haptic_engine) as? AppHapticEngine)
        ?: AppHapticEngine(window).also { window.setTag(R.id.app_haptic_engine, it) }
    // Do not forward flags that could bypass the user's haptic preferences.
    return engine.perform(appHapticsConfig, feedbackConstant)
}

fun performAppCompatHapticFeedback(view: View, appHapticsConfig: AppHapticsConfig, feedbackConstant: Int, flags: Int = 0): Boolean =
    view.performAppHapticFeedback(appHapticsConfig, feedbackConstant, flags)

private class AppHapticEngine(private val view: View) {
    private val vibrator: Vibrator? = view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private val limiter = HapticEventLimiter()
    private val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    private val envelopeLimits: EnvelopeLimits? = runCatching {
        vibrator?.takeIf { it.hasVibrator() && it.areEnvelopeEffectsSupported() }?.envelopeEffectInfo?.let {
            EnvelopeLimits(it.minControlPointDurationMillis, it.maxControlPointDurationMillis, it.maxSize, it.maxDurationMillis)
        }
    }.getOrNull()

    fun perform(config: AppHapticsConfig, constant: Int): Boolean {
        if (!config.enabled || !view.isHapticFeedbackEnabled) return false
        @Suppress("DEPRECATION")
        val systemEnabled = Settings.System.getInt(view.context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        if (!systemEnabled) return false
        val event = when (constant) {
            HapticFeedbackConstants.CONFIRM -> AppHapticEvent.CONFIRMATION
            HapticFeedbackConstants.REJECT -> AppHapticEvent.REJECTION
            HapticFeedbackConstants.GESTURE_START, HapticFeedbackConstants.DRAG_START, HapticFeedbackConstants.LONG_PRESS -> AppHapticEvent.DRAG_START
            HapticFeedbackConstants.GESTURE_END -> AppHapticEvent.DRAG_END
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE -> AppHapticEvent.LIMIT
            HapticFeedbackConstants.TOGGLE_ON -> AppHapticEvent.TOGGLE_ON
            HapticFeedbackConstants.TOGGLE_OFF -> AppHapticEvent.TOGGLE_OFF
            HapticFeedbackConstants.SEGMENT_TICK -> AppHapticEvent.NAVIGATION_STEP
            HapticFeedbackConstants.CONTEXT_CLICK, HapticFeedbackConstants.VIRTUAL_KEY -> AppHapticEvent.ACTION
            else -> AppHapticEvent.SELECTION
        }
        if (!limiter.accept(event, SystemClock.uptimeMillis())) return false
        val hardware = vibrator ?: return view.performHapticFeedback(constant)
        if (!hardware.hasVibrator()) return false
        val effect = runCatching {
            envelopeLimits?.let { limits -> HapticPolicy.envelope(event, config.intensityScale, limits) }?.let { points ->
                VibrationEffect.BasicEnvelopeBuilder().apply {
                    setInitialSharpness(points.first().sharpness)
                    points.forEach { addControlPoint(it.intensity, it.sharpness, it.durationMs) }
                }.build()
            } ?: primitiveEffect(hardware, event, config.intensityScale)
        }.getOrNull()
        if (effect != null) {
            if (runCatching { hardware.vibrate(effect, attributes) }.isSuccess) return true
        }
        return view.performHapticFeedback(constant)
    }

    private fun primitiveEffect(hardware: Vibrator, event: AppHapticEvent, scale: Float): VibrationEffect? {
        val steps = HapticPolicy.composition(event, scale) ?: return null
        val requiredPrimitives = steps.map { it.primitiveId }.distinct().toIntArray()
        if (hardware.areAllPrimitivesSupported(*requiredPrimitives)) {
            return VibrationEffect.startComposition().apply {
                steps.forEach { step ->
                    addPrimitive(step.primitiveId, step.scale, step.delayMs)
                }
            }.compose()
        }
        if (!hardware.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) return null
        val fallbackStrength = (scale.takeIf { it.isFinite() } ?: HapticIntensity.MEDIUM.scale).coerceIn(0.05f, 1f)
        return VibrationEffect.startComposition().apply {
            addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, fallbackStrength)
        }.compose()
    }
}
