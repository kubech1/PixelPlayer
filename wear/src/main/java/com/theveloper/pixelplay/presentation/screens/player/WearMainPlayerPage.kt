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
@OptIn(ExperimentalWearFoundationApi::class)
internal fun MainPlayerPage(
    state: WearPlayerState,
    isPhoneConnected: Boolean,
    isWatchOutputSelected: Boolean = false,
    isAmbient: Boolean,
    activeVolumeState: WearVolumeState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSetActiveVolume: (Int) -> Unit,
    rotaryEnabled: Boolean,
    activeOutputRouteType: String,
    onVolumeClick: () -> Unit,
    onOutputClick: () -> Unit,
    onMoreClick: () -> Unit,
    onQueueClick: () -> Unit,
    onQueueShortcutRevealChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalWearPalette.current
    val volumeEnabled = isPhoneConnected || isWatchOutputSelected
    val columnState = rememberResponsiveColumnState(
        contentPadding = {
            PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = 2.dp,
                bottom = 20.dp,
            )
        },
    )

    val livePositionMs by rememberLivePositionMs(state)
    val trackProgressTarget = if (state.totalDurationMs > 0L) {
        (livePositionMs.toFloat() / state.totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val trackProgress by animateFloatAsState(
        targetValue = trackProgressTarget,
        animationSpec = tween(durationMillis = 280),
        label = "trackProgress",
    )
    val queueShortcutRevealTarget by remember(columnState.state) {
        derivedStateOf {
            val layoutInfo = columnState.state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 1) return@derivedStateOf 0f
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 0f

            val firstVisibleIndex = visibleItems.minOf { it.index }
            val lastVisibleIndex = visibleItems.maxOf { it.index }
            val visibleCount = (lastVisibleIndex - firstVisibleIndex + 1).coerceAtLeast(1)
            val maxFirstVisibleIndex = (totalItems - visibleCount).coerceAtLeast(0)
            if (maxFirstVisibleIndex == 0) return@derivedStateOf 0f

            if (lastVisibleIndex >= totalItems - 1) {
                1f
            } else {
                (firstVisibleIndex.toFloat() / maxFirstVisibleIndex.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    val queueShortcutReveal by animateFloatAsState(
        targetValue = queueShortcutRevealTarget,
        animationSpec = tween(durationMillis = 220),
        label = "queueShortcutReveal",
    )
    val rotaryFocusRequester = remember { FocusRequester() }
    val rotaryVolumeUiState = remember(activeVolumeState.level, activeVolumeState.max) {
        VolumeUiState(
            current = activeVolumeState.level.coerceAtLeast(0),
            max = activeVolumeState.max.coerceAtLeast(0),
            min = 0,
        )
    }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var volumeOverlayInteractionTick by remember { mutableIntStateOf(0) }
    val volumeProgress = if (activeVolumeState.max > 0) {
        (activeVolumeState.level.toFloat() / activeVolumeState.max.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    LaunchedEffect(rotaryEnabled) {
        if (!rotaryEnabled) {
            showVolumeOverlay = false
        }
    }
    LaunchedEffect(volumeOverlayInteractionTick, rotaryEnabled) {
        if (!rotaryEnabled || volumeOverlayInteractionTick == 0) return@LaunchedEffect
        showVolumeOverlay = true
        delay(2_000L)
        showVolumeOverlay = false
    }
    LaunchedEffect(queueShortcutReveal) {
        onQueueShortcutRevealChanged(queueShortcutReveal)
    }

    val rotaryModifier = if (rotaryEnabled && volumeEnabled && activeVolumeState.max > 0) {
        Modifier
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = volumeRotaryBehavior(
                    volumeUiStateProvider = { rotaryVolumeUiState },
                    onRotaryVolumeInput = { newVolume ->
                        showVolumeOverlay = true
                        volumeOverlayInteractionTick++
                        onSetActiveVolume(newVolume)
                    },
                ),
                focusRequester = rotaryFocusRequester,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(rotaryModifier)
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            columnState = columnState,
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                HeaderBlock(
                    state = state,
                    isPhoneConnected = isPhoneConnected,
                    isWatchOutputSelected = isWatchOutputSelected,
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                MainControlsRow(
                    isPlaying = state.isPlaying,
                    isEmpty = state.isEmpty,
                    enabled = if (isWatchOutputSelected) !state.isEmpty else isPhoneConnected,
                    outlined = isAmbient,
                    trackProgress = trackProgress,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                )
            }

            item {
                SecondaryControlsRow(
                    volumeEnabled = volumeEnabled,
                    deviceEnabled = volumeEnabled,
                    outlined = isAmbient,
                    deviceRouteType = if (isWatchOutputSelected) {
                        com.theveloper.pixelplay.shared.WearVolumeState.ROUTE_TYPE_WATCH
                    } else {
                        activeOutputRouteType
                    },
                    moreEnabled = !state.isEmpty,
                    onVolumeClick = onVolumeClick,
                    onOutputClick = onOutputClick,
                    onMoreClick = onMoreClick,
                    deviceActiveColor = palette.shuffleActive,
                )
            }

            item { Spacer(modifier = Modifier.height(50.dp)) }
        }

        PlayerCrownVolumeIndicator(
            progress = volumeProgress,
            visible = showVolumeOverlay,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
        )

        BottomQueueShortcut(
            revealProgress = queueShortcutReveal,
            enabled = isPhoneConnected,
            onClick = onQueueClick,
            modifier = Modifier
                .align(Alignment.BottomCenter),
        )

        if (!isAmbient) {
            AlwaysOnScalingPositionIndicator(
                listState = columnState.state,
                modifier = Modifier.align(Alignment.CenterEnd),
                color = palette.textPrimary,
            )
        }
    }
}

@Composable
internal fun rememberLivePositionMs(state: WearPlayerState): androidx.compose.runtime.State<Long> {
    val safeDuration = state.totalDurationMs.coerceAtLeast(0L)
    val safeAnchorPosition = state.currentPositionMs.coerceIn(0L, safeDuration)
    val positionKey = remember(
        state.songId,
        safeAnchorPosition,
        safeDuration,
        state.isPlaying,
        state.positionUpdatedElapsedRealtimeMs,
    ) {
        "${state.songId}|$safeAnchorPosition|$safeDuration|${state.isPlaying}|${state.positionUpdatedElapsedRealtimeMs}"
    }
    return produceState(
        initialValue = state.livePositionFromAnchor(safeAnchorPosition, safeDuration),
        key1 = positionKey,
    ) {
        value = state.livePositionFromAnchor(safeAnchorPosition, safeDuration)
        if (!state.isPlaying || safeDuration <= 0L) {
            return@produceState
        }

        while (true) {
            val next = state.livePositionFromAnchor(safeAnchorPosition, safeDuration)
            value = next
            if (next >= safeDuration) break
            delay(250L)
        }
    }
}

internal fun WearPlayerState.livePositionFromAnchor(
    safeAnchorPosition: Long,
    safeDuration: Long,
): Long {
    if (!isPlaying || safeDuration <= 0L) return safeAnchorPosition
    val anchorElapsedRealtime = positionUpdatedElapsedRealtimeMs
    if (anchorElapsedRealtime <= 0L) return safeAnchorPosition
    val elapsed = (SystemClock.elapsedRealtime() - anchorElapsedRealtime).coerceAtLeast(0L)
    return (safeAnchorPosition + elapsed).coerceIn(0L, safeDuration)
}

@Composable
internal fun PlayerCrownVolumeIndicator(
    progress: Float,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalWearPalette.current
    val overlayAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 180 else 220),
        label = "playerVolumeOverlayAlpha",
    )
    val overlayScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = tween(durationMillis = if (visible) 180 else 220),
        label = "playerVolumeOverlayScale",
    )

    if (overlayAlpha <= 0.01f && !visible) return

    Box(
        modifier = modifier.graphicsLayer {
            alpha = overlayAlpha
            scaleX = overlayScale
            scaleY = overlayScale
            transformOrigin = TransformOrigin(0f, 0.5f)
        }
    ) {
        CurvedVolumeIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            startAngle = 144f,
            sweepAngle = 72f,
            strokeWidth = 4.dp,
            inset = 10.dp,
            trackColor = palette.surfaceContainerColor().copy(alpha = 0.42f),
            progressColor = palette.controlContainer.copy(alpha = 0.94f),
        )
    }
}

@Composable
internal fun HeaderBlock(
    state: WearPlayerState,
    isPhoneConnected: Boolean,
    isWatchOutputSelected: Boolean = false,
) {
    val palette = LocalWearPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.songTitle.ifEmpty { "Song name" },
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = when {
                isWatchOutputSelected -> {
                    val artistAlbum = buildList {
                        if (state.artistName.isNotBlank()) add(state.artistName)
                        if (state.albumName.isNotBlank()) add(state.albumName)
                    }.joinToString(" · ")
                    if (artistAlbum.isNotBlank()) artistAlbum else "On watch"
                }
                !isPhoneConnected -> "No phone"
                state.artistName.isNotEmpty() -> state.artistName
                state.isEmpty -> "Waiting playback"
                else -> "Artist name"
            },
            style = MaterialTheme.typography.body1,
            color = when {
                isWatchOutputSelected -> palette.textSecondary
                !isPhoneConnected -> palette.textError
                else -> palette.textSecondary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

//        if (isWatchOutputSelected) {
//            Text(
//                text = "On watch",
//                style = MaterialTheme.typography.caption3,
//                color = palette.shuffleActive.copy(alpha = 0.85f),
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth(),
//            )
//        }
    }
}

@Composable
internal fun MainControlsRow(
    isPlaying: Boolean,
    isEmpty: Boolean,
    enabled: Boolean,
    outlined: Boolean,
    trackProgress: Float,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlattenedControlButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous",
            enabled = enabled,
            outlined = outlined,
            onClick = onPrevious,
            width = 44.dp,
            height = 54.dp,
        )

        Spacer(modifier = Modifier.width(10.dp))

        CenterPlayButton(
            isPlaying = isPlaying,
            enabled = enabled && !isEmpty,
            outlined = outlined,
            trackProgress = trackProgress,
            onClick = onTogglePlayPause,
        )

        Spacer(modifier = Modifier.width(10.dp))

        FlattenedControlButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next",
            enabled = enabled,
            outlined = outlined,
            onClick = onNext,
            width = 44.dp,
            height = 54.dp,
        )
    }
}

