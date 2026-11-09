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
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
internal fun rememberMiniPlayerSkipGesture(
    enabled: Boolean,
    song: Song,
    queueIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Triple<Modifier, () -> Float, Song> {
    val offset = remember { Animatable(0f) }
    var outgoingSong by remember { mutableStateOf<Song?>(null) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val animations = LocalMotionPreferences.current.animationsEnabled
    val haptics = LocalHapticFeedback.current
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    val currentSong by rememberUpdatedState(song)
    val currentQueueIndex by rememberUpdatedState(queueIndex)
    LaunchedEffect(enabled, density, rtl, animations) {
        outgoingSong = null
        offset.snapTo(0f)
    }
    val modifier = if (!enabled) Modifier else Modifier.pointerInput(density, rtl, animations) {
        val tracker = VelocityTracker()
        var distance = 0f
        var job: Job? = null
        try {
            detectHorizontalDragGestures(
                onDragStart = {
                    job?.cancel()
                    outgoingSong = null
                    tracker.resetTracking()
                    distance = 0f
                },
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
                    val width = size.width.toFloat()
                    val skip = resolveMiniPlayerSkip(distance, tracker.calculateVelocity().x, width, density, rtl)
                    val from = currentSong
                    val fromIndex = currentQueueIndex
                    if (skip != null) {
                        if (animations) outgoingSong = from
                        if (skip == MiniPlayerSkip.NEXT) next() else previous()
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                    job = scope.launch {
                        if (skip == null || !animations) {
                            offset.animateTo(0f, if (animations) PixelMotion.structural() else snap())
                            return@launch
                        }
                        val direction = if ((skip == MiniPlayerSkip.NEXT) != rtl) -1f else 1f
                        offset.animateTo(width * direction, PixelMotion.structural())
                        val changed = withTimeoutOrNull(150L) {
                            snapshotFlow { currentSong.id != from.id || currentQueueIndex != fromIndex }.first { it }
                        } == true
                        if (changed) offset.snapTo(-width * direction)
                        outgoingSong = null
                        offset.animateTo(0f, if (changed) PixelMotion.feedback() else PixelMotion.structural())
                    }
                }
            )
        } finally {
            job?.cancel()
        }
    }
    return Triple(modifier, remember { { offset.value } }, outgoingSong ?: song)
}

internal fun shouldDismissCollapsedPlayer(startedCollapsed: Boolean, distancePx: Float, velocityPx: Float, density: Float): Boolean =
    startedCollapsed && (distancePx >= 48f * density || (distancePx > 0f && velocityPx >= 800f * density))
