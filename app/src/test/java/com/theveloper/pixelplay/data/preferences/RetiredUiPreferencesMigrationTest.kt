package com.theveloper.pixelplay.data.preferences

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RetiredUiPreferencesMigrationTest {
    @Test fun removesOnlyObsoleteKeysAndIsIdempotent() = runTest {
        val migration = RetiredUiPreferencesMigration()
        val enabled = booleanPreferencesKey("nav_compact_on_scroll")
        val smooth = booleanPreferencesKey("use_smooth_corners")
        val tapToClose = booleanPreferencesKey("tap_background_closes_player")
        val blurStrength = floatPreferencesKey("animated_lyrics_blur_strength")
        val collagePattern = stringPreferencesKey("collage_pattern")
        val collageAutoRotate = booleanPreferencesKey("collage_auto_rotate")
        val blurEnabled = booleanPreferencesKey("animated_lyrics_blur_enabled")
        val accuracy = intPreferencesKey("album_art_color_accuracy_v1")
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("nav_bar_style") to "full_width",
            booleanPreferencesKey("nav_bar_compact_mode") to true,
            intPreferencesKey("nav_bar_corner_radius") to 12,
            enabled to false,
            smooth to true,
            tapToClose to true,
            blurStrength to 4f,
            collagePattern to "vinyl_stack",
            collageAutoRotate to true,
            blurEnabled to true,
            accuracy to 7
        )
        assertTrue(migration.shouldMigrate(preferences))
        val result = migration.migrate(preferences)
        assertEquals(3, result.asMap().size)
        assertEquals(false, result[enabled])
        assertNull(result[smooth])
        assertNull(result[tapToClose])
        assertNull(result[blurStrength])
        assertNull(result[collagePattern])
        assertNull(result[collageAutoRotate])
        assertEquals(true, result[blurEnabled])
        assertEquals(7, result[accuracy])
        assertFalse(migration.shouldMigrate(result))
        assertEquals(result, migration.migrate(result))
        assertFalse(migration.shouldMigrate(emptyPreferences()))
    }
}
