package com.theveloper.pixelplay.presentation.components.bottomchrome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun BottomChromeGradient(modifier: Modifier = Modifier) {
    val metrics = LocalBottomChrome.current
    val background = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxWidth().height(metrics.contentBottomPadding + 24.dp).drawBehind {
        val compactHeight = (metrics.systemNavBarInset + 8.dp + 56.dp + 24.dp).toPx()
        val height = androidx.compose.ui.util.lerp(size.height, compactHeight.coerceAtMost(size.height), metrics.mergeFractionProvider())
        val top = size.height - height
        drawRect(Brush.verticalGradient(listOf(background.copy(alpha = 0f), background.copy(alpha = 0.85f)), startY = top, endY = size.height), Offset(0f, top), Size(size.width, height))
    })
}
