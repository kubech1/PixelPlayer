package com.theveloper.pixelplay.presentation.components.scoped

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import kotlin.math.roundToInt

internal class PlayerGlassBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf<Offset?>(null)
}

internal val LocalPlayerGlassBackdrop = staticCompositionLocalOf<PlayerGlassBackdrop?> { null }

/** Samples only the mesh behind each control. Content and interaction stay in the normal layer. */
@Composable
internal fun Modifier.playerGlass(tint: Color, shape: Shape? = null): Modifier {
    val backdrop = LocalPlayerGlassBackdrop.current
    val effects = LocalVisualEffects.current
    val blurred = effects.allowsDepthEffects && !effects.isPowerSaveMode
    val clipped = if (shape != null) clip(shape) else clipToBounds()
    if (!blurred || backdrop == null) return clipped.background(tint.copy(alpha = 0.76f))

    val glass = rememberGraphicsLayer()
    var origin by remember { mutableStateOf<Offset?>(null) }
    return clipped
        .onGloballyPositioned { origin = it.positionInRoot() }
        .drawWithCache {
            val radius = (if (effects.allowsRealtimeBlur) 13.dp else 8.dp).toPx()
            val inset = (radius * 2).roundToInt()
            val layerSize = IntSize(size.width.roundToInt() + 2 * inset, size.height.roundToInt() + 2 * inset)
            glass.renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
            onDrawBehind {
                val source = backdrop.origin
                val target = origin
                if (source != null && target != null) {
                    val offset = source - target
                    glass.record(this, layoutDirection, layerSize) {
                        translate(offset.x + inset, offset.y + inset) { drawLayer(backdrop.layer) }
                    }
                    translate(-inset.toFloat(), -inset.toFloat()) { drawLayer(glass) }
                }
                drawRect(tint.copy(alpha = 0.52f))
            }
        }
}
