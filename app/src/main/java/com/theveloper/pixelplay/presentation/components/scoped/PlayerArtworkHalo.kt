package com.theveloper.pixelplay.presentation.components.scoped

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
private const val BalancedHaloSize = 1.25f
private const val FullHaloSize = 1.9f
private const val HaloEntranceMillis = 550

private sealed interface HaloThumbnail {
    data object Loading : HaloThumbnail
    data class Ready(val bitmap: ImageBitmap?) : HaloThumbnail
}

private data class HaloBlend(val from: ImageBitmap?, val to: ImageBitmap?)

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
        if (effects.allowsDepthEffects) {
            val loaded = if (uri.isNullOrBlank()) HaloThumbnail.Ready(null)
            else rememberHaloThumbnail(occurrence, uri).value
            val progress = remember { Animatable(1f) }
            var blend by remember { mutableStateOf(HaloBlend(null, null)) }
            val full = effects.allowsRealtimeBlur
            LaunchedEffect(loaded, visible, animated, full) {
                if (!visible || loaded !is HaloThumbnail.Ready) return@LaunchedEffect
                val target = loaded.bitmap
                if (!animated) {
                    blend = HaloBlend(target, target)
                    progress.snapTo(1f)
                } else if (blend.to !== target) {
                    val previous = if (progress.value >= 0.5f) blend.to else blend.from
                    progress.snapTo(0f)
                    blend = HaloBlend(previous, target)
                }
                if (progress.value < 1f) {
                    progress.animateTo(1f, tween(
                        durationMillis = if (full) HaloEntranceMillis else 300,
                        easing = FastOutSlowInEasing
                    ))
                    blend = HaloBlend(target, target)
                }
            }
            val dark = LocalPixelPlayDarkTheme.current
            val baseOpacity = if (full) {
                if (dark) 0.80f else 0.68f
            } else {
                if (dark) 0.55f else 0.35f
            }
            val opacity = animateFloatAsState(
                targetValue = baseOpacity * if (paused) (if (full) 0.8f else 0.6f) else 1f,
                animationSpec = if (animated) tween(300) else snap(),
                label = "Artwork halo opacity"
            )
            val density = LocalDensity.current
            val hardware = LocalView.current.isHardwareAccelerated
            val radius = with(density) { 16.dp.toPx() }
            val blur = remember(full, hardware, radius) {
                if (full && hardware) {
                    runCatching {
                        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL).asComposeRenderEffect()
                    }.getOrNull()
                } else null
            }
            val padding = if (blur != null) 32.dp else 0.dp
            val hasImage = blend.from != null || blend.to != null
            if (hasImage) {
                Spacer(
                    Modifier.artworkEffectBounds(
                        bounds,
                        scale = if (full) FullHaloSize else BalancedHaloSize,
                        padding = padding,
                        upwardBias = if (full) 0.02f else 0f
                    )
                        .graphicsLayer {
                            val fade = progress.value
                            val (from, to) = blend
                            alpha = opacity.value * when {
                                from == null -> fade
                                to == null -> 1f - fade
                                else -> 1f
                            }
                            val growth = if (from == null && to != null) 0.96f + 0.04f * fade else 1f
                            scaleX = growth
                            scaleY = growth
                            compositingStrategy = CompositingStrategy.Offscreen
                            renderEffect = blur
                        }
                        .drawWithCache {
                            val pad = padding.roundToPx()
                            val width = (size.width.roundToInt() - 2 * pad).coerceAtLeast(0)
                            val height = (size.height.roundToInt() - 2 * pad).coerceAtLeast(0)
                            val mask = Brush.radialGradient(
                                0f to Color.White,
                                0.68f to Color.White.copy(alpha = 0.88f),
                                0.84f to Color.White.copy(alpha = if (full) 0.42f else 0.48f),
                                1f to Color.Transparent,
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = (maxOf(width, height) / 2f).coerceAtLeast(1f)
                            )
                            val imageOffset = IntOffset(pad, pad)
                            val imageSize = IntSize(width, height)
                            onDrawBehind {
                                if (width == 0 || height == 0) return@onDrawBehind
                                tracePlayerDepthDraw(PlayerHaloDrawTrace) {
                                    val (from, to) = blend
                                    fun drawThumbnail(image: ImageBitmap, alpha: Float = 1f) {
                                        drawImage(
                                            image,
                                            dstOffset = imageOffset,
                                            dstSize = imageSize,
                                            alpha = alpha,
                                            filterQuality = FilterQuality.Low
                                        )
                                    }
                                    when {
                                        from == null -> to?.let { drawThumbnail(it) }
                                        to == null -> drawThumbnail(from)
                                        from === to -> drawThumbnail(to)
                                        else -> {
                                            val fade = progress.value
                                            if (fade < 1f) drawThumbnail(from)
                                            drawThumbnail(to, fade)
                                        }
                                    }
                                    // Soften artwork extremes behind the header and track text.
                                    if (full) {
                                        drawRect(
                                            (if (dark) Color.Black else Color.White).copy(alpha = if (dark) 0.24f else 0.36f),
                                            blendMode = BlendMode.SrcAtop
                                        )
                                    }
                                    drawRect(mask, blendMode = BlendMode.DstIn)
                                }
                            }
                        }
                )
            }
        }
    }
}

