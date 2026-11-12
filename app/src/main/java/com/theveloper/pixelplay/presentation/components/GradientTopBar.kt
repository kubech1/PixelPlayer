package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.PixelPlayStatusBarStyle
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion
import com.theveloper.pixelplay.ui.theme.pillSurface
import com.theveloper.pixelplay.ui.theme.rememberPillSurfaceColors
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreGradientTopBar(
    title: String,
    startColor: Color,
    endColor: Color,
    contentColor: Color,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigationIconClick: () -> Unit,
) {
    val gradientBrush = remember(startColor, endColor) {
        Brush.verticalGradient(colors = listOf(startColor, endColor))
    }

    PixelPlayStatusBarStyle(color = startColor)

    LargeTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = title,
                color = contentColor,
                fontFamily = GoogleSansRounded
            )
        },
        expandedHeight = 160.dp,
        modifier = Modifier.background(brush = gradientBrush),
        navigationIcon = {
            IconButton(
                modifier = Modifier.padding(start = 10.dp),
                onClick = onNavigationIconClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = contentColor
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = startColor
                )
            }
        },
        colors = topAppBarColors(
            containerColor = Color.Transparent, // Background is handled by the gradient brush
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, // Or a color that contrasts well with your typical gradient
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer // Same as title
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeGradientTopBar(
    onNavigationIconClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    isScrolled: Boolean = false,
) {
    val pillColors = rememberPillSurfaceColors()
    val motionEnabled = LocalMotionPreferences.current.animationsEnabled
    val pillProgress by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = if (motionEnabled) PixelMotion.structural() else snap(),
        label = "Home top bar pill"
    )
    val background = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    val compact = pillProgress >= 0.5f
    val titleSize = lerp(34f, 22f, pillProgress)
    PixelPlayStatusBarStyle(color = background)

    Layout(
        modifier = Modifier.fillMaxWidth().zIndex(5f)
            .drawWithCache {
                val scrim = Brush.verticalGradient(
                    0f to background,
                    0.45f to background,
                    1f to background.copy(alpha = 0f),
                )
                onDrawBehind { drawRect(scrim, alpha = pillProgress) }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        content = {
            Box(Modifier.graphicsLayer { alpha = pillProgress }.pillSurface(CircleShape, pillColors))
            Text(
                text = stringResource(R.string.home_title),
                fontFamily = GoogleSansRounded,
                fontSize = titleSize.sp,
                lineHeight = (titleSize + 6f).sp,
                fontWeight = FontWeight.Bold,
                color = if (compact) pillColors.contentColor else onSurface,
                modifier = Modifier.semantics { heading() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledIconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (compact) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (compact) pillColors.contentColor else onSurface
                    ),
                    modifier = Modifier.size(48.dp),
                    onClick = onTelegramClick
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = stringResource(R.string.topbar_cd_cloud_streaming)
                    )
                }
                FilledIconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (compact) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (compact) pillColors.contentColor else onSurface
                    ),
                    modifier = Modifier.size(48.dp),
                    onClick = onNavigationIconClick
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_settings_24),
                        contentDescription = stringResource(R.string.common_settings)
                    )
                }
            }
            Box(Modifier.graphicsLayer { alpha = pillProgress }.pillSurface(CircleShape, pillColors))
        }
    ) { (titleSurface, title, actions, actionsSurface), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val margin = minOf(16.dp.roundToPx(), width / 2)
        val gap = 8.dp.roundToPx()
        val available = (width - 2 * margin).coerceAtLeast(0)
        val actionsPlaceable = actions.measure(Constraints(maxWidth = available, maxHeight = height))
        val titleSpace = (available - actionsPlaceable.width - gap).coerceAtLeast(0)
        val labelInsets = minOf(20.dp.roundToPx(), titleSpace / 3)
        val titlePlaceable = title.measure(Constraints(maxWidth = (titleSpace - labelInsets).coerceAtLeast(0), maxHeight = height))
        val rowHeight = maxOf(titlePlaceable.height, actionsPlaceable.height, 48.dp.roundToPx())
        val titlePillWidth = titlePlaceable.width + labelInsets
        val titleSurfacePlaceable = titleSurface.measure(Constraints.fixed(titlePillWidth, rowHeight))
        val actionsSurfacePlaceable = actionsSurface.measure(Constraints.fixed(actionsPlaceable.width, rowHeight))
        layout(width, constraints.constrainHeight(rowHeight + 16.dp.roundToPx())) {
            val progress = pillProgress
            val rowTop = lerp(4.dp.toPx(), 8.dp.toPx(), progress).roundToInt()
            val expandedTitleX = margin + 4.dp.roundToPx()
            val compactTitleX = margin + labelInsets / 2
            val titleX = lerp(expandedTitleX.toFloat(), compactTitleX.toFloat(), progress).roundToInt()
            val expandedActionsX = width - margin - actionsPlaceable.width
            val actionsX = expandedActionsX
            titleSurfacePlaceable.placeRelative(margin, rowTop)
            actionsSurfacePlaceable.placeRelative(actionsX, rowTop)
            titlePlaceable.placeRelative(titleX, (rowHeight - titlePlaceable.height) / 2 + rowTop)
            actionsPlaceable.placeRelative(actionsX, (rowHeight - actionsPlaceable.height) / 2 + rowTop)
        }
    }
}
