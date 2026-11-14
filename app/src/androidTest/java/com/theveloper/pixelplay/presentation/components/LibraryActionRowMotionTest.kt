package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.subcomps.LibraryActionRow
import com.theveloper.pixelplay.presentation.screens.CompactLibraryTabButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryActionRowMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun shuffleMovesThroughIntermediatePositionsWhenTheTabPillAppears() {
        val compact = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(326.dp)) {
                    LibraryActionRow(
                        onMainActionClick = {}, iconRotation = 0f, onSortClick = {},
                        showSortButton = true, showLocateButton = true,
                        isPlaylistTab = false, isFoldersTab = false, currentFolder = null,
                        folderRootPath = "", folderRootLabel = "", onFolderClick = {},
                        onNavigateBack = {}, compactNavigationVisible = compact.value,
                        compactNavigation = { CompactLibraryTabButton("Canciones") {} },
                    )
                }
            }
        }
        val label = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.common_shuffle_play)
        val shuffle = compose.onNodeWithContentDescription(label)
        val start = shuffle.assertIsDisplayed().getUnclippedBoundsInRoot().left
        compose.runOnIdle { compact.value = true }
        compose.mainClock.advanceTimeBy(80)
        val middle = shuffle.assertIsDisplayed().getUnclippedBoundsInRoot().left
        compose.mainClock.advanceTimeBy(1500)
        val end = shuffle.assertIsDisplayed().getUnclippedBoundsInRoot().left
        assertTrue("Shuffle should move right", end > start)
        assertTrue("Shuffle should pass through an intermediate position", middle > start && middle < end)

        compose.runOnIdle { compact.value = false }
        compose.mainClock.advanceTimeBy(80)
        val returnMiddle = shuffle.assertIsDisplayed().getUnclippedBoundsInRoot().left
        compose.mainClock.advanceTimeBy(1500)
        val returnEnd = shuffle.assertIsDisplayed().getUnclippedBoundsInRoot().left
        assertTrue("Shuffle should return to start", returnEnd <= start + 1.dp)
        assertTrue("Shuffle should pass through an intermediate position on return", returnMiddle > returnEnd && returnMiddle < end)
    }

    @Test
    fun compactTabRemainsReadableAndClickableAtNormalAndLargeTextSizes() {
        val compact = mutableStateOf(false)
        val fontScale = mutableStateOf(1f)
        var tabClicks = 0
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale.value)) {
                    Box(Modifier.width(326.dp)) {
                        LibraryActionRow(
                            onMainActionClick = {}, iconRotation = 0f, onSortClick = {},
                            showSortButton = true, showLocateButton = true,
                            isPlaylistTab = false, isFoldersTab = false, currentFolder = null,
                            folderRootPath = "", folderRootLabel = "", onFolderClick = {},
                            onNavigateBack = {}, compactNavigationVisible = compact.value,
                            compactNavigation = {
                                CompactLibraryTabButton("Canciones") { tabClicks++ }
                            },
                        )
                    }
                }
            }
        }
        compose.runOnIdle { compact.value = true }
        for (scale in listOf(1f, 2f)) {
            compose.runOnIdle { fontScale.value = scale }
            val tab = compose.onNodeWithText("Canciones").assertIsDisplayed()
            val bounds = tab.getUnclippedBoundsInRoot()
            assertTrue("The tab label must have room to be recognized", bounds.right - bounds.left > 40.dp)
            tab.performClick()
        }
        assertEquals(2, tabClicks)
    }
}
