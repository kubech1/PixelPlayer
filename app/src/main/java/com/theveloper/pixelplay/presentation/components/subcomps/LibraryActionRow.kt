package com.theveloper.pixelplay.presentation.components.subcomps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.MusicFolder
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion
import java.io.File

@Composable
fun LibraryActionRow(
    onMainActionClick: () -> Unit,
    iconRotation: Float,
    onSortClick: () -> Unit,
    onLocateClick: () -> Unit = {},
    showSortButton: Boolean,
    showLocateButton: Boolean = false,
    locateEnabled: Boolean = showLocateButton,
    showImportButton: Boolean = true,
    isPlaylistTab: Boolean,
    onImportM3uClick: () -> Unit = {},
    isFoldersTab: Boolean,
    modifier: Modifier = Modifier,
    currentFolder: MusicFolder?,
    folderRootPath: String,
    folderRootLabel: String,
    onFolderClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    compactNavigation: (@Composable () -> Unit)? = null,
    compactNavigationVisible: Boolean = false,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val compactActive = compactNavigation != null && compactNavigationVisible
        val animated = LocalMotionPreferences.current.animationsEnabled
        val showLabels = if (!compactActive) {
            maxWidth >= 285.dp || fontScale <= 1.3f
        } else {
            maxWidth >= 420.dp && fontScale <= 1.15f && !isPlaylistTab
        }
        val showImportLabel = !compactActive && maxWidth >= 315.dp && fontScale <= 1.15f
        LookaheadScope {
        val motionModifier = if (animated) {
            Modifier.animateBounds(this, boundsTransform = BoundsTransform { _, _ -> PixelMotion.structural() })
        } else Modifier
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (compactNavigation != null) {
                AnimatedVisibility(
                    visible = compactActive,
                    modifier = if (compactActive) Modifier.weight(1f) else Modifier,
                    enter = if (animated) {
                        expandHorizontally(animationSpec = PixelMotion.structural()) +
                            fadeIn(animationSpec = tween(PixelMotion.STANDARD_DURATION_MS))
                    } else EnterTransition.None,
                    exit = if (animated) {
                        shrinkHorizontally(animationSpec = PixelMotion.structural()) +
                            fadeOut(animationSpec = tween(PixelMotion.QUICK_DURATION_MS))
                    } else ExitTransition.None,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        compactNavigation()
                    }
                }
            }
            if (isFoldersTab && !compactActive) {
                Row(modifier = Modifier.weight(1f)) {
                    Breadcrumbs(
                        currentFolder = currentFolder,
                        rootPath = folderRootPath,
                        rootLabel = folderRootLabel,
                        onFolderClick = onFolderClick,
                        onNavigateBack = onNavigateBack,
                    )
                }
            } else if (!isFoldersTab) {
                val mainLabel = stringResource(if (isPlaylistTab) R.string.library_action_new else R.string.common_shuffle)
                val mainDescription = stringResource(
                    if (isPlaylistTab) R.string.library_cd_create_new_playlist else R.string.common_shuffle_play
                )
                LibraryPrimaryAction(
                    modifier = motionModifier,
                    label = mainLabel,
                    description = mainDescription,
                    showLabel = showLabels,
                    onClick = onMainActionClick,
                ) {
                    Icon(
                        imageVector = if (isPlaylistTab) Icons.AutoMirrored.Rounded.PlaylistAdd else Icons.Rounded.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                            .rotate(if (LocalMotionPreferences.current.animationsEnabled) iconRotation else 0f),
                    )
                }
                if (isPlaylistTab && showImportButton) {
                    LibraryPrimaryAction(
                        modifier = motionModifier,
                        label = stringResource(R.string.common_import),
                        description = stringResource(R.string.library_cd_import_m3u_playlist),
                        showLabel = showImportLabel,
                        onClick = onImportM3uClick,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_upload_file_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (!compactActive) Spacer(Modifier.weight(1f))
            }
            if (showLocateButton) {
                LibraryCircularAction(
                    modifier = motionModifier,
                    label = stringResource(R.string.library_cd_locate_current_song),
                    enabled = locateEnabled,
                    onClick = onLocateClick,
                    sortIcon = false,
                )
            }
            if (showSortButton) {
                LibraryCircularAction(
                    modifier = motionModifier,
                    label = stringResource(R.string.library_cd_sort_options),
                    onClick = onSortClick,
                    sortIcon = true,
                )
            }
        }
        }
    }
}

