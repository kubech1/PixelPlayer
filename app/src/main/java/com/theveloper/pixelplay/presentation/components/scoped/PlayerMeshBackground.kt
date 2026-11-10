package com.theveloper.pixelplay.presentation.components.scoped

import android.graphics.RuntimeShader
import android.os.Trace
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.lerp
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import kotlin.math.pow
import kotlin.random.Random

internal const val PlayerMeshDrawTrace = "PixelPlay.playerMesh.draw"
private const val Grain = 1.5f / 255f
private const val MinimumTextContrast = 4.5f

internal data class MeshRgb(val r: Float, val g: Float, val b: Float) {
    fun mix(to: MeshRgb, progress: Float) =
        MeshRgb(lerp(r, to.r, progress), lerp(g, to.g, progress), lerp(b, to.b, progress))

    fun color() = Color(r, g, b, colorSpace = ColorSpaces.LinearSrgb).convert(ColorSpaces.Srgb)

    companion object {
        fun from(color: Color): MeshRgb = with(color.convert(ColorSpaces.LinearSrgb)) {
            MeshRgb(red, green, blue)
        }
    }
}

internal data class PlayerMeshPalette(
    val positions: List<Offset>,
    val colors: List<MeshRgb>
) {
    fun mix(to: PlayerMeshPalette, progress: Float) = PlayerMeshPalette(
        positions.zip(to.positions) { from, next -> androidx.compose.ui.geometry.lerp(from, next, progress) },
        colors.zip(to.colors) { from, next -> from.mix(next, progress) }
    )
}

internal fun playerMeshPositions(songId: String): List<Offset> {
    val random = Random(songId.hashCode())
    return List(4) { index ->
        Offset(
            (index % 2) * 0.5f + 0.08f + random.nextFloat() * 0.34f,
            (index / 2) * 0.5f + 0.08f + random.nextFloat() * 0.34f
        )
    }
}

internal fun playerMeshPalette(songId: String, scheme: ColorScheme): PlayerMeshPalette {
    val base = MeshRgb.from(scheme.primaryContainer)
    return PlayerMeshPalette(
        playerMeshPositions(songId),
        listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer, scheme.surfaceContainer)
            .map { base.mix(MeshRgb.from(it), 0.30f) }
    )
}

internal data class PlayerMeshContrast(val base: MeshRgb, val strength: Float)

internal fun playerMeshContrast(
    colors: List<MeshRgb>,
    base: MeshRgb,
    foreground: MeshRgb
): PlayerMeshContrast {
    val text = luminance(foreground)
    fun contrast(background: Float): Float =
        (maxOf(text, background) + 0.05f) / (minOf(text, background) + 0.05f)

    val baseLuminance = luminance(base)
    fun safeSolid(): PlayerMeshContrast {
        val channel = if (contrast(0f) >= contrast(1f)) 0f else 1f
        return PlayerMeshContrast(MeshRgb(channel, channel, channel), 0f)
    }
    if (contrast(baseLuminance) < MinimumTextContrast) return safeSolid()

    // The normalized shader weights are positive. Each output channel is bounded
    // by its extreme input channels, plus the maximum sRGB grain and quantization.
    val min = MeshRgb(colors.minOf { it.r }, colors.minOf { it.g }, colors.minOf { it.b })
    val max = MeshRgb(colors.maxOf { it.r }, colors.maxOf { it.g }, colors.maxOf { it.b })
    fun safe(strength: Float): Boolean {
        val bound = if (baseLuminance < text) max else min
        val sign = if (baseLuminance < text) 1f else -1f
        val mixed = base.mix(bound, strength)
        val margin = sign * (Grain * strength + 0.5f / 255f)
        val extreme = luminance(
            MeshRgb(shiftSrgb(mixed.r, margin), shiftSrgb(mixed.g, margin), shiftSrgb(mixed.b, margin))
        )
        return (extreme - text) * (baseLuminance - text) >= 0f && contrast(extreme) >= MinimumTextContrast
    }
    if (!safe(0f)) return safeSolid()
    if (safe(1f)) return PlayerMeshContrast(base, 1f)

    var low = 0f
    var high = 1f
    repeat(8) {
        val middle = (low + high) / 2f
        if (safe(middle)) low = middle else high = middle
    }
    return PlayerMeshContrast(base, low)
}

private fun luminance(rgb: MeshRgb) = 0.2126f * rgb.r + 0.7152f * rgb.g + 0.0722f * rgb.b

private fun shiftSrgb(linear: Float, delta: Float): Float {
    val encoded = if (linear <= 0.0031308f) 12.92f * linear else 1.055f * linear.pow(1f / 2.4f) - 0.055f
    val shifted = (encoded + delta).coerceIn(0f, 1f)
    return if (shifted <= 0.04045f) shifted / 12.92f else ((shifted + 0.055f) / 1.055f).pow(2.4f)
}

