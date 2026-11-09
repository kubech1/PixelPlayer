package com.theveloper.pixelplay.presentation.components.bottomchrome

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

@Immutable
data class PillGradientColors(val start: Color = Color.Transparent, val end: Color = Color.Transparent)
val LocalPillGradientColors = compositionLocalOf { PillGradientColors() }

/** Both pills sample one root-coordinate gradient, including in RTL and multi-window. */
fun Modifier.continuousPillGradient(fraction: () -> Float): Modifier = composed {
    val colors = LocalPillGradientColors.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var rootWidth by remember { mutableFloatStateOf(0f) }
    this.onGloballyPositioned {
        origin = it.positionInRoot()
        rootWidth = it.findRootCoordinates().size.width.toFloat()
    }.drawBehind {
        drawRect(Brush.horizontalGradient(listOf(colors.start, colors.end), -origin.x, rootWidth - origin.x), alpha = fraction().coerceIn(0f, 1f) * 0.5f)
    }
}
