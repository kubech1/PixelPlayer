package com.theveloper.pixelplay.presentation.components.scoped

import org.junit.Assert.*
import org.junit.Test

class MiniPlayerGesturePolicyTest {
    @Test fun horizontalDistanceAndVelocityThresholds() {
        assertNull(resolveMiniPlayerSkip(-99f, -1999f, 400f, 2f, false))
        assertEquals(MiniPlayerSkip.NEXT, resolveMiniPlayerSkip(-100f, 0f, 400f, 2f, false))
        assertEquals(MiniPlayerSkip.PREVIOUS, resolveMiniPlayerSkip(1f, 2000f, 400f, 2f, false))
    }
    @Test fun horizontalDirectionReversesInRtl() {
        assertEquals(MiniPlayerSkip.PREVIOUS, resolveMiniPlayerSkip(-100f, 0f, 400f, 2f, true))
        assertEquals(MiniPlayerSkip.NEXT, resolveMiniPlayerSkip(100f, 0f, 400f, 2f, true))
    }
    @Test fun downwardDismissRequiresCollapsedStartAndDensityScaledThreshold() {
        assertFalse(shouldDismissCollapsedPlayer(false, 200f, 3000f, 2f))
        assertFalse(shouldDismissCollapsedPlayer(true, 95f, 1599f, 2f))
        assertTrue(shouldDismissCollapsedPlayer(true, 96f, 0f, 2f))
        assertTrue(shouldDismissCollapsedPlayer(true, 1f, 1600f, 2f))
        assertFalse(shouldDismissCollapsedPlayer(true, -96f, -1600f, 2f))
    }
}
