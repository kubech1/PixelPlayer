package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMeshBackgroundTest {
    @Test
    fun positionsAreStableAndSpreadAcrossTheViewport() {
        val points = playerMeshPositions("remote:234")
        assertEquals(points, playerMeshPositions("remote:234"))
        assertNotEquals(points, playerMeshPositions("local:234"))
        assertEquals(4, points.size)
        points.forEachIndexed { index, point ->
            val left = (index % 2) * 0.5f
            val top = (index / 2) * 0.5f
            assertTrue(point.x in (left + 0.08f)..(left + 0.42f))
            assertTrue(point.y in (top + 0.08f)..(top + 0.42f))
        }
    }

    @Test
    fun limitsBrightDarkAndSaturatedAnchorsDuringPaletteChanges() {
        val bright = listOf(Color.White, Color.Yellow, Color.Cyan, Color.Magenta).map(MeshRgb::from)
        val dark = listOf(Color.Black, Color(0xFF150820), Color.Blue, Color(0xFF082012)).map(MeshRgb::from)
        val frames = listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map { blend ->
            bright.zip(dark) { from, to -> from.mix(to, blend) }
        }
        val samples = listOf(Color(0xFF646464) to Color.White, Color(0xFF888888) to Color.Black)
        for ((base, text) in samples) {
            val background = MeshRgb.from(base)
            val foreground = MeshRgb.from(text)
            for (anchors in frames) {
                val contrast = playerMeshContrast(anchors, background, foreground)
                val worst = extremeChannels(anchors, brighter = text == Color.White)
                val color = contrast.base.mix(worst, contrast.strength).color().toArgb()
                val grain = (1.5f * contrast.strength + 0.5f).toInt()
                val direction = if (text == Color.White) grain else -grain
                val affected = androidRgb(
                    ((color shr 16 and 255) + direction).coerceIn(0, 255),
                    ((color shr 8 and 255) + direction).coerceIn(0, 255),
                    ((color and 255) + direction).coerceIn(0, 255)
                )
                assertTrue(contrastRatio(text.toArgb(), affected) >= 4.5)
            }
        }
        assertTrue(playerMeshContrast(bright, MeshRgb.from(Color(0xFF646464)), MeshRgb.from(Color.White)).strength < 1f)
    }

    @Test
    fun usesAReadableSolidColorWhenTheBaseIsAlreadyUnsafe() {
        val base = MeshRgb.from(Color(0xFF777777))
        val result = playerMeshContrast(List(4) { base }, base, base)
        assertEquals(0f, result.strength, 0f)
        assertTrue(contrastRatio(base.color().toArgb(), result.base.color().toArgb()) >= 4.5)
    }

    private fun extremeChannels(colors: List<MeshRgb>, brighter: Boolean): MeshRgb {
        fun channel(value: (MeshRgb) -> Float) =
            if (brighter) colors.maxOf(value) else colors.minOf(value)
        return MeshRgb(channel { it.r }, channel { it.g }, channel { it.b })
    }

    private fun androidRgb(r: Int, g: Int, b: Int): Int = 0xff000000.toInt() or (r shl 16) or (g shl 8) or b

    private fun contrastRatio(first: Int, second: Int): Double {
        fun luminance(argb: Int): Double {
            fun channel(shift: Int): Double {
                val value = (argb shr shift and 255) / 255.0
                return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
}
