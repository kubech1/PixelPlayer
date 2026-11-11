package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.MotionPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun celebratesOnlyLocalAdditionAndClearsOnRemovalOrTrackChange() {
        val song = mutableStateOf("one")
        val favorite = mutableStateOf(true)
        val animations = mutableStateOf(true)
        var clicks = 0
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color.Red, tertiary = Color.Red)) {
                CompositionLocalProvider(LocalMotionPreferences provides MotionPreferences(animations.value)) {
                    Box(Modifier.size(132.dp).background(Color.Black).testTag("favorite-root")) {
                        FavoriteButton(
                            songId = song.value,
                            isFavorite = favorite.value,
                            onToggle = {
                                clicks++
                                favorite.value = !favorite.value
                            },
                            containerColor = Color.Gray,
                            contentColor = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(48.dp).testTag("favorite-button"),
                            style = FavoriteButtonStyle.Full
                        )
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        val button = compose.onNodeWithTag("favorite-button")
        button.assertIsOn()
        assertEquals(0, burstPixels())
        compose.runOnIdle { favorite.value = false }
        compose.mainClock.advanceTimeBy(160)
        button.assertIsOff()
        assertEquals(0, burstPixels())
        compose.runOnIdle { favorite.value = true }
        compose.mainClock.advanceTimeBy(160)
        assertEquals(0, burstPixels())
        button.performClick()
        compose.mainClock.advanceTimeBy(160)
        button.assertIsOff()
        assertEquals(0, burstPixels())
        button.performClick()
        compose.mainClock.advanceTimeBy(210)
        button.assertIsOn()
        assertTrue("The celebration extends beyond the button", burstPixels() > 0)
        button.performClick()
        compose.mainClock.advanceTimeBy(48)
        assertEquals(0, burstPixels())
        button.performClick()
        compose.mainClock.advanceTimeBy(210)
        assertTrue(burstPixels() > 0)
        compose.runOnIdle { song.value = "two" }
        compose.mainClock.advanceTimeBy(48)
        button.assertIsOn()
        assertEquals(0, burstPixels())
        compose.runOnIdle { animations.value = false }
        compose.mainClock.advanceTimeBy(48)
        button.performClick()
        compose.mainClock.advanceTimeBy(48)
        button.performClick()
        compose.mainClock.advanceTimeBy(210)
        button.assertIsOn()
        assertEquals(0, burstPixels())
        assertEquals(6, clicks)
    }

    private fun burstPixels(): Int {
        val image = compose.onNodeWithTag("favorite-root").captureToImage()
        val pixels = image.toPixelMap()
        val center = image.width / 2f
        val buttonRadius = image.width * 24f / 132f
        var red = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val dx = x - center
                val dy = y - center
                if (dx * dx + dy * dy <= buttonRadius * buttonRadius) continue
                val color = pixels[x, y]
                if (color.red > 0.10f && color.red - color.green > 0.08f) red++
            }
        }
        return red
    }
}
