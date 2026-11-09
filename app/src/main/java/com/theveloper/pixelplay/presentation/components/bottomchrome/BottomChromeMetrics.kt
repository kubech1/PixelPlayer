package com.theveloper.pixelplay.presentation.components.bottomchrome

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DefaultMergeFractionProvider: () -> Float = { 0f }

/**
 * Centralized resting metrics for the floating bottom chrome (navigation pill + mini player pill).
 *
 * All [Dp] dimensions represent the **resting (unmerged)** geometry and remain constant during scroll
 * so list `contentPadding` never triggers per-frame remeasurement. Dynamic compaction/merging is
 * read exclusively in layout/draw phases via [mergeFractionProvider].
 */
@Immutable
data class BottomChromeMetrics(
    val systemNavBarInset: Dp = 0.dp,
    val navBarOccupiedHeight: Dp = 0.dp,
    val contentBottomPadding: Dp = 0.dp,
    val miniPlayerRestingHeight: Dp = 64.dp,
    val bottomGradientHeight: Dp = 0.dp,
    val mergeFractionProvider: () -> Float = DefaultMergeFractionProvider
)

val LocalBottomChrome = compositionLocalOf { BottomChromeMetrics() }
