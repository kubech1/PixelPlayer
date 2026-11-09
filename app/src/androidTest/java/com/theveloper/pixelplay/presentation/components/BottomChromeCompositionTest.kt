package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.theveloper.pixelplay.BottomNavItem
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.bottomchrome.*
import com.theveloper.pixelplay.presentation.navigation.Screen
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class BottomChromeCompositionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mergingAndSeparatingDoNotRecomposeList() {
        val listCompositions = AtomicInteger()
        lateinit var chrome: BottomChromeState
        compose.setContent {
            chrome = rememberBottomChromeState()
            val metrics = remember(chrome) {
                BottomChromeMetrics(contentBottomPadding = 160.dp, mergeFractionProvider = chrome.mergeFractionProvider)
            }
            MaterialTheme {
                CompositionLocalProvider(LocalBottomChrome provides metrics, LocalBottomChromeState provides chrome) {
                    Box(Modifier.fillMaxSize()) {
                        CountedList(listCompositions)
                        PlayerInternalNavigationBar(
                            navController = rememberNavController(),
                            navItems = persistentListOf(
                                BottomNavItem("Home", R.string.nav_bar_home, R.drawable.rounded_home_24, screen = Screen.Home),
                                BottomNavItem("Search", R.string.nav_bar_search, R.drawable.rounded_search_24, screen = Screen.Search),
                                BottomNavItem("Library", R.string.nav_bar_library, R.drawable.rounded_library_music_24, screen = Screen.Library)
                            ),
                            currentRoute = Screen.Home.route,
                            hasSong = true,
                            mergeFractionProvider = chrome.mergeFractionProvider,
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp)
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val initial = listCompositions.get()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            chrome.nestedScrollConnection.onPostScroll(Offset(0f, -1000f), Offset.Zero, NestedScrollSource.UserInput)
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle {
            assertTrue(chrome.mergeFractionProvider() > 0.9f)
            assertEquals(initial, listCompositions.get())
            chrome.reset()
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle {
            assertTrue(chrome.mergeFractionProvider() < 0.1f)
            assertEquals(initial, listCompositions.get())
        }
    }

    @Composable
    private fun CountedList(count: AtomicInteger) {
        val metrics = LocalBottomChrome.current
        SideEffect { count.incrementAndGet() }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = metrics.contentBottomPadding)) {
            items((0 until 50).toList()) { Text("Synthetic row $it") }
        }
    }
}
