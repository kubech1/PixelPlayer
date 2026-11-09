package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.theveloper.pixelplay.data.model.Song

internal val PlayerCarouselArtworkRadius = 18.dp

internal data class PlayerArtworkIdentity(val songId: String, val uri: String?, val queueIndex: Int)

internal enum class ArtworkEndpoint { MINI, CAROUSEL }

internal class ArtworkAnchor(val identity: PlayerArtworkIdentity, val layer: GraphicsLayer) {
    var coordinates by mutableStateOf<LayoutCoordinates?>(null)
    var recorded by mutableStateOf(false)
}

@Stable
internal class PlayerArtworkTransition(
    private val currentIdentity: State<PlayerArtworkIdentity?>,
    private val progress: State<() -> Float>,
    private val miniOffset: State<() -> Offset>,
    private val carouselOffset: State<() -> Offset>
) {
    val identity get() = currentIdentity.value
    var sheetViewport by mutableStateOf<LayoutCoordinates?>(null)
    var fullPlayerViewport by mutableStateOf<LayoutCoordinates?>(null)
    private var mini by mutableStateOf<ArtworkAnchor?>(null)
    private var carousel by mutableStateOf<ArtworkAnchor?>(null)
    private var armedFor by mutableStateOf<PlayerArtworkIdentity?>(null)
    private val clip = Path()
    val isMoving by derivedStateOf { progress.value().let { it > 0.001f && it < 0.999f } }
    private val replacesArtwork by derivedStateOf {
        isMoving && armedFor != null && armedFor == identity &&
            bounds(mini, sheetViewport) != null && bounds(carousel, fullPlayerViewport) != null
    }

    fun attach(endpoint: ArtworkEndpoint, anchor: ArtworkAnchor) {
        when (endpoint) {
            ArtworkEndpoint.MINI -> mini = anchor
            ArtworkEndpoint.CAROUSEL -> carousel = anchor
        }
    }

    fun detach(endpoint: ArtworkEndpoint, anchor: ArtworkAnchor) {
        when (endpoint) {
            ArtworkEndpoint.MINI -> if (mini === anchor) mini = null
            ArtworkEndpoint.CAROUSEL -> if (carousel === anchor) carousel = null
        }
    }

    private fun bounds(anchor: ArtworkAnchor?, viewport: LayoutCoordinates?): Rect? {
        val coords = anchor?.coordinates ?: return null
        if (anchor.identity != identity || !coords.isAttached || viewport?.isAttached != true) return null
        return viewport.localBoundingBoxOf(coords, clipBounds = false).takeUnless { it.isEmpty }
    }

    // The first moving frame decides whether this gesture can share its artwork.
    // If a cold or delayed carousel is still absent, keep the normal player fades.
    fun onMotionChanged(moving: Boolean) {
        armedFor = identity?.takeIf {
            moving && bounds(mini, sheetViewport) != null &&
                bounds(carousel, fullPlayerViewport) != null &&
                (mini?.recorded == true || carousel?.recorded == true)
        }
    }

    fun hides(anchor: ArtworkAnchor): Boolean =
        replacesArtwork && anchor.identity == armedFor

    fun draw(scope: DrawScope): Unit = with(scope) {
        if (!replacesArtwork) return
        val fraction = progress.value()
        val from = bounds(mini, sheetViewport)?.translate(miniOffset.value()) ?: return
        val to = bounds(carousel, fullPlayerViewport)?.translate(carouselOffset.value()) ?: return
        val movingBounds = interpolateArtworkBounds(from, to, fraction)
        val miniLayer = mini?.takeIf { it.recorded }?.layer
        val carouselLayer = carousel?.takeIf { it.recorded }?.layer
        val artwork = (if (fraction < 0.7f) miniLayer ?: carouselLayer else carouselLayer ?: miniLayer) ?: return
        if (artwork.size.width == 0 || artwork.size.height == 0) return

        val radius = lerp(from.minDimension / 2f, PlayerCarouselArtworkRadius.toPx(), fraction)
        clip.rewind()
        clip.addRoundRect(RoundRect(movingBounds, CornerRadius(radius)))
        clipPath(clip) {
            translate(movingBounds.left, movingBounds.top) {
                scale(
                    scaleX = movingBounds.width / artwork.size.width,
                    scaleY = movingBounds.height / artwork.size.height,
                    pivot = Offset.Zero
                ) { drawLayer(artwork) }
            }
        }
    }
}

private fun interpolateArtworkBounds(from: Rect, to: Rect, fraction: Float): Rect = Rect(
    lerp(from.left, to.left, fraction),
    lerp(from.top, to.top, fraction),
    lerp(from.right, to.right, fraction),
    lerp(from.bottom, to.bottom, fraction)
)

@Composable
internal fun rememberPlayerArtworkTransition(
    identity: PlayerArtworkIdentity?,
    enabled: Boolean,
    progress: () -> Float,
    miniOffset: () -> Offset,
    carouselOffset: () -> Offset
): PlayerArtworkTransition? {
    if (!enabled) return null
    val latestIdentity = rememberUpdatedState(identity)
    val latestProgress = rememberUpdatedState(progress)
    val latestMiniOffset = rememberUpdatedState(miniOffset)
    val latestCarouselOffset = rememberUpdatedState(carouselOffset)
    val transition = remember {
        PlayerArtworkTransition(latestIdentity, latestProgress, latestMiniOffset, latestCarouselOffset)
    }
    LaunchedEffect(transition) {
        snapshotFlow { transition.isMoving }.collect(transition::onMotionChanged)
    }
    return transition
}

@Composable
internal fun Modifier.capturePlayerArtwork(
    transition: PlayerArtworkTransition?,
    song: Song,
    endpoint: ArtworkEndpoint
): Modifier {
    val identity = transition?.identity ?: return this
    if (identity.songId != song.id || identity.uri != song.albumArtUriString) return this
    val layer = rememberGraphicsLayer()
    val anchor = remember(identity, layer) { ArtworkAnchor(identity, layer) }
    DisposableEffect(transition, endpoint, anchor) {
        transition.attach(endpoint, anchor)
        onDispose { transition.detach(endpoint, anchor) }
    }
    return remember(this, transition, anchor) {
        onPlaced { anchor.coordinates = it }.drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            anchor.recorded = true
            if (!transition.hides(anchor)) drawLayer(layer)
        }
    }
}
