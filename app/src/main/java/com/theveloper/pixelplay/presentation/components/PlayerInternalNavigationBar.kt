package com.theveloper.pixelplay.presentation.components

import com.theveloper.pixelplay.presentation.components.bottomchrome.continuousPillGradient
import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.lerp
import androidx.navigation.NavHostController
import com.theveloper.pixelplay.BottomNavItem
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.bottomchrome.LocalBottomChromeState
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.navigation.navigateToTopLevelSafely
import com.theveloper.pixelplay.ui.theme.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Some OEM freeform/floating-window modes can report a bottom inset close to the whole window height.
internal val MaxNavigationBarBottomInset = 96.dp
internal val NavigationPillHeight = 56.dp
internal val NavigationPillLabelLineHeight = 16.sp

private val NavigationPillEdge = 4.dp
private val NavigationTabMinWidth = 76.dp
private val NavigationTabTouchSize = 48.dp
private val MergedNavigationSize = 56.dp
private val IconOnlyNavigationWidth = 184.dp

internal fun sanitizeNavigationBarBottomInset(systemNavBarInset: Dp): Dp {
    if (!systemNavBarInset.value.isFinite()) return 0.dp
    return systemNavBarInset.coerceIn(0.dp, MaxNavigationBarBottomInset)
}

internal fun calculatePlayerSheetCollapsedTargetY(
    containerHeightPx: Float,
    collapsedContentHeightPx: Float,
    bottomMarginPx: Float,
    bottomSpacerPx: Float
): Float {
    val safeContainerHeightPx = containerHeightPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeCollapsedContentHeightPx = collapsedContentHeightPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeBottomMarginPx = bottomMarginPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeBottomSpacerPx = bottomSpacerPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val maxTargetY = (safeContainerHeightPx - safeCollapsedContentHeightPx).coerceAtLeast(0f)

    return (safeContainerHeightPx - safeCollapsedContentHeightPx - safeBottomMarginPx - safeBottomSpacerPx)
        .coerceIn(0f, maxTargetY)
}

