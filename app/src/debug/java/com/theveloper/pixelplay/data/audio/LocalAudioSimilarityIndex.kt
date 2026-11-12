package com.theveloper.pixelplay.data.audio

import android.net.Uri
import com.theveloper.pixelplay.data.database.SourceType
import com.theveloper.pixelplay.data.model.Song
import java.util.LinkedHashMap

/** Session-scoped LRU: avoids repeat decoding without adding a Room schema or persisting stale features. */
class LocalAudioSimilarityIndex(
    private val analyzer: LocalAudioAnalyzer,
    private val maxEntries: Int = 128
) {
    private data class CacheKey(val uri: String, val modified: Long, val duration: Long, val sizeBytes: Long?, val version: Int)
    private val profiles = object : LinkedHashMap<CacheKey, AudioSimilarityProfile>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, AudioSimilarityProfile>?): Boolean = size > maxEntries
    }

    @Synchronized
    fun profile(song: Song): AudioSimilarityProfile? {
        if (SourceType.fromContentUri(song.contentUriString) != SourceType.LOCAL || song.contentUriString.isBlank()) return null
        return profile(Uri.parse(song.contentUriString), song.dateModified, song.duration, null)?.profile
    }

    /** Diagnostic entry point returns whether the profile came from the session cache. */
    @Synchronized
    fun profile(uri: Uri, modified: Long, durationMs: Long, sizeBytes: Long?): CachedAudioProfile? {
        if (uri.scheme != "content" || durationMs <= 0) return null
        val key = CacheKey(uri.toString(), modified, durationMs, sizeBytes, AudioSimilarity.PROFILE_VERSION)
        profiles[key]?.let { return CachedAudioProfile(it, cacheHit = true, analysisDurationMs = 0L) }
        val start = System.nanoTime()
        val analyzed = try { analyzer.analyze(uri) } catch (_: Exception) { return null }
        profiles[key] = analyzed
        return CachedAudioProfile(analyzed, cacheHit = false, analysisDurationMs = (System.nanoTime() - start) / 1_000_000L)
    }

    /** Returns candidates ranked by deterministic acoustic score; seed and non-local sources are excluded. */
    fun similarTo(seed: Song, candidates: Iterable<Song>, limit: Int = 20): List<Pair<Song, AudioSimilarityResult>> {
        if (limit <= 0) return emptyList()
        val seedProfile = profile(seed) ?: return emptyList()
        return candidates.asSequence()
            .filter { it.id != seed.id && SourceType.fromContentUri(it.contentUriString) == SourceType.LOCAL }
            .mapNotNull { candidate ->
                val candidateProfile = profile(candidate) ?: return@mapNotNull null
                AudioSimilarity.compare(seedProfile, candidateProfile)?.let { candidate to it }
            }
            .sortedByDescending { it.second.score }
            .take(limit)
            .toList()
    }
}

data class CachedAudioProfile(
    val profile: AudioSimilarityProfile,
    val cacheHit: Boolean,
    val analysisDurationMs: Long
)
