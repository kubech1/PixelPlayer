package com.theveloper.pixelplay.presentation.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.theveloper.pixelplay.data.model.HomeMixKind
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val MixPosterSide = 1000f
private val PosterSize = Size(MixPosterSide, MixPosterSide)

internal fun DrawScope.withMixPosterCoordinates(
    flipX: Boolean,
    flipY: Boolean,
    draw: DrawScope.() -> Unit,
) {
    val scaleX = size.width / MixPosterSide
    val scaleY = size.height / MixPosterSide
    withTransform({
        translate(if (flipX) size.width else 0f, if (flipY) size.height else 0f)
        scale(if (flipX) -scaleX else scaleX, if (flipY) -scaleY else scaleY, Offset.Zero)
    }, draw)
}

internal fun DrawScope.drawMixPoster(kind: HomeMixKind, fineDetail: Boolean) {
    when (kind) {
        HomeMixKind.DAILY -> drawCopperGrooves(fineDetail)
        HomeMixKind.UNDERPLAYED -> drawPearlLoop()
        HomeMixKind.REDISCOVER -> drawVioletFold()
        HomeMixKind.ARTIST -> drawPrism()
        HomeMixKind.GENRE -> drawStarTrails()
        HomeMixKind.RECENTLY_ADDED -> drawCoralFan()
    }
    if (fineDetail) {
        PosterGrain.forEachIndexed { index, dot ->
            drawCircle(
                if (index % 2 == 0) Color.White.copy(alpha = .095f) else Color.Black.copy(alpha = .14f),
                radius = if (index % 3 == 0) 2f else 1.25f,
                center = dot,
            )
        }
    }
}

/** Representative colors from the poster's ground and illuminated shapes. */
internal fun mixPosterColorScheme(kind: HomeMixKind, theme: ColorScheme, dark: Boolean): ColorScheme {
    val (primary, secondary, tertiary) = when (kind) {
        HomeMixKind.DAILY -> Triple(Color(0xFFDA8561), Color(0xFF9F4C90), Color(0xFFFFBF83))
        HomeMixKind.UNDERPLAYED -> Triple(Color(0xFF20768D), Color(0xFF62D9D0), Color(0xFFF5C5E0))
        HomeMixKind.REDISCOVER -> Triple(Color(0xFF8360AA), Color(0xFF6A82C4), Color(0xFFBFEAF1))
        HomeMixKind.ARTIST -> Triple(Color(0xFF8D9A58), Color(0xFF69C4AB), Color(0xFFF7CF97))
        HomeMixKind.GENRE -> Triple(Color(0xFF603755), Color(0xFFEEA9CE), Color(0xFF82A9E0))
        HomeMixKind.RECENTLY_ADDED -> Triple(Color(0xFF74394C), Color(0xFFCB7285), Color(0xFFFFE5C8))
    }
    val concentration = if (dark) 0.42f else 0.54f
    return theme.copy(
        primary = lerp(theme.surface, primary, concentration),
        primaryContainer = lerp(theme.surface, primary, concentration),
        secondaryContainer = lerp(theme.surface, secondary, concentration),
        tertiaryContainer = lerp(theme.surface, tertiary, concentration),
    )
}

private val PosterGrain = Random(31).let { random ->
    List(230) { Offset(random.nextFloat() * MixPosterSide, random.nextFloat() * MixPosterSide) }
}

private val GrooveCenter = Offset(663f, 547f)
private val CopperDisc = Path().apply { addOval(Rect(center = GrooveCenter, radius = 940f)) }
private val CopperLight = Brush.sweepGradient(
    0f to Color(0xFF41222F),
    .09f to Color(0xFF1C1118),
    .23f to Color(0xFFDA8561),
    .30f to Color(0xFFFFBF83),
    .38f to Color(0xFF6A3042),
    .43f to Color(0xFF9F4C90),
    .55f to Color(0xFF382032),
    .66f to Color(0xFFAC6546),
    .77f to Color(0xFF3C2529),
    .89f to Color(0xFFA93D73),
    .94f to Color(0xFFFF93CC),
    1f to Color(0xFF41222F),
    center = GrooveCenter,
)

