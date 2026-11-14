package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.lerp
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.ThemePreference
import com.theveloper.pixelplay.presentation.viewmodel.ColorSchemePair
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences

/**
 * Theme state for the player sheet.
 *
 * Expansion-dependent alpha and elevation are not included here.
 * They are computed inline in the consuming composable's `graphicsLayer` / `derivedStateOf`,
 * reading directly from the [Animatable] expansion fraction during the draw phase.
 * This eliminates per-frame recomposition that the old [Transition]-based approach caused.
 */
internal data class SheetThemeState(
    val ambientColorScheme: ColorScheme?,
    val isPreparingPlayback: Boolean,
    val miniReadyAlpha: Float,
    val miniAppearScale: Float
)

internal fun resolvePlayerSheetTargetScheme(
    isAlbumArtTheme: Boolean,
    hasAlbumArt: Boolean,
    currentSongActiveScheme: ColorScheme?,
    lastAlbumScheme: ColorScheme?,
    systemColorScheme: ColorScheme
): ColorScheme {
    return when {
        !isAlbumArtTheme || !hasAlbumArt -> systemColorScheme
        currentSongActiveScheme != null -> currentSongActiveScheme
        lastAlbumScheme != null -> lastAlbumScheme
        else -> systemColorScheme
    }
}

@Composable
internal fun rememberSheetThemeState(
    activePlayerSchemePair: ColorSchemePair?,
    isDarkTheme: Boolean,
    playerThemePreference: String,
    currentSong: Song?,
    themedAlbumArtUri: String?,
    preparingSongId: String?,
    systemColorScheme: ColorScheme
): SheetThemeState {
    val isAlbumArtTheme = playerThemePreference == ThemePreference.ALBUM_ART
    val hasAlbumArt = !currentSong?.albumArtUriString.isNullOrBlank()

    val activePlayerScheme = remember(activePlayerSchemePair, isDarkTheme) {
        activePlayerSchemePair?.let { if (isDarkTheme) it.dark else it.light }
    }
    val currentSongActiveScheme = remember(
        activePlayerScheme,
        currentSong?.albumArtUriString,
        themedAlbumArtUri
    ) {
        if (
            activePlayerScheme != null &&
            hasAlbumArt &&
            currentSong.albumArtUriString == themedAlbumArtUri
        ) {
            activePlayerScheme
        } else {
            null
        }
    }

    var lastAlbumScheme by remember(isDarkTheme) { mutableStateOf<ColorScheme?>(null) }
    LaunchedEffect(currentSongActiveScheme, isDarkTheme) {
        if (currentSongActiveScheme != null) {
            lastAlbumScheme = currentSongActiveScheme
        }
    }

    val isPreparingPlayback = remember(preparingSongId, currentSong?.id) {
        preparingSongId != null && preparingSongId == currentSong?.id
    }

    val ambientColorScheme = if (isAlbumArtTheme) {
        // While artwork loads, keep the previous hue. Tracks without artwork use the solid base.
        resolvePlayerSheetTargetScheme(
            isAlbumArtTheme = true,
            hasAlbumArt = hasAlbumArt,
            currentSongActiveScheme = currentSongActiveScheme,
            lastAlbumScheme = lastAlbumScheme,
            systemColorScheme = systemColorScheme
        ).takeUnless { it === systemColorScheme }
    } else {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        remember(context, configuration, isDarkTheme) {
            runCatching {
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }.getOrNull()
        }
    }

    val animationsEnabled = LocalMotionPreferences.current.animationsEnabled
    val miniAppearProgress = remember { Animatable(0f) }
    LaunchedEffect(currentSong?.id, animationsEnabled) {
        if (currentSong == null) {
            miniAppearProgress.snapTo(0f)
        } else if (!animationsEnabled) {
            miniAppearProgress.snapTo(1f)
        } else if (miniAppearProgress.value < 1f) {
            miniAppearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
    }

    val miniReadyAlpha = miniAppearProgress.value
    val miniAppearScale = lerp(0.985f, 1f, miniAppearProgress.value)

    // NOTE: miniAlpha and effectivePlayerAreaElevation are no longer computed here.
    // They were driven by the expansion fraction via the Transition API, which
    // read `playerContentExpansionFraction.value` during composition — causing
    // per-frame recomposition of UnifiedPlayerSheetV2 during every gesture.
    //
    // These values are now computed inline at their consumption sites:
    //   - miniAlpha → inside graphicsLayer in UnifiedPlayerMiniAndFullLayers
    //   - elevation → inside derivedStateOf for visualCardShadowElevation

    return SheetThemeState(
        ambientColorScheme = ambientColorScheme,
        isPreparingPlayback = isPreparingPlayback,
        miniReadyAlpha = miniReadyAlpha,
        miniAppearScale = miniAppearScale
    )
}
