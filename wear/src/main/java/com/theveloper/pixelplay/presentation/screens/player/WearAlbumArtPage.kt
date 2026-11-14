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
internal fun AlbumArtPage(
    state: WearPlayerState,
    albumArt: Bitmap?,
    onTap: () -> Unit,
) {
    val palette = LocalWearPalette.current
    val textColors = remember(albumArt, palette.textPrimary, palette.textSecondary) {
        deriveAlbumOverlayTextColors(
            albumArt = albumArt,
            fallbackPrimary = palette.textPrimary,
            fallbackSecondary = palette.textSecondary,
        )
    }
    val contrastOverlay = remember(albumArt, textColors) {
        deriveAlbumContrastOverlay(
            albumArt = albumArt,
            textColors = textColors,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onTap),
    ) {
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val fallbackBrush = remember(palette) { palette.radialBackgroundBrush() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackBrush),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.78f to Color.Transparent,
                            0.94f to Color.Black.copy(alpha = 0.52f),
                            1f to Color.Black.copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.32f),
                            0.14f to Color.Black.copy(alpha = 0.08f),
                            0.24f to Color.Transparent,
                            0.68f to Color.Transparent,
                            0.86f to Color.Black.copy(alpha = 0.10f),
                            1f to Color.Black.copy(alpha = 0.34f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to contrastOverlay.topColor.copy(alpha = contrastOverlay.topAlpha),
                            contrastOverlay.topFadeEnd to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            contrastOverlay.bottomFadeStart to Color.Transparent,
                            1f to contrastOverlay.bottomColor.copy(alpha = contrastOverlay.bottomAlpha),
                        ),
                    ),
                ),
        )

        LargeAlbumClockText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .zIndex(5f),
            color = textColors.clock,
            shadow = textColors.clockShadow,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.songTitle.ifEmpty { "No song playing" },
                style = MaterialTheme.typography.title2.copy(
                    shadow = textColors.bottomShadow,
                ),
                fontWeight = FontWeight.Bold,
                color = textColors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.artistName.ifEmpty { "Connect phone playback" },
                style = MaterialTheme.typography.body1.copy(
                    shadow = textColors.bottomShadow,
                ),
                color = textColors.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun LargeAlbumClockText(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    shadow: Shadow? = null,
) {
    val displayTime by produceState(initialValue = "--:--") {
        val formatter = DateTimeFormatter.ofPattern("H:mm")
        while (true) {
            val now = LocalTime.now()
            value = now.format(formatter)
            // Sleep until the next minute boundary so we never recompose mid-minute.
            val secondsToNextMinute = (60 - now.second).coerceAtLeast(1)
            delay(secondsToNextMinute * 1000L)
        }
    }
    val gSansFlex = remember {
        FontFamily(
            Font(
                resId = R.font.gflex_variable,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(650),
                    FontVariation.width(146f),
                    FontVariation.Setting("ROND", 56f),
                    FontVariation.Setting("XTRA", 520f),
                    FontVariation.Setting("YOPQ", 90f),
                    FontVariation.Setting("YTLC", 505f),
                ),
            ),
        )
    }

    Text(
        text = displayTime,
        color = color,
        fontFamily = gSansFlex,
        fontWeight = FontWeight(760),
        fontSize = 26.sp,
        lineHeight = 26.sp,
        style = if (shadow != null) TextStyle(shadow = shadow) else TextStyle.Default,
        modifier = modifier,
    )
}

internal data class AlbumOverlayTextColors(
    val clock: Color,
    val title: Color,
    val artist: Color,
    val clockShadow: Shadow,
    val bottomShadow: Shadow,
)

internal data class AlbumContrastOverlay(
    val topColor: Color,
    val topAlpha: Float,
    val topFadeEnd: Float,
    val bottomColor: Color,
    val bottomAlpha: Float,
    val bottomFadeStart: Float,
)

