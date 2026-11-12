package com.theveloper.pixelplay.presentation.screens.search.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.theveloper.pixelplay.data.model.Genre
import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class GenreScene {
    ELECTRIC_GUITAR, METAL, DANCE, ELECTRONIC, POP, HOUSE, TROPICAL_HOUSE, JAZZ,
    CLASSICAL, URBAN, LATIN, FOLK, ABSTRACT
}

/** Compact scenes drawn natively; the caller clips the artwork to a circle. */
@Composable
internal fun GenreArtwork(genre: Genre, colors: List<Color>, modifier: Modifier = Modifier) {
    val scene = remember(genre.name) { genreScene(normalizeGenreName(genre.name)) }
    val seed = remember(genre.id, genre.name) {
        val hash = genre.id.ifBlank { genre.name }.hashCode()
        (hash xor (hash ushr 16)) and Int.MAX_VALUE
    }
    val palette = remember(scene, colors) { paletteFor(scene, colors) }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        // All scene geometry uses a 1 x 1 coordinate plane; this keeps small artwork readable.
        scale(size.width, size.height, pivot = Offset.Zero) {
            when (scene) {
                GenreScene.ELECTRIC_GUITAR -> drawElectricGuitar(palette)
                GenreScene.METAL -> drawMetal(palette)
                GenreScene.DANCE -> drawDance(palette)
                GenreScene.ELECTRONIC -> drawElectronic(palette)
                GenreScene.POP -> drawPop(palette)
                GenreScene.HOUSE -> drawHouse(palette)
                GenreScene.TROPICAL_HOUSE -> drawTropicalHouse(palette)
                GenreScene.JAZZ -> drawJazz(palette)
                GenreScene.CLASSICAL -> drawClassical(palette)
                GenreScene.URBAN -> drawUrban(palette)
                GenreScene.LATIN -> drawLatin(palette)
                GenreScene.FOLK -> drawFolk(palette)
                GenreScene.ABSTRACT -> drawAbstract(palette, seed)
            }
        }
    }
}

private val GenreDiacritics = Regex("\\p{Mn}+")
private val GenreSeparators = Regex("[\\s_-]+")
private val GenreArtworkSize = Size(1f, 1f)

private fun normalizeGenreName(value: String): String = Normalizer
    .normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace(GenreDiacritics, "")
    .replace(GenreSeparators, " ")
    .trim()

private fun genreScene(name: String): GenreScene = when {
    "tropical house" in name -> GenreScene.TROPICAL_HOUSE
    "metal" in name -> GenreScene.METAL
    listOf("rock", "punk", "grunge").any(name::contains) -> GenreScene.ELECTRIC_GUITAR
    "house" in name -> GenreScene.HOUSE
    listOf("dance", "disco").any(name::contains) -> GenreScene.DANCE
    listOf("electronic", "electronica", "electro", "synth", "techno", "trance", "drum and bass", "dnb", "phonk").any(name::contains) -> GenreScene.ELECTRONIC
    "pop" in name -> GenreScene.POP
    listOf("jazz", "blues", "swing", "sax").any(name::contains) -> GenreScene.JAZZ
    listOf("classical", "clasica", "orches", "chamber", "piano", "baroque").any(name::contains) -> GenreScene.CLASSICAL
    listOf("hip hop", "rap", "urban", "reggaeton").any(name::contains) -> GenreScene.URBAN
    listOf("latin", "latino", "salsa", "cumbia", "bachata", "merengue", "tango", "tropical").any(name::contains) -> GenreScene.LATIN
    listOf("folk", "country", "acoustic", "bluegrass").any(name::contains) -> GenreScene.FOLK
    else -> GenreScene.ABSTRACT
}

private data class ScenePalette(val background: Color, val main: Color, val light: Color)