@Composable
internal fun PlayerMeshBackground(
    songId: String,
    targetScheme: ColorScheme,
    baseColor: Color,
    foregroundColor: Color,
    expansion: () -> Float,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val shader = remember(view) { if (view.isHardwareAccelerated) runCatching { RuntimeShader(MeshShader) }.getOrNull() else null }
    val brush = remember(shader) { shader?.let(::ShaderBrush) }
    val target = remember(songId, targetScheme) { playerMeshPalette(songId, targetScheme) }
    val animate = LocalMotionPreferences.current.animationsEnabled
    val progress = remember { Animatable(1f) }
    var endpoints by remember { mutableStateOf(target to target) }
    LaunchedEffect(target, animate) {
        val (from, to) = endpoints
        if (!animate) {
            endpoints = target to target
            progress.snapTo(1f)
        } else if (to != target || progress.value < 1f) {
            val visible = from.mix(to, progress.value)
            progress.snapTo(0f)
            endpoints = visible to target
            progress.animateTo(1f, tween(600))
        }
    }

    Spacer(modifier.graphicsLayer { alpha = expansion().coerceIn(0f, 1f) }.drawWithCache {
        val base = MeshRgb.from(baseColor)
        val text = MeshRgb.from(foregroundColor)
        shader?.setFloatUniform("resolution", size.width, size.height)
        onDrawBehind {
            tracePlayerDepthDraw(PlayerMeshDrawTrace) {
                val (from, to) = endpoints
                val blend = progress.value
                val visible = if (blend >= 1f) to else from.mix(to, blend)
                val contrast = playerMeshContrast(visible.colors, base, text)
                if (brush == null || shader == null || contrast.strength == 0f) {
                    drawRect(contrast.base.color())
                } else {
                    shader.setFloatUniform("base", contrast.base.r, contrast.base.g, contrast.base.b)
                    shader.setFloatUniform("strength", contrast.strength)
                    shader.setFloatUniform("progress", blend)
                    shader.setPalette("from", from)
                    shader.setPalette("to", to)
                    drawRect(brush)
                }
            }
        }
    })
}

private fun RuntimeShader.setPalette(prefix: String, palette: PlayerMeshPalette) {
    for (index in palette.positions.indices) {
        val (x, y) = palette.positions[index]
        val (r, g, b) = palette.colors[index]
        setFloatUniform("${prefix}Point$index", x, y)
        setFloatUniform("${prefix}Color$index", r, g, b)
    }
}

internal inline fun DrawScope.tracePlayerDepthDraw(name: String, draw: DrawScope.() -> Unit) {
    if (!Trace.isEnabled()) {
        draw()
        return
    }
    Trace.beginSection(name)
    try {
        draw()
    } finally {
        Trace.endSection()
    }
}

private const val MeshShader = """
    uniform float2 resolution;
    uniform float2 fromPoint0, fromPoint1, fromPoint2, fromPoint3;
    uniform float2 toPoint0, toPoint1, toPoint2, toPoint3;
    uniform float3 fromColor0, fromColor1, fromColor2, fromColor3;
    uniform float3 toColor0, toColor1, toColor2, toColor3;
    uniform float3 base;
    uniform float strength, progress;

    float weight(float2 p, float2 a, float2 b) {
        float2 d = p - mix(a, b, progress);
        return 1.0 / (dot(d, d) + 0.045);
    }

    half4 main(float2 pixel) {
        float2 p = pixel / max(resolution, float2(1.0));
        float w0 = weight(p, fromPoint0, toPoint0);
        float w1 = weight(p, fromPoint1, toPoint1);
        float w2 = weight(p, fromPoint2, toPoint2);
        float w3 = weight(p, fromPoint3, toPoint3);
        float3 mesh = (w0 * mix(fromColor0, toColor0, progress) +
                       w1 * mix(fromColor1, toColor1, progress) +
                       w2 * mix(fromColor2, toColor2, progress) +
                       w3 * mix(fromColor3, toColor3, progress)) / (w0 + w1 + w2 + w3);
        float3 linearRgb = clamp(mix(base, mesh, strength), 0.0, 1.0);
        float3 srgb = mix(12.92 * linearRgb, 1.055 * pow(linearRgb, float3(1.0 / 2.4)) - 0.055,
                          step(float3(0.0031308), linearRgb));
        float noise = fract(sin(dot(floor(pixel), float2(12.9898, 78.233))) * 43758.5453) - 0.5;
        srgb = clamp(srgb + (3.0 / 255.0) * noise * strength, 0.0, 1.0);
        linearRgb = mix(srgb / 12.92, pow((srgb + 0.055) / 1.055, float3(2.4)),
                     step(float3(0.04045), srgb));
        return half4(fromLinearSrgb(linearRgb), 1.0);
    }
"""
