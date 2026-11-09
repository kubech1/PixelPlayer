package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.scoped.ArtworkEndpoint
import com.theveloper.pixelplay.presentation.components.scoped.PlayerArtworkIdentity
import com.theveloper.pixelplay.presentation.components.scoped.capturePlayerArtwork
import com.theveloper.pixelplay.presentation.components.scoped.rememberPlayerArtworkTransition
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class PlayerArtworkTransitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun movesArtworkAndReversesWithoutRecomposingContent() {
        val progress = mutableFloatStateOf(0f)
        val count = AtomicInteger()
        setPlayer(progress = { progress.floatValue }, compositions = count)

        assertPixel(20, 140, Color.Red)
        val initialCompositions = count.get()
        compose.runOnIdle { progress.floatValue = 0.5f }
        assertPixel(85, 100, Color.Red)
        assertPixel(20, 140, Color.Black)
        assertPixel(155, 45, Color.Black)

        compose.runOnIdle { progress.floatValue = 0.25f }
        assertPixel(45, 125, Color.Red)
        compose.runOnIdle { progress.floatValue = 0f }
        assertPixel(20, 140, Color.Red)
        assertEquals(initialCompositions, count.get())
    }

    @Test fun keepsOriginalArtworkWhenCarouselAppearsAfterGestureStarts() {
        val progress = mutableFloatStateOf(0f)
        val showCarousel = mutableStateOf(false)
        setPlayer(progress = { progress.floatValue }, showCarousel = { showCarousel.value })

        compose.runOnIdle { progress.floatValue = 0.5f }
        assertPixel(20, 140, Color.Red)
        compose.runOnIdle { showCarousel.value = true }
        assertPixel(20, 140, Color.Red)
        assertPixel(85, 100, Color.Black)
    }

    private fun setPlayer(
        progress: () -> Float,
        showCarousel: () -> Boolean = { true },
        compositions: AtomicInteger = AtomicInteger()
    ) {
        val song = Song.emptySong()
        compose.setContent {
            val transition = requireNotNull(rememberPlayerArtworkTransition(
                identity = PlayerArtworkIdentity(song.id, song.albumArtUriString, queueIndex = 0),
                enabled = true,
                progress = progress,
                miniOffset = { Offset.Zero },
                carouselOffset = { Offset.Zero }
            ))
            Box(
                Modifier.size(220.dp, 180.dp).testTag("artwork-root").background(Color.Black)
                    .onPlaced { transition.sheetViewport = it }
            ) {
                val miniCapture = Modifier.capturePlayerArtwork(transition, song, ArtworkEndpoint.MINI)
                Box(Modifier.offset(10.dp, 130.dp).size(20.dp).then(miniCapture).background(Color.Red))
                if (showCarousel()) {
                    Box(Modifier.matchParentSize().onPlaced { transition.fullPlayerViewport = it }) {
                        SideEffect { compositions.incrementAndGet() }
                        val capture = Modifier.capturePlayerArtwork(transition, song, ArtworkEndpoint.CAROUSEL)
                        Box(Modifier.offset(100.dp, 10.dp).size(100.dp).then(capture).background(Color.Red))
                    }
                }
                Box(Modifier.matchParentSize().drawBehind { transition.draw(this) })
            }
        }
    }

    private fun assertPixel(x: Int, y: Int, expected: Color) {
        val image = compose.onNodeWithTag("artwork-root").captureToImage()
        val scale = image.width / 220f
        assertEquals(expected, image.toPixelMap()[(x * scale).roundToInt(), (y * scale).roundToInt()])
    }
}