private fun paletteFor(scene: GenreScene, theme: List<Color>) = when (scene) {
    GenreScene.ELECTRIC_GUITAR -> ScenePalette(Color(0xFF781A25), Color(0xFFE8413B), Color(0xFFF5F0E9))
    GenreScene.METAL -> ScenePalette(Color(0xFF29182F), Color(0xFFB8BAC4), Color(0xFF747886))
    GenreScene.DANCE -> ScenePalette(Color(0xFF594176), Color(0xFFC484C7), Color(0xFFF5CBE9))
    GenreScene.ELECTRONIC -> ScenePalette(Color(0xFF100F24), Color(0xFF43E9F5), Color(0xFFAA54FF))
    GenreScene.POP -> ScenePalette(Color(0xFF164B81), Color(0xFFFF7565), Color(0xFF4BC5E8))
    GenreScene.HOUSE -> ScenePalette(Color(0xFF15111B), Color(0xFFFFB957), Color(0xFFFFD99A))
    GenreScene.TROPICAL_HOUSE -> ScenePalette(Color(0xFF553E77), Color(0xFFFFC5AD), Color(0xFFB9F3E8))
    GenreScene.JAZZ -> ScenePalette(Color(0xFFF0E3D0), Color(0xFFFF842E), Color(0xFF70472F))
    else -> ScenePalette(
        theme.getOrElse(0) { Color(0xFF466B91) },
        theme.getOrElse(1) { Color(0xFFB5D8D5) },
        theme.getOrElse(2) { Color(0xFF263C59) },
    )
}

private fun DrawScope.drawElectricGuitar(c: ScenePalette) {
    drawRect(Brush.linearGradient(listOf(c.background, Color(0xFF391620)), at(.0f,.0f), at(1f,1f)), size = GenreArtworkSize)
    // Strat-like outline: paired horns, waist and rounded lower bout.
    val body = Path().apply {
        moveTo(.49f,.48f); cubicTo(.42f,.40f,.38f,.28f,.30f,.25f)
        cubicTo(.24f,.23f,.21f,.29f,.23f,.37f); lineTo(.27f,.48f)
        cubicTo(.18f,.52f,.15f,.62f,.20f,.73f); cubicTo(.27f,.91f,.46f,.88f,.50f,.76f)
        cubicTo(.56f,.89f,.75f,.87f,.81f,.73f); cubicTo(.86f,.61f,.81f,.51f,.71f,.47f)
        lineTo(.76f,.34f); cubicTo(.79f,.25f,.73f,.21f,.67f,.24f)
        cubicTo(.60f,.27f,.58f,.39f,.53f,.48f); close()
    }
    drawPath(body, Brush.linearGradient(listOf(Color(0xFFFF6853), Color(0xFFCB202D), Color(0xFF9D1829)), at(.2f,.25f), at(.8f,.85f)))
    // Pickguard follows the body contour; the bridge and pickups attach to the body.
    val guard = Path().apply {
        moveTo(.44f,.48f); cubicTo(.51f,.42f,.60f,.45f,.67f,.50f)
        lineTo(.64f,.67f); cubicTo(.58f,.72f,.47f,.72f,.40f,.66f)
        cubicTo(.38f,.59f,.39f,.52f,.44f,.48f); close()
    }
    drawPath(guard, Color(0xFFF5F1E9))
    drawRoundRect(Color(0xFF3D3031), at(.43f,.59f), extent(.18f,.035f), CornerRadius(.012f,.012f))
    drawRoundRect(Color(0xFF3D3031), at(.46f,.65f), extent(.14f,.035f), CornerRadius(.012f,.012f))
    drawRoundRect(Color(0xFFC7AB83), at(.46f,.70f), extent(.12f,.035f), CornerRadius(.008f,.008f))
    // One attached diagonal neck, aimed upper-right; fret marks stay on its surface.
    val neck = Path().apply { moveTo(.54f,.49f); lineTo(.91f,.09f); lineTo(.98f,.16f); lineTo(.62f,.56f); close() }
    drawPath(neck, Color(0xFF855C3E))
    val fingerboard = Path().apply { moveTo(.57f,.49f); lineTo(.91f,.12f); lineTo(.95f,.16f); lineTo(.61f,.53f); close() }
    drawPath(fingerboard, Color(0xFF2D2630))
    for (index in 1..5) {
        val t = index / 7f
        drawLine(Color(0xFFE9D8BD), at(.59f + .32f*t,.50f - .35f*t), at(.62f + .32f*t,.53f - .35f*t), .008f)
    }
    drawRoundRect(Color(0xFFD5C8AF), at(.88f,.08f), extent(.12f,.09f), CornerRadius(.015f,.015f))
}

