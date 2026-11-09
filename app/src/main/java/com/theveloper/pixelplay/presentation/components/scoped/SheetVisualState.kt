package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

internal fun collapsedPillCompactionOffset(restingHeight: Dp, navigationHeight: Dp, navigationHidden: Boolean): Dp =
    restingHeight - 56.dp + if (navigationHidden) 0.dp else navigationHeight + 8.dp

internal data class SheetVisualState(
    val currentBottomPadding: Dp,
    val baseBottomPadding: Dp,
    /** Draw-phase provider: read this inside graphicsLayer to avoid layout relayout per frame. */
    val playerContentAreaHeightPxProvider: () -> Float,
    /** Layout-phase provider: read inside .offset { } to avoid recomposition per drag frame. */
    val visualSheetTranslationYProvider: () -> Float,
    val sheetCornerRadiusProvider: () -> Dp,
    /** Draw-phase providers: read inside graphicsLayer to avoid layout relayout per frame. */
    val currentHorizontalPaddingStartPxProvider: () -> Float,
    val currentHorizontalPaddingEndPxProvider: () -> Float,
    val effectiveExpansionFractionProvider: () -> Float,
    val miniArtworkAnchorOffsetProvider: () -> Offset,
    val carouselArtworkAnchorOffsetProvider: () -> Offset
)

@Composable
internal fun rememberSheetVisualState(
    showPlayerContentArea: Boolean,
    miniPlayerRestingHeight: Dp,
    collapsedStateHorizontalPadding: Dp,
    predictiveBackCollapseProgress: State<Float>,
    playerContentExpansionFraction: Animatable<Float, AnimationVector1D>,
    containerHeight: Dp,
    currentSheetTranslationY: Animatable<Float, AnimationVector1D>,
    sheetCollapsedTargetY: Float,
    mergeFractionProvider: () -> Float,
    navigationRowHeight: Dp,
    isNavBarHidden: Boolean,
): SheetVisualState {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    return remember(
        density,
        direction,
        showPlayerContentArea,
        miniPlayerRestingHeight,
        collapsedStateHorizontalPadding,
        predictiveBackCollapseProgress,
        playerContentExpansionFraction,
        containerHeight,
        currentSheetTranslationY,
        sheetCollapsedTargetY,
        mergeFractionProvider,
        navigationRowHeight,
        isNavBarHidden
    ) {
        val baseBottomPadding = with(density) {
            (containerHeight - miniPlayerRestingHeight - sheetCollapsedTargetY.toDp()).coerceAtLeast(0.dp)
        }
        val miniHeightPx = with(density) { miniPlayerRestingHeight.toPx() }
        val compactHeightPx = with(density) { 56.dp.toPx() }
        val containerHeightPx = with(density) { containerHeight.toPx() }
        val restingStartPx = with(density) { collapsedStateHorizontalPadding.toPx() }
        val compactInsetPx = if (isNavBarHidden) 0f else with(density) { 64.dp.toPx() }
        val compactionOffsetPx = with(density) {
            collapsedPillCompactionOffset(miniPlayerRestingHeight, navigationRowHeight, isNavBarHidden).toPx()
        }
        val expansion = {
            (playerContentExpansionFraction.value * (1f - predictiveBackCollapseProgress.value)).coerceIn(0f, 1f)
        }
        val baseTranslationY = {
            androidx.compose.ui.util.lerp(currentSheetTranslationY.value, sheetCollapsedTargetY, predictiveBackCollapseProgress.value)
        }
        val visualTranslationY = {
            baseTranslationY() + compactionOffsetPx * mergeFractionProvider() * (1f - expansion())
        }
        val collapsedStart = { restingStartPx + compactInsetPx * mergeFractionProvider() }
        val currentStart = { if (showPlayerContentArea) collapsedStart() * (1f - expansion()) else restingStartPx }
        val radius = {
            lerp(lerp(miniPlayerRestingHeight / 2, 28.dp, mergeFractionProvider()), 0.dp, expansion())
        }

        SheetVisualState(
            currentBottomPadding = 0.dp,
            baseBottomPadding = baseBottomPadding,
            playerContentAreaHeightPxProvider = height@{
                if (!showPlayerContentArea) return@height 0f
                val fraction = expansion()
                val translationY = baseTranslationY()
                val compactDelta = (miniHeightPx - compactHeightPx) * mergeFractionProvider()
                if (translationY <= sheetCollapsedTargetY) {
                    val targetBottom = androidx.compose.ui.util.lerp(
                        sheetCollapsedTargetY + miniHeightPx, containerHeightPx, fraction
                    )
                    (targetBottom - translationY - compactDelta * (1f - fraction)).coerceAtLeast(0f)
                } else {
                    androidx.compose.ui.util.lerp(miniHeightPx - compactDelta, containerHeightPx, fraction)
                }
            },
            visualSheetTranslationYProvider = visualTranslationY,
            sheetCornerRadiusProvider = radius,
            currentHorizontalPaddingStartPxProvider = currentStart,
            currentHorizontalPaddingEndPxProvider = {
                if (showPlayerContentArea) restingStartPx * (1f - expansion()) else restingStartPx
            },
            effectiveExpansionFractionProvider = expansion,
            // Project the laid-out artwork to each settled position in the moving sheet viewport.
            miniArtworkAnchorOffsetProvider = {
                val directionSign = if (direction == LayoutDirection.Ltr) 1f else -1f
                Offset(
                    directionSign * (collapsedStart() - currentStart()),
                    sheetCollapsedTargetY + compactionOffsetPx * mergeFractionProvider() - visualTranslationY()
                )
            },
            carouselArtworkAnchorOffsetProvider = {
                Offset(0f, -visualTranslationY())
            }
        )
    }
}
