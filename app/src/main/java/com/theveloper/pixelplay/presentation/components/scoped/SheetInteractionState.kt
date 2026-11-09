package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.theveloper.pixelplay.presentation.viewmodel.PlayerSheetState
import kotlinx.coroutines.CoroutineScope

internal data class SheetInteractionState(
    val playerShadowShape: Shape,
    val sheetVerticalDragGestureHandler: SheetVerticalDragGestureHandler,
    val canDragSheet: Boolean
)

@Composable
internal fun rememberSheetInteractionState(
    scope: CoroutineScope,
    velocityTracker: VelocityTracker,
    sheetMotionController: SheetMotionController,
    playerContentExpansionFraction: Animatable<Float, AnimationVector1D>,
    currentSheetTranslationY: Animatable<Float, AnimationVector1D>,
    visualOvershootScaleY: Animatable<Float, AnimationVector1D>,
    sheetCollapsedTargetY: Float,
    sheetExpandedTargetY: Float,
    miniPlayerContentHeightPx: Float,
    currentSheetContentState: PlayerSheetState,
    showPlayerContentArea: Boolean,
    sheetCornerRadiusProvider: () -> Dp,
    onAnimateSheet: suspend (
        targetExpanded: Boolean,
        animationSpec: AnimationSpec<Float>?,
        initialVelocity: Float
    ) -> Unit,
    onExpandSheetState: () -> Unit,
    onCollapseSheetState: () -> Unit,
    onDismissPlaylist: () -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onDraggingPlayerAreaChange: (Boolean) -> Unit
): SheetInteractionState {
    val playerShadowShape = remember(sheetCornerRadiusProvider) { PlayerSheetDynamicShape(sheetCornerRadiusProvider) }

    val collapsedYState = rememberUpdatedState(sheetCollapsedTargetY)
    val expandedYState = rememberUpdatedState(sheetExpandedTargetY)
    val miniHeightState = rememberUpdatedState(miniPlayerContentHeightPx)
    val densityState = rememberUpdatedState(LocalDensity.current)
    val currentSheetState = rememberUpdatedState(currentSheetContentState)
    val onAnimateSheetState = rememberUpdatedState(onAnimateSheet)
    val onExpandSheetStateState = rememberUpdatedState(onExpandSheetState)
    val dismissState = rememberUpdatedState(onDismissPlaylist)
    val motionState = rememberUpdatedState(com.theveloper.pixelplay.ui.theme.LocalMotionPreferences.current.animationsEnabled)
    val onCollapseSheetStateState = rememberUpdatedState(onCollapseSheetState)
    val onDraggingChangeState = rememberUpdatedState(onDraggingChange)
    val onDraggingPlayerAreaChangeState = rememberUpdatedState(onDraggingPlayerAreaChange)

    val sheetVerticalDragGestureHandler = remember(
        scope,
        velocityTracker,
        sheetMotionController,
        playerContentExpansionFraction,
        currentSheetTranslationY,
        visualOvershootScaleY
    ) {
        SheetVerticalDragGestureHandler(
            scope = scope,
            velocityTracker = velocityTracker,
            densityProvider = { densityState.value },
            sheetMotionController = sheetMotionController,
            playerContentExpansionFraction = playerContentExpansionFraction,
            currentSheetTranslationY = currentSheetTranslationY,
            expandedYProvider = { expandedYState.value },
            collapsedYProvider = { collapsedYState.value },
            miniHeightPxProvider = { miniHeightState.value },
            currentSheetStateProvider = { currentSheetState.value },
            visualOvershootScaleY = visualOvershootScaleY,
            onDraggingChange = { onDraggingChangeState.value(it) },
            onDraggingPlayerAreaChange = { onDraggingPlayerAreaChangeState.value(it) },
            onAnimateSheet = { targetExpanded, animationSpec, initialVelocity ->
                onAnimateSheetState.value(targetExpanded, animationSpec, initialVelocity)
            },
            onExpandSheetState = { onExpandSheetStateState.value() },
            onCollapseSheetState = { onCollapseSheetStateState.value() },
            onDismissPlaylist = { dismissState.value() },
            animationsEnabled = { motionState.value }
        )
    }

    return SheetInteractionState(
        playerShadowShape = playerShadowShape,
        sheetVerticalDragGestureHandler = sheetVerticalDragGestureHandler,
        canDragSheet = showPlayerContentArea
    )
}

private class PlayerSheetDynamicShape(private val radiusProvider: () -> Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = with(density) { radiusProvider().toPx() }.coerceIn(0f, size.minDimension / 2f)
        return Outline.Rounded(RoundRect(Rect(0f, 0f, size.width, size.height), CornerRadius(radius)))
    }
}