private fun DrawScope.drawMetal(c: ScenePalette) {
    drawRect(c.background, size = GenreArtworkSize)
    // Three broad diagonal planes make a single steel object, not a shard mosaic.
    val silhouette = Path().apply {
        moveTo(.18f,.69f); lineTo(.36f,.25f); lineTo(.71f,.12f); lineTo(.86f,.36f)
        lineTo(.67f,.79f); lineTo(.31f,.86f); close()
    }
    drawPath(silhouette, Brush.linearGradient(listOf(Color(0xFFE0E0E5), Color(0xFF737784), Color(0xFFB8BAC4)), at(.1f,.1f), at(.9f,.9f)))
    drawPath(Path().apply { moveTo(.18f,.69f); lineTo(.36f,.25f); lineTo(.56f,.47f); lineTo(.31f,.86f); close() }, Color.White.copy(alpha=.24f))
    drawPath(Path().apply { moveTo(.36f,.25f); lineTo(.71f,.12f); lineTo(.56f,.47f); close() }, Color(0xFF25232D).copy(alpha=.28f))
    drawPath(Path().apply { moveTo(.56f,.47f); lineTo(.86f,.36f); lineTo(.67f,.79f); lineTo(.31f,.86f); close() }, Color(0xFF36333F).copy(alpha=.20f))
}

private fun DrawScope.drawDance(c: ScenePalette) {
    drawRect(
        Brush.radialGradient(listOf(Color(0xFF824784), c.background, Color(0xFF281B41)), at(.64f, .20f), .85f),
        size = GenreArtworkSize,
    )
    // A single broad satin fold, with a narrow specular ridge and one quiet return fold.
    val fold = Path().apply {
        moveTo(-.1f,.24f); cubicTo(.16f,.04f,.31f,.14f,.48f,.43f)
        cubicTo(.63f,.69f,.79f,.72f,1.1f,.48f); lineTo(1.1f,.82f)
        cubicTo(.76f,1.02f,.56f,.83f,.40f,.61f); cubicTo(.22f,.35f,.10f,.34f,-.1f,.53f); close()
    }
    drawPath(fold, Brush.linearGradient(
        .08f to Color(0xFF604079),
        .28f to Color(0xFF8D5DAD),
        .44f to c.main,
        .55f to Color(0xFFFFC8EC),
        .62f to Color(0xFFD88BC8),
        .75f to Color(0xFF664580),
        .91f to Color(0xFFAA67A5),
        start = at(.18f, .24f), end = at(.81f, .82f),
    ))
    val sheen = Path().apply {
        moveTo(-.03f, .28f)
        cubicTo(.22f, .13f, .33f, .29f, .48f, .50f)
        cubicTo(.66f, .73f, .83f, .73f, 1.05f, .57f)
    }
    clipPath(fold) {
        drawPath(sheen, Color(0xFFF7B5ED).copy(alpha = .34f), style = Stroke(.08f, cap = StrokeCap.Round))
        drawPath(
            sheen,
            Brush.horizontalGradient(
                listOf(Color(0x35FDE1F5), Color(0xFFFEE6F8), Color(0x8CE8CCF9), Color(0x14F3BEE5)),
                startX = .13f,
                endX = .97f,
            ),
            style = Stroke(.020f, cap = StrokeCap.Round),
        )
        drawPath(fold, c.light.copy(alpha = .48f), style = Stroke(.017f))
    }
}

private fun DrawScope.drawElectronic(c: ScenePalette) {
    drawRect(
        Brush.radialGradient(listOf(Color(0xFF4C3181), Color(0xFF241744), c.background), at(.63f, .58f), .67f),
        size = GenreArtworkSize,
    )
    val beams = listOf(
        at(-.08f, .32f) to at(1.06f, .80f),
        at(.86f, -.04f) to at(.26f, 1.08f),
        at(-.09f, .69f) to at(1.10f, .44f),
        at(.14f, 1.04f) to at(.87f, .17f),
    )
    beams.forEachIndexed { index, (start, end) ->
        val color = if (index % 2 == 0) c.main else c.light
        drawLine(color.copy(alpha = .12f), start, end, .09f)
        drawLine(color.copy(alpha = .36f), start, end, .043f)
        drawLine(color, start, end, .015f)
    }
}