@Composable
internal fun FlattenedControlButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    outlined: Boolean,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
) {
    val palette = LocalWearPalette.current
    val shape = CircleShape
    val container = when {
        outlined -> Color.Transparent
        enabled -> palette.transportContainer
        else -> palette.controlDisabledContainer
    }
    val tint = when {
        outlined && enabled -> palette.transportContainer
        enabled -> palette.transportContent
        else -> palette.controlDisabledContent
    }
    val borderColor = if (enabled) {
        palette.transportContainer.copy(alpha = 0.82f)
    } else {
        palette.controlDisabledContent.copy(alpha = 0.34f)
    }

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(container)
            .then(
                if (outlined) {
                    Modifier.border(1.5.dp, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
internal fun CenterPlayButton(
    isPlaying: Boolean,
    enabled: Boolean,
    outlined: Boolean,
    trackProgress: Float,
    onClick: () -> Unit,
) {
    val palette = LocalWearPalette.current

    val animatedCurve by animateFloatAsState(
        targetValue = if (isPlaying) 0.08f else 0.00f,
        animationSpec = spring(),
        label = "playStarCurve",
    )
    val rotation = remember { Animatable(0f) }
    val isInteractive by WearLifecycleState.isInteractive.collectAsState(
        initial = WearLifecycleState.isInteractiveNow,
    )
    LaunchedEffect(isPlaying, isInteractive) {
        if (!isPlaying || !isInteractive) {
            val normalizedRotation = ((rotation.value % 360f) + 360f) % 360f
            rotation.snapTo(normalizedRotation)
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 616,
                    easing = FastOutSlowInEasing,
                ),
            )
            return@LaunchedEffect
        }
        while (true) {
            val current = rotation.value
            rotation.animateTo(
                targetValue = current + 360f,
                animationSpec = tween(
                    durationMillis = 13800,
                    easing = LinearEasing,
                ),
            )
            if (rotation.value >= 3600f) {
                rotation.snapTo(rotation.value % 360f)
            }
        }
    }
    val animatedRotation = rotation.value
    val animatedSize by animateDpAsState(
        targetValue = if (isPlaying) 60.dp else 56.dp,
        animationSpec = spring(),
        label = "playButtonSize",
    )
    val container by animateColorAsState(
        targetValue = when {
            outlined -> Color.Transparent
            enabled -> palette.controlContainer
            else -> palette.controlDisabledContainer
        },
        animationSpec = spring(),
        label = "playContainer",
    )
    val tint by animateColorAsState(
        targetValue = when {
            outlined && enabled -> palette.controlContainer
            enabled -> palette.controlContent
            else -> palette.controlDisabledContent
        },
        animationSpec = spring(),
        label = "playTint",
    )

    val ringProgress = trackProgress.coerceIn(0f, 1f)
    val playButtonShape = RoundedStarShape(
        sides = 8,
        curve = animatedCurve.toDouble(),
        rotation = animatedRotation,
    )

    Box(
        modifier = Modifier.size(animatedSize + 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val ringInset = (strokeWidth / 2f) + 1.5.dp.toPx()
            val ringRadius = ((size.minDimension - (ringInset * 2f)) / 2f).coerceAtLeast(0f)
            val ringPath = buildPlayButtonRingPath(
                centerX = size.width / 2f,
                centerY = size.height / 2f,
                radius = ringRadius,
                curve = animatedCurve,
                rotation = animatedRotation,
            )
            val ringStroke = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )

            drawPath(
                path = ringPath,
                color = palette.surfaceContainerLowest, //.copy(alpha = 0.62f),
                style = ringStroke,
            )

            val pathMeasure = PathMeasure().apply {
                setPath(ringPath, forceClosed = true)
            }
            val pathLength = pathMeasure.length
            val progressLength = pathLength * ringProgress
            if (progressLength > 0f && pathLength > 0f) {
                val normalizedRotation = ((animatedRotation % 360f) + 360f) % 360f
                val startDistance = (pathLength * (normalizedRotation / 360f)).coerceIn(0f, pathLength)
                val boundedProgress = progressLength.coerceAtMost(pathLength)
                val progressPath = Path()

                if (startDistance + boundedProgress <= pathLength) {
                    pathMeasure.getSegment(
                        startDistance = startDistance,
                        stopDistance = startDistance + boundedProgress,
                        destination = progressPath,
                        startWithMoveTo = true,
                    )
                } else {
                    val firstLeg = pathLength - startDistance
                    pathMeasure.getSegment(
                        startDistance = startDistance,
                        stopDistance = pathLength,
                        destination = progressPath,
                        startWithMoveTo = true,
                    )
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = (boundedProgress - firstLeg).coerceAtLeast(0f),
                        destination = progressPath,
                        startWithMoveTo = false,
                    )
                }

                drawPath(
                    path = progressPath,
                    color = palette.controlContainer.copy(alpha = if (enabled) 1f else 0.95f),
                    style = ringStroke,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(animatedSize)
                .clip(playButtonShape)
                .background(container)
                .then(
                    if (outlined) {
                        Modifier.border(
                            width = 1.6.dp,
                            color = if (enabled) {
                                palette.controlContainer.copy(alpha = 0.86f)
                            } else {
                                palette.controlDisabledContent.copy(alpha = 0.34f)
                            },
                            shape = playButtonShape,
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = tint,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

internal fun buildPlayButtonRingPath(
    centerX: Float,
    centerY: Float,
    radius: Float,
    curve: Float,
    rotation: Float,
    sides: Int = 8,
    steps: Int = 320,
): Path {
    val twoPi = Math.PI * 2.0
    val startAngle = -Math.PI / 2.0
    val angleStep = twoPi / steps.toDouble()
    val rotationRad = Math.toRadians(rotation.toDouble())
    val ringPath = Path()
    val boundedCurve = curve.coerceIn(0f, 0.12f)
    val boundedRadius = radius.coerceAtLeast(0f)

    fun pointAt(t: Double): Offset {
        val angle = startAngle + t
        val wave = 1f + (boundedCurve * cos((sides * angle)).toFloat())
        val distance = boundedRadius * wave
        val x = centerX + (distance * cos(angle - rotationRad).toFloat())
        val y = centerY + (distance * sin(angle - rotationRad).toFloat())
        return Offset(x, y)
    }

    val firstPoint = pointAt(0.0)
    ringPath.moveTo(firstPoint.x, firstPoint.y)
    var t = angleStep
    while (t <= twoPi) {
        val point = pointAt(t)
        ringPath.lineTo(point.x, point.y)
        t += angleStep
    }
    ringPath.close()
    return ringPath
}

@Composable
internal fun SecondaryControlsRow(
    volumeEnabled: Boolean,
    deviceEnabled: Boolean,
    outlined: Boolean,
    deviceRouteType: String,
    moreEnabled: Boolean,
    onVolumeClick: () -> Unit,
    onOutputClick: () -> Unit,
    onMoreClick: () -> Unit,
    deviceActiveColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 10.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.Top,
    ) {
        SecondaryActionSlot(lower = false) {
            SecondaryActionButton(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                enabled = volumeEnabled,
                active = false,
                activeColor = deviceActiveColor,
                outlined = outlined,
                raisedInactiveStyle = true,
                onClick = onVolumeClick,
                contentDescription = "Volume",
            )
        }
        SecondaryActionSlot(lower = true) {
            SecondaryActionButton(
                icon = outputRouteIcon(deviceRouteType),
                enabled = deviceEnabled,
                active = deviceRouteType == com.theveloper.pixelplay.shared.WearVolumeState.ROUTE_TYPE_WATCH,
                activeColor = deviceActiveColor,
                outlined = outlined,
                raisedInactiveStyle = true,
                onClick = onOutputClick,
                contentDescription = "Output device",
            )
        }
        SecondaryActionSlot(lower = false) {
            SecondaryActionButton(
                icon = Icons.Rounded.MoreVert,
                enabled = moreEnabled,
                active = false,
                activeColor = deviceActiveColor,
                outlined = outlined,
                raisedInactiveStyle = true,
                heavilyMutedWhenDisabled = true,
                onClick = onMoreClick,
                contentDescription = "More options",
            )
        }
    }
}

@Composable
internal fun SecondaryActionButton(
    icon: ImageVector,
    enabled: Boolean,
    active: Boolean,
    activeColor: Color,
    outlined: Boolean,
    raisedInactiveStyle: Boolean = false,
    heavilyMutedWhenDisabled: Boolean = false,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val palette = LocalWearPalette.current
    val activeContainerColor = activeColor.copy(alpha = 0.84f)
    val inactiveContainerColor = if (raisedInactiveStyle) {
        palette.surfaceContainerHighest
    } else {
        palette.chipContainer
    }
    val inactiveContentColor = if (raisedInactiveStyle) {
        palette.textPrimary.copy(alpha = 0.94f)
    } else {
        palette.chipContent
    }
    val disabledContainerColor = if (heavilyMutedWhenDisabled) {
        palette.surfaceContainerLowest.copy(alpha = 0.92f)
    } else {
        palette.controlDisabledContainer
    }
    val disabledContentColor = if (heavilyMutedWhenDisabled) {
        palette.textSecondary.copy(alpha = 0.48f)
    } else {
        palette.controlDisabledContent
    }
    val container by animateColorAsState(
        targetValue = when {
            outlined -> Color.Transparent
            !enabled -> disabledContainerColor
            active -> activeContainerColor
            else -> inactiveContainerColor
        },
        animationSpec = spring(),
        label = "secondaryContainer",
    )
    val tint by animateColorAsState(
        targetValue = when {
            outlined && enabled -> if (active) activeColor else inactiveContentColor
            !enabled -> disabledContentColor
            active -> if (activeContainerColor.luminance() > 0.52f) Color.Black else Color.White
            else -> inactiveContentColor
        },
        animationSpec = spring(),
        label = "secondaryTint",
    )

    val shape = RoundedCornerShape(18.dp)
    val borderColor = when {
        !enabled -> disabledContentColor.copy(alpha = 0.32f)
        active -> activeColor.copy(alpha = 0.84f)
        else -> inactiveContentColor.copy(alpha = 0.62f)
    }

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 36.dp)
            .clip(shape)
            .background(container)
            .then(
                if (outlined) {
                    Modifier.border(1.4.dp, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
internal fun SecondaryActionSlot(
    lower: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.size(width = 48.dp, height = 48.dp),
        contentAlignment = if (lower) Alignment.BottomCenter else Alignment.TopCenter,
    ) {
        content()
    }
}

@Composable
internal fun BottomQueueShortcut(
    revealProgress: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalWearPalette.current
    val clampedProgress = revealProgress.coerceIn(0f, 1f)
    if (clampedProgress <= 0.01f) return

    val containerColor by animateColorAsState(
        targetValue = if (enabled) palette.controlContainer else palette.controlDisabledContainer,
        animationSpec = spring(),
        label = "queueShortcutContainer",
    )
    val iconColor by animateColorAsState(
        targetValue = if (enabled) palette.controlContent else palette.controlDisabledContent,
        animationSpec = spring(),
        label = "queueShortcutIcon",
    )

    val edgeHeight = lerp(16.dp, 66.dp, clampedProgress)
    val iconSize = lerp(14.dp, 24.dp, clampedProgress)
    val containerAlpha = (clampedProgress * 1.1f).coerceIn(0f, 1f)

    EdgeButton(
        onClick = onClick,
        enabled = enabled && clampedProgress > 0.65f,
        buttonSize = EdgeButtonSize.Small,
        colors = M3ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = iconColor,
            disabledContainerColor = palette.controlDisabledContainer,
            disabledContentColor = palette.controlDisabledContent,
        ),
        modifier = modifier
            //.height(edgeHeight)
            .graphicsLayer {
                alpha = containerAlpha
                scaleY = 0.55f + (0.45f * clampedProgress)
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
        ,
    ) {
        M3Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = "Queue",
            modifier = Modifier.size(iconSize),
        )
    }
}
