package com.theveloper.pixelplay.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.compositeOver
import com.theveloper.pixelplay.data.preferences.VisualEffectsLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PillSurfaceContrastTest {
    @Test
    fun `wcag contrast helper matches canonical black-white and identity ratios`() {
        assertEquals(21.0, calculateWcagContrastRatio(Color.White, Color.Black), 0.05)
        assertEquals(1.0, calculateWcagContrastRatio(Color(0xFF336699), Color(0xFF336699)), 0.001)
    }

    @Test
    fun `neutral content has sufficient contrast in both pill states and themes`() {
        val extremeColors = listOf(
            Color.Black, Color.White, Color.Red, Color.Yellow, Color.Cyan, Color.Magenta
        )
        for (isDark in listOf(false, true)) {
            val colors = resolvePillSurfaceColors(isDark, VisualEffectsLevel.BALANCED)
            assertEquals(if (isDark) Color.White else Color.Black, colors.contentColor)
            assertTrue(calculateWcagContrastRatio(colors.contentColor, colors.controlColor) >= MIN_LABEL_CONTRAST_RATIO)
            for (backdrop in extremeColors) {
                val restingSurface = colors.surfaceColor.compositeOver(backdrop)
                assertTrue(calculateWcagContrastRatio(colors.contentColor, restingSurface) >= MIN_LABEL_CONTRAST_RATIO)
                for (tint in extremeColors) {
                    val compactSurface = tint.copy(alpha = COMPACT_PILL_TINT_ALPHA).compositeOver(restingSurface)
                    assertTrue(
                        calculateWcagContrastRatio(colors.contentColor, compactSurface) >= MIN_LABEL_CONTRAST_RATIO,
                        "Insufficient compact contrast: dark=$isDark, tint=$tint, backdrop=$backdrop"
                    )
                }
            }
        }
    }

    @Test
    fun `minimal visual effects uses neutral shadow and omits light border`() {
        val minimal = resolvePillSurfaceColors(isDark = true, VisualEffectsLevel.MINIMAL)
        val balanced = resolvePillSurfaceColors(isDark = true, VisualEffectsLevel.BALANCED)
        assertFalse(minimal.hasLightBorder)
        assertEquals(DefaultShadowColor, minimal.shadowAmbientColor)
        assertEquals(DefaultShadowColor, minimal.shadowSpotColor)
        assertTrue(balanced.hasLightBorder)
        assertEquals(balanced.surfaceColor, minimal.surfaceColor)
    }
}
