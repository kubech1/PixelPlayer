package com.theveloper.pixelplay.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.data.preferences.VisualEffectsLevel
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import kotlin.math.max
import kotlin.math.min

internal const val COMPACT_PILL_TINT_ALPHA = 0.12f
internal const val MIN_LABEL_CONTRAST_RATIO = 4.5

@Immutable
internal data class PillSurfaceColors(
    val surfaceColor: Color,
    val contentColor: Color,
    val controlColor: Color,
    val shadowAmbientColor: Color,
    val shadowSpotColor: Color,
    val hasLightBorder: Boolean,
    val topHighlightAlpha: Float
)

/** WCAG contrast ratio in sRGB relative luminance space. */
internal fun calculateWcagContrastRatio(foreground: Color, opaqueBackground: Color): Double {
    val background = opaqueBackground.copy(alpha = 1f)
    val l1 = foreground.compositeOver(background).luminance().toDouble()
    val l2 = background.luminance().toDouble()
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
}

internal fun resolvePillSurfaceColors(
    isDark: Boolean,
    effectsLevel: VisualEffectsLevel
): PillSurfaceColors {
    val isMinimal = effectsLevel == VisualEffectsLevel.MINIMAL
    return PillSurfaceColors(
        surfaceColor = (if (isDark) Color(0xFF202020) else Color(0xFFF6F6F6)).copy(alpha = 0.92f),
        contentColor = if (isDark) Color.White else Color.Black,
        controlColor = if (isDark) Color(0xFF3B3B3B) else Color(0xFFDEDEDE),
        shadowAmbientColor = if (isMinimal) DefaultShadowColor else Color.Black.copy(alpha = 0.30f),
        shadowSpotColor = if (isMinimal) DefaultShadowColor else Color.Black.copy(alpha = 0.40f),
        hasLightBorder = !isMinimal,
        topHighlightAlpha = if (isDark) 0.16f else 0.45f
    )
}

@Composable
internal fun rememberPillSurfaceColors(): PillSurfaceColors {
    val isDark = LocalPixelPlayDarkTheme.current
    val effectsLevel = LocalVisualEffects.current.effectiveLevel
    return remember(isDark, effectsLevel) { resolvePillSurfaceColors(isDark, effectsLevel) }
}

/** Applies the neutral pill material below the optional compact artwork tint. */
@Composable
internal fun Modifier.pillSurface(
    shape: Shape,
    colors: PillSurfaceColors,
    elevation: Dp = 6.dp
): Modifier {
    val borderBrush = remember(colors.topHighlightAlpha) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = colors.topHighlightAlpha),
            0.5f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.06f)
        )
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = colors.shadowAmbientColor,
            spotColor = colors.shadowSpotColor
        )
        .background(colors.surfaceColor, shape)
        .then(if (colors.hasLightBorder) Modifier.border(1.dp, borderBrush, shape) else Modifier)
}
