package com.theveloper.pixelplay.presentation.screens

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.HorizontalPageIndicator as M3HorizontalPageIndicator
import androidx.wear.compose.material3.Icon as M3Icon
import com.google.android.horologist.audio.ui.VolumeUiState
import com.google.android.horologist.audio.ui.volumeRotaryBehavior
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.WearLifecycleState
import com.theveloper.pixelplay.presentation.components.AlwaysOnScalingPositionIndicator
import com.theveloper.pixelplay.presentation.components.CurvedVolumeIndicator
import com.theveloper.pixelplay.presentation.components.outputRouteIcon
import com.theveloper.pixelplay.presentation.components.WearTopTimeText
import com.theveloper.pixelplay.presentation.shapes.RoundedStarShape
import com.theveloper.pixelplay.presentation.theme.LocalWearPalette
import com.theveloper.pixelplay.presentation.theme.radialBackgroundBrush
import com.theveloper.pixelplay.presentation.theme.surfaceContainerColor
import com.theveloper.pixelplay.presentation.viewmodel.WearPlayerViewModel
import com.theveloper.pixelplay.shared.WearPlayerState
import com.theveloper.pixelplay.shared.WearSyncedLyricLine
import com.theveloper.pixelplay.shared.WearVolumeState
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.abs
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@Composable
fun PlayerScreen(
    onBrowseCategoryClick: (browseType: String, title: String) -> Unit = { _, _ -> },
    onVolumeClick: () -> Unit = {},
    onOutputClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    viewModel: WearPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playerState.collectAsState()
    val isPhoneConnected by viewModel.isPhoneConnected.collectAsState()
    val isWatchOutputSelected by viewModel.isWatchOutputSelected.collectAsState()
    val activeOutputRouteType by viewModel.activeOutputRouteType.collectAsState()
    val activeVolumeState by viewModel.activeVolumeState.collectAsState()
    val albumArt by viewModel.albumArt.collectAsState()

    PlayerContent(
        state = state,
        albumArt = albumArt,
        isPhoneConnected = isPhoneConnected,
        isWatchOutputSelected = isWatchOutputSelected,
        activeVolumeState = activeVolumeState,
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSetActiveVolume = viewModel::setActiveVolume,
        activeOutputRouteType = activeOutputRouteType,
        onBrowseCategoryClick = onBrowseCategoryClick,
        onVolumeClick = onVolumeClick,
        onOutputClick = onOutputClick,
        onMoreClick = onMoreClick,
        onQueueClick = onQueueClick,
    )
}