@Composable
fun PlayerInternalNavigationBar(
    navController: NavHostController,
    navItems: ImmutableList<BottomNavItem>,
    currentRoute: String?,
    modifier: Modifier = Modifier,
    hasSong: Boolean,
    mergeFractionProvider: () -> Float,
    onSearchIconDoubleTap: () -> Unit = {},
    albumTint: androidx.compose.ui.graphics.Color? = null
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val chrome = LocalBottomChromeState.current
    val scope = rememberCoroutineScope()
    val searchDoubleTap by rememberUpdatedState(onSearchIconDoubleTap)
    val animations = LocalMotionPreferences.current.animationsEnabled
    val selectedIndex = navItems.indexOfFirst { it.screen.route == currentRoute }.coerceAtLeast(0)
    val left = remember { Animatable(selectedIndex.toFloat()) }
    val right = remember { Animatable(selectedIndex + 1f) }
    LaunchedEffect(selectedIndex, animations) {
        val forward = selectedIndex >= left.value
        launch { left.animateTo(selectedIndex.toFloat(), if (animations) spring(0.75f, if (forward) 400f else 800f) else snap()) }
        launch { right.animateTo(selectedIndex + 1f, if (animations) spring(0.75f, if (forward) 800f else 400f) else snap()) }
    }
    var lastSearchTap by remember { mutableLongStateOf(0L) }
    val labels = navItems.map { stringResource(it.labelResId) }
    val labelStyle = LocalTextStyle.current.copy(
        fontSize = 12.sp,
        lineHeight = NavigationPillLabelLineHeight,
        fontWeight = FontWeight.Medium
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidth = remember(labels, labelStyle, textMeasurer, density) {
        with(density) { labels.maxOf { textMeasurer.measure(it, labelStyle, maxLines = 1).size.width }.toDp() }
    }
    val restingWidth = maxOf(NavigationTabMinWidth, labelWidth + 16.dp) * navItems.size + NavigationPillEdge * 2
    val activeLabel = labels[selectedIndex]
    val showNavigationLabel = stringResource(R.string.show_navigation_current_tab, activeLabel)
    val scheme = MaterialTheme.colorScheme
    val pillColors = resolvePillSurfaceColors(scheme, LocalPixelPlayDarkTheme.current, albumTint,
        com.theveloper.pixelplay.presentation.utils.LocalVisualEffects.current.effectiveLevel)
    BoxWithConstraints(modifier, propagateMinConstraints = true) {
        val availableWidth = maxWidth
        val rest = restingWidth.coerceAtMost(availableWidth)
        val merged = MergedNavigationSize.coerceAtMost(availableWidth)
        val tabsTouchWidth = NavigationTabTouchSize * navItems.size + NavigationPillEdge * 2
        val tabFadeEnd = 0.4f
        val tabsEnd = if (rest > merged) {
            ((rest - tabsTouchWidth) / (rest - merged)).coerceIn(0f, tabFadeEnd)
        } else {
            0f
        }
        val fused by remember(hasSong, mergeFractionProvider, tabsEnd) {
            derivedStateOf {
                val fraction = mergeFractionProvider()
                hasSong && fraction > 0f && fraction >= tabsEnd
            }
        }
        Layout(
            modifier = Modifier.layout { measurable, constraints ->
                val f = mergeFractionProvider().coerceIn(0f, 1f)
                val compact = if (hasSong) merged else IconOnlyNavigationWidth.coerceAtMost(rest)
                val width = lerp(rest.roundToPx(), compact.roundToPx(), f)
                val compactHeight = (if (hasSong) MergedNavigationSize else 52.dp).roundToPx()
                    .coerceAtMost(constraints.maxHeight)
                val height = lerp(constraints.maxHeight, compactHeight, f)
                val child = measurable.measure(Constraints.fixed(width, height))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val centeredStart = (constraints.maxWidth - width) / 2
                    val start = if (hasSong) lerp(centeredStart, 0, f) else centeredStart
                    child.placeRelative(start, constraints.maxHeight - height)
                }
            }.pillSurface(CircleShape, albumTint).clip(CircleShape).continuousPillGradient(mergeFractionProvider)
                .then(if (fused) Modifier.semantics {
                    contentDescription = showNavigationLabel
                }.clickable(role = Role.Button) { chrome?.reset() } else Modifier.selectableGroup()),
            content = {
                Box(Modifier.graphicsLayer {
                    alpha = if (hasSong) 1f - mergeFractionProvider().coerceIn(0f, 1f) else 1f
                }.background(scheme.secondaryContainer, CircleShape))
                navItems.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val label = labels[index]
                    Column(
                        Modifier.graphicsLayer {
                            val f = mergeFractionProvider()
                            alpha = if (hasSong && !selected) (1f - (f - 0.2f) / 0.5f).coerceIn(0f, 1f) else 1f
                            scaleX = 0.8f + 0.2f * alpha
                            scaleY = scaleX
                        }.then(if (fused) Modifier.clearAndSetSemantics {} else Modifier
                            .semantics { contentDescription = label }
                            .selectable(selected, enabled = currentRoute != null, role = Role.Tab) click@{
                            val now = SystemClock.elapsedRealtime()
                            if (selected) chrome?.reselect(item.screen.route)
                            else {
                                if (!navController.navigateToTopLevelSafely(item.screen.route)) {
                                    lastSearchTap = 0L
                                    return@click
                                }
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
                            }
                            if (item.screen.route == Screen.Search.route) {
                                if (lastSearchTap > 0 && now - lastSearchTap <= 350) {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                    lastSearchTap = 0L
                                    if (selected) searchDoubleTap() else scope.launch {
                                        delay(160L)
                                        searchDoubleTap()
                                    }
                                } else lastSearchTap = now
                            } else lastSearchTap = 0
                        }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(painterResource(if (selected && item.selectedIconResId != null && item.selectedIconResId != 0) item.selectedIconResId else item.iconResId), null,
                            tint = when {
                                selected && fused -> pillColors.contentIconColor
                                selected -> pillColors.selectedContentColor
                                else -> pillColors.contentIconColor
                            },
                            modifier = Modifier.size(24.dp))
                        Text(label, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (selected) pillColors.selectedContentColor else pillColors.contentLabelColor,
                            modifier = Modifier.clearAndSetSemantics {}.layout { measurable, constraints ->
                                val child = measurable.measure(constraints.copy(minHeight = 0))
                                val visibility = (1f - mergeFractionProvider() / tabFadeEnd).coerceIn(0f, 1f)
                                layout(child.width, (child.height * visibility).toInt()) {
                                    if (visibility > 0f) child.placeRelativeWithLayer(0, 0) { alpha = visibility; scaleY = visibility }
                                }
                            })
                    }
                }
            }
        ) { measurables, constraints ->
            val f = mergeFractionProvider().coerceIn(0f, 1f)
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val edge = NavigationPillEdge.roundToPx().coerceAtMost(width / 2)
            val itemWidth = (width - 2 * edge) / navItems.size
            val leftPx = (edge + left.value * itemWidth + 2.dp.toPx()).toInt()
            val rightPx = (edge + right.value * itemWidth - 2.dp.toPx()).toInt()
            val restingIndicatorWidth = (rightPx - leftPx).coerceAtLeast(0)
            val compactIndicatorWidth = if (hasSong) width else 48.dp.roundToPx().coerceAtMost(itemWidth)
            val indicatorWidth = androidx.compose.ui.util.lerp(restingIndicatorWidth, compactIndicatorWidth, f)
            val verticalEdge = NavigationPillEdge.roundToPx().coerceAtMost(height / 2)
            val indicator = measurables[0].measure(Constraints.fixed(indicatorWidth, height - 2 * verticalEdge))
            val centerProgress = if (hasSong) ((f - tabsEnd) / (1f - tabsEnd)).coerceIn(0f, 1f) else 0f
            val visualItemWidth = lerp(itemWidth, width, centerProgress)
            val items = measurables.drop(1).map { it.measure(Constraints.fixed(visualItemWidth, height)) }
            layout(width, height) {
                val compactIndicatorStart = if (hasSong) 0 else edge + selectedIndex * itemWidth + (itemWidth - compactIndicatorWidth) / 2
                indicator.placeRelative(lerp(leftPx, compactIndicatorStart, f), verticalEdge)
                items.forEachIndexed { index, child ->
                    child.placeRelative(lerp(edge + index * itemWidth, 0, centerProgress), 0)
                }
            }
        }
    }
}
