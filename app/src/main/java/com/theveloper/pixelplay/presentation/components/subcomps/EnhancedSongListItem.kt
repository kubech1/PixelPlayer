package com.theveloper.pixelplay.presentation.components.subcomps

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import coil.size.Size
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.AutoScrollingText
import com.theveloper.pixelplay.presentation.components.ShimmerBox
import androidx.compose.ui.res.stringResource
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.SmartImage
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.MIN_LABEL_CONTRAST_RATIO
import com.theveloper.pixelplay.ui.theme.PixelMotion
import com.theveloper.pixelplay.ui.theme.calculateWcagContrastRatio

@Immutable
private data class EnhancedSongAnimationTarget(
    val isHighlighted: Boolean = false,
    val isSelected: Boolean = false
)

private val DefaultRowShape = RoundedCornerShape(22.dp)
private val DefaultAlbumShape = RoundedCornerShape(10.dp)

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

/**
 * Enhanced song list item with multi-selection support.
 * 
 * @param song The song to display
 * @param isPlaying Whether this song is currently playing
 * @param isCurrentSong Whether this is the current song in the queue (may be paused)
 * @param isLoading Whether to show loading shimmer state
 * @param showAlbumArt Whether to show the album art
 * @param albumArtSize Size of the album art thumbnail when shown
 * @param customShape Optional custom shape for the surface
 * @param isSelected Whether this item is selected in multi-selection mode
 * @param isSelectionMode Whether multi-selection mode is active
 * @param onLongPress Callback for long press gesture (activates selection)
 * @param onMoreOptionsClick Callback for more options button
 * @param onClick Callback for tap gesture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSongListItem(
    modifier: Modifier = Modifier,
    song: Song,
    isPlaying: Boolean,
    isCurrentSong: Boolean = false,
    isLoading: Boolean = false,
    showAlbumArt: Boolean = true,
    albumArtSize: Dp = 50.dp,
    customShape: androidx.compose.ui.graphics.Shape? = null,
    containerColorOverride: Color? = null,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    isSelectionMode: Boolean = false,
    showMoreOptionsButton: Boolean = true,
    useLibraryPill: Boolean = false,
    artworkColors: Pair<Color, Color>? = null,
    onLongPress: () -> Unit = {},
    onMoreOptionsClick: (Song) -> Unit,
    onClick: () -> Unit
) {
    val albumArtTargetSizePx = with(LocalDensity.current) { albumArtSize.roundToPx() }
    val isHighlighted = isCurrentSong && !isLoading && !useLibraryPill
    val motionEnabled = LocalMotionPreferences.current.animationsEnabled
    var wasEverActive by remember { mutableStateOf(isHighlighted || isSelected) }
    if (isHighlighted || isSelected) {
        wasEverActive = true
    }

    val highlightProgress: Float
    val selectionVisualProgress: Float
    val selectionScaleProgress: Float

    if (wasEverActive) {
        val transition = updateTransition(
            targetState = EnhancedSongAnimationTarget(
                isHighlighted = isHighlighted,
                isSelected = isSelected
            ),
            label = "EnhancedSongListItemTransition"
        )
        highlightProgress = transition.animateFloat(
            transitionSpec = { if (motionEnabled) tween(durationMillis = 400) else snap() },
            label = "highlightProgress"
        ) { state ->
            if (state.isHighlighted) 1f else 0f
        }.value
        selectionVisualProgress = transition.animateFloat(
            transitionSpec = { if (motionEnabled) tween(durationMillis = 250) else snap() },
            label = "selectionVisualProgress"
        ) { state ->
            if (state.isSelected) 1f else 0f
        }.value
        selectionScaleProgress = transition.animateFloat(
            transitionSpec = {
                if (motionEnabled) spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ) else snap()
            },
            label = "selectionScaleProgress"
        ) { state ->
            if (state.isSelected) 1f else 0f
        }.value
    } else {
        highlightProgress = 0f
        selectionVisualProgress = 0f
        selectionScaleProgress = 0f
    }

    val animatedCornerRadius = lerpDp(22.dp, 50.dp, highlightProgress)
    val animatedAlbumCornerRadius = lerpDp(10.dp, 50.dp, highlightProgress)
    val selectionScale = lerpFloat(1f, 0.98f, selectionScaleProgress)
    val selectionBorderWidth = lerpDp(0.dp, 2.5.dp, selectionVisualProgress)

    val surfaceShape = remember(animatedCornerRadius, customShape, isHighlighted, useLibraryPill) {
        when {
            useLibraryPill -> CircleShape
            customShape != null && !isHighlighted -> customShape
            animatedCornerRadius == 22.dp -> DefaultRowShape
            else -> RoundedCornerShape(animatedCornerRadius)
        }
    }

    val albumShape = remember(animatedAlbumCornerRadius) {
        if (animatedAlbumCornerRadius == 10.dp) DefaultAlbumShape else RoundedCornerShape(animatedAlbumCornerRadius)
    }

    val colors = MaterialTheme.colorScheme
    val baseContainerColor = containerColorOverride ?: colors.surfaceContainerLow
    val playbackContainerColor = lerpColor(baseContainerColor, colors.primaryContainer, highlightProgress)
    val containerColor = lerpColor(playbackContainerColor, colors.secondaryContainer, selectionVisualProgress)

    val baseContentColor = colors.onSurface
    val playbackContentColor = lerpColor(baseContentColor, colors.onPrimaryContainer, highlightProgress)
    val contentColor = lerpColor(playbackContentColor, colors.onSecondaryContainer, selectionVisualProgress)

    val selectionBorderColor = lerpColor(colors.primary.copy(alpha = 0f), colors.primary, selectionVisualProgress)
    val mvContainerColor = lerpColor(colors.onSurface, colors.primaryContainer, highlightProgress)
    val mvContentColor = lerpColor(colors.surfaceContainerHigh, colors.onPrimaryContainer, highlightProgress)
    val selectionOverlayColor = lerpColor(
        Color.Transparent,
        colors.primary.copy(alpha = 0.7f),
        selectionVisualProgress
    )
    val selectionOverlayContentColor = lerpColor(
        Color.Transparent,
        colors.onPrimary,
        selectionVisualProgress
    )
    val showSelectionDecoration = selectionVisualProgress > 0.001f
    val libraryArtworkModifier = if (useLibraryPill && artworkColors != null) {
        Modifier.libraryArtworkWash(
            artworkColors,
            containerColor,
            contentColor.copy(alpha = 0.7f),
            isCurrentSong && isPlaying && !isSelectionMode,
        )
    } else Modifier

    if (isLoading) {
        // Shimmer Placeholder Layout
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(surfaceShape),
            shape = surfaceShape,
            color = containerColorOverride ?: colors.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(showAlbumArt) {
                    ShimmerBox(
                        modifier = Modifier
                            .size(albumArtSize)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if(showAlbumArt) 0.dp else 4.dp)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                ShimmerBox(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            }
        }
    } else {
        val currentOnClick by rememberUpdatedState(onClick)
        val currentOnLongPress by rememberUpdatedState(onLongPress)
        // Actual Song Item Layout
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .then(if (selectionScale != 1f) Modifier.scale(selectionScale) else Modifier)
                .then(
                    if (showSelectionDecoration) {
                        Modifier.border(
                            width = selectionBorderWidth,
                            color = selectionBorderColor,
                            shape = surfaceShape
                        )
                    } else {
                        Modifier
                    }
                )
                .pointerInput(isSelectionMode) {
                    detectTapGestures(
                        onTap = { 
                            if (isSelectionMode) {
                                // In selection mode, tap toggles selection
                                currentOnLongPress()
                            } else {
                                currentOnClick() 
                            }
                        },
                        onLongPress = {
                            // Long press always activates/toggles selection
                            currentOnLongPress()
                        }
                    )
                },
            shape = surfaceShape,
            color = containerColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(libraryArtworkModifier)
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showAlbumArt) {
                    Box(
                        modifier = Modifier
                            .size(albumArtSize)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        SmartImage(
                            model = song.albumArtUriString,
                            contentDescription = song.title,
                            shape = albumShape,
                            crossfadeDurationMillis = 0,
                            targetSize = Size(albumArtTargetSizePx, albumArtTargetSizePx),
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Selection check overlay on album art
                        if (showSelectionDecoration) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = selectionOverlayColor,
                                        shape = albumShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectionIndex != null && selectionIndex >= 0) {
                                    Text(
                                        text = selectionIndex.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = selectionOverlayContentColor
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = stringResource(R.string.common_selected),
                                        tint = selectionOverlayContentColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        if (useLibraryPill && isCurrentSong && !isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .background(colors.surfaceContainerHighest, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayingEqIcon(
                                    modifier = Modifier.size(width = 13.dp, height = 11.dp),
                                    color = colors.onSurface,
                                    isPlaying = isPlaying && motionEnabled
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    if (isHighlighted && !isSelectionMode) {
                        AutoScrollingText(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            gradientEdgeColor = containerColor,
                        )

                    } else {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            color = contentColor,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.displayArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val showPlayingIndicator = !useLibraryPill && isCurrentSong && !isSelectionMode
                val showTrailingAction = showMoreOptionsButton && !isSelectionMode

                if (showPlayingIndicator) {
                     PlayingEqIcon(
                         modifier = Modifier
                             .padding(start = 8.dp)
                             .size(width = 18.dp, height = 16.dp),
                         color = contentColor,
                         isPlaying = isPlaying
                     )
                }

                if (showPlayingIndicator || showTrailingAction) {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                if (showTrailingAction) {
                    FilledIconButton(
                        onClick = { onMoreOptionsClick(song) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = mvContentColor,
                            contentColor = mvContainerColor
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.common_more_options_for, song.title),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.libraryArtworkWash(
    palette: Pair<Color, Color>,
    background: Color,
    textColor: Color,
    playing: Boolean
): Modifier {
    val effects = LocalVisualEffects.current
    val animated = LocalMotionPreferences.current.animationsEnabled
    val opacity = animateFloatAsState(
        targetValue = if (playing && effects.allowsDepthEffects) 1f else 0f,
        animationSpec = if (animated && effects.allowsDepthEffects) {
            tween(PixelMotion.STANDARD_DURATION_MS)
        } else snap(),
        label = "Library artwork wash"
    )
    val (primary, secondary) = palette
    val primaryAlpha = if (effects.allowsRealtimeBlur) 0.48f else 0.40f
    val secondaryAlpha = if (effects.allowsRealtimeBlur) 0.28f else 0.21f
    val tintStrength = remember(palette, background, textColor, primaryAlpha, secondaryAlpha) {
        var safe = 0f
        var upper = 1f
        repeat(8) {
            val candidate = (safe + upper) / 2f
            val first = primary.copy(alpha = primaryAlpha * candidate)
            val second = secondary.copy(alpha = secondaryAlpha * candidate)
            val underPrimary = first.compositeOver(background)
            val underSecondary = second.compositeOver(background)
            val underBoth = second.compositeOver(underPrimary)
            if (listOf(underPrimary, underSecondary, underBoth).all {
                    calculateWcagContrastRatio(textColor, it) >= MIN_LABEL_CONTRAST_RATIO
                }) {
                safe = candidate
            } else {
                upper = candidate
            }
        }
        safe
    }
    return drawWithCache {
        val ltr = layoutDirection == LayoutDirection.Ltr
        fun leadingX(fraction: Float): Float = size.width * if (ltr) fraction else 1f - fraction
        val middle = size.width / 2f
        val first = Brush.radialGradient(
            0f to primary.copy(alpha = primaryAlpha * tintStrength),
            0.48f to primary.copy(alpha = primaryAlpha * tintStrength * 0.55f),
            1f to primary.copy(alpha = 0f),
            center = Offset(leadingX(0.105f), size.height * 0.58f),
            radius = (size.width * 0.39f).coerceAtLeast(1f)
        )
        val second = Brush.radialGradient(
            0f to secondary.copy(alpha = secondaryAlpha * tintStrength),
            0.50f to secondary.copy(alpha = secondaryAlpha * tintStrength * 0.60f),
            1f to secondary.copy(alpha = 0f),
            center = Offset(leadingX(0.18f), size.height * 0.28f),
            radius = (size.width * 0.30f).coerceAtLeast(1f)
        )
        onDrawBehind {
            val alpha = opacity.value
            if (alpha > 0f) {
                clipRect(left = if (ltr) 0f else middle, right = if (ltr) middle else size.width) {
                    drawRect(brush = first, alpha = alpha)
                    drawRect(brush = second, alpha = alpha)
                }
            }
        }
    }
}