@Composable
private fun PlayerContent(
    state: WearPlayerState,
    albumArt: Bitmap?,
    isPhoneConnected: Boolean,
    isWatchOutputSelected: Boolean = false,
    activeVolumeState: WearVolumeState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSetActiveVolume: (Int) -> Unit,
    activeOutputRouteType: String,
    onBrowseCategoryClick: (browseType: String, title: String) -> Unit,
    onVolumeClick: () -> Unit,
    onOutputClick: () -> Unit,
    onMoreClick: () -> Unit,
    onQueueClick: () -> Unit,
) {
    val palette = LocalWearPalette.current
    val isAmbient by WearLifecycleState.isAmbient.collectAsState()
    // Memoize: radialGradient allocates Shader inputs on every call. PlayerContent
    // recomposes whenever the play-button ring animation ticks, so without this
    // we'd churn the GC for nothing.
    val background = remember(palette) { palette.radialBackgroundBrush() }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    var mainPageQueueReveal by remember { mutableFloatStateOf(0f) }
    var albumRevealProgress by remember { mutableFloatStateOf(0f) }
    val isMainPlayerPage = pagerState.currentPage == 1
    val isMainPlayerOverlayVisible =
        mainPageQueueReveal > 0.05f || albumRevealProgress > 0.05f
    val showPageIndicator =
        !isMainPlayerPage || (pagerState.isScrollInProgress && !isMainPlayerOverlayVisible)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isAmbient) {
                    Modifier.background(Color.Black)
                } else {
                    Modifier.background(background)
                }
            ),
    ) {

        if (!isAmbient && albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.45f),
                    //.blur(12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.55f to Color.Transparent,
                                0.80f to palette.gradientTop.copy(alpha = 0.45f),
                                1f to palette.gradientBottom.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> {
                    WearLyricsPage(
                        state = state,
                        isPhoneConnected = isPhoneConnected,
                        isWatchOutputSelected = isWatchOutputSelected,
                        isAmbient = isAmbient,
                    )
                }

                1 -> {
                    PlayerMainPageHost(
                        state = state,
                        albumArt = albumArt,
                        isCurrentPage = pagerState.currentPage == 1,
                        isAmbient = isAmbient,
                        isPhoneConnected = isPhoneConnected,
                        isWatchOutputSelected = isWatchOutputSelected,
                        activeVolumeState = activeVolumeState,
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSetActiveVolume = onSetActiveVolume,
                        activeOutputRouteType = activeOutputRouteType,
                        onVolumeClick = onVolumeClick,
                        onOutputClick = onOutputClick,
                        onMoreClick = onMoreClick,
                        onQueueClick = onQueueClick,
                        onQueueShortcutRevealChanged = { mainPageQueueReveal = it },
                        onAlbumRevealProgressChanged = { albumRevealProgress = it },
                    )
                }

                else -> {
                    BrowseScreen(
                        onCategoryClick = onBrowseCategoryClick,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showPageIndicator,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 240)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(6f),
        ) {
            M3HorizontalPageIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .padding(bottom = 10.dp),
                selectedColor = palette.textPrimary,
                unselectedColor = palette.textPrimary.copy(alpha = 0.52f),
                backgroundColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun PlayerMainPageHost(
    state: WearPlayerState,
    albumArt: Bitmap?,
    isCurrentPage: Boolean,
    isAmbient: Boolean,
    isPhoneConnected: Boolean,
    isWatchOutputSelected: Boolean,
    activeVolumeState: WearVolumeState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSetActiveVolume: (Int) -> Unit,
    activeOutputRouteType: String,
    onVolumeClick: () -> Unit,
    onOutputClick: () -> Unit,
    onMoreClick: () -> Unit,
    onQueueClick: () -> Unit,
    onQueueShortcutRevealChanged: (Float) -> Unit,
    onAlbumRevealProgressChanged: (Float) -> Unit,
) {
    val palette = LocalWearPalette.current
    val density = LocalDensity.current
    val canShowAlbumArt = !state.isEmpty
    var isDraggingAlbumReveal by remember { mutableStateOf(false) }
    var rawAlbumRevealProgress by remember { mutableFloatStateOf(0f) }
    val albumRevealProgress by animateFloatAsState(
        targetValue = rawAlbumRevealProgress,
        animationSpec = if (isDraggingAlbumReveal) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            )
        },
        label = "albumRevealProgress",
    )

    LaunchedEffect(canShowAlbumArt, isCurrentPage) {
        if (!canShowAlbumArt || !isCurrentPage) {
            isDraggingAlbumReveal = false
            rawAlbumRevealProgress = 0f
        }
    }

    SideEffect {
        onAlbumRevealProgressChanged(albumRevealProgress)
    }

    BackHandler(enabled = albumRevealProgress > 0.01f) {
        isDraggingAlbumReveal = false
        rawAlbumRevealProgress = 0f
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val heightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val revealDistancePx = heightPx * 0.62f
        val overlayOffsetPx = -heightPx * (1f - albumRevealProgress)
        val mainTranslationY = -heightPx * 0.08f * albumRevealProgress
        val mainScale = 1f - (0.04f * albumRevealProgress)
        val mainAlpha = 1f - (0.26f * albumRevealProgress)
        val timeAlpha = (1f - (albumRevealProgress * 1.6f)).coerceIn(0f, 1f)

        fun settleAlbumReveal(progress: Float, velocityY: Float = 0f) {
            isDraggingAlbumReveal = false
            rawAlbumRevealProgress = if (shouldOpenAlbumArtReveal(progress, velocityY)) 1f else 0f
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MainPlayerPage(
                state = state,
                isPhoneConnected = isPhoneConnected,
                isWatchOutputSelected = isWatchOutputSelected,
                isAmbient = isAmbient,
                activeVolumeState = activeVolumeState,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSetActiveVolume = onSetActiveVolume,
                rotaryEnabled = isCurrentPage && albumRevealProgress <= 0.01f,
                activeOutputRouteType = activeOutputRouteType,
                onVolumeClick = onVolumeClick,
                onOutputClick = onOutputClick,
                onMoreClick = onMoreClick,
                onQueueClick = onQueueClick,
                onQueueShortcutRevealChanged = onQueueShortcutRevealChanged,
                modifier = Modifier.graphicsLayer {
                    translationY = mainTranslationY
                    scaleX = mainScale
                    scaleY = mainScale
                    alpha = mainAlpha
                },
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(42.dp)
                    .albumArtRevealDragGesture(
                        enabled = canShowAlbumArt && isCurrentPage,
                        currentProgress = albumRevealProgress,
                        revealDistancePx = revealDistancePx,
                        onDragStart = {
                            isDraggingAlbumReveal = true
                            rawAlbumRevealProgress = albumRevealProgress
                        },
                        onProgressChange = { rawAlbumRevealProgress = it },
                        onRelease = { progress, velocityY ->
                            settleAlbumReveal(progress, velocityY)
                        },
                    )
                    .clickable(enabled = canShowAlbumArt && isCurrentPage) {
                        isDraggingAlbumReveal = false
                        rawAlbumRevealProgress = 1f
                    }
                    .zIndex(5f),
            )

            WearTopTimeText(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = timeAlpha }
                    .zIndex(6f),
                color = palette.textPrimary,
            )

            if (!isAmbient && (canShowAlbumArt || albumRevealProgress > 0.01f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = overlayOffsetPx
                            alpha = albumRevealProgress
                        }
                        .albumArtRevealDragGesture(
                            enabled = isCurrentPage,
                            currentProgress = albumRevealProgress,
                            revealDistancePx = revealDistancePx,
                            onDragStart = {
                                isDraggingAlbumReveal = true
                                rawAlbumRevealProgress = albumRevealProgress
                            },
                            onProgressChange = { rawAlbumRevealProgress = it },
                            onRelease = { progress, velocityY ->
                                settleAlbumReveal(progress, velocityY)
                            },
                        )
                        .zIndex(8f),
                ) {
                    AlbumArtPage(
                        state = state,
                        albumArt = albumArt,
                        onTap = {
                            isDraggingAlbumReveal = false
                            rawAlbumRevealProgress = 0f
                        },
                    )
                }
            }
        }
    }
}