private fun Modifier.artworkEffectBounds(
    bounds: () -> Rect?,
    scale: Float,
    padding: Dp = 0.dp,
    upwardBias: Float = 0f
) =
    layout { measurable, constraints ->
        val rect = bounds()
        val pad = padding.roundToPx()
        val width = rect?.let { (it.width * scale).roundToInt() + 2 * pad } ?: 0
        val height = rect?.let { (it.height * scale).roundToInt() + 2 * pad } ?: 0
        val child = measurable.measure(Constraints.fixed(width, height))
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (rect != null && width > 0 && height > 0) {
                child.place((rect.center.x - width / 2f).roundToInt(), (rect.center.y - height / 2f - rect.height * upwardBias).roundToInt())
            }
        }
    }

@Composable
private fun rememberHaloThumbnail(occurrence: String, uri: String): androidx.compose.runtime.State<HaloThumbnail> {
    val context = LocalContext.current
    // Key the state as well as the job so a new track cannot display the previous bitmap.
    return key(occurrence, uri) {
        produceState<HaloThumbnail>(initialValue = HaloThumbnail.Loading, context) {
            val size = coil.size.Size(ThumbnailSide, ThumbnailSide)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(size)
                .scale(Scale.FILL)
                .allowHardware(false)
                .memoryCacheKey(albumArtMemoryCacheKey(uri, size))
                .diskCachePolicy(if (LocalArtworkUri.isLocalArtworkUri(uri)) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .build()
            val result = context.imageLoader.execute(request) as? SuccessResult
            val bitmap = result?.let { success ->
                withContext(Dispatchers.Default) {
                    runCatching {
                        val thumbnail = success.drawable.toBitmap(ThumbnailSide, ThumbnailSide, Bitmap.Config.ARGB_8888)
                        smoothHaloThumbnail(thumbnail).asImageBitmap()
                    }.getOrNull()
                }
            }
            value = HaloThumbnail.Ready(bitmap)
        }
    }
}

// Filter the colors at thumbnail size before stretching them across the screen.
private fun smoothHaloThumbnail(thumbnail: Bitmap): Bitmap {
    val width = thumbnail.width
    val height = thumbnail.height
    val pixels = IntArray(width * height)
    val horizontal = IntArray(pixels.size)
    val filtered = IntArray(pixels.size)
    val kernel = intArrayOf(1, 4, 6, 4, 1)
    thumbnail.getPixels(pixels, 0, width, 0, 0, width, height)

    fun blur(source: IntArray, destination: IntArray, alongRows: Boolean) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                var alpha = 0
                var red = 0
                var green = 0
                var blue = 0
                for (index in kernel.indices) {
                    val offset = index - kernel.size / 2
                    val sampleX = if (alongRows) (x + offset).coerceIn(0, width - 1) else x
                    val sampleY = if (alongRows) y else (y + offset).coerceIn(0, height - 1)
                    val pixel = source[sampleY * width + sampleX]
                    val weight = (pixel ushr 24) * kernel[index]
                    alpha += weight
                    red += ((pixel ushr 16) and 255) * weight
                    green += ((pixel ushr 8) and 255) * weight
                    blue += (pixel and 255) * weight
                }
                if (alpha > 0) {
                    destination[y * width + x] = android.graphics.Color.argb(
                        (alpha + 8) / 16,
                        (red + alpha / 2) / alpha,
                        (green + alpha / 2) / alpha,
                        (blue + alpha / 2) / alpha
                    )
                }
            }
        }
    }

    blur(pixels, horizontal, alongRows = true)
    blur(horizontal, filtered, alongRows = false)
    return Bitmap.createBitmap(filtered, width, height, Bitmap.Config.ARGB_8888)
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
