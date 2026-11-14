package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelPlayStatusBarStyle
import com.theveloper.pixelplay.ui.theme.pillSurface
import com.theveloper.pixelplay.ui.theme.rememberPillSurfaceColors
import kotlin.math.roundToInt

private val TopBarGap = 8.dp
private val TopBarMargin = 16.dp
private val TopBarTouchTarget = 48.dp
private val TopBarMaxPillWidth = 360.dp

@Composable
private fun compactTopBarHeight(): Dp {
    val lineHeight = MaterialTheme.typography.titleMedium.lineHeight
    return with(LocalDensity.current) { maxOf(TopBarTouchTarget, lineHeight.toDp() + 12.dp) }
}

@Composable
internal fun collapsedTopBarHeight(): Dp = with(LocalDensity.current) {
    WindowInsets.safeDrawing.getTop(this).toDp() + compactTopBarHeight() + TopBarGap * 2
}

@Composable
internal fun rememberCollapsibleTopBarHeight(minimum: Float, maximum: Float): Animatable<Float, *> {
    val height = remember { Animatable(maximum) }
    val previous = remember { floatArrayOf(minimum, maximum) }
    LaunchedEffect(minimum, maximum) {
        if (minimum != previous[0] || maximum != previous[1]) {
            val expanded = ((height.value - previous[0]) / (previous[1] - previous[0]))
                .coerceIn(0f, 1f)
            previous[0] = minimum
            previous[1] = maximum
            height.snapTo(lerp(minimum, maximum, expanded))
        }
    }
    return height
}

internal fun topBarPillProgress(collapse: Float, animated: Boolean): Float {
    return if (animated) ((collapse - 0.6f) / (1f - 0.6f)).coerceIn(0f, 1f)
    else if (collapse >= 0.8f) 1f else 0f
}

