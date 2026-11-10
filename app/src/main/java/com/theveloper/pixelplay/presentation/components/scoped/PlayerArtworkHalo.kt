package com.theveloper.pixelplay.presentation.components.scoped

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.theveloper.pixelplay.presentation.components.albumArtMemoryCacheKey
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.LocalPixelPlayDarkTheme
import com.theveloper.pixelplay.ui.theme.PixelMotion
import com.theveloper.pixelplay.utils.LocalArtworkUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal const val PlayerHaloDrawTrace = "PixelPlay.playerHalo.draw"
private const val ThumbnailSide = 32
private const val HaloSizeMultiplier = 1.25f

@Immutable
internal data class PlayerArtworkDepth(val expansion: () -> Float, val targetTint: Color)

internal val LocalPlayerArtworkDepth = compositionLocalOf<PlayerArtworkDepth?> { null }

@Composable
internal fun PlayerArtworkHalo(
    occurrence: String,
    uri: String?,
    tint: Color,
    paused: Boolean,
    visible: Boolean,
    bounds: () -> Rect?,
    modifier: Modifier = Modifier
) {
    val effects = LocalVisualEffects.current
    val animated = LocalMotionPreferences.current.animationsEnabled
    val tintState = animateColorAsState(
        targetValue = if (effects.allowsDepthEffects) tint else Color.Black,
        animationSpec = if (animated && effects.allowsDepthEffects) PixelMotion.structuralSlow() else snap(),
        label = "Artwork shadow tint"
    )
    val shape = remember { RoundedCornerShape(PlayerCarouselArtworkRadius) }
    Box(modifier) {
        Spacer(
            Modifier.artworkEffectBounds(bounds, scale = 1f)
                .graphicsLayer {
                    shadowElevation = 24.dp.toPx()
                    this.shape = shape
                    ambientShadowColor = if (effects.isMinimal) Color.Black else tintState.value.copy(alpha = 0.45f)
                    spotShadowColor = if (effects.isMinimal) Color.Black else tintState.value.copy(alpha = 0.55f)
                }
        )
        if (effects.allowsDepthEffects && !uri.isNullOrBlank()) {
            val thumbnail by rememberHaloThumbnail(occurrence, uri)
            val imageReady = thumbnail != null
            val scale = remember(occurrence, animated) { Animatable(if (animated) 0.92f else 1f) }
            var lastAnimatedOccurrence by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(occurrence, animated, visible, imageReady) {
                if (!animated) {
                    scale.snapTo(1f)
                } else if (visible && imageReady && lastAnimatedOccurrence != occurrence) {
                    lastAnimatedOccurrence = occurrence
                    scale.snapTo(0.92f)
                    scale.animateTo(1f, PixelMotion.feedback())
                } else if (visible && imageReady) {
                    scale.snapTo(1f)
                }
            }
            val opacity = animateFloatAsState(
                targetValue = (if (LocalPixelPlayDarkTheme.current) 0.55f else 0.35f) * if (paused) 0.6f else 1f,
                animationSpec = if (animated) tween(300) else snap(),
                label = "Artwork halo opacity"
            )
            val density = LocalDensity.current
            val hardware = LocalView.current.isHardwareAccelerated
            val radius = with(density) { 16.dp.toPx() }
            val blur = remember(effects.allowsRealtimeBlur, hardware, radius) {
                if (effects.allowsRealtimeBlur && hardware) {
                    runCatching {
                        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL).asComposeRenderEffect()
                    }.getOrNull()
                } else null
            }
            val padding = if (blur != null) 32.dp else 0.dp
            val bitmap = thumbnail
            if (bitmap != null) {
                Spacer(
                    Modifier.artworkEffectBounds(bounds, HaloSizeMultiplier, padding)
                        .graphicsLayer {
                            alpha = opacity.value
                            scaleX = scale.value
                            scaleY = scale.value
                            compositingStrategy = CompositingStrategy.Offscreen
                            renderEffect = blur
                        }
                        .drawWithCache {
                            val pad = padding.roundToPx()
                            val width = (size.width.roundToInt() - 2 * pad).coerceAtLeast(0)
                            val height = (size.height.roundToInt() - 2 * pad).coerceAtLeast(0)
                            val mask = Brush.radialGradient(
                                0f to Color.White,
                                0.72f to Color.White.copy(alpha = 0.85f),
                                1f to Color.Transparent,
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = (maxOf(width, height) / 2f).coerceAtLeast(1f)
                            )
                            onDrawBehind {
                                if (width == 0 || height == 0) return@onDrawBehind
                                tracePlayerDepthDraw(PlayerHaloDrawTrace) {
                                    drawImage(
                                        bitmap,
                                        dstOffset = IntOffset(pad, pad),
                                        dstSize = IntSize(width, height),
                                        filterQuality = FilterQuality.Low
                                    )
                                    drawRect(mask, blendMode = BlendMode.DstIn)
                                }
                            }
                        }
                )
            }
        }
    }
}

private fun Modifier.artworkEffectBounds(bounds: () -> Rect?, scale: Float, padding: Dp = 0.dp) =
    layout { measurable, constraints ->
        val rect = bounds()
        val pad = padding.roundToPx()
        val width = rect?.let { (it.width * scale).roundToInt() + 2 * pad } ?: 0
        val height = rect?.let { (it.height * scale).roundToInt() + 2 * pad } ?: 0
        val child = measurable.measure(Constraints.fixed(width, height))
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (rect != null && width > 0 && height > 0) {
                child.place((rect.center.x - width / 2f).roundToInt(), (rect.center.y - height / 2f).roundToInt())
            }
        }
    }

@Composable
private fun rememberHaloThumbnail(occurrence: String, uri: String): androidx.compose.runtime.State<ImageBitmap?> {
    val context = LocalContext.current
    // Key the state as well as the job so a new track cannot display the previous bitmap.
    return key(occurrence, uri) {
        produceState<ImageBitmap?>(initialValue = null, context) {
            val size = coil.size.Size(ThumbnailSide, ThumbnailSide)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(size)
                .scale(Scale.FILL)
                .allowHardware(false)
                .memoryCacheKey(albumArtMemoryCacheKey(uri, size))
                .diskCachePolicy(if (LocalArtworkUri.isLocalArtworkUri(uri)) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .build()
            val result = context.imageLoader.execute(request) as? SuccessResult ?: return@produceState
            value = withContext(Dispatchers.Default) {
                runCatching {
                    result.drawable.toBitmap(ThumbnailSide, ThumbnailSide, Bitmap.Config.ARGB_8888).asImageBitmap()
                }.getOrNull()
            }
        }
    }
}

internal fun Modifier.artworkLightEdge(mask: () -> Rect): Modifier = drawWithCache {
    val inset = 0.5.dp.toPx()
    val radius = (PlayerCarouselArtworkRadius.toPx() - inset).coerceAtLeast(0f)
    val bounds = mask()
    val brush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.22f),
        0.5f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.18f),
        startY = bounds.top,
        endY = maxOf(bounds.top + 1f, bounds.bottom)
    )
    onDrawWithContent {
        drawContent()
        if (bounds.width > 2 * inset && bounds.height > 2 * inset) {
            drawRoundRect(
                brush,
                topLeft = bounds.topLeft + Offset(inset, inset),
                size = Size(bounds.width - 2 * inset, bounds.height - 2 * inset),
                cornerRadius = CornerRadius(radius),
                style = Stroke(2 * inset)
            )
        }
    }
}