private fun DrawScope.drawPop(c: ScenePalette) {
    drawRect(Brush.linearGradient(listOf(Color(0xFF164879), Color(0xFF263C79), c.background), at(.0f,.0f), at(1f,1f)), size = GenreArtworkSize)
    // Coral portal against a cobalt stage, grounded by a perspective floor and a single key light.
    val floor = Path().apply { moveTo(0f,.66f); lineTo(1f,.66f); lineTo(1f,1f); lineTo(0f,1f); close() }
    drawPath(floor, Brush.linearGradient(listOf(Color(0xFF244C86), Color(0xFF101B3D)), at(.5f,.65f), at(.5f,1f)))
    val portal = Path().apply {
        moveTo(.23f,.68f); lineTo(.23f,.36f); cubicTo(.23f,.12f,.77f,.12f,.77f,.36f)
        lineTo(.77f,.68f); lineTo(.66f,.68f); lineTo(.66f,.36f); cubicTo(.66f,.25f,.34f,.25f,.34f,.36f); lineTo(.34f,.68f); close()
    }
    drawPath(portal, Brush.linearGradient(listOf(Color(0xFFFF9A82), c.main, Color(0xFFB84161)), at(.15f,.2f), at(.85f,.8f)))
    drawLine(Color.White.copy(alpha=.76f), at(.5f,.03f), at(.5f,.65f), .018f, cap=StrokeCap.Round)
    drawCircle(Color(0xFFFFE6CC), .048f, at(.5f,.67f))
    drawPath(Path().apply { moveTo(.50f,.67f); lineTo(.14f,1f); lineTo(.86f,1f); close() }, Brush.verticalGradient(listOf(Color(0xFFFFA080).copy(alpha=.30f), Color.Transparent), .66f, 1f))
}

private fun DrawScope.drawHouse(c: ScenePalette) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF261B2C), Color(0xFF100E17), Color(0xFF090A11)), 0f, 1f), size = GenreArtworkSize)
    // Enclosed club: ceiling lamps and amber pools reflected on the perspective floor.
    val floor = Path().apply { moveTo(0f,.60f); lineTo(1f,.60f); lineTo(1f,1f); lineTo(0f,1f); close() }
    drawPath(floor, Brush.verticalGradient(listOf(Color(0xFF28212A), Color(0xFF111017)), .58f, 1f))
    drawLine(Color(0xFF4B3B39), at(0f,.60f), at(1f,.60f), .012f)
    drawLine(c.main, at(.29f,.0f), at(.29f,.30f), .012f)
    drawLine(c.main.copy(alpha=.72f), at(.72f,.0f), at(.72f,.25f), .009f)
    drawCircle(c.light, .036f, at(.29f,.32f)); drawCircle(c.main.copy(alpha=.9f), .024f, at(.72f,.27f))
    // Flattened ellipses sit on the floor plane and taper into their reflections.
    drawOval(Brush.radialGradient(listOf(c.main.copy(alpha=.48f), c.main.copy(alpha=.14f), Color.Transparent), at(.30f,.69f), .37f), at(.05f,.63f), extent(.58f,.13f))
    drawOval(Brush.radialGradient(listOf(c.light.copy(alpha=.35f), c.main.copy(alpha=.12f), Color.Transparent), at(.71f,.69f), .29f), at(.49f,.63f), extent(.46f,.10f))
    for (ring in 0..3) {
        val offset = ring * .045f
        drawOval(
            c.main.copy(alpha = .55f - ring * .09f),
            topLeft = at(.26f - offset, .735f - offset * .3f),
            size = extent(.16f + 2f * offset, .037f + offset * .6f),
            style = Stroke(.008f),
        )
    }
}

