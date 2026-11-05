package com.theveloper.pixelplay.ui.glancewidget

import kotlin.math.floor
import kotlin.math.sqrt

/** Conservative aggregate payload allocation: five artworks, one background and one progress image per size. */
internal data class WidgetBitmapBudget(val payloadBytes: Long, val sizeBuckets: Int, val artworkSlots: Int = 5) {
    val artworkBytes: Int get() = (payloadBytes * 60 / 100 / sizeBuckets.coerceAtLeast(2) / artworkSlots.coerceAtLeast(1)).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    val backgroundBytes: Int get() = (payloadBytes * 20 / 100 / sizeBuckets.coerceAtLeast(2)).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    val progressBytes: Int get() = (payloadBytes * 10 / 100 / sizeBuckets.coerceAtLeast(2)).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
}

internal fun fitWidgetImage(width: Int, height: Int, maxBytes: Int): Pair<Int, Int>? {
    if (width <= 0 || height <= 0 || maxBytes < 4) return null
    val maxPixels = maxBytes / 4L
    val scale = sqrt(maxPixels.toDouble() / (width.toDouble() * height)).coerceAtMost(1.0)
    var w = floor(width * scale).toInt().coerceAtLeast(1)
    var h = floor(height * scale).toInt().coerceAtLeast(1)
    // Rounding very narrow images up to one pixel must still respect the budget.
    if (w.toLong() * h > maxPixels) {
        if (w >= h) w = (maxPixels / h).toInt().coerceAtLeast(1)
        else h = (maxPixels / w).toInt().coerceAtLeast(1)
    }
    return w to h
}
