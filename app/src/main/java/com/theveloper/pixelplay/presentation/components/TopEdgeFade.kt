package com.theveloper.pixelplay.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer

/** Softens content as it passes behind a fixed header without affecting touch or semantics. */
fun Modifier.topEdgeFade(
    background: Color,
    height: Dp = 24.dp,
    visible: () -> Boolean,
): Modifier = drawWithContent {
    drawContent()
    if (visible()) {
        val fadeHeight = height.toPx().coerceAtMost(size.height)
        if (fadeHeight > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(background, background.copy(alpha = 0f)),
                    startY = 0f,
                    endY = fadeHeight,
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, fadeHeight),
            )
        }
    }
}

/** Keeps scrolling text legible where a compact floating header overlaps the list. */
@Composable
fun CollapsibleHeaderScrim(height: Dp, collapseFraction: () -> Float) {
    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier.fillMaxWidth()
            .height(height + 24.dp)
            .graphicsLayer { alpha = collapseFraction().coerceIn(0f, 1f) }
            .background(
                Brush.verticalGradient(
                    0f to background,
                    0.72f to background,
                    1f to background.copy(alpha = 0f),
                )
            )
    )
}
