package com.theveloper.pixelplay.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

/** Read-only compatibility vocabulary for retiring old settings; contains no inference code. */
object RetiredAiData {
    const val WORK_TAG = "ai_generation_worker"

    private val providers = setOf(
        "gemini", "deepseek", "groq", "mistral", "nvidia", "kimi", "glm",
        "openai", "openrouter", "ollama", "custom"
    )
    private val providerSettings = setOf("api_key", "model", "system_prompt", "base_url")
    private val keys = setOf(
        "ai_provider", "safe_token_limit", "ai_temperature", "ai_top_p", "ai_top_k",
        "ai_max_tokens", "ai_presence_penalty", "ai_frequency_penalty", "ai_sample_size",
        "ai_digest_mode", "ai_include_extended_fields"
    ) + providers.flatMap { provider -> providerSettings.map { "${provider}_$it" } }

    fun isPreferenceKey(name: String): Boolean = name in keys

    fun normalizePlaylistSource(source: String?): String =
        if (source.isNullOrBlank() || source.equals("AI", ignoreCase = true)) "LOCAL" else source
}

/** Runs before DataStore exposes values; repeating it is safe and leaves unrelated keys intact. */
internal class RetiredAiPreferencesMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.asMap().keys.any { RetiredAiData.isPreferenceKey(it.name) }

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            asMap().keys.filter { RetiredAiData.isPreferenceKey(it.name) }.forEach { key ->
                @Suppress("UNCHECKED_CAST")
                remove(key as Preferences.Key<Any>)
            }
        }.toPreferences()

    override suspend fun cleanUp() = Unit
}
