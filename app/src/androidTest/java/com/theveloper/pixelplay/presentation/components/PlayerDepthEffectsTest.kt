package com.theveloper.pixelplay.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.theveloper.pixelplay.data.preferences.VisualEffectsLevel
import com.theveloper.pixelplay.data.preferences.CarouselStyle
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.scoped.LocalPlayerArtworkDepth
import com.theveloper.pixelplay.presentation.components.scoped.PlayerArtworkDepth
import com.theveloper.pixelplay.presentation.components.scoped.PlayerArtworkHalo
import com.theveloper.pixelplay.presentation.components.scoped.PlayerMeshBackground
import com.theveloper.pixelplay.presentation.utils.EffectiveVisualEffects
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.LocalPixelPlayDarkTheme
import com.theveloper.pixelplay.ui.theme.MotionPreferences
import java.io.File
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerDepthEffectsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun rendersMeshAndConfinesTheExtraBlurToTheHalo() {
        val cover = createCover()

        val level = mutableStateOf(EffectiveVisualEffects(VisualEffectsLevel.MINIMAL))
        val scheme = darkColorScheme(
            primaryContainer = Color.Black,
            onPrimaryContainer = Color.White,
            secondaryContainer = Color.Blue,
            tertiaryContainer = Color.Green,
            surfaceContainer = Color(0xFF602080)
        )
        try {
            compose.setContent {
                CompositionLocalProvider(
                    LocalVisualEffects provides level.value,
                    LocalMotionPreferences provides MotionPreferences(animationsEnabled = false),
                    LocalPixelPlayDarkTheme provides true
                ) {
                    val density = LocalDensity.current
                    val left = with(density) { 40.dp.toPx() }
                    val right = with(density) { 160.dp.toPx() }
                    Box(Modifier.size(200.dp).testTag("depth-root").background(Color.Black)) {
                        if (level.value.allowsDepthEffects) {
                            PlayerMeshBackground(
                                songId = "test-cover",
                                targetScheme = scheme,
                                baseColor = { Color.Black },
                                foregroundColor = Color.White,
                                expansion = { 1f },
                                modifier = Modifier.matchParentSize()
                            )
                        }
                        PlayerArtworkHalo(
                            occurrence = "test-cover",
                            uri = cover.toURI().toString(),
                            tint = Color.Black,
                            paused = false,
                            visible = true,
                            bounds = { Rect(left, left, right, right) },
                            modifier = Modifier.matchParentSize()
                        )
                        Box(Modifier.offset(40.dp, 40.dp).size(120.dp).background(Color.White))
                    }
                }
            }

            val minimal = capture()
            assertEquals(Color.Black, pixel(minimal, 4, 4))
            compose.runOnIdle { level.value = EffectiveVisualEffects(VisualEffectsLevel.BALANCED) }
            compose.waitUntil(15_000L) { pixel(capture(), 36, 100).red > 0.25f }
            val balanced = capture()
            assertNotEquals(pixel(balanced, 4, 4), pixel(balanced, 195, 195))
            assertTrue(pixel(balanced, 36, 100).red > pixel(minimal, 36, 100).red)

            compose.runOnIdle { level.value = EffectiveVisualEffects(VisualEffectsLevel.FULL) }
            val full = capture()
            assertNotEquals(pixel(balanced, 34, 100), pixel(full, 34, 100))
            assertEquals(pixel(balanced, 4, 4), pixel(full, 4, 4))
            assertEquals(Color.White, pixel(full, 100, 100))

            compose.runOnIdle { level.value = EffectiveVisualEffects(VisualEffectsLevel.FULL, isPowerSaveMode = true) }
            assertEquals(pixel(balanced, 34, 100), pixel(capture(), 34, 100))
        } finally {
            cover.delete()
        }
    }

    @Test
    fun keepsTheActiveArtworkVisibleWithTwoPeekLayout() {
        val cover = createCover()
        val queue = List(3) { index ->
            Song.emptySong().copy(
                id = "depth-$index",
                title = "Depth $index",
                albumArtUriString = cover.toURI().toString()
            )
        }.toImmutableList()
        try {
            compose.setContent {
                CompositionLocalProvider(
                    LocalPlayerArtworkDepth provides PlayerArtworkDepth({ 1f }, Color.Red),
                    LocalMotionPreferences provides MotionPreferences(animationsEnabled = false)
                ) {
                    AlbumCarouselSection(
                        currentSong = queue[1],
                        queue = queue,
                        expansionFraction = 1f,
                        currentMediaItemIndex = 1,
                        carouselStyle = CarouselStyle.TWO_PEEK,
                        modifier = Modifier.size(240.dp, 144.dp),
                        onSongSelected = { _, _ -> }
                    )
                }
            }
            val active = compose.onNodeWithContentDescription("Album art of Depth 1").assertIsDisplayed()
            val bounds = active.getUnclippedBoundsInRoot()
            assertTrue(bounds.right - bounds.left in 140.dp..148.dp)
        } finally {
            cover.delete()
        }
    }

    private fun createCover(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("depth-halo", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private fun capture(): ImageBitmap = compose.onNodeWithTag("depth-root").captureToImage()

    private fun pixel(image: ImageBitmap, x: Int, y: Int): Color {
        val density = image.width / 200f
        return image.toPixelMap()[(x * density).roundToInt(), (y * density).roundToInt()]
    }
}
