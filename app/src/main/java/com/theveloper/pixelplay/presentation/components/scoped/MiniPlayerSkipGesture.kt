package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.LayoutDirection
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

internal enum class MiniPlayerSkip { PREVIOUS, NEXT }

internal fun resolveMiniPlayerSkip(distance: Float, velocity: Float, width: Float, density: Float, rtl: Boolean): MiniPlayerSkip? {
    if (width <= 0f || !distance.isFinite() || !velocity.isFinite()) return null
    val direction = when {
        abs(distance) >= width * 0.25f -> distance.sign
        abs(velocity) >= 1000f * density -> velocity.sign
        else -> return null
    }
    return if ((direction < 0f) != rtl) MiniPlayerSkip.NEXT else MiniPlayerSkip.PREVIOUS
}

/** Only cover art and text consume this offset; the pill and transport button stay still. */
@Composable
internal fun rememberMiniPlayerSkipGesture(enabled: Boolean, onPrevious: () -> Unit, onNext: () -> Unit): Pair<Modifier, () -> Float> {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val animations = LocalMotionPreferences.current.animationsEnabled
    val haptics = LocalHapticFeedback.current
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    val modifier = if (!enabled) Modifier else Modifier.pointerInput(density, rtl, animations) {
        val tracker = VelocityTracker()
        var distance = 0f
        var job: Job? = null
        detectHorizontalDragGestures(
            onDragStart = { job?.cancel(); tracker.resetTracking(); distance = 0f },
            onHorizontalDrag = { change, delta ->
                change.consume()
                tracker.addPosition(change.uptimeMillis, change.position)
                distance += delta
                job?.cancel()
                job = scope.launch { offset.snapTo(distance) }
            },
            onDragCancel = {
                job?.cancel()
                job = scope.launch { offset.animateTo(0f, if (animations) PixelMotion.structural() else snap()) }
            },
            onDragEnd = {
                job?.cancel()
                val skip = resolveMiniPlayerSkip(distance, tracker.calculateVelocity().x, size.width.toFloat(), density, rtl)
                job = scope.launch {
                    if (skip != null) {
                        val direction = if ((skip == MiniPlayerSkip.NEXT) != rtl) -1f else 1f
                        offset.animateTo(size.width * direction, if (animations) PixelMotion.feedback() else snap())
                        if (skip == MiniPlayerSkip.NEXT) next() else previous()
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        offset.snapTo(-size.width * direction)
                        offset.animateTo(0f, if (animations) PixelMotion.feedback() else snap())
                    } else offset.animateTo(0f, if (animations) PixelMotion.structural() else snap())
                }
            }
        )
    }
    return modifier to remember { { offset.value } }
}

internal fun shouldDismissCollapsedPlayer(startedCollapsed: Boolean, distancePx: Float, velocityPx: Float, density: Float): Boolean =
    startedCollapsed && (distancePx >= 48f * density || (distancePx > 0f && velocityPx >= 800f * density))
