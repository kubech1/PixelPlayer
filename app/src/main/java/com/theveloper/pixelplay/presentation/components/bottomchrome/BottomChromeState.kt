package com.theveloper.pixelplay.presentation.components.bottomchrome

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.theveloper.pixelplay.ui.theme.PixelMotion
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Stable
class BottomChromeState internal constructor(private val scope: CoroutineScope, private val density: Float) {
    private val fraction = Animatable(0f)
    val mergeFractionProvider: () -> Float = { fraction.value }
    private val policy = BottomChromeMergePolicy(SystemClock::elapsedRealtime)
    private var idle: Job? = null
    private var animation: Job? = null
    private var target = false
    var enabled = true
    var animationsEnabled = true
    private val reselections = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val reselectionEvents = reselections.asSharedFlow()
    fun reselect(route: String) { reselections.tryEmit(route) }

    fun reset() {
        idle?.cancel()
        policy.reset()
        update()
    }

    private fun update() {
        if (target == policy.merged && animationsEnabled) return
        target = policy.merged
        animation?.cancel()
        animation = scope.launch {
            fraction.animateTo(if (target) 1f else 0f,
                if (animationsEnabled) PixelMotion.structural() else snap())
        }
    }

    private fun scheduleIdle() {
        idle?.cancel()
        idle = scope.launch { delay(800); policy.expire(); update() }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            policy.scroll(consumed.x / density, consumed.y / density, available.y / density, enabled)
            update()
            if (consumed.y != 0f && kotlin.math.abs(consumed.y) >= kotlin.math.abs(consumed.x)) scheduleIdle()
            return Offset.Zero
        }
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            policy.flingEnded()
            scheduleIdle()
            return Velocity.Zero
        }
    }
}

val LocalBottomChromeState = compositionLocalOf<BottomChromeState?> { null }

@Composable
fun rememberBottomChromeState(): BottomChromeState {
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    return remember(scope, density) { BottomChromeState(scope, density) }
}
