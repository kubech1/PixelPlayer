package com.theveloper.pixelplay.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.theveloper.pixelplay.data.model.Playlist
import com.theveloper.pixelplay.data.database.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class RetiredAiDataTest {
    @Test
    fun `migration removes all retired provider settings and is idempotent`() = runTest {
        val keys = listOf("gemini_api_key", "custom_base_url", "openai_model", "ollama_system_prompt", "ai_provider")
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("navidrome_password") to "synthetic-password",
            stringPreferencesKey("jellyfin_api_key") to "synthetic-token",
            booleanPreferencesKey("haptics_enabled") to false,
            booleanPreferencesKey("safe_token_limit") to true
        ).apply { keys.forEach { this[stringPreferencesKey(it)] = "synthetic" } }
        val migration = RetiredAiPreferencesMigration()
        assertTrue(migration.shouldMigrate(preferences))
        val migrated = migration.migrate(preferences)
        assertFalse(migration.shouldMigrate(migrated))
        assertEquals(migrated, migration.migrate(migrated))
        assertEquals("synthetic-password", migrated[stringPreferencesKey("navidrome_password")])
        assertEquals("synthetic-token", migrated[stringPreferencesKey("jellyfin_api_key")])
        assertEquals(false, migrated[booleanPreferencesKey("haptics_enabled")])
        assertEquals(3, migrated.asMap().size)
    }

    @Test
    fun `old generated playlist keeps identity songs cover and order as local playlist`() {
        val playlist = Gson().fromJson(
            """{"id":"existing-list","name":"Existing list","songIds":["remote:2","17"],"createdAt":100,"lastModified":200,"isAiGenerated":true,"isQueueGenerated":false,"coverImageUri":"content://synthetic/cover","coverColorArgb":42,"source":"AI"}""",
            Playlist::class.java
        )
        val entity = playlist.toEntity()
        assertEquals("LOCAL", entity.source)
        assertEquals("existing-list", entity.id)
        assertEquals(listOf("remote:2", "17"), playlist.songIds)
        assertEquals(100L, entity.createdAt)
        assertEquals(200L, entity.lastModified)
        assertEquals("content://synthetic/cover", entity.coverImageUri)
        assertEquals(42, entity.coverColorArgb)
        assertFalse(Gson().toJson(playlist).contains("isAiGenerated"))
        assertEquals("TELEGRAM", RetiredAiData.normalizePlaylistSource("TELEGRAM"))
    }
}