internal fun deriveAlbumOverlayTextColors(
    albumArt: Bitmap?,
    fallbackPrimary: Color,
    fallbackSecondary: Color,
): AlbumOverlayTextColors {
    if (albumArt == null || albumArt.width <= 0 || albumArt.height <= 0) {
        val defaultShadow = Shadow(
            color = Color.Black.copy(alpha = 0.56f),
            offset = Offset(0f, 1.6f),
            blurRadius = 5f,
        )
        return AlbumOverlayTextColors(
            clock = fallbackPrimary,
            title = fallbackPrimary,
            artist = fallbackSecondary.copy(alpha = 0.92f),
            clockShadow = defaultShadow,
            bottomShadow = defaultShadow,
        )
    }

    val topBg = sampleRegionAverageColor(albumArt, startYFraction = 0f, endYFraction = 0.24f)
    val bottomBg = sampleRegionAverageColor(albumArt, startYFraction = 0.66f, endYFraction = 1f)

    val preferredClock = deriveClockTintFromAlbumArt(albumArt, fallbackPrimary)
    val clockColor = deriveReadableTintedColor(
        preferred = preferredClock,
        background = topBg,
        minContrast = 4.8,
        tintStrength = 0.34f,
    )
    val titleColor = deriveReadableTintedColor(
        preferred = fallbackPrimary.copy(alpha = 0.99f),
        background = bottomBg,
        minContrast = 5.2,
        tintStrength = 0.18f,
    )
    val artistColor = deriveReadableTintedColor(
        preferred = fallbackSecondary.copy(alpha = 0.96f),
        background = bottomBg,
        minContrast = 4.4,
        tintStrength = 0.22f,
    ).copy(alpha = 0.97f)

    val clockShadow = if (clockColor.luminance() < 0.5f) {
        Shadow(
            color = Color.White.copy(alpha = 0.36f),
            offset = Offset(0f, 1.2f),
            blurRadius = 4f,
        )
    } else {
        Shadow(
            color = Color.Black.copy(alpha = 0.58f),
            offset = Offset(0f, 1.6f),
            blurRadius = 5f,
        )
    }
    val bottomShadow = if (titleColor.luminance() < 0.5f) {
        Shadow(
            color = Color.White.copy(alpha = 0.30f),
            offset = Offset(0f, 1.2f),
            blurRadius = 4f,
        )
    } else {
        Shadow(
            color = Color.Black.copy(alpha = 0.55f),
            offset = Offset(0f, 1.6f),
            blurRadius = 5f,
        )
    }

    return AlbumOverlayTextColors(
        clock = clockColor,
        title = titleColor,
        artist = artistColor,
        clockShadow = clockShadow,
        bottomShadow = bottomShadow,
    )
}

internal fun deriveAlbumContrastOverlay(
    albumArt: Bitmap?,
    textColors: AlbumOverlayTextColors,
): AlbumContrastOverlay {
    if (albumArt == null || albumArt.width <= 0 || albumArt.height <= 0) {
        return AlbumContrastOverlay(
            topColor = Color.Black,
            topAlpha = 0.30f,
            topFadeEnd = 0.30f,
            bottomColor = Color.Black,
            bottomAlpha = 0.36f,
            bottomFadeStart = 0.64f,
        )
    }

    val topBg = sampleRegionAverageColor(albumArt, startYFraction = 0f, endYFraction = 0.24f)
    val bottomBg = sampleRegionAverageColor(albumArt, startYFraction = 0.62f, endYFraction = 1f)

    val topBase = if (textColors.clock.luminance() > 0.5f) Color.Black else Color.White
    val topAlpha = solveScrimAlphaForContrast(
        textColor = textColors.clock,
        backgroundColor = topBg,
        scrimBaseColor = topBase,
        minContrast = 6.0,
        extraHeadroom = 0.10f,
    )

    val bottomBase = if (textColors.title.luminance() > 0.5f) Color.Black else Color.White
    val bottomTitleAlpha = solveScrimAlphaForContrast(
        textColor = textColors.title,
        backgroundColor = bottomBg,
        scrimBaseColor = bottomBase,
        minContrast = 6.4,
        extraHeadroom = 0.10f,
    )
    val bottomArtistAlpha = solveScrimAlphaForContrast(
        textColor = textColors.artist,
        backgroundColor = bottomBg,
        scrimBaseColor = bottomBase,
        minContrast = 5.1,
        extraHeadroom = 0.08f,
    )

    return AlbumContrastOverlay(
        topColor = topBase,
        topAlpha = topAlpha,
        topFadeEnd = 0.33f,
        bottomColor = bottomBase,
        bottomAlpha = max(bottomTitleAlpha, bottomArtistAlpha),
        bottomFadeStart = 0.56f,
    )
}

internal fun solveScrimAlphaForContrast(
    textColor: Color,
    backgroundColor: Color,
    scrimBaseColor: Color,
    minContrast: Double,
    extraHeadroom: Float,
): Float {
    val opaqueBg = toOpaqueArgb(backgroundColor)
    val textArgb = textColor.toArgb()
    val currentContrast = ColorUtils.calculateContrast(textArgb, opaqueBg)
    if (currentContrast >= minContrast) {
        return 0f
    }

    var alpha = 0f
    while (alpha <= 0.84f) {
        val scrimArgb = ColorUtils.setAlphaComponent(
            scrimBaseColor.toArgb(),
            (alpha * 255f).toInt().coerceIn(0, 255),
        )
        val compositedBg = ColorUtils.compositeColors(scrimArgb, opaqueBg)
        val contrast = ColorUtils.calculateContrast(textArgb, compositedBg)
        if (contrast >= minContrast) {
            return (alpha + extraHeadroom).coerceIn(0f, 0.84f)
        }
        alpha += 0.03f
    }

    return 0.84f
}

