package com.theveloper.pixelplay.data.preferences

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RetiredNavigationPreferencesMigrationTest {
    @Test fun removesOnlyObsoleteKeysAndIsIdempotent() = runTest {
        val migration = RetiredNavigationPreferencesMigration()
        val enabled = booleanPreferencesKey("nav_compact_on_scroll")
        val smooth = booleanPreferencesKey("use_smooth_corners")
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("nav_bar_style") to "full_width",
            booleanPreferencesKey("nav_bar_compact_mode") to true,
            intPreferencesKey("nav_bar_corner_radius") to 12,
            enabled to false, smooth to true
        )
        assertTrue(migration.shouldMigrate(preferences))
        val result = migration.migrate(preferences)
        assertEquals(1, result.asMap().size)
        assertEquals(false, result[enabled])
        assertNull(result[smooth])
        assertFalse(migration.shouldMigrate(result))
        assertEquals(result, migration.migrate(result))
        assertFalse(migration.shouldMigrate(emptyPreferences()))
    }
}