private fun DrawScope.drawTropicalHouse(c: ScenePalette) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF40365F), Color(0xFFB8799B), Color(0xFFFFC4A9), Color(0xFF48436C)), 0f, 1f), size = GenreArtworkSize)
    drawCircle(Color(0xFFFFD1B1).copy(alpha=.92f), .13f, at(.70f,.39f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF645B8D), Color(0xFF234B6B)), .52f, 1f), at(0f,.52f), extent(1f,.48f))
    val shore = Path().apply {
        moveTo(-.05f, .96f)
        cubicTo(.29f, .94f, .49f, .83f, .71f, .80f)
        cubicTo(.86f, .78f, .95f, .73f, 1.03f, .68f)
        lineTo(1.03f, 1.04f)
        lineTo(-.05f, 1.04f)
        close()
    }
    drawPath(shore, Brush.linearGradient(
        listOf(Color(0xFFC0808D), Color(0xFFF2AD9E), Color(0xFFB7819B)),
        start = at(.36f, .75f), end = at(.91f, 1f),
    ))
    val wave = Path().apply {
        moveTo(-.06f, .80f)
        cubicTo(.12f, .55f, .32f, .48f, .47f, .57f)
        cubicTo(.53f, .62f, .49f, .70f, .40f, .68f)
        cubicTo(.46f, .75f, .58f, .79f, .77f, .72f)
        cubicTo(.89f, .67f, .97f, .64f, 1.06f, .63f)
        lineTo(1.06f, .72f)
        cubicTo(.92f, .80f, .67f, .86f, .48f, .86f)
        cubicTo(.27f, .86f, .17f, .94f, -.06f, .97f)
        close()
    }
    drawPath(wave, Brush.linearGradient(
        .02f to Color(0xFF5476A2), .24f to Color(0xFF6CE5DB), .40f to c.light,
        .49f to Color(0xFFFDF4E1), .58f to Color(0xFFC2A8DF), .67f to Color(0xFF56B4D5),
        .82f to Color(0xFFF2C4D9), .98f to Color(0xFFBAE5DE),
        start = at(.11f, .50f), end = at(.70f, .95f),
    ))
    clipPath(wave) {
        drawPath(wave, Color(0xFFFFF4E5).copy(alpha = .88f), style = Stroke(.021f))
    }
}

private fun DrawScope.drawJazz(c: ScenePalette) {
    drawRect(Color(0xFFF0E4D3), size = GenreArtworkSize)
    // Window light throws a long quiet shadow across the listening-room floor.
    drawRect(Color(0xFFFFF8E9), at(.59f,.10f), extent(.28f,.34f))
    drawLine(Color(0xFFC9B69C), at(.73f,.10f), at(.73f,.44f), .012f)
    drawLine(Color(0xFFC9B69C), at(.59f,.27f), at(.87f,.27f), .012f)
    drawPath(Path().apply { moveTo(.59f,.44f); lineTo(.91f,.44f); lineTo(.75f,.72f); lineTo(.43f,.72f); close() }, Color(0xFFBFAF9D).copy(alpha=.42f))
    drawRect(Color(0xFFD7C7B3), at(0f,.77f), extent(1f,.23f))
    // Upholstered chair has a back, arms, separate seat, front rail and visible legs.
    drawRoundRect(Color(0xFF7B3D22).copy(alpha=.26f), at(.15f,.82f), extent(.72f,.09f), CornerRadius(.05f,.05f))
    val velvet = Brush.linearGradient(
        listOf(Color(0xFFFCA145), c.main, Color(0xFFAE4920)),
        start = at(.22f, .30f), end = at(.76f, .78f),
    )
    drawRoundRect(velvet, at(.27f, .32f), extent(.48f, .34f), CornerRadius(.12f))
    drawRoundRect(Color(0xFFC75D23), at(.19f, .59f), extent(.62f, .17f), CornerRadius(.055f))
    drawRoundRect(velvet, at(.12f, .48f), extent(.15f, .28f), CornerRadius(.06f))
    drawRoundRect(velvet, at(.73f, .48f), extent(.15f, .28f), CornerRadius(.06f))
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFFFB454), Color(0xFFD67129)), .58f, .68f),
        at(.25f, .58f), extent(.50f, .10f), CornerRadius(.045f),
    )
    drawLine(c.light, at(.27f,.75f), at(.23f,.91f), .035f, cap=StrokeCap.Round)
    drawLine(c.light, at(.75f,.75f), at(.79f,.91f), .035f, cap=StrokeCap.Round)
}

