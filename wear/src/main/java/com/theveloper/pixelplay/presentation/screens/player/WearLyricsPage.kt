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
internal fun WearLyricsPage(
    state: WearPlayerState,
    isPhoneConnected: Boolean,
    isWatchOutputSelected: Boolean,
    isAmbient: Boolean,
) {
    val palette = LocalWearPalette.current
    val columnState = rememberResponsiveColumnState()
    val lyrics = state.lyrics
    val syncedLines = lyrics?.synced.orEmpty()
    val activeLineIndex by rememberActiveLyricLineIndex(
        state = state,
        lines = syncedLines,
    )

    LaunchedEffect(state.songId, activeLineIndex, isAmbient) {
        if (activeLineIndex >= 0) {
            val targetItem = (activeLineIndex + 2).coerceAtLeast(0)
            if (isAmbient) {
                columnState.state.scrollToItem(targetItem)
            } else {
                columnState.state.animateScrollToItem(targetItem)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            columnState = columnState,
        ) {
            item { Spacer(modifier = Modifier.height(28.dp)) }

            item {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.caption2,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (lyrics?.hasLyrics == true) {
                if (syncedLines.isNotEmpty()) {
                    syncedLines.forEachIndexed { index, line ->
                        item(key = "${line.timeMs}-$index") {
                            WearSyncedLyricLineItem(
                                line = line,
                                active = index == activeLineIndex,
                                isAmbient = isAmbient,
                            )
                        }
                    }
                } else {
                    lyrics.plain.forEachIndexed { index, line ->
                        item(key = "plain-$index") {
                            WearPlainLyricLineItem(line = line)
                        }
                    }
                }
            } else {
                item {
                    WearLyricsEmptyState(
                        isPhoneConnected = isPhoneConnected,
                        isWatchOutputSelected = isWatchOutputSelected,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(42.dp)) }
        }

        AlwaysOnScalingPositionIndicator(
            listState = columnState.state,
            modifier = Modifier.align(Alignment.CenterEnd),
            color = palette.textPrimary,
            )
    }
}

@Composable
internal fun WearSyncedLyricLineItem(
    line: WearSyncedLyricLine,
    active: Boolean,
    isAmbient: Boolean,
) {
    val palette = LocalWearPalette.current
    val targetTextColor = if (active) palette.textPrimary else palette.textSecondary.copy(alpha = 0.72f)
    val textColor = if (isAmbient) {
        targetTextColor
    } else {
        animateColorAsState(
            targetValue = targetTextColor,
            animationSpec = spring(),
            label = "wearLyricLineColor",
        ).value
    }
    val scale = if (isAmbient) {
        1f
    } else {
        animateFloatAsState(
            targetValue = if (active) 1.08f else 1f,
            animationSpec = spring(),
            label = "wearLyricLineScale",
        ).value
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = line.line.ifBlank { line.translation.orEmpty() },
            style = if (active) MaterialTheme.typography.body1 else MaterialTheme.typography.caption1,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        val secondaryText = line.translation?.takeIf { it.isNotBlank() }
            ?: line.romanization?.takeIf { it.isNotBlank() }
        if (active && !secondaryText.isNullOrBlank() && secondaryText != line.line) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.caption2,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}

@Composable
internal fun WearPlainLyricLineItem(line: String) {
    val palette = LocalWearPalette.current
    Text(
        text = line,
        style = MaterialTheme.typography.caption1,
        color = palette.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
    )
}

@Composable
internal fun WearLyricsEmptyState(
    isPhoneConnected: Boolean,
    isWatchOutputSelected: Boolean,
) {
    val palette = LocalWearPalette.current
    val message = when {
        isWatchOutputSelected -> "Phone lyrics sync is available while controlling phone playback"
        !isPhoneConnected -> "Connect phone to sync lyrics"
        else -> "No lyrics synced yet"
    }
    Text(
        text = message,
        style = MaterialTheme.typography.caption1,
        color = palette.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    )
}

internal fun List<WearSyncedLyricLine>.activeLyricLineIndex(positionMs: Long): Int {
    if (isEmpty()) return -1
    val index = indexOfLast { it.timeMs.toLong() <= positionMs }
    return index.coerceAtLeast(0)
}

@Composable
internal fun rememberActiveLyricLineIndex(
    state: WearPlayerState,
    lines: List<WearSyncedLyricLine>,
): androidx.compose.runtime.State<Int> {
    val safeDuration = state.totalDurationMs.coerceAtLeast(0L)
    val safeAnchorPosition = state.currentPositionMs.coerceIn(0L, safeDuration)
    val positionKey = remember(
        state.songId,
        safeAnchorPosition,
        safeDuration,
        state.isPlaying,
        state.positionUpdatedElapsedRealtimeMs,
        lines,
    ) {
        "${state.songId}|$safeAnchorPosition|$safeDuration|${state.isPlaying}|${state.positionUpdatedElapsedRealtimeMs}|${lines.size}|${lines.firstOrNull()?.timeMs}|${lines.lastOrNull()?.timeMs}"
    }

    return produceState(
        initialValue = lines.activeLyricLineIndex(
            state.livePositionFromAnchor(safeAnchorPosition, safeDuration),
        ),
        key1 = positionKey,
    ) {
        if (lines.isEmpty()) {
            value = -1
            return@produceState
        }

        while (true) {
            val livePositionMs = state.livePositionFromAnchor(safeAnchorPosition, safeDuration)
            val currentIndex = lines.activeLyricLineIndex(livePositionMs)
            value = currentIndex

            if (!state.isPlaying || safeDuration <= 0L) {
                return@produceState
            }

            val nextLineTimeMs = lines.getOrNull(currentIndex + 1)?.timeMs?.toLong()
                ?: return@produceState
            val delayUntilNextLine = (nextLineTimeMs - livePositionMs)
                .coerceIn(80L, 60_000L)
            delay(delayUntilNextLine)
        }
    }
}
