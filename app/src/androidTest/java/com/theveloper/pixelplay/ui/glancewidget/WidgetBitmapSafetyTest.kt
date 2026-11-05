package com.theveloper.pixelplay.ui.glancewidget

import android.graphics.Bitmap
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.theveloper.pixelplay.presentation.utils.AppHapticsConfig
import com.theveloper.pixelplay.presentation.utils.AppHapticEvent
import com.theveloper.pixelplay.presentation.utils.performAppHapticFeedback
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetBitmapSafetyTest {
    @Test fun memoryFallbackInflatesPlatformControlsWithTintAndActions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = createWidgetMemoryFallback(context).apply(context, android.widget.FrameLayout(context))
        listOf(com.theveloper.pixelplay.R.id.widget_fallback_previous,
            com.theveloper.pixelplay.R.id.widget_fallback_play,
            com.theveloper.pixelplay.R.id.widget_fallback_next).forEach { id ->
            val button = root.findViewById<android.widget.ImageButton>(id)
            assertEquals(android.graphics.Color.WHITE, button.imageTintList?.defaultColor)
            assertTrue(button.hasOnClickListeners())
            assertTrue(button.isEnabled)
        }
    }
    @Test fun largeArtworkIsSizedBeforePublishingAndCacheSeparatesSizes() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = Bitmap.createBitmap(4096, 2048, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { output ->
            source.compress(Bitmap.CompressFormat.JPEG, 80, output)
            output.toByteArray()
        }
        source.recycle()
        val small = checkNotNull(loadSizedWidgetArtwork(context, bytes, null, 100, 100, 40_000))
        val large = checkNotNull(loadSizedWidgetArtwork(context, bytes, null, 200, 200, 160_000))
        assertTrue(small.width <= 100 && small.height <= 100 && small.byteCount <= 40_000)
        assertTrue(large.width <= 200 && large.height <= 200 && large.byteCount <= 160_000)
        assertNotSame(small, large)
        val configuration = android.content.res.Configuration(context.resources.configuration).apply { densityDpi += 1 }
        val differentDensity = context.createConfigurationContext(configuration)
        assertNotSame(small, loadSizedWidgetArtwork(differentDensity, bytes, null, 100, 100, 40_000))
        assertSame(small, loadSizedWidgetArtwork(context, bytes, null, 100, 100, 40_000))
        assertNull(loadSizedWidgetArtwork(context, bytes, null, 200, 200, 3))
        assertNull(loadSizedWidgetArtwork(context, byteArrayOf(1,2,3), null, 100,100,40_000))
    }
    @Test fun disabledOrDetachedViewsNeverRequestHaptics() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = View(context)
        assertFalse(view.performAppHapticFeedback(AppHapticsConfig(enabled = false), AppHapticEvent.CONFIRMATION))
        assertFalse(view.performAppHapticFeedback(AppHapticsConfig(enabled = true), AppHapticEvent.SELECTION))
    }
}