private fun DrawScope.drawCopperGrooves(fineDetail: Boolean) {
    drawRect(Color(0xFF261824), size = PosterSize)
    drawCircle(CopperLight, 940f, GrooveCenter)
    for (radius in 96..875 step if (fineDetail) 19 else 39) {
        drawCircle(Color.Black.copy(alpha = if (radius % 3 == 0) .38f else .20f),
            radius.toFloat(), GrooveCenter, style = Stroke(if (radius % 3 == 0) 3.5f else 2f))
        if (fineDetail) {
            drawCircle(Color(0xFFFFD8BD).copy(alpha = .14f),
                radius + 4f, GrooveCenter, style = Stroke(1.6f))
        }
    }
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFF20131D),
            .31f to Color(0xFF331A26),
            .78f to Color.Transparent,
            1f to Color.Transparent,
            center = GrooveCenter,
            radius = 490f,
        ),
        490f, GrooveCenter,
    )
    drawCircle(Color(0xFF2B1821), 94f, GrooveCenter)
    drawCircle(Color(0xFFFFB997).copy(alpha = .55f), 91f, GrooveCenter, style = Stroke(3f))
    drawCircle(Color(0xFFE9A484), 13f, GrooveCenter)
    drawRect(
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .16f),
            .40f to Color.Transparent,
            .78f to Color.Transparent,
            1f to Color.Black.copy(alpha = .37f),
            endY = MixPosterSide,
        ),
        size = PosterSize,
    )
}

private val AquaLoop = Path().apply {
    moveTo(-90f, 150f)
    cubicTo(198f, 238f, 216f, 35f, 367f, 98f)
    cubicTo(550f, 194f, 37f, 347f, 203f, 521f)
    cubicTo(347f, 680f, 828f, 465f, 1046f, 793f)
    lineTo(1054f, 1080f)
    cubicTo(839f, 679f, 364f, 901f, 153f, 756f)
    cubicTo(-49f, 616f, -77f, 390f, 111f, 241f)
    cubicTo(29f, 270f, -37f, 260f, -90f, 220f)
    close()
}

private val AquaGlint = Path().apply {
    moveTo(-67f, 198f)
    cubicTo(87f, 247f, 234f, 116f, 341f, 114f)
    cubicTo(381f, 172f, 49f, 287f, 109f, 497f)
    cubicTo(178f, 758f, 710f, 550f, 949f, 791f)
    cubicTo(995f, 839f, 1032f, 884f, 1060f, 931f)
}

private fun DrawScope.drawPearlLoop() {
    drawRect(
        Brush.radialGradient(listOf(Color(0xFF20768D), Color(0xFF102942), Color(0xFF0A2029)),
            center = Offset(716f, 332f), radius = 836f),
        size = PosterSize,
    )
    drawPath(
        AquaLoop,
        Brush.linearGradient(
            .02f to Color(0xFF143451), .22f to Color(0xFF69BBC1), .31f to Color(0xFFE6FFF7),
            .41f to Color(0xFF62D9D0), .55f to Color(0xFF33798C), .64f to Color(0xFF273D77),
            .73f to Color(0xFFA7FFF0), .80f to Color(0xFFF5C5E0), .88f to Color(0xFF488DA7),
            .95f to Color(0xFF0F555D), 1f to Color(0xFF60EBCE),
            start = Offset(105f, 220f), end = Offset(619f, 996f),
        ),
    )
    clipPath(AquaLoop) {
        drawPath(AquaGlint, Color(0xFFABF8ED).copy(alpha = .17f), style = Stroke(59f))
        drawPath(AquaGlint, Color(0xFFF3FFF1).copy(alpha = .60f), style = Stroke(9f))
        drawPath(
            AquaLoop,
            Brush.linearGradient(listOf(Color(0xFFA5FFE8), Color.Transparent, Color(0xFFDDABF2)),
                end = Offset(840f, 833f)),
            style = Stroke(9f),
        )
    }
}

private val VioletFold = Path().apply {
    moveTo(385f, -80f)
    cubicTo(187f, 133f, 260f, 231f, 460f, 318f)
    cubicTo(692f, 423f, 710f, 702f, 1080f, 800f)
    lineTo(1100f, -80f)
    close()
}

private val VioletFace = Path().apply {
    moveTo(665f, -72f)
    cubicTo(503f, 132f, 462f, 169f, 325f, 207f)
    cubicTo(486f, 348f, 689f, 291f, 813f, 497f)
    cubicTo(887f, 602f, 1009f, 610f, 1100f, 605f)
    lineTo(1100f, -72f)
    close()
}

