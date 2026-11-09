package com.theveloper.pixelplay.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.compositeOver
import com.theveloper.pixelplay.data.preferences.AlbumArtPaletteStyle
import com.theveloper.pixelplay.data.preferences.VisualEffectsLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PillSurfaceContrastTest {

    @Test
    fun `wcag contrast helper matches canonical black-white and identity ratios`() {
        assertEquals(21.0, calculateWcagContrastRatio(Color.White, Color.Black), 0.05)
        assertEquals(21.0, calculateWcagContrastRatio(Color.Black, Color.White), 0.05)
        assertEquals(1.0, calculateWcagContrastRatio(Color(0xFF336699), Color(0xFF336699)), 0.001)
    }

    @Test
    fun `pill surface maintains icon and label contrast across extreme synthetic palettes`() {
        val extremeSeeds = listOf(
            "near_white" to Color(0xFFFAFAFA),
            "near_black" to Color(0xFF050505),
            "saturated_crimson" to Color(0xFFFF0044),
            "saturated_yellow" to Color(0xFFFFEA00),
            "saturated_cyan" to Color(0xFF00FFDD),
            "saturated_violet" to Color(0xFF7B00FF)
        )
        val extremeScrollingBackdrops = listOf(
            "pure_black" to Color.Black,
            "pure_white" to Color.White
        )

        for ((seedName, seedColor) in extremeSeeds) {
            for (style in AlbumArtPaletteStyle.entries) {
                val pair = generateColorSchemeFromSeed(seedColor, style)
                for (isDark in listOf(false, true)) {
                    val scheme = if (isDark) pair.dark else pair.light
                    val albumTint = resolveActiveAlbumTint(pair, isDark)
                    val resolved = resolvePillSurfaceColors(
                        colorScheme = scheme,
                        isDark = isDark,
                        albumTint = albumTint,
                        effectsLevel = VisualEffectsLevel.BALANCED
                    )

                    val backdropsToCheck = extremeScrollingBackdrops + ("theme_bg" to scheme.background)
                    for ((backdropName, backdrop) in backdropsToCheck) {
                        val compositedSurface = resolved.surfaceColor.compositeOver(backdrop)
                        val iconContrast = calculateWcagContrastRatio(
                            resolved.contentIconColor,
                            compositedSurface
                        )
                        val labelContrast = calculateWcagContrastRatio(
                            resolved.contentLabelColor,
                            compositedSurface
                        )

                        assertTrue(
                            iconContrast >= MIN_ICON_CONTRAST_RATIO - 1e-3,
                            "Icon contrast $iconContrast < 3.0 for seed=$seedName style=$style dark=$isDark backdrop=$backdropName"
                        )
                        assertTrue(
                            labelContrast >= MIN_LABEL_CONTRAST_RATIO - 1e-3,
                            "Label contrast $labelContrast < 4.5 for seed=$seedName style=$style dark=$isDark backdrop=$backdropName"
                        )
                    }

                    val selectedContrast = calculateWcagContrastRatio(
                        resolved.selectedContentColor,
                        resolved.selectedIndicatorColor
                    )
                    assertTrue(
                        selectedContrast >= MIN_LABEL_CONTRAST_RATIO - 1e-3,
                        "Selected tab contrast $selectedContrast < 4.5 for seed=$seedName style=$style dark=$isDark"
                    )
                }
            }
        }
    }

    @Test
    fun `minimal visual effects uses neutral shadow and omits light border`() {
        val pair = generateColorSchemeFromSeed(Color(0xFF00AAFF), AlbumArtPaletteStyle.TONAL_SPOT)
        val minimal = resolvePillSurfaceColors(
            colorScheme = pair.dark,
            isDark = true,
            albumTint = Color(0xFF00AAFF),
            effectsLevel = VisualEffectsLevel.MINIMAL
        )
        val balanced = resolvePillSurfaceColors(
            colorScheme = pair.dark,
            isDark = true,
            albumTint = Color(0xFF00AAFF),
            effectsLevel = VisualEffectsLevel.BALANCED
        )

        assertFalse(minimal.hasLightBorder)
        assertEquals(DefaultShadowColor, minimal.shadowAmbientColor)
        assertEquals(DefaultShadowColor, minimal.shadowSpotColor)

        assertTrue(balanced.hasLightBorder)
        assertEquals(0.84f, balanced.surfaceColor.alpha, 0.001f)
        assertNull(resolveActiveAlbumTint(null, isDark = true))
    }
}
