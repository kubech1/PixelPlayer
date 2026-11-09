package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

internal fun collapsedPillCompactionOffset(restingHeight: Dp, navigationHeight: Dp, navigationHidden: Boolean): Dp =
    restingHeight - 56.dp + if (navigationHidden) 0.dp else navigationHeight + 8.dp

internal data class SheetVisualState(
    val currentBottomPadding: Dp,
    val baseBottomPadding: Dp,
    val collapsedStateHorizontalPaddingPx: Int,
    /** Draw-phase provider: read this inside graphicsLayer to avoid layout relayout per frame. */
    val playerContentAreaHeightPxProvider: () -> Float,
    /** Layout-phase provider: read inside .offset { } to avoid recomposition per drag frame. */
    val visualSheetTranslationYProvider: () -> Float,
    val overallSheetTopCornerRadiusProvider: () -> Dp,
    val playerContentActualBottomRadiusProvider: () -> Dp,
    /** Draw-phase providers: read inside graphicsLayer to avoid layout relayout per frame. */
    val currentHorizontalPaddingStartPxProvider: () -> Float,
    val currentHorizontalPaddingEndPxProvider: () -> Float
)

@Composable
internal fun rememberSheetVisualState(
    showPlayerContentArea: Boolean,
    miniPlayerRestingHeight: Dp,
    collapsedStateHorizontalPadding: Dp,
    predictiveBackCollapseProgress: Float,
    playerContentExpansionFraction: Animatable<Float, AnimationVector1D>,
    containerHeight: Dp,
    currentSheetTranslationY: Animatable<Float, AnimationVector1D>,
    sheetCollapsedTargetY: Float,
    mergeFractionProvider: () -> Float,
    navigationRowHeight: Dp,
    isNavBarHidden: Boolean,
): SheetVisualState {
    // Compute in px to be read inside graphicsLayer (draw phase) — zero relayout per drag frame.
    val density = LocalDensity.current
    val baseBottomPadding = remember(containerHeight, sheetCollapsedTargetY, density, miniPlayerRestingHeight) {
        val targetYDp = with(density) { sheetCollapsedTargetY.toDp() }
        (containerHeight - miniPlayerRestingHeight - targetYDp)
            .coerceAtLeast(0.dp)
    }

    val currentBottomPadding = 0.dp

    val miniHeightPx = remember(density, miniPlayerRestingHeight) { with(density) { miniPlayerRestingHeight.toPx() } }
    val containerHeightPx = remember(containerHeight, density) { with(density) { containerHeight.toPx() } }
    val predictiveBackCollapseProgressState = rememberUpdatedState(predictiveBackCollapseProgress)
    val baseTranslationYProvider: () -> Float = remember(
        currentSheetTranslationY,
        sheetCollapsedTargetY
    ) {
        {
            val progress = predictiveBackCollapseProgressState.value
            currentSheetTranslationY.value * (1f - progress) +
                (sheetCollapsedTargetY * progress)
        }
    }

    val playerContentAreaHeightPxProvider: () -> Float = remember(
        showPlayerContentArea,
        playerContentExpansionFraction,
        predictiveBackCollapseProgress,
        miniHeightPx,
        containerHeightPx,
        baseTranslationYProvider,
        mergeFractionProvider,
        sheetCollapsedTargetY
    ) {
        {
            if (showPlayerContentArea) {
                val effectiveFraction = playerContentExpansionFraction.value * (1f - predictiveBackCollapseProgress)
                val safeFraction = effectiveFraction.coerceIn(0f, 1f)
                val translationY = baseTranslationYProvider()
                
                if (translationY <= sheetCollapsedTargetY) {
                    val targetBottom = androidx.compose.ui.util.lerp(
                        sheetCollapsedTargetY + miniHeightPx,
                        containerHeightPx,
                        safeFraction
                    )
                    (targetBottom - translationY - (miniHeightPx - with(density) { 56.dp.toPx() }) * mergeFractionProvider() * (1f - safeFraction)).coerceAtLeast(0f)
                } else {
                    androidx.compose.ui.util.lerp(miniHeightPx - (miniHeightPx - with(density) { 56.dp.toPx() }) * mergeFractionProvider(), containerHeightPx, safeFraction)
                }
            } else {
                0f
            }
        }
    }

    val overallSheetTopCornerRadiusProvider: () -> Dp = remember(playerContentExpansionFraction, mergeFractionProvider, predictiveBackCollapseProgress, miniPlayerRestingHeight) {
        {
            val expansion = (playerContentExpansionFraction.value * (1f - predictiveBackCollapseProgress)).coerceIn(0f, 1f)
            lerp(lerp(miniPlayerRestingHeight / 2, 28.dp, mergeFractionProvider()), 0.dp, expansion)
        }
    }
    val playerContentActualBottomRadiusProvider = overallSheetTopCornerRadiusProvider
    val collapsedStateHorizontalPaddingPx = with(density) { collapsedStateHorizontalPadding.toPx() }

    val currentHorizontalPaddingStartPxProvider: () -> Float = remember(
        isNavBarHidden, mergeFractionProvider,
        showPlayerContentArea,
        collapsedStateHorizontalPaddingPx,
        playerContentExpansionFraction,
        predictiveBackCollapseProgress
    ) {
        {
            if (showPlayerContentArea) {
                val effectiveFraction = playerContentExpansionFraction.value * (1f - predictiveBackCollapseProgress)
                val safeFraction = effectiveFraction.coerceIn(0f, 1f)
                androidx.compose.ui.util.lerp(collapsedStateHorizontalPaddingPx + (if (isNavBarHidden) 0f else with(density) { 64.dp.toPx() }) * mergeFractionProvider(), 0f, safeFraction)
            } else {
                collapsedStateHorizontalPaddingPx
            }
        }
    }

    val currentHorizontalPaddingEndPxProvider: () -> Float = remember(
        showPlayerContentArea,
        collapsedStateHorizontalPaddingPx,
        playerContentExpansionFraction,
        predictiveBackCollapseProgress
    ) {
        {
            if (showPlayerContentArea) {
                val effectiveFraction = playerContentExpansionFraction.value * (1f - predictiveBackCollapseProgress)
                val safeFraction = effectiveFraction.coerceIn(0f, 1f)
                androidx.compose.ui.util.lerp(collapsedStateHorizontalPaddingPx, 0f, safeFraction)
            } else {
                collapsedStateHorizontalPaddingPx
            }
        }
    }

    val compactionOffsetPx = with(density) {
        collapsedPillCompactionOffset(miniPlayerRestingHeight, navigationRowHeight, isNavBarHidden).toPx()
    }
    val visualSheetTranslationYProvider: () -> Float = remember(baseTranslationYProvider, mergeFractionProvider, compactionOffsetPx, playerContentExpansionFraction, predictiveBackCollapseProgress) {
        {
            val weight = 1f - (playerContentExpansionFraction.value * (1f - predictiveBackCollapseProgress)).coerceIn(0f, 1f)
            baseTranslationYProvider() + compactionOffsetPx * mergeFractionProvider() * weight
        }
    }

    return SheetVisualState(
        currentBottomPadding = currentBottomPadding,
        baseBottomPadding = baseBottomPadding,
        collapsedStateHorizontalPaddingPx = collapsedStateHorizontalPaddingPx.toInt().coerceAtLeast(0),
        playerContentAreaHeightPxProvider = playerContentAreaHeightPxProvider,
        visualSheetTranslationYProvider = visualSheetTranslationYProvider,
        overallSheetTopCornerRadiusProvider = overallSheetTopCornerRadiusProvider,
        playerContentActualBottomRadiusProvider = playerContentActualBottomRadiusProvider,
        currentHorizontalPaddingStartPxProvider = currentHorizontalPaddingStartPxProvider,
        currentHorizontalPaddingEndPxProvider = currentHorizontalPaddingEndPxProvider
    )
}
