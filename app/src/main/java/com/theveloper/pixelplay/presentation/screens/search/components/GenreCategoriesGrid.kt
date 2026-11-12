package com.theveloper.pixelplay.presentation.screens.search.components

import com.theveloper.pixelplay.presentation.components.bottomchrome.LocalBottomChrome

import com.theveloper.pixelplay.presentation.components.bottomchrome.NavigationReselectionEffect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.data.model.Genre
import com.theveloper.pixelplay.presentation.components.MiniPlayerHeight
import com.theveloper.pixelplay.presentation.components.SmartImage
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.ui.theme.LocalPixelPlayDarkTheme
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.rememberPillSurfaceColors
import androidx.compose.ui.res.stringResource
import com.theveloper.pixelplay.R
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight

private val GridGenreLabelSize = TextAutoSize.StepBased(14.sp, 18.sp, .5.sp)

@OptIn(UnstableApi::class)
@Composable
fun GenreCategoriesGrid(
    genres: List<Genre>,
    onGenreClick: (Genre) -> Unit,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedGenreIds: Set<String> = emptySet(),
    onGenreLongPress: (Genre) -> Unit = {},
    onGenreSelectionToggle: (Genre) -> Unit = {},
    getSelectionIndex: (String) -> Int? = { null }
) {
    if (genres.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_genres_available), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val customGenreIcons = playerViewModel.customGenreIcons.collectAsStateWithLifecycle(
        initialValue = emptyMap(),
        context = kotlin.coroutines.EmptyCoroutineContext
    ).value

    // Persistence: Collect from ViewModel
    val prefersGrid by playerViewModel.isGenreGridView.collectAsStateWithLifecycle()
    val animationsEnabled = LocalMotionPreferences.current.animationsEnabled
    val fontScale = LocalDensity.current.fontScale

    val browseState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    NavigationReselectionEffect(com.theveloper.pixelplay.presentation.navigation.Screen.Search.route) { browseState.animateScrollToItem(0) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .clip(AbsoluteSmoothCornerShape(
                cornerRadiusTR = 24.dp,
                smoothnessAsPercentTR = 70,
                cornerRadiusTL = 24.dp,
                smoothnessAsPercentTL = 70,
                cornerRadiusBR = 0.dp,
                smoothnessAsPercentBR = 70,
                cornerRadiusBL = 0.dp,
                smoothnessAsPercentBL = 70
            ))
    ) {
        val gridFits = maxWidth >= 310.dp && fontScale < 1.35f
        val isGridView = prefersGrid && gridFits
        LazyVerticalGrid(
            state = browseState,
            columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 28.dp + LocalBottomChrome.current.navBarOccupiedHeight + MiniPlayerHeight
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.browse_by_genre),
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (gridFits) {
                        val animatedCornerRadius by animateDpAsState(
                            targetValue = if (!isGridView) 12.dp else 50.dp,
                            animationSpec = if (animationsEnabled) PixelMotion.structural() else snap(),
                            label = "shapeAnimation"
                        )
                        androidx.compose.material3.FilledIconButton(
                            onClick = { playerViewModel.toggleGenreViewMode() },
                            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(animatedCornerRadius)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                contentDescription = "Toggle Grid/List View"
                            )
                        }
                    }
                }
            }
            items(genres, key = { it.id }, contentType = { "genre" }) { genre ->
                GenreCard(
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = if (animationsEnabled) PixelMotion.structural() else snap()
                    ),
                    genre = genre,
                    customIcons = customGenreIcons,
                    onClick = { onGenreClick(genre) },
                    isGridView = isGridView,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedGenreIds.contains(genre.id),
                    selectionIndex = getSelectionIndex(genre.id),
                    onLongPress = { onGenreLongPress(genre) },
                    onSelectionToggle = { onGenreSelectionToggle(genre) }
                )
            }
        }
    }
}

