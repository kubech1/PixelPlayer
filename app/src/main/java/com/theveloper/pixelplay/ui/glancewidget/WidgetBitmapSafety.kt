package com.theveloper.pixelplay.ui.glancewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.SizeF
import android.widget.RemoteViews
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.service.MusicService
import java.security.MessageDigest
import timber.log.Timber

internal val LocalWidgetBitmapBudget = staticCompositionLocalOf { WidgetBitmapBudget(256 * 1024L, 2) }

internal fun widgetBitmapBudget(context: Context, id: GlanceId, artworkSlots: Int = 5): WidgetBitmapBudget {
    val metrics = context.resources.displayMetrics
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(GlanceAppWidgetManager(context).getAppWidgetId(id))
    val count = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)?.size ?: 2
    return WidgetBitmapBudget(minOf(4 * 1024 * 1024L, metrics.widthPixels.toLong() * metrics.heightPixels * 4).coerceAtLeast(0), count.coerceAtLeast(2), artworkSlots)
}

/** Framework validation is the final authority; retain controls even when a host rejects bitmap memory. */
abstract class MemorySafeGlanceWidget : GlanceAppWidget() {
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        val memoryRejected = generateSequence(throwable) { it.cause }.any {
            it is IllegalArgumentException && it.message.orEmpty().contains("bitmap memory", ignoreCase = true)
        }
        if (!memoryRejected) { super.onCompositionError(context, glanceId, appWidgetId, throwable); return }
        Timber.w(throwable, "Widget bitmap payload rejected; publishing controls without artwork")
        val views = createWidgetMemoryFallback(context)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun createWidgetMemoryFallback(context: Context): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_memory_fallback)
    listOf(R.id.widget_fallback_previous to PlayerActions.PREVIOUS,
        R.id.widget_fallback_play to PlayerActions.PLAY_PAUSE,
        R.id.widget_fallback_next to PlayerActions.NEXT).forEach { (viewId, action) ->
        val intent = Intent(context, MusicService::class.java).setAction(action)
            .putExtra(MusicService.EXTRA_FORCE_FOREGROUND_ON_START, true)
        views.setColorStateList(viewId, "setImageTintList", android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE))
        views.setOnClickPendingIntent(viewId, PendingIntent.getForegroundService(context, viewId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    }
    return views
}

internal fun boundedWidgetBitmap(bitmap: Bitmap, maxBytes: Int): Bitmap? {
    val (w, h) = fitWidgetImage(bitmap.width, bitmap.height, maxBytes) ?: return null
    val result = if (w == bitmap.width && h == bitmap.height) bitmap else Bitmap.createScaledBitmap(bitmap, w, h, true)
    return result.takeIf { it.byteCount <= maxBytes }
}

internal fun loadSizedWidgetArtwork(context: Context, bytes: ByteArray?, uri: String?, width: Int, height: Int, maxBytes: Int): Bitmap? {
    val (w, h) = fitWidgetImage(width, height, maxBytes) ?: return null
    val content = bytes?.let { MessageDigest.getInstance("SHA-256").digest(it).joinToString("") { b -> "%02x".format(b) } }
        ?: uri?.let { "uri:$it" } ?: return null
    val key = "$content:$w:$h:${context.resources.displayMetrics.densityDpi}:$maxBytes"
    AlbumArtBitmapCache.getBitmap(key)?.let { return it }
    return runCatching {
        val decoded = if (bytes != null) decodeSizedWidgetBytes(bytes, w, h)
            else decodeWidgetAlbumArtBitmap(context, checkNotNull(uri), w, h)
        decoded?.let { original ->
            boundedWidgetBitmap(original, maxBytes)?.let { bounded ->
                if (bounded !== original) original.recycle()
                AlbumArtBitmapCache.putBitmap(key, bounded)
                bounded
            }
        }
    }.onFailure { Timber.w(it, "Widget artwork unavailable") }.getOrNull()
}

internal fun decodeSizedWidgetBytes(bytes: ByteArray, width: Int, height: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth.toLong() / sample > width.toLong() * 2 || bounds.outHeight.toLong() / sample > height.toLong() * 2) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }) ?: return null
    // Bound dimensions as well as payload bytes, including non-power-of-two source sizes.
    val scale = minOf(1.0, width.toDouble() / decoded.width, height.toDouble() / decoded.height)
    val w = (decoded.width * scale).toInt().coerceAtLeast(1)
    val h = (decoded.height * scale).toInt().coerceAtLeast(1)
    return if (w == decoded.width && h == decoded.height) decoded else Bitmap.createScaledBitmap(decoded, w, h, true).also { decoded.recycle() }
}