@Composable
fun CollapsibleCommonTopBar(
    title: String,
    collapseFraction: () -> Float,
    headerHeight: Dp,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    expandedTitleStartPadding: Dp = 20.dp,
    expandedTitleEndPadding: Dp = 24.dp,
    expandedTitleHeight: Dp = 88.dp,
    titleStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    expandedTitleScale: Float = 1.2f,
    expandedTitleFontSize: TextUnit = titleStyle.fontSize,
    maxLines: Int = 1,
    expandedSubtitleMaxLines: Int = 1,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enableExpandedTitleWidthCompression: Boolean = true,
    titleWidthCompressionThreshold: Dp? = null,
    titleMinWidthAxis: Float = 78f,
    syncStatusBarWithContainer: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = rememberPillSurfaceColors()
    val animated = LocalMotionPreferences.current.animationsEnabled
    val latestCollapse by rememberUpdatedState(collapseFraction)
    val compact by remember(animated) {
        derivedStateOf { topBarPillProgress(latestCollapse(), animated) >= 0.5f }
    }
    val height = compactTopBarHeight()
    val density = LocalDensity.current
    var actionsWidthPx by remember { mutableIntStateOf(0) }
    val compactFontSize = MaterialTheme.typography.titleMedium.fontSize
    val compactFont = rememberGoogleSansFlexFontFamily(FontWeight.Bold, 100f)
    if (syncStatusBarWithContainer) {
        PixelPlayStatusBarStyle(color = MaterialTheme.colorScheme.surface)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight.coerceAtLeast(collapsedTopBarHeight()))
            .zIndex(5f)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        val pillProgress = topBarPillProgress(latestCollapse(), animated)
        Box(
            Modifier.fillMaxSize().zIndex(1f).offset {
                IntOffset(0, (TopBarGap.toPx() * pillProgress).roundToInt())
            }
        ) {
            ExpressiveTopBarContent(
                title = title,
                collapseFraction = if (animated) latestCollapse().coerceIn(0f, 1f) else if (compact) 1f else 0f,
                subtitle = subtitle,
                expandedTitleStartPadding = expandedTitleStartPadding,
                expandedTitleEndPadding = expandedTitleEndPadding,
                collapsedTitleStartPadding = TopBarMargin + TopBarTouchTarget,
                collapsedTitleEndPadding = with(density) { actionsWidthPx.toDp() } + TopBarMargin * 2 + TopBarGap,
                containerHeightRange = expandedTitleHeight to height,
                titleStyle = titleStyle,
                titleScaleRange = 1f to 1f,
                titleFontSizeRange = (expandedTitleFontSize * expandedTitleScale) to compactFontSize,
                maxLines = if (compact) 1 else maxLines,
                expandedSubtitleMaxLines = expandedSubtitleMaxLines,
                contentColor = lerpColor(contentColor, colors.contentColor, pillProgress),
                subtitleColor = subtitleColor,
                enableExpandedTitleWidthCompression = enableExpandedTitleWidthCompression,
                titleWidthCompressionThreshold = titleWidthCompressionThreshold,
                titleMinWidthAxis = titleMinWidthAxis,
                supportingContent = supportingContent
            )
        }

        Layout(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TopBarMargin),
            content = {
                Box(
                    Modifier.graphicsLayer {
                        alpha = topBarPillProgress(latestCollapse(), animated)
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }.pillSurface(CircleShape, colors)
                )
                FilledIconButton(
                    modifier = Modifier.size(TopBarTouchTarget),
                    onClick = onBackClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = lerpColor(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            Color.Transparent,
                            pillProgress
                        ),
                        contentColor = lerpColor(
                            MaterialTheme.colorScheme.onSurface,
                            colors.contentColor,
                            pillProgress
                        )
                    )
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back))
                }
                Text(
                    text = title,
                    modifier = Modifier.graphicsLayer { alpha = 0f }
                        .clearAndSetSemantics { hideFromAccessibility() },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = compactFont,
                        fontWeight = FontWeight.Bold,
                        lineHeight = compactFontSize * 1.1f,
                    ),
                    color = colors.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.onSizeChanged { actionsWidthPx = it.width },
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        ) { (surface, back, label, trailing), constraints ->
            val available = constraints.maxWidth
            val maxPill = minOf(available, TopBarMaxPillWidth.roundToPx())
            val backButton = back.measure(Constraints(maxWidth = maxPill, maxHeight = constraints.maxHeight))
            val afterBack = maxPill - backButton.width
            val buttons = trailing.measure(Constraints(maxWidth = afterBack, maxHeight = constraints.maxHeight))
            val spaceForTitle = afterBack - buttons.width
            val endPadding = minOf((if (buttons.width == 0) 12.dp else 8.dp).roundToPx(), spaceForTitle)
            val actionGap = if (buttons.width > 0) minOf(8.dp.roundToPx(), spaceForTitle - endPadding) else 0
            val titleLabel = label.measure(
                Constraints(maxWidth = spaceForTitle - endPadding - actionGap, maxHeight = constraints.maxHeight)
            )
            val rowHeight = maxOf(height.roundToPx(), backButton.height, titleLabel.height, buttons.height)
            val pillWidth = backButton.width + titleLabel.width + actionGap + buttons.width + endPadding
            val pill = surface.measure(Constraints.fixed(pillWidth, rowHeight))
            val top = TopBarGap.roundToPx()
            layout(available, constraints.constrainHeight(top + rowHeight)) {
                val progress = topBarPillProgress(latestCollapse(), animated)
                val rowY = lerp(4.dp.toPx(), top.toFloat(), progress).roundToInt()
                pill.placeRelative(0, top)
                backButton.placeRelative(0, rowY + (rowHeight - backButton.height) / 2)
                titleLabel.placeRelative(backButton.width, rowY + (rowHeight - titleLabel.height) / 2)
                // Keep the actions at the trailing edge until the pill starts to appear.
                val actionsX = lerp(available - buttons.width, pillWidth - endPadding - buttons.width, progress)
                buttons.placeRelative(actionsX, rowY + (rowHeight - buttons.height) / 2)
            }
        }
    }
}