@Composable
private fun GenreCard(
    modifier: Modifier = Modifier,
    genre: Genre,
    customIcons: Map<String, Int>,
    onClick: () -> Unit,
    isGridView: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    onLongPress: () -> Unit = {},
    onSelectionToggle: () -> Unit = {}
) {
    val isDark = LocalPixelPlayDarkTheme.current
    val animationsEnabled = LocalMotionPreferences.current.animationsEnabled
    val pillColors = rememberPillSurfaceColors()
    val onBackgroundColor = pillColors.contentColor
    val customIcon = customIcons[genre.name]
    val iconGradientColors = remember(genre, isDark) {
        val iconScheme = com.theveloper.pixelplay.ui.theme.GenreThemeUtils.getGenreColorScheme(
            genre = genre,
            isDark = isDark
        )
        listOf(iconScheme.primary, iconScheme.tertiary, iconScheme.secondary)
    }
    val shape = CircleShape

    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = if (animationsEnabled) PixelMotion.feedback() else snap(),
        label = "genreCardSelectionScale"
    )
    val selectionBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = if (animationsEnabled) tween(PixelMotion.QUICK_DURATION_MS) else snap(),
        label = "genreCardSelectionBorder"
    )

    val minimumCardHeight = if (isGridView) 72.dp else 80.dp
    val selectionBadgeEnter = if (animationsEnabled) {
        expandHorizontally(
            expandFrom = Alignment.End,
            animationSpec = PixelMotion.structural()
        ) +
            scaleIn(animationSpec = PixelMotion.feedback()) +
            fadeIn(animationSpec = tween(PixelMotion.QUICK_DURATION_MS))
    } else {
        EnterTransition.None
    }
    val selectionBadgeExit = if (animationsEnabled) {
        shrinkHorizontally(
            shrinkTowards = Alignment.End,
            animationSpec = PixelMotion.structural()
        ) +
            scaleOut(animationSpec = PixelMotion.structural()) +
            fadeOut(animationSpec = tween(PixelMotion.QUICK_DURATION_MS))
    } else {
        ExitTransition.None
    }
    val iconSize = if (isGridView) 48.dp else 56.dp
    val badgeSize = if (isGridView) 26.dp else 32.dp
    val sidePadding = if (isGridView) 7.dp else 12.dp
    val itemSpacing = if (isGridView) 7.dp else 12.dp
    val singleWordInGrid = isGridView && genre.name.none(Char::isWhitespace)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(selectionScale)
            .clip(shape)
            .border(
                width = selectionBorderWidth,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else onBackgroundColor.copy(alpha = 0.08f),
                shape = shape
            )
            .combinedClickable(
                role = Role.Button,
                onClick = {
                    if (isSelectionMode) onSelectionToggle() else onClick()
                },
                onLongClick = onLongPress
            )
            .semantics {
                selected = isSelected
            },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = pillColors.surfaceColor,
            contentColor = onBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minimumCardHeight)
                .padding(horizontal = sidePadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            Box(
                modifier = Modifier.size(iconSize).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (customIcon != null) {
                    SmartImage(
                        model = customIcon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    GenreArtwork(genre = genre, colors = iconGradientColors)
                }
            }

            Text(
                text = genre.name,
                modifier = Modifier.weight(1f),
                autoSize = if (singleWordInGrid) GridGenreLabelSize else null,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                ),
                color = onBackgroundColor,
                softWrap = !singleWordInGrid,
                maxLines = if (singleWordInGrid) 1 else 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )

            AnimatedVisibility(
                visible = isSelectionMode,
                enter = selectionBadgeEnter,
                exit = selectionBadgeExit
            ) {
                Box(
                    modifier = Modifier.size(badgeSize),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = CircleShape
                            )
                            .then(
                                if (isSelected) Modifier
                                else Modifier.border(
                                    width = 1.dp,
                                    color = onBackgroundColor.copy(alpha = 0.44f),
                                    shape = CircleShape
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected && selectionIndex != null && selectionIndex >= 0) {
                            Text(
                                text = selectionIndex.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
