package com.theveloper.pixelplay.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.data.preferences.VisualEffectsLevel
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import com.theveloper.pixelplay.presentation.viewmodel.ColorSchemePair
import kotlin.math.max
import kotlin.math.min

private const val DARK_PILL_SURFACE_ALPHA = 0.84f
private const val LIGHT_PILL_SURFACE_ALPHA = 0.88f
private const val ALBUM_TINT_WEIGHT = 0.10f
private const val SHADOW_AMBIENT_ALPHA = 0.30f
private const val SHADOW_SPOT_ALPHA = 0.40f
private const val DARK_TOP_HIGHLIGHT_ALPHA = 0.16f
private const val LIGHT_TOP_HIGHLIGHT_ALPHA = 0.45f
private const val BOTTOM_SHADE_ALPHA = 0.06f

internal const val MIN_ICON_CONTRAST_RATIO = 3.0
internal const val MIN_LABEL_CONTRAST_RATIO = 4.5

@Immutable
data class PillSurfaceColors(
    val surfaceColor: Color,
    val tintColor: Color,
    val shadowAmbientColor: Color,
    val shadowSpotColor: Color,
    val hasLightBorder: Boolean,
    val topHighlightAlpha: Float,
    val bottomShadeAlpha: Float,
    val contentIconColor: Color,
    val contentLabelColor: Color,
    val selectedIndicatorColor: Color,
    val selectedContentColor: Color
)

/**
 * Pure WCAG 2.1 contrast ratio calculation in sRGB relative luminance space.
 * Avoids `android.graphics.Color` stubs in JVM unit tests where `isReturnDefaultValues = true`.
 */
