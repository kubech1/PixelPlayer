package com.theveloper.pixelplay.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal class RetiredNavigationPreferencesMigration : DataMigration<Preferences> {
    private val keys = listOf(
        stringPreferencesKey("nav_bar_style"),
        booleanPreferencesKey("nav_bar_compact_mode"),
        intPreferencesKey("nav_bar_corner_radius"),
        booleanPreferencesKey("use_smooth_corners")
    )
    override suspend fun shouldMigrate(currentData: Preferences) = keys.any { currentData.asMap().containsKey(it) }
    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        keys.forEach { remove(it) }
    }.toPreferences()
    override suspend fun cleanUp() = Unit
}
