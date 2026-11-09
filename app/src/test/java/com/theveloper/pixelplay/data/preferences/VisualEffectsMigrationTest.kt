package com.theveloper.pixelplay.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.theveloper.pixelplay.presentation.utils.EffectiveVisualEffects
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VisualEffectsMigrationTest {

    private val legacyBlurKey = booleanPreferencesKey("disable_blur_all_over")
    private val visualEffectsKey = stringPreferencesKey("visual_effects_level")
    private val unrelatedKey = booleanPreferencesKey("haptics_enabled")

    @Test
    fun `without legacy key migration does not run and repository defaults to BALANCED`() = runTest {
        val prefs = mutablePreferencesOf(unrelatedKey to true)
        val migration = VisualEffectsPreferencesMigration()

        assertFalse(migration.shouldMigrate(prefs))

        val tempDir = Files.createTempDirectory("visual-effects-default-test")
        try {
            val dataStore = PreferenceDataStoreFactory.create(
                migrations = listOf(migration),
                scope = backgroundScope,
                produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
            )
            val repository = UserPreferencesRepository(dataStore = dataStore, json = Json)

            assertEquals(VisualEffectsLevel.BALANCED, repository.visualEffectsLevelFlow.first())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `legacy true migrates to MINIMAL and deletes legacy key`() = runTest {
        val prefs = mutablePreferencesOf(
            legacyBlurKey to true,
            unrelatedKey to false
        )
        val migration = VisualEffectsPreferencesMigration()

        assertTrue(migration.shouldMigrate(prefs))
        val migrated = migration.migrate(prefs)

        assertFalse(migration.shouldMigrate(migrated))
        assertNull(migrated[legacyBlurKey])
        assertEquals(VisualEffectsLevel.MINIMAL.storageKey, migrated[visualEffectsKey])
        assertEquals(false, migrated[unrelatedKey])
    }

    @Test
    fun `legacy false migrates to BALANCED and deletes legacy key`() = runTest {
        val prefs = mutablePreferencesOf(
            legacyBlurKey to false,
            unrelatedKey to true
        )
        val migration = VisualEffectsPreferencesMigration()

        assertTrue(migration.shouldMigrate(prefs))
        val migrated = migration.migrate(prefs)

        assertFalse(migration.shouldMigrate(migrated))
        assertNull(migrated[legacyBlurKey])
        assertEquals(VisualEffectsLevel.BALANCED.storageKey, migrated[visualEffectsKey])
        assertEquals(true, migrated[unrelatedKey])
    }

    @Test
    fun `migration is idempotent and preserves an already stored visual effects level`() = runTest {
        val prefsWithBoth = mutablePreferencesOf(
            legacyBlurKey to true,
            visualEffectsKey to VisualEffectsLevel.FULL.storageKey,
            unrelatedKey to true
        )
        val migration = VisualEffectsPreferencesMigration()

        val firstPass = migration.migrate(prefsWithBoth)
        val secondPass = migration.migrate(firstPass)

        assertEquals(firstPass, secondPass)
        assertNull(secondPass[legacyBlurKey])
        assertEquals(VisualEffectsLevel.FULL.storageKey, secondPass[visualEffectsKey])
        assertEquals(true, secondPass[unrelatedKey])

        val tempDir = Files.createTempDirectory("visual-effects-datastore-migration-test")
        try {
            val file = tempDir.resolve("settings.preferences_pb").toFile()
            val seedStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { file }
            )
            seedStore.edit { it[legacyBlurKey] = true }

            // Verify repository round-trip on migrated preferences snapshot
            val migratedStore = PreferenceDataStoreFactory.create(
                migrations = listOf(VisualEffectsPreferencesMigration()),
                scope = backgroundScope,
                produceFile = { tempDir.resolve("migrated.preferences_pb").toFile() }
            )
            migratedStore.edit {
                it[legacyBlurKey] = true
            }
            // Apply migration directly to verify end-to-end key mapping with repository
            migratedStore.updateData { current ->
                if (migration.shouldMigrate(current)) migration.migrate(current) else current
            }
            val repository = UserPreferencesRepository(dataStore = migratedStore, json = Json)
            assertEquals(VisualEffectsLevel.MINIMAL, repository.visualEffectsLevelFlow.first())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `battery saver steps down effective visual effects by one level`() {
        assertEquals(
            VisualEffectsLevel.BALANCED,
            EffectiveVisualEffects(VisualEffectsLevel.FULL, isPowerSaveMode = true).effectiveLevel
        )
        assertEquals(
            VisualEffectsLevel.MINIMAL,
            EffectiveVisualEffects(VisualEffectsLevel.BALANCED, isPowerSaveMode = true).effectiveLevel
        )
        assertEquals(
            VisualEffectsLevel.MINIMAL,
            EffectiveVisualEffects(VisualEffectsLevel.MINIMAL, isPowerSaveMode = true).effectiveLevel
        )
        assertEquals(
            VisualEffectsLevel.FULL,
            EffectiveVisualEffects(VisualEffectsLevel.FULL, isPowerSaveMode = false).effectiveLevel
        )
    }
}
