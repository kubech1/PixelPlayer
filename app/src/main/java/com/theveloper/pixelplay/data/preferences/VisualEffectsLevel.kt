package com.theveloper.pixelplay.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

enum class VisualEffectsLevel(val storageKey: String) {
    FULL("full"),
    BALANCED("balanced"),
    MINIMAL("minimal");

    fun downgradeOneLevel(): VisualEffectsLevel = when (this) {
        FULL -> BALANCED
        BALANCED -> MINIMAL
        MINIMAL -> MINIMAL
    }

    companion object {
        val DEFAULT: VisualEffectsLevel = BALANCED

        fun fromStorageKey(key: String?): VisualEffectsLevel =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Migrates the legacy `disable_blur_all_over` boolean preference to `visual_effects_level`
 * (`true -> MINIMAL`, `false -> BALANCED`) and deletes the legacy key.
 *
 * Safe to run repeatedly and preserves an already-stored `visual_effects_level`.
 */
internal class VisualEffectsPreferencesMigration : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.contains(LEGACY_DISABLE_BLUR_KEY)

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            val legacyDisableBlur = this[LEGACY_DISABLE_BLUR_KEY]
            if (legacyDisableBlur != null && !contains(VISUAL_EFFECTS_LEVEL_KEY)) {
                val targetLevel = if (legacyDisableBlur) {
                    VisualEffectsLevel.MINIMAL
                } else {
                    VisualEffectsLevel.BALANCED
                }
                this[VISUAL_EFFECTS_LEVEL_KEY] = targetLevel.storageKey
            }
            remove(LEGACY_DISABLE_BLUR_KEY)
        }.toPreferences()

    override suspend fun cleanUp() = Unit

    companion object {
        internal val LEGACY_DISABLE_BLUR_KEY = booleanPreferencesKey("disable_blur_all_over")
        internal val VISUAL_EFFECTS_LEVEL_KEY = stringPreferencesKey("visual_effects_level")
    }
}