private val VioletLip = Path().apply {
    moveTo(271f, 141f)
    cubicTo(373f, 299f, 633f, 267f, 768f, 469f)
    cubicTo(874f, 614f, 982f, 684f, 1105f, 716f)
}

private fun DrawScope.drawVioletFold() {
    drawRect(Brush.linearGradient(listOf(Color(0xFF12192D), Color(0xFF213962), Color(0xFF1D2545)),
        end = Offset(982f, 863f)), size = PosterSize)
    drawPath(
        VioletFold,
        Brush.linearGradient(
            .06f to Color(0xFF33234F), .21f to Color(0xFF8360AA), .32f to Color(0xFFDCC4F5),
            .39f to Color(0xFF79458E), .47f to Color(0xFF3A245C), .60f to Color(0xFF884BB7),
            .76f to Color(0xFFBFEAF1), .91f to Color(0xFF6041A7), 1f to Color(0xFF221D4B),
            start = Offset(333f, 26f), end = Offset(1000f, 708f),
        ),
    )
    clipPath(VioletFold) {
        drawPath(
            VioletFace,
            Brush.linearGradient(
                .08f to Color(0xFF523877), .21f to Color(0xFFE2BDF6), .32f to Color(0xFF7C50B1),
                .49f to Color(0xFF332756), .63f to Color(0xFF6A82C4), .69f to Color(0xFFCDFFFA),
                .76f to Color(0xFFA58EDA), .85f to Color(0xFF4D2E7F), 1f to Color(0xFFCA90D6),
                start = Offset(341f, 78f), end = Offset(961f, 510f),
            ),
        )
        drawPath(VioletLip, Color(0xFFC998E8).copy(alpha = .25f), style = Stroke(42f))
        drawPath(VioletLip, Color(0xFFECCCF5).copy(alpha = .63f), style = Stroke(8f))
        drawPath(VioletFold, Color(0xFFDECEFE).copy(alpha = .46f), style = Stroke(6f))
    }
}

private val PrismFace = Path().apply {
    moveTo(495f, 199f)
    cubicTo(646f, 107f, 827f, 162f, 935f, 277f)
    cubicTo(1101f, 451f, 992f, 646f, 846f, 747f)
    lineTo(606f, 908f)
    cubicTo(440f, 1000f, 245f, 871f, 287f, 729f)
    cubicTo(330f, 621f, 537f, 576f, 641f, 486f)
    cubicTo(729f, 400f, 684f, 264f, 495f, 199f)
    close()
}

private fun DrawScope.drawPrism() {
    drawRect(Brush.linearGradient(listOf(Color(0xFF1D302B), Color(0xFF153332), Color(0xFF534429)),
        end = Offset(1000f, 938f)), size = PosterSize)
    drawPath(PrismFace, Brush.linearGradient(
        .02f to Color(0xFF4B623C), .21f to Color(0xFFD7F09F), .28f to Color(0xFFFCF2C3),
        .38f to Color(0xFF8D9A58), .51f to Color(0xFF2A665C), .61f to Color(0xFF69C4AB),
        .72f to Color(0xFFC1EDE0), .81f to Color(0xFFF7CF97), 1f to Color(0xFF626646),
        start = Offset(349f, 446f), end = Offset(929f, 751f),
    ))
    clipPath(PrismFace) {
        for (index in 0..8) {
            val inset = index * 21f
            drawOval(
                Color(0xFFEDF8C9).copy(alpha = .16f),
                topLeft = Offset(353f + inset, 192f + inset),
                size = Size(625f - inset, 680f - inset),
                style = Stroke(if (index % 3 == 0) 4f else 2f),
            )
        }
        drawPath(PrismFace, Color(0xFFC1F6B4).copy(alpha = .62f), style = Stroke(8f))
    }
}

private val StarPoints = Random(51).let { random ->
    List(74) { Offset(random.nextFloat() * MixPosterSide, random.nextFloat() * MixPosterSide) }
}

