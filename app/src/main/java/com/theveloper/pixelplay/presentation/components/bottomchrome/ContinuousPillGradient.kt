package com.theveloper.pixelplay.presentation.components.bottomchrome

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.theveloper.pixelplay.ui.theme.COMPACT_PILL_TINT_ALPHA

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
    }.drawWithCache {
        val width = rootWidth
        val brush = if (width > 0f && (colors.start.alpha > 0f || colors.end.alpha > 0f)) {
            Brush.horizontalGradient(listOf(colors.start, colors.end), endX = width)
        } else null
        onDrawBehind {
            val alpha = fraction().coerceIn(0f, 1f) * COMPACT_PILL_TINT_ALPHA
            if (brush != null && alpha > 0f) {
                val x = origin.x
                translate(left = -x) {
                    drawRect(brush, topLeft = Offset(x, 0f), size = size, alpha = alpha)
                }
            }
        }
    }
}
