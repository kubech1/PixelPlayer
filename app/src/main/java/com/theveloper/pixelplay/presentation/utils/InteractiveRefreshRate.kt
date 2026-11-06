package com.theveloper.pixelplay.presentation.utils

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.preferredFrameRate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ask the system for a high category only during active interaction when Android ARR
 * (`Display.hasArrSupport()`) is active. On One UI 9 devices such as SM-S921B where
 * `hasArrSupport()` is false and `REFRESH_RATE_MODE_SEAMLESS` (10–120 Hz) is managed natively,
 * voting `FrameRateCategory.High` caps SurfaceFlinger to `frameRateCategoryRate.high = 90.0 Hz`,
 * so we defer to the system seamless controller.
 */
@Composable
fun Modifier.interactiveRefreshRate(directTouch: Boolean = false): Modifier {
    val view = LocalView.current
    if (view.display?.hasArrSupport() != true) return this

    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var touching by remember { mutableStateOf(false) }
    var scrolling by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lastScroll = remember { longArrayOf(0) }
    val idleJob = remember { arrayOfNulls<Job>(1) }
    val connection = remember {
        object : NestedScrollConnection {
            private fun observe(offset: Offset) {
                if (offset == Offset.Zero) return
                lastScroll[0] = SystemClock.uptimeMillis()
                scrolling = true
                if (idleJob[0]?.isActive == true) return
                idleJob[0] = scope.launch {
                    do { delay(100) } while (SystemClock.uptimeMillis() - lastScroll[0] < 100)
                    scrolling = false
                }
            }
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                observe(available)
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                observe(consumed)
                return Offset.Zero
            }
        }
    }
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(resumed) {
        if (!resumed) { touching = false; scrolling = false; idleJob[0]?.cancel() }
    }
    DisposableEffect(Unit) { onDispose { idleJob[0]?.cancel() } }
    val category = if (resumed && (touching || scrolling)) FrameRateCategory.High else FrameRateCategory.Default
    val base = preferredFrameRate(category).nestedScroll(connection)
    return if (!directTouch) base else base.pointerInput(Unit) {
        try {
            awaitPointerEventScope {
                while (true) touching = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
            }
        } finally { touching = false }
    }
}