private fun DrawScope.drawStarTrails() {
    val focus = Offset(511f, 671f)
    val horizon = Offset(732f, 543f)
    drawRect(Brush.radialGradient(listOf(Color(0xFF603755), Color(0xFF252840), Color(0xFF171627)),
        center = horizon, radius = 810f), size = PosterSize)
    StarPoints.forEachIndexed { index, point ->
        drawCircle(Color(0xFFF8E2E7).copy(alpha = if (index % 7 == 0) .78f else .36f),
            radius = if (index % 7 == 0) 3.5f else 1.8f, center = point)
    }
    rotate(-22f, focus) {
        for (index in 0..8) {
            val inset = index * 24f
            val position = Offset(-34f + inset, 433f + inset * .58f)
            val ring = Size(1070f - inset * 2, 476f - inset * 1.16f)
            drawArc(
                Brush.horizontalGradient(listOf(
                    Color(0x00D283AC), Color(0xFFEEA9CE), Color(0xFF82A9E0), Color(0x26BA90D2),
                ), startX = 15f, endX = 1020f),
                startAngle = 2f + index * 5f,
                sweepAngle = 290f - index * 7f,
                useCenter = false,
                topLeft = position,
                size = ring,
                alpha = if (index % 3 == 0) .65f else .31f,
                style = Stroke(if (index % 3 == 0) 9f else 4f),
            )
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFCDAEA), Color(0xFFAC70A5), Color.Transparent),
        center = horizon, radius = 140f), 140f, horizon)
    drawCircle(Color(0xFFFFEDF2), 14f, horizon)
}

private data class FanPleat(val path: Path, val brush: Brush)

private val FanPleats: List<FanPleat> = run {
    val hub = Offset(849f, 1100f)
    val angles = listOf(3.40f, 3.62f, 3.83f, 4.04f, 4.25f, 4.46f, 4.67f, 4.88f, 5.10f)
    val ends = angles.mapIndexed { index, angle ->
        val radius = 870f + index * 17f
        hub + Offset(cos(angle) * radius, sin(angle) * radius)
    }
    ends.zipWithNext().map { (left, right) ->
        val middle = (left + right) / 2f
        val crest = middle + (middle - hub) * .055f
        val path = Path().apply {
            moveTo(hub.x, hub.y)
            cubicTo(hub.x * .62f + left.x * .38f, hub.y * .69f + left.y * .31f,
                left.x + 28f, left.y + 89f, left.x, left.y)
            quadraticTo(crest.x, crest.y, right.x, right.y)
            cubicTo(right.x + 28f, right.y + 89f,
                hub.x * .62f + right.x * .38f, hub.y * .69f + right.y * .31f, hub.x, hub.y)
            close()
        }
        val sheen = Brush.linearGradient(
            0f to Color(0xFF622842), .17f to Color(0xFF9A506A), .36f to Color(0xFFE79388),
            .51f to Color(0xFFFFDDAC), .61f to Color(0xFFFEBC98), .76f to Color(0xFFC76E79),
            .91f to Color(0xFFA45270), 1f to Color(0xFF71354C),
            start = left, end = right,
        )
        FanPleat(path, sheen)
    }
}

private fun DrawScope.drawCoralFan() {
    drawRect(Brush.radialGradient(listOf(Color(0xFF74394C), Color(0xFF51243F), Color(0xFF271B31)),
        center = Offset(424f, 395f), radius = 790f), size = PosterSize)
    FanPleats.forEach { pleat ->
        drawPath(pleat.path, pleat.brush)
        drawPath(pleat.path, Color(0xFFFFE5C8).copy(alpha = .34f), style = Stroke(3f))
    }
}

private val FanSilhouette = Path().apply {
    FanPleats.forEach { addPath(it.path) }
}

internal fun DrawScope.drawMixPosterShine(kind: HomeMixKind, progress: Float) {
    val surface = when (kind) {
        HomeMixKind.DAILY -> CopperDisc
        HomeMixKind.UNDERPLAYED -> AquaLoop
        HomeMixKind.REDISCOVER -> VioletFold
        HomeMixKind.ARTIST -> PrismFace
        HomeMixKind.RECENTLY_ADDED -> FanSilhouette
        HomeMixKind.GENRE -> return
    }
    val x = -750f + 2250f * progress
    clipPath(surface) {
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                .48f to Color(0x29FFE9D7),
                .60f to Color(0x12F4C2FD),
                1f to Color.Transparent,
                start = Offset(x, 200f),
                end = Offset(x + 420f, 420f),
            ),
            size = PosterSize,
        )
    }
}
