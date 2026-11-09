package com.theveloper.pixelplay.presentation.components.bottomchrome

import kotlin.math.abs
import kotlin.math.sign

/** Scroll distances are dp; time is monotonic milliseconds. No Compose or Android dependency. */
internal class BottomChromeMergePolicy(private val clock: () -> Long) {
    var merged = false
        private set
    private var accumulated = 0f
    private var lastScroll = Long.MIN_VALUE

    fun reset() {
        merged = false
        accumulated = 0f
        lastScroll = Long.MIN_VALUE
    }

    fun scroll(x: Float, y: Float, availableY: Float, enabled: Boolean) {
        if (!enabled) { reset(); return }
        if (!x.isFinite() || !y.isFinite() || abs(x) > abs(y)) return
        if (availableY > 0f) { reset(); return }
        if (y == 0f) return
        lastScroll = clock()
        if (accumulated.sign != y.sign) accumulated = 0f
        accumulated += y
        if (abs(accumulated) >= 24f) {
            merged = accumulated < 0f
            accumulated = 0f
        }
    }

    fun flingEnded() { lastScroll = clock() }

    fun expire() {
        if (lastScroll != Long.MIN_VALUE && clock() - lastScroll >= 800L) reset()
    }
}