internal fun toOpaqueArgb(color: Color): Int {
    val argb = color.toArgb()
    return if (android.graphics.Color.alpha(argb) >= 255) {
        argb
    } else {
        ColorUtils.compositeColors(argb, Color.Black.toArgb())
    }
}

internal fun deriveReadableTintedColor(
    preferred: Color,
    background: Color,
    minContrast: Double,
    tintStrength: Float,
): Color {
    val lightBase = Color(0xFFF7F7F7)
    val darkBase = Color(0xFF111111)

    val lightContrast = ColorUtils.calculateContrast(lightBase.toArgb(), background.toArgb())
    val darkContrast = ColorUtils.calculateContrast(darkBase.toArgb(), background.toArgb())
    val highContrastBase = if (lightContrast >= darkContrast) lightBase else darkBase

    val preferredContrast = ColorUtils.calculateContrast(preferred.toArgb(), background.toArgb())
    if (preferredContrast >= minContrast) return preferred

    val clampedTintStrength = tintStrength.coerceIn(0f, 0.5f)
    val tintSteps = floatArrayOf(
        clampedTintStrength,
        clampedTintStrength * 0.72f,
        clampedTintStrength * 0.46f,
        clampedTintStrength * 0.24f,
        0f,
    )

    tintSteps.forEach { blend ->
        val candidate = androidx.compose.ui.graphics.lerp(highContrastBase, preferred, blend)
        val contrast = ColorUtils.calculateContrast(candidate.toArgb(), background.toArgb())
        if (contrast >= minContrast) {
            return candidate
        }
    }

    return highContrastBase
}

internal fun sampleRegionAverageColor(
    albumArt: Bitmap,
    startYFraction: Float,
    endYFraction: Float,
): Color {
    val width = albumArt.width
    val height = albumArt.height
    val yStart = (height * startYFraction).toInt().coerceIn(0, height - 1)
    val yEnd = (height * endYFraction).toInt().coerceIn(yStart + 1, height)
    val step = (min(width, (yEnd - yStart).coerceAtLeast(1)) / 24).coerceAtLeast(1)

    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    var count = 0L

    var y = yStart
    while (y < yEnd) {
        var x = 0
        while (x < width) {
            val pixel = albumArt.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha >= 28) {
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                if (r + g + b > 30) {
                    redSum += r
                    greenSum += g
                    blueSum += b
                    count++
                }
            }
            x += step
        }
        y += step
    }

    if (count == 0L) return Color.Black
    return Color(
        android.graphics.Color.rgb(
            (redSum / count).toInt(),
            (greenSum / count).toInt(),
            (blueSum / count).toInt(),
        )
    )
}

internal fun deriveClockTintFromAlbumArt(albumArt: Bitmap?, fallback: Color): Color {
    if (albumArt == null || albumArt.width <= 0 || albumArt.height <= 0) return fallback

    val width = albumArt.width
    val height = albumArt.height
    val step = (min(width, height) / 28).coerceAtLeast(1)

    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    var count = 0L

    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = albumArt.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha >= 28) {
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                if (r + g + b > 30) {
                    redSum += r
                    greenSum += g
                    blueSum += b
                    count++
                }
            }
            x += step
        }
        y += step
    }

    if (count == 0L) return fallback

    val avgColor = android.graphics.Color.rgb(
        (redSum / count).toInt(),
        (greenSum / count).toInt(),
        (blueSum / count).toInt(),
    )
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(avgColor, hsl)
    hsl[1] = max(0.42f, hsl[1]).coerceAtMost(0.92f)
    hsl[2] = max(0.68f, hsl[2]).coerceAtMost(0.90f)

    var tinted = Color(ColorUtils.HSLToColor(hsl))
    val lum = tinted.luminance()
    tinted = when {
        lum < 0.60f -> androidx.compose.ui.graphics.lerp(tinted, Color.White, 0.32f)
        lum > 0.92f -> androidx.compose.ui.graphics.lerp(tinted, Color.White, 0.08f)
        else -> tinted
    }
    return tinted.copy(alpha = 0.98f)
}