@Composable
private fun LibraryPrimaryAction(
    modifier: Modifier = Modifier,
    label: String,
    description: String,
    showLabel: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val animated = LocalMotionPreferences.current.animationsEnabled
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        icon()
        AnimatedVisibility(
            visible = showLabel,
            enter = if (animated) {
                expandHorizontally(animationSpec = PixelMotion.structural()) +
                    fadeIn(animationSpec = tween(PixelMotion.STANDARD_DURATION_MS))
            } else EnterTransition.None,
            exit = if (animated) {
                shrinkHorizontally(animationSpec = PixelMotion.structural()) +
                    fadeOut(animationSpec = tween(PixelMotion.QUICK_DURATION_MS))
            } else ExitTransition.None,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryCircularAction(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    sortIcon: Boolean,
    enabled: Boolean = true,
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            modifier = Modifier.size(48.dp).semantics { contentDescription = label },
        ) {
            val color = androidx.compose.material3.LocalContentColor.current
            Canvas(Modifier.size(24.dp)) {
                val unit = size.width / 24f
                fun segment(x1: Int, y1: Int, x2: Int, y2: Int) = drawLine(
                    color,
                    Offset(x1 * unit, y1 * unit),
                    Offset(x2 * unit, y2 * unit),
                    strokeWidth = 1.8f * unit,
                    cap = StrokeCap.Round,
                )
                if (sortIcon) {
                    segment(5, 5, 5, 18)
                    segment(2, 15, 5, 18)
                    segment(5, 18, 8, 15)
                    segment(11, 6, 22, 6)
                    segment(11, 12, 19, 12)
                    segment(11, 18, 16, 18)
                } else {
                    drawCircle(color, radius = 4f * unit)
                }
            }
        }
    }
}

@Composable
fun Breadcrumbs(
    currentFolder: MusicFolder?,
    rootPath: String,
    rootLabel: String,
    onFolderClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val rowState = rememberLazyListState()
    val pathSegments = remember(currentFolder?.path, rootPath, rootLabel) {
        val path = currentFolder?.path ?: rootPath
        val normalizedRoot = rootPath.removeSuffix("/")
        val normalizedPath = path.removeSuffix("/")
        val relativePath = normalizedPath
            .removePrefix(normalizedRoot)
            .removePrefix("/")

        if (!normalizedPath.startsWith(normalizedRoot) || relativePath.isEmpty() || normalizedPath == normalizedRoot) {
            listOf(rootLabel to rootPath)
        } else {
            listOf(rootLabel to rootPath) + relativePath.split("/").scan("") { acc, segment ->
                "$acc/$segment"
            }.drop(1).map {
                val file = File(rootPath, it)
                file.name to file.path
            }
        }
    }

    val showStartFade by remember { derivedStateOf { rowState.canScrollBackward } }
    val showEndFade by remember { derivedStateOf { rowState.canScrollForward } }

    LaunchedEffect(pathSegments.size) {
        if (pathSegments.isNotEmpty()) {
            rowState.animateScrollToItem(pathSegments.lastIndex + 1)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(36.dp),
            enabled = currentFolder != null
        ) {
            val icon = if (currentFolder == null) Icons.Rounded.Home else Icons.AutoMirrored.Rounded.ArrowBack
            Icon(
                imageVector = icon,
                contentDescription = stringResource(if (currentFolder == null) R.string.nav_bar_home else R.string.common_back)
            )
        }
        Spacer(Modifier.width(8.dp))

        LazyRow(
            state = rowState,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                // 1. Forzamos que el contenido se dibuje en una capa separada.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    // 2. Dibujamos el contenido original (el LazyRow).
                    drawContent()

                    // 3. Dibujamos los gradientes que actúan como "máscaras de borrado".
                    val gradientWidth = 24.dp.toPx()

                    // Máscara para el borde IZQUIERDO
                    if (showStartFade) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                // Gradiente de transparente a opaco (negro)
                                colors = listOf(Color.Transparent, Color.Black),
                                endX = gradientWidth
                            ),
                            // DstIn mantiene el contenido del LazyRow solo donde esta capa es opaca.
                            blendMode = BlendMode.DstIn
                        )
                    }

                    // Máscara para el borde DERECHO
                    if (showEndFade) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                // Gradiente de opaco (negro) a transparente
                                colors = listOf(Color.Black, Color.Transparent),
                                startX = this.size.width - gradientWidth
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            item { Spacer(modifier = Modifier.width(12.dp)) }

            items(pathSegments.size, key = { pathSegments[it].second }) { index ->
                val (name, path) = pathSegments[index]
                val isLast = index == pathSegments.lastIndex
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = GoogleSansRounded,
                    color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !isLast) { onFolderClick(path) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                if (!isLast) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.width(12.dp)) }
        }
    }
}
