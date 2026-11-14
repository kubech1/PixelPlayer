package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.theveloper.pixelplay.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollapsibleCommonTopBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun titleMovesAndShrinksIntoThePillAsTheHeaderCollapses() {
        val collapse = mutableFloatStateOf(0f)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(360.dp, 200.dp)) {
                    val compactHeight = collapsedTopBarHeight()
                    CollapsibleCommonTopBar(
                        title = "Ajustes",
                        collapseFraction = { collapse.floatValue },
                        headerHeight = androidx.compose.ui.unit.lerp(180.dp, compactHeight, collapse.floatValue),
                        onBackClick = {},
                        syncStatusBarWithContainer = false,
                    )
                }
            }
        }
        val title = compose.onNodeWithText("Ajustes")
        val expanded = title.assertIsDisplayed().getUnclippedBoundsInRoot()
        compose.runOnIdle { collapse.floatValue = 0.5f }
        val halfway = title.assertIsDisplayed().getUnclippedBoundsInRoot()
        compose.runOnIdle { collapse.floatValue = 1f }
        val collapsed = title.assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("The title should travel upward", expanded.top > halfway.top && halfway.top > collapsed.top)
        assertTrue("The title should shrink", expanded.right - expanded.left > collapsed.right - collapsed.left)
    }

    @Test
    fun keepsBackAndActionUsableAtDoubleFontScaleInNarrowRtlLayout() {
        val collapse = mutableFloatStateOf(0f)
        val title = mutableStateOf("A very long header title")
        var backs = 0
        var actions = 0
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    Box(Modifier.size(240.dp, 180.dp)) {
                        CollapsibleCommonTopBar(
                            title = title.value,
                            collapseFraction = { collapse.floatValue },
                            headerHeight = if (collapse.floatValue == 1f) collapsedTopBarHeight() else 180.dp,
                            onBackClick = { backs++ },
                            syncStatusBarWithContainer = false,
                            actions = {
                                IconButton(onClick = { actions++ }, modifier = Modifier.testTag("action")) {
                                    Text("+")
                                }
                            }
                        )
                    }
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val back = compose.onNodeWithContentDescription(context.getString(R.string.common_back))
        val action = compose.onNodeWithTag("action")
        for (progress in listOf(0f, 0.75f, 1f)) {
            compose.runOnIdle { collapse.floatValue = progress }
            val backBounds = back.assertIsDisplayed().getUnclippedBoundsInRoot()
            val actionBounds = action.assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(backBounds.right - backBounds.left >= 47.9.dp)
            assertTrue(backBounds.bottom - backBounds.top >= 47.9.dp)
            assertTrue(actionBounds.right <= backBounds.left)
        }
        val longTitleAction = action.getUnclippedBoundsInRoot()
        compose.runOnIdle { title.value = "Hi" }
        val shortTitleAction = action.assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(shortTitleAction.left > longTitleAction.left + 20.dp)
        back.performClick()
        action.performClick()
        assertEquals(1, backs)
        assertEquals(1, actions)
    }
}