private fun Modifier.albumArtRevealDragGesture(
    enabled: Boolean,
    currentProgress: Float,
    revealDistancePx: Float,
    onDragStart: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onRelease: (Float, Float) -> Unit,
): Modifier {
    if (!enabled) return this

    return this.pointerInput(enabled, currentProgress, revealDistancePx) {
        val velocityTracker = VelocityTracker()
        var dragProgress = currentProgress
        var openingFromClosed = false
        detectVerticalDragGestures(
            onDragStart = {
                velocityTracker.resetTracking()
                dragProgress = currentProgress
                openingFromClosed = currentProgress <= 0.01f
                onDragStart()
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                val progressDelta = if (openingFromClosed) {
                    abs(dragAmount) / revealDistancePx
                } else {
                    -dragAmount / revealDistancePx
                }
                dragProgress = (dragProgress + progressDelta).coerceIn(0f, 1f)
                onProgressChange(dragProgress)
            },
            onDragEnd = {
                onRelease(dragProgress, velocityTracker.calculateVelocity().y)
            },
            onDragCancel = {
                onRelease(dragProgress, 0f)
            },
        )
    }
}

private fun shouldOpenAlbumArtReveal(
    progress: Float,
    velocityY: Float,
): Boolean = when {
    velocityY <= -900f -> true
    velocityY >= 900f -> false
    progress >= 0.34f -> true
    else -> false
}
