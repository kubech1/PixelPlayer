package com.theveloper.pixelplay.presentation.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.presentation.components.scoped.collapsedPillCompactionOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerBottomAnchoringTest {

    @Test
    fun compactionAlignsPlayerWithNavigationAtLargeTextSize() {
        val restPlayerTop = 300.dp - 24.dp - 8.dp - 76.dp - 8.dp - 80.dp
        val compactPlayerTop = restPlayerTop + collapsedPillCompactionOffset(80.dp, 76.dp, false)
        assertEquals(300.dp - 24.dp - 8.dp - 56.dp, compactPlayerTop)
    }

    @Test
    fun compactPlayerKeepsItsBottomEdgeWhenNavigationIsHidden() {
        val restPlayerTop = 300.dp - 24.dp - 8.dp - 80.dp
        val compactPlayerTop = restPlayerTop + collapsedPillCompactionOffset(80.dp, 76.dp, true)
        assertEquals(300.dp - 24.dp - 8.dp - 56.dp, compactPlayerTop)
    }

    @Test
    fun sanitizeNavigationBarBottomInset_clampsImpossibleFloatingWindowInsets() {
        assertEquals(0.dp, sanitizeNavigationBarBottomInset(Dp.Unspecified))
        assertEquals(0.dp, sanitizeNavigationBarBottomInset((-24).dp))
        assertEquals(48.dp, sanitizeNavigationBarBottomInset(48.dp))
        assertEquals(MaxNavigationBarBottomInset, sanitizeNavigationBarBottomInset(420.dp))
    }

    @Test
    fun calculatePlayerSheetCollapsedTargetY_usesMeasuredContainerHeight() {
        val targetY = calculatePlayerSheetCollapsedTargetY(
            containerHeightPx = 2_000f,
            collapsedContentHeightPx = 160f,
            bottomMarginPx = 280f,
            bottomSpacerPx = 20f
        )

        assertEquals(1_540f, targetY, 0.001f)
    }

    @Test
    fun calculatePlayerSheetCollapsedTargetY_neverPlacesSheetOutsideTopEdge() {
        val targetY = calculatePlayerSheetCollapsedTargetY(
            containerHeightPx = 700f,
            collapsedContentHeightPx = 160f,
            bottomMarginPx = 1_200f,
            bottomSpacerPx = 20f
        )

        assertEquals(0f, targetY, 0.001f)
    }
}
