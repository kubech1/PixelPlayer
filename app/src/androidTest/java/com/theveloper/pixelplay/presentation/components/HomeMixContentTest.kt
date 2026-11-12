package com.theveloper.pixelplay.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.HomeMix
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.data.model.LibraryTabId
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.MotionPreferences
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeMixContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun quickAccessKeepsBothDestinationsClickableAtLargeTextScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val lastAlbum = context.getString(R.string.home_quick_recent_album)
        val favorites = context.getString(R.string.library_tab_liked)
        val albumRoute = Screen.AlbumDetail.createRoute(42)
        val favoritesRoute = Screen.Library.createRoute(LibraryTabId.LIKED)
        val opened = mutableListOf<String>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    HomeQuickAccess(
                        listOf(
                            HomeQuickLink(lastAlbum, albumRoute, R.drawable.rounded_album_24, "Fixture album"),
                            HomeQuickLink(favorites, favoritesRoute, R.drawable.rounded_favorite_24),
                        ),
                        onOpen = { opened.add(it) },
                    )
                }
            }
        }

        compose.onNodeWithText(lastAlbum).assertHasClickAction().performClick()
        compose.onNodeWithText(favorites).assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(listOf(albumRoute, favoritesRoute), opened) }
    }

    @Test
    fun mixActionsOpenAndPlayIndependentlyAtLargeTextScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dailyTitle = context.getString(R.string.home_mix_daily)
        val playLabel = context.getString(R.string.home_mix_play, dailyTitle)
        val rotatedTitle = context.getString(R.string.home_mix_rediscover)
        val song = Song.emptySong().copy(id = "fixture", title = "Fixture")
        val daily = HomeMix("daily", HomeMixKind.DAILY, songs = persistentListOf(song), cover = null)
        val rotation = daily.copy(id = "rediscover", kind = HomeMixKind.REDISCOVER)
        val opened = mutableListOf<String>()
        val played = mutableListOf<String>()

        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalMotionPreferences provides MotionPreferences(animationsEnabled = false)
            ) {
                MaterialTheme {
                    HomeMixCarousel(
                        mixes = listOf(daily, rotation),
                        onOpen = { opened.add(it.id) },
                        onPlay = { mix, _ -> played.add(mix.id) },
                    )
                }
            }
        }

        compose.onNodeWithText(dailyTitle).assertHasClickAction().performClick()
        compose.onNodeWithContentDescription(playLabel).assertHasClickAction().performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(rotatedTitle))
        compose.onNodeWithText(rotatedTitle).assertHasClickAction().performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.home_mix_play, rotatedTitle))
            .assertHasClickAction().performClick()
        compose.runOnIdle {
            assertEquals(listOf(daily.id, rotation.id), opened)
            assertEquals(listOf(daily.id, rotation.id), played)
        }
    }
}
