package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.theveloper.pixelplay.presentation.viewmodel.PlayerSheetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

internal data class FullPlayerCompositionPolicy(
    val shouldRenderFullPlayer: Boolean
)

/**
 * Decides whether the full-player composable tree should be in composition.
 *
 * Accepts [Animatable] instead of a raw Float so that the expansion fraction is
 * read inside [derivedStateOf] / [snapshotFlow] — never as a `remember` key or
 * `LaunchedEffect` key. This prevents per-frame recomposition of the caller during
 * sheet drag gestures.
 */
@Composable
internal fun rememberFullPlayerCompositionPolicy(
    currentSongId: String?,
    currentSheetState: PlayerSheetState,
    expansionFraction: Animatable<Float, AnimationVector1D>,
    collapsedWarmDelayMs: Long = 200L
): FullPlayerCompositionPolicy {
    // Keep the full-player tree warm across song changes once composed; tearing down
    // and recreating the entire FullPlayerContent tree on every song change causes jank.
    var keepFullPlayerComposed by remember { mutableStateOf(false) }

    LaunchedEffect(currentSongId != null, currentSheetState) {
        if (currentSongId == null) {
            keepFullPlayerComposed = false
            return@LaunchedEffect
        }

        if (keepFullPlayerComposed || currentSheetState == PlayerSheetState.EXPANDED) {
            keepFullPlayerComposed = true
        } else {
            // Warm the hidden full-player tree shortly after the collapsed state settles.
            delay(collapsedWarmDelayMs)
            keepFullPlayerComposed = true
        }
    }

    LaunchedEffect(currentSongId != null) {
        if (currentSongId == null || keepFullPlayerComposed) return@LaunchedEffect
        snapshotFlow {
            keepFullPlayerComposed || expansionFraction.value > 0.05f
        }.first { it }
        if (!keepFullPlayerComposed) keepFullPlayerComposed = true
    }

    // Read expansion fraction inside derivedStateOf so that changes only trigger
    // recomposition of direct consumers when the Boolean result flips.
    val shouldRenderFullPlayer by remember(currentSongId, currentSheetState) {
        derivedStateOf {
            currentSongId != null && (
                currentSheetState == PlayerSheetState.EXPANDED ||
                    expansionFraction.value > 0.015f ||
                    keepFullPlayerComposed
                )
        }
    }

    return FullPlayerCompositionPolicy(
        shouldRenderFullPlayer = shouldRenderFullPlayer
    )
}