private fun DrawScope.drawClassical(c: ScenePalette) {
    drawRect(c.background, size = GenreArtworkSize)
    for (line in 0..4) drawLine(c.main.copy(alpha=.62f), at(.13f,.24f+line*.09f), at(.87f,.24f+line*.09f), .012f)
    drawRoundRect(c.light, at(.17f,.58f), extent(.66f,.25f), CornerRadius(.035f,.035f))
    for (key in 0..7) drawLine(c.background, at(.17f+key*.0825f,.59f), at(.17f+key*.0825f,.82f), .012f)
    listOf(1,2,4,5,6).forEach { drawRoundRect(c.main, at(.23f+it*.072f,.58f), extent(.04f,.13f), CornerRadius(.01f,.01f)) }
}

private fun DrawScope.drawUrban(c: ScenePalette) {
    drawRect(c.background, size = GenreArtworkSize)
    val heights = listOf(.25f,.42f,.68f,.38f,.74f,.48f,.27f)
    heights.forEachIndexed { index, height -> drawRoundRect(if(index==4)c.light else c.main, at(.14f+index*.105f,.82f-height), extent(.065f,height), CornerRadius(.03f,.03f)) }
    drawArc(c.light.copy(alpha=.85f),205f,130f,false,at(.2f,.1f),extent(.6f,.46f),style=Stroke(.025f,cap=StrokeCap.Round))
}

private fun DrawScope.drawLatin(c: ScenePalette) {
    drawRect(c.background, size = GenreArtworkSize)
    drawCircle(c.light,.28f,at(.5f,.5f)); drawCircle(c.background,.18f,at(.5f,.5f)); drawCircle(c.main,.07f,at(.5f,.5f))
    for(index in 0..7) {
        val angle=index*PI/4
        drawLine(c.background,at(.5f+cos(angle).toFloat()*.31f,.5f+sin(angle).toFloat()*.31f),at(.5f+cos(angle).toFloat()*.44f,.5f+sin(angle).toFloat()*.44f),.035f,cap=StrokeCap.Round)
    }
}

private fun DrawScope.drawFolk(c: ScenePalette) {
    drawRect(c.background, size = GenreArtworkSize)
    drawOval(c.light,at(.28f,.43f),extent(.44f,.43f)); drawCircle(c.background,.09f,at(.5f,.61f))
    val neck=Path().apply { moveTo(.45f,.52f); lineTo(.81f,.12f); lineTo(.91f,.20f); lineTo(.55f,.60f); close() }
    drawPath(neck,c.main)
    for(index in 0..3) drawLine(Color.White.copy(alpha=.88f),at(.48f+index*.018f,.53f-index*.018f),at(.85f+index*.018f,.15f-index*.018f),.007f)
}

private fun DrawScope.drawAbstract(c: ScenePalette, seed: Int) {
    drawRect(c.background, size = GenreArtworkSize)
    val rotation = (seed % 34 - 17).toFloat()
    rotate(rotation, pivot=at(.5f,.5f)) {
        drawOval(c.main.copy(alpha=.88f),at(.16f,.21f),extent(.67f,.59f))
        drawOval(c.light,at(.31f,.15f),extent(.40f,.69f),style=Stroke(.055f))
        drawLine(c.background,at(.19f,.31f+(seed%18)*.01f),at(.81f,.68f),.075f,cap=StrokeCap.Round)
        for(index in 0..2) drawCircle(if((seed shr index) and 1 == 0)c.light else c.background,.04f+index*.008f,at(.27f+index*.22f,.30f+index*.19f))
    }
}

private fun at(x: Float, y: Float) = Offset(x, y)
private fun extent(width: Float, height: Float) = Size(width, height)