internal fun calculateWcagContrastRatio(foreground: Color, opaqueBackground: Color): Double {
    val bgOpaque = opaqueBackground.copy(alpha = 1f)
    val fgComposited = if (foreground.alpha < 1f) {
        foreground.compositeOver(bgOpaque)
    } else {
        foreground
    }
    val l1 = fgComposited.luminance().toDouble()
    val l2 = bgOpaque.luminance().toDouble()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

internal fun worstCaseContrastAgainstBackdrops(
    foreground: Color,
    translucentSurface: Color,
    backdrops: List<Color>
): Double = backdrops.minOf { backdrop ->
    val compositedSurface = translucentSurface.compositeOver(backdrop.copy(alpha = 1f))
    calculateWcagContrastRatio(foreground, compositedSurface)
}

private fun ensureContrastAgainstBackdrops(
    preferred: Color,
    fallbackOnSurface: Color,
    translucentSurface: Color,
    backdrops: List<Color>,
    minRatio: Double
): Color {
    val candidateOpaque = preferred.copy(alpha = 1f)
    if (worstCaseContrastAgainstBackdrops(candidateOpaque, translucentSurface, backdrops) >= minRatio) {
        return candidateOpaque
    }

    val onSurfaceOpaque = fallbackOnSurface.copy(alpha = 1f)
    if (worstCaseContrastAgainstBackdrops(onSurfaceOpaque, translucentSurface, backdrops) >= minRatio) {
        return interpolateToThreshold(candidateOpaque, onSurfaceOpaque, translucentSurface, backdrops, minRatio)
    }

    val whiteWorst = worstCaseContrastAgainstBackdrops(Color.White, translucentSurface, backdrops)
    val blackWorst = worstCaseContrastAgainstBackdrops(Color.Black, translucentSurface, backdrops)
    val highContrastAnchor = if (whiteWorst >= blackWorst) Color.White else Color.Black
    return interpolateToThreshold(onSurfaceOpaque, highContrastAnchor, translucentSurface, backdrops, minRatio)
}

private fun interpolateToThreshold(
    start: Color,
    target: Color,
    translucentSurface: Color,
    backdrops: List<Color>,
    minRatio: Double
): Color {
    if (worstCaseContrastAgainstBackdrops(target, translucentSurface, backdrops) < minRatio) {
        return target
    }
    var low = 0f
    var high = 1f
    repeat(12) {
        val mid = (low + high) * 0.5f
        val blended = lerp(start, target, mid)
        if (worstCaseContrastAgainstBackdrops(blended, translucentSurface, backdrops) >= minRatio) {
            high = mid
        } else {
            low = mid
        }
    }
    return lerp(start, target, high)
}

/**
 * Resolves the active album tint when the player theme uses album art (`activeSchemePair != null`),
 * returning `null` otherwise so callers fall back to `MaterialTheme.colorScheme.primary`.
 */
fun resolveActiveAlbumTint(
    activePlayerSchemePair: ColorSchemePair?,
    isDark: Boolean
): Color? = activePlayerSchemePair?.let { pair ->
    (if (isDark) pair.dark else pair.light).primary
}

fun resolvePillSurfaceColors(
    colorScheme: ColorScheme,
    isDark: Boolean,
    albumTint: Color? = null,
    effectsLevel: VisualEffectsLevel = VisualEffectsLevel.DEFAULT
): PillSurfaceColors {
    val tintOpaque = (albumTint ?: colorScheme.primary).copy(alpha = 1f)
    val baseOpaque = colorScheme.surfaceContainer.copy(alpha = 1f)
    val tintedOpaque = lerp(baseOpaque, tintOpaque, ALBUM_TINT_WEIGHT)
    val surfaceAlpha = if (isDark) DARK_PILL_SURFACE_ALPHA else LIGHT_PILL_SURFACE_ALPHA
    val surfaceColor = tintedOpaque.copy(alpha = surfaceAlpha)

    val isMinimal = effectsLevel == VisualEffectsLevel.MINIMAL
    val shadowAmbient = if (isMinimal) DefaultShadowColor else tintOpaque.copy(alpha = SHADOW_AMBIENT_ALPHA)
    val shadowSpot = if (isMinimal) DefaultShadowColor else tintOpaque.copy(alpha = SHADOW_SPOT_ALPHA)
    val topHighlightAlpha = if (isDark) DARK_TOP_HIGHLIGHT_ALPHA else LIGHT_TOP_HIGHLIGHT_ALPHA

    // Bound contrast across theme background as well as pure black and pure white content scrolling behind.
    val referenceBackdrops = listOf(colorScheme.background, Color.Black, Color.White)

    val contentIconColor = ensureContrastAgainstBackdrops(
        preferred = colorScheme.onSurfaceVariant,
        fallbackOnSurface = colorScheme.onSurface,
        translucentSurface = surfaceColor,
        backdrops = referenceBackdrops,
        minRatio = MIN_ICON_CONTRAST_RATIO
    )
    val contentLabelColor = ensureContrastAgainstBackdrops(
        preferred = colorScheme.onSurfaceVariant,
        fallbackOnSurface = colorScheme.onSurface,
        translucentSurface = surfaceColor,
        backdrops = referenceBackdrops,
        minRatio = MIN_LABEL_CONTRAST_RATIO
    )

    val indicatorColor = colorScheme.secondaryContainer.copy(alpha = 1f)
    val selectedContentColor = ensureContrastAgainstBackdrops(
        preferred = colorScheme.onSecondaryContainer,
        fallbackOnSurface = colorScheme.onSurface,
        translucentSurface = indicatorColor,
        backdrops = listOf(indicatorColor),
        minRatio = MIN_LABEL_CONTRAST_RATIO
    )

    return PillSurfaceColors(
        surfaceColor = surfaceColor,
        tintColor = tintOpaque,
        shadowAmbientColor = shadowAmbient,
        shadowSpotColor = shadowSpot,
        hasLightBorder = !isMinimal,
        topHighlightAlpha = topHighlightAlpha,
        bottomShadeAlpha = BOTTOM_SHADE_ALPHA,
        contentIconColor = contentIconColor,
        contentLabelColor = contentLabelColor,
        selectedIndicatorColor = indicatorColor,
        selectedContentColor = selectedContentColor
    )
}

/**
 * Applies the floating pill material in strict layer order:
 * 1. Tinted shadow (neutral in `MINIMAL` visual effects mode).
 * 2. Translucent surface (`0.84` dark / `0.88` light) blended 10 % toward [albumTint] or `primary`.
 * 3. 1 dp vertical light-catch border (omitted in `MINIMAL`).
 */
fun Modifier.pillSurface(
    shape: Shape,
    albumTint: Color? = null,
    elevation: Dp = 6.dp
): Modifier = composed {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalPixelPlayDarkTheme.current
    val effectsLevel = LocalVisualEffects.current.effectiveLevel

    val colors = remember(colorScheme, isDark, albumTint, effectsLevel) {
        resolvePillSurfaceColors(
            colorScheme = colorScheme,
            isDark = isDark,
            albumTint = albumTint,
            effectsLevel = effectsLevel
        )
    }
    val borderBrush = remember(colors.topHighlightAlpha, colors.bottomShadeAlpha) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = colors.topHighlightAlpha),
            0.5f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = colors.bottomShadeAlpha)
        )
    }

    this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = colors.shadowAmbientColor,
            spotColor = colors.shadowSpotColor
        )
        .background(
            color = colors.surfaceColor,
            shape = shape
        )
        .then(
            if (colors.hasLightBorder) {
                Modifier.border(
                    width = 1.dp,
                    brush = borderBrush,
                    shape = shape
                )
            } else {
                Modifier
            }
        )
}
