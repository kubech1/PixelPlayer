package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import com.theveloper.pixelplay.data.model.HomeMix
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion

@Composable
internal fun HomeMixArtwork(
    mix: HomeMix,
    modifier: Modifier = Modifier,
    shineProgress: (() -> Float)? = null,
) {
    val selection = remember(mix.kind, mix.songs) { rotatingComposition(mix) }
    val candidates = remember(mix.cover, mix.songs) { albumCandidates(mix) }
    var failed by rememberSaveable(candidates) { mutableStateOf(arrayListOf<String>()) }
    var failedAt by rememberSaveable(candidates) { mutableLongStateOf(0L) }
    LaunchedEffect(candidates) {
        val age = System.currentTimeMillis() - failedAt
        if (failedAt != 0L && (age < 0L || age >= 60_000L)) {
            failed = arrayListOf()
            failedAt = 0L
        }
    }
    val daily = mix.kind == HomeMixKind.DAILY
    val covers = candidates.asSequence()
        .filterNot { it.uri in failed }
        .distinctBy { it.album ?: it.uri }
        .take(if (daily) DailyAlbumPlacements.size else 1)
        .toList()
    val animationsEnabled = LocalMotionPreferences.current.animationsEnabled
    val effects = LocalVisualEffects.current
    val coverFade = if (animationsEnabled) PixelMotion.QUICK_DURATION_MS else 0

    BoxWithConstraints(modifier.clip(RoundedCornerShape(16.dp))) {
        val flipX = selection?.flipX == true
        val flipY = selection?.flipY == true
        Canvas(Modifier.fillMaxSize()) {
            withMixPosterCoordinates(flipX, flipY) {
                drawMixPoster(mix.kind, effects.allowsDepthEffects)
            }
        }
        if (shineProgress != null) {
            Canvas(Modifier.fillMaxSize()) {
                val fraction = shineProgress()
                if (fraction > 0f && fraction < 1f) {
                    withMixPosterCoordinates(flipX, flipY) {
                        drawMixPosterShine(mix.kind, fraction)
                    }
                }
            }
        }
        covers.forEachIndexed { index, cover ->
            val placement = if (daily) DailyAlbumPlacements[index] else requireNotNull(selection).placement
            key(cover.uri) {
                var ready by remember { mutableStateOf(false) }
                SmartImage(
                    model = cover.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset(maxWidth * placement.left, maxWidth * placement.top)
                        .size(maxWidth * placement.fraction)
                        .graphicsLayer {
                            alpha = if (ready) 1f else 0f
                            shadowElevation = if (effects.allowsDepthEffects) 3.dp.toPx() else 0f
                            shape = placement.shape
                        }
                        .border(1.dp, Color.White.copy(alpha = .38f), placement.shape),
                    shape = placement.shape,
                    targetSize = MixCoverRequestSize,
                    crossfadeDurationMillis = coverFade,
                    onState = { state ->
                        ready = state is AsyncImagePainter.State.Success
                        if (state is AsyncImagePainter.State.Error && cover.uri !in failed) {
                            failed = ArrayList(failed).apply { add(cover.uri) }
                            failedAt = System.currentTimeMillis()
                        }
                    },
                )
            }
        }
    }
}

/** The detail screen owns the animation so scrolling the artwork offscreen cannot restart it. */
@Composable
internal fun rememberMixPosterShine(mix: HomeMix?): (() -> Float)? {
    val enabled = LocalMotionPreferences.current.animationsEnabled && LocalVisualEffects.current.allowsDepthEffects
    if (!enabled || mix == null || mix.kind == HomeMixKind.GENRE) return null
    val progress = remember(mix.id) { Animatable(0f) }
    LaunchedEffect(progress) {
        progress.animateTo(1f, tween(PixelMotion.STANDARD_DURATION_MS))
    }
    return remember(progress) { { progress.value } }
}

private data class AlbumPlacement(
    val left: Float,
    val top: Float,
    val fraction: Float,
    val shape: Shape,
)

private data class RotatingComposition(
    val placement: AlbumPlacement,
    val flipX: Boolean,
    val flipY: Boolean,
)

private data class AlbumCandidate(val uri: String, val album: String?)

private val MixCoverRequestSize = coil.size.Size(288, 288)
private val SoftSquare = RoundedCornerShape(16)
private val Squircle = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(.5f * w, 0f)
    cubicTo(.94f * w, 0f, w, .06f * h, w, .5f * h)
    cubicTo(w, .94f * h, .94f * w, h, .5f * w, h)
    cubicTo(.06f * w, h, 0f, .94f * h, 0f, .5f * h)
    cubicTo(0f, .06f * h, .06f * w, 0f, .5f * w, 0f)
    close()
}

private val DailyAlbumPlacements = listOf(
    AlbumPlacement(.095f, .085f, .32f, SoftSquare),
    AlbumPlacement(.32f, .29f, .33f, SoftSquare),
    AlbumPlacement(.55f, .43f, .32f, SoftSquare),
)

private fun rotatingComposition(mix: HomeMix): RotatingComposition? {
    val base = when (mix.kind) {
        HomeMixKind.DAILY -> return null
        HomeMixKind.UNDERPLAYED -> AlbumPlacement(.47f, .025f, .49f, CircleShape)
        HomeMixKind.REDISCOVER -> AlbumPlacement(-.045f, .51f, .60f, Squircle)
        HomeMixKind.ARTIST -> AlbumPlacement(-.095f, .155f, .53f, SoftSquare)
        HomeMixKind.GENRE -> AlbumPlacement(.14f, -.09f, .50f, Squircle)
        HomeMixKind.RECENTLY_ADDED -> AlbumPlacement(.47f, -.055f, .47f, SoftSquare)
    }
    // Use the ordered IDs already persisted by the mix. Date refreshes and regenerated
    // selections can change the composition without making it depend on image load order.
    val seed = mix.songs.fold(mix.kind.ordinal) { hash, song -> 31 * hash + song.id.hashCode() }
    val alternate = seed.ushr(4) and 1 == 1
    val flipY = alternate && mix.kind == HomeMixKind.REDISCOVER
    val flipX = alternate && !flipY
    val placement = base.copy(
        left = if (flipX) 1f - base.left - base.fraction else base.left,
        top = if (flipY) 1f - base.top - base.fraction else base.top,
        shape = if (alternate) {
            when (mix.kind) {
                HomeMixKind.REDISCOVER -> SoftSquare
                HomeMixKind.ARTIST, HomeMixKind.RECENTLY_ADDED -> Squircle
                HomeMixKind.GENRE -> CircleShape
                else -> base.shape
            }
        } else base.shape,
    )
    return RotatingComposition(placement, flipX, flipY)
}

private fun albumCandidates(mix: HomeMix): List<AlbumCandidate> =
    (sequenceOf(mix.cover).filterNotNull() + mix.songs.asSequence()).mapNotNull { song ->
        val uri = song.albumArtUriString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val album = song.album.takeIf { it.isNotBlank() }?.let { title ->
            val artist = song.albumArtist?.takeIf { it.isNotBlank() } ?: song.artist
            "${title.trim().lowercase()}\u0000${artist.trim().lowercase()}"
        }
        AlbumCandidate(uri, album)
    }.distinctBy { it.uri }.toList()
