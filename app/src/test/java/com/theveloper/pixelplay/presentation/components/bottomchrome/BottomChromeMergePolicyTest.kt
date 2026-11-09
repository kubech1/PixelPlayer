package com.theveloper.pixelplay.presentation.components.bottomchrome

import org.junit.Assert.*
import org.junit.Test

class BottomChromeMergePolicyTest {
    private var now = 0L
    private val policy = BottomChromeMergePolicy { now }
    private fun scroll(y: Float, x: Float = 0f, available: Float = 0f, enabled: Boolean = true) = policy.scroll(x, y, available, enabled)

    @Test fun accumulatesConsumedDistanceAtBoundary() {
        scroll(-23f); assertFalse(policy.merged)
        scroll(-1f); assertTrue(policy.merged)
        scroll(23f); assertTrue(policy.merged)
        scroll(1f); assertFalse(policy.merged)
    }
    @Test fun directionChangeResetsAccumulator() {
        scroll(-20f); scroll(2f); scroll(-4f); assertFalse(policy.merged)
        scroll(-20f); assertTrue(policy.merged)
    }
    @Test fun ignoresHorizontalCarousel() {
        scroll(-30f, x = 40f); assertFalse(policy.merged)
        scroll(-24f); scroll(30f, x = 40f); assertTrue(policy.merged)
    }
    @Test fun topOverscrollSeparates() {
        scroll(-24f); scroll(0f, available = 1f); assertFalse(policy.merged)
    }
    @Test fun idleStartsAtLastDeltaAndFlingEnd() {
        scroll(-24f); now = 799; policy.expire(); assertTrue(policy.merged)
        policy.flingEnded(); now = 1598; policy.expire(); assertTrue(policy.merged)
        now = 1599; policy.expire(); assertFalse(policy.merged)
    }
    @Test fun disabledPolicyForPreferenceOrTalkBackCannotMerge() {
        scroll(-24f); scroll(-40f, enabled = false); assertFalse(policy.merged)
        scroll(-23f); assertFalse(policy.merged)
    }
    @Test fun routeKeyboardSheetAndCircleResetDistance() {
        scroll(-20f); policy.reset(); scroll(-4f); assertFalse(policy.merged)
    }
}
