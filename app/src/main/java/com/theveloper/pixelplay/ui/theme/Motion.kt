package com.theveloper.pixelplay.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * Tokens de movimiento propios del lenguaje visual contemporáneo de PixelPlayer.
 *
 * Regla rectora: la estructura espacial se desplaza críticamente amortiguada (sin rebote),
 * mientras que la respuesta directa a un toque del usuario conserva un rebote ligero (~12 %).
 */
object PixelMotion {
    const val QUICK_DURATION_MS = 150
    const val STANDARD_DURATION_MS = 300
    const val SLOW_DURATION_MS = 450

    val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    fun <T> structural(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 1f,
            stiffness = 380f,
            visibilityThreshold = visibilityThreshold
        )

    fun <T> structuralSlow(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 1f,
            stiffness = 200f,
            visibilityThreshold = visibilityThreshold
        )

    fun <T> feedback(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 0.55f,
            stiffness = 700f,
            visibilityThreshold = visibilityThreshold
        )

    fun <T> indicator(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 0.75f,
            stiffness = 600f,
            visibilityThreshold = visibilityThreshold
        )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object PixelPlayMotionScheme : MotionScheme {
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.7f, stiffness = 1400f)

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        PixelMotion.structural()

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        PixelMotion.structuralSlow()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = PixelMotion.QUICK_DURATION_MS)

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = PixelMotion.STANDARD_DURATION_MS)

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = PixelMotion.SLOW_DURATION_MS)
}

@Immutable
data class MotionPreferences(
    val animationsEnabled: Boolean = true,
    val touchExplorationEnabled: Boolean = false
)

val LocalMotionPreferences = compositionLocalOf { MotionPreferences() }
