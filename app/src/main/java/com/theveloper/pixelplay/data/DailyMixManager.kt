package com.theveloper.pixelplay.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.theveloper.pixelplay.data.database.EngagementDao
import com.theveloper.pixelplay.data.database.SongEngagementEntity
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.data.model.HomeMixSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.util.concurrent.TimeUnit
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyMixManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engagementDao: EngagementDao
) {

    private val gson = Gson()
    private val legacyScoresFile = File(context.filesDir, "song_scores.json")
    private val fileLock = Any()
    private val statsType = object : TypeToken<MutableMap<String, SongEngagementStats>>() {}.type

    // P2-2: Async migration scope — migration runs on IO without blocking the main thread.
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Deferred result: null if no migration needed, completes when done.
    private val migrationDeferred: Deferred<Unit>

    // Flag to track if we've migrated legacy data
    private var legacyMigrationComplete = false

    data class SongEngagementStats(
        val playCount: Int = 0,
        val totalPlayDurationMs: Long = 0L,
        val lastPlayedTimestamp: Long = 0L
    )

    init {
        // P2-2: Launch migration asynchronously — does NOT block the calling thread.
        // Any method that needs migrated data should call migrationDeferred.await() first.
        migrationDeferred = managerScope.async {
            migrateLegacyDataIfNeeded()
        }
    }

    /**
     * Migrates engagements from legacy JSON file to Room database.
     * This runs once on startup if the legacy file exists.
     * P2-2: This is now a suspend fun running on an IO coroutine, so no runBlocking needed.
     * The synchronized block only guards the file read (no suspension points inside).
     */
    private suspend fun migrateLegacyDataIfNeeded() {
        if (legacyMigrationComplete || !legacyScoresFile.exists()) {
            legacyMigrationComplete = true
            return
        }

        // Read legacy data within the lock (only file I/O —  no suspend calls allowed inside synchronized)
        val entitiesToInsert: List<SongEngagementEntity>? = synchronized(fileLock) {
            if (legacyMigrationComplete) return@synchronized null

            try {
                val legacyData = readLegacyEngagementsLocked()
                if (legacyData.isNotEmpty()) {
                    legacyData.map { (songId, stats) ->
                        SongEngagementEntity(
                            songId = songId,
                            playCount = stats.playCount.coerceAtLeast(0),
                            totalPlayDurationMs = stats.totalPlayDurationMs.coerceAtLeast(0L),
                            lastPlayedTimestamp = stats.lastPlayedTimestamp.coerceAtLeast(0L)
                        )
                    }
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read legacy engagement data", e)
                null
            }
        }

        // Perform the suspend DB call outside the synchronized block
        if (entitiesToInsert != null) {
            try {
                engagementDao.upsertEngagements(entitiesToInsert)
                Log.i(TAG, "Migrated ${entitiesToInsert.size} engagement records from JSON to Room")

                // Rename legacy file as backup instead of deleting
                val backupFile = File(context.filesDir, "song_scores.json.bak")
                legacyScoresFile.renameTo(backupFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert legacy engagement data into Room", e)
            }
        }

        legacyMigrationComplete = true
    }

    /**
     * Reads engagements from Room database.
     * P2-2: Awaits migration completion before querying, ensuring data consistency
     * without blocking any thread at startup.
     */
    private suspend fun readEngagements(): Map<String, SongEngagementStats> {
        migrationDeferred.await() // Only waits if migration is still running
        return engagementDao.getAllEngagements().associate { entity ->
            entity.songId to SongEngagementStats(
                playCount = entity.playCount,
                totalPlayDurationMs = entity.totalPlayDurationMs,
                lastPlayedTimestamp = entity.lastPlayedTimestamp
            )
        }
    }

    /**
     * Legacy method to read from JSON file during migration.
     */
    private fun readLegacyEngagementsLocked(): MutableMap<String, SongEngagementStats> {
        if (!legacyScoresFile.exists()) {
            return mutableMapOf()
        }

        val raw = runCatching { legacyScoresFile.readText() }
            .onFailure { Log.e(TAG, "Failed to read legacy song scores file", it) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return mutableMapOf()

        return runCatching {
            val element = gson.fromJson(raw, JsonElement::class.java)
            parseEngagementElement(element)
        }.getOrElse { throwable ->
            Log.e(TAG, "Failed to parse legacy song scores file", throwable)
            mutableMapOf()
        }
    }

    private fun parseEngagementElement(element: JsonElement?): MutableMap<String, SongEngagementStats> {
        if (element == null || element.isJsonNull) {
            return mutableMapOf()
        }

        if (element.isJsonObject) {
            return parseEngagementObject(element.asJsonObject)
        }

        return runCatching {
            val parsed: MutableMap<String, SongEngagementStats> = gson.fromJson(element, statsType)
            parsed.mapValuesTo(mutableMapOf()) { (_, stats) -> sanitizeStats(stats) }
        }.getOrElse {
            Log.w(TAG, "Unsupported song engagement format, ignoring it")
            mutableMapOf()
        }
    }

    private fun parseEngagementObject(obj: JsonObject): MutableMap<String, SongEngagementStats> {
        val result = mutableMapOf<String, SongEngagementStats>()
        for ((key, value) in obj.entrySet()) {
            val stats = parseStatsValue(key, value)
            if (stats != null) {
                result[key] = stats
            } else {
                Log.w(TAG, "Skipping song engagement entry for \"$key\" because it could not be parsed: $value")
            }
        }
        return result
    }

    private fun parseStatsValue(key: String, value: JsonElement): SongEngagementStats? {
        if (value.isJsonObject) {
            val parsedStats = runCatching {
                gson.fromJson(value, SongEngagementStats::class.java)
            }.getOrNull()

            if (parsedStats != null) {
                return sanitizeStats(parsedStats)
            }

            val extracted = extractScore(value)
            if (extracted != null) {
                return SongEngagementStats(playCount = extracted)
            }
        } else {
            val extracted = extractScore(value)
            if (extracted != null) {
                return SongEngagementStats(playCount = extracted)
            }
        }

        Log.w(TAG, "Encountered unsupported engagement value for \"$key\": $value")
        return null
    }

    private fun extractScore(value: JsonElement): Int? {
        if (value.isJsonPrimitive) {
            val primitive = value.asJsonPrimitive
            return when {
                primitive.isNumber -> primitive.asNumber.toInt()
                primitive.isString -> primitive.asString.toIntOrNull()
                else -> null
            }
        }

        if (value.isJsonObject) {
            val obj = value.asJsonObject
            for (key in SCORE_KEY_CANDIDATES) {
                val candidate = obj.get(key)
                if (candidate != null && candidate.isJsonPrimitive) {
                    val primitive = candidate.asJsonPrimitive
                    val parsed = when {
                        primitive.isNumber -> primitive.asNumber.toInt()
                        primitive.isString -> primitive.asString.toIntOrNull()
                        else -> null
                    }
                    if (parsed != null) {
                        return parsed
                    }
                }
            }
        }

        return null
    }

    private fun sanitizeStats(stats: SongEngagementStats): SongEngagementStats {
        return stats.copy(
            playCount = stats.playCount.coerceAtLeast(0),
            totalPlayDurationMs = stats.totalPlayDurationMs.coerceAtLeast(0L),
            lastPlayedTimestamp = stats.lastPlayedTimestamp.coerceAtLeast(0L)
        )
    }

    /**
     * Records a song play using Room's atomic upsert operation.
     * More efficient than JSON read-modify-write.
     */
    suspend fun recordPlay(
        songId: String,
        songDurationMs: Long = 0L,
        timestamp: Long = System.currentTimeMillis()
    ) {
        engagementDao.recordPlay(
            songId = songId,
            durationMs = songDurationMs.coerceAtLeast(0L),
            timestamp = timestamp.coerceAtLeast(0L)
        )
    }

    suspend fun incrementScore(songId: String) {
        recordPlay(songId)
    }

    suspend fun getScore(songId: String): Int {
        return engagementDao.getPlayCount(songId) ?: 0
    }

    suspend fun getEngagementStats(songId: String): SongEngagementStats? {
        return engagementDao.getEngagement(songId)?.let { entity ->
            SongEngagementStats(
                playCount = entity.playCount,
                totalPlayDurationMs = entity.totalPlayDurationMs,
                lastPlayedTimestamp = entity.lastPlayedTimestamp
            )
        }
    }

    suspend fun getAllEngagementStats(): Map<String, SongEngagementStats> {
        return readEngagements()
    }

    /** Builds one discovery-led daily mix and three deterministic rotating mixes. */
    suspend fun generateHomeMixSnapshots(
        allSongs: List<Song>,
        favoriteSongIds: Set<String>,
        localDate: LocalDate,
        recentSnapshots: List<HomeMixSnapshot>,
        limit: Int = 24,
        variantSeed: Long = 0L,
        filterKey: String = "ALL",
        catalogFingerprint: String = "",
        zoneId: ZoneId = ZoneId.systemDefault(),
        dailyExcludedSongIds: Set<String> = emptySet()
    ): List<HomeMixSnapshot> {
        if (allSongs.isEmpty() || limit <= 0) return emptyList()
        val dailyCandidates = allSongs.filterNot { it.id in dailyExcludedSongIds }.ifEmpty { allSongs }
        val dailySize = minOf(dailySizeForCatalog(allSongs.size), dailyCandidates.size, limit)
        val now = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        val engagements = readEngagements()
        val songsById = allSongs.associateBy(Song::id)
        val artistAffinity = mutableMapOf<Long, Double>()
        val genreAffinity = mutableMapOf<String, Double>()
        engagements.forEach { (id, stats) ->
            val song = songsById[id] ?: return@forEach
            val weight = stats.playCount.coerceAtLeast(0).toDouble() + stats.totalPlayDurationMs.coerceAtLeast(0L) / 60_000.0
            if (weight <= 0.0) return@forEach
            artistIds(song).forEach { artistAffinity.merge(it.id, weight, Double::plus) }
            normalizeGenreKey(song.genre)?.let { genreAffinity.merge(it, weight, Double::plus) }
        }
        favoriteSongIds.forEach { id -> songsById[id]?.let { song ->
            artistIds(song).forEach { artistAffinity.merge(it.id, 2.0, Double::plus) }
        } }
        val seed = localDate.toEpochDay() xor variantSeed
        val recentSnapshots = recentSnapshots.filter { snapshot ->
            runCatching { LocalDate.parse(snapshot.localDate) }.getOrNull()?.let { date ->
                !date.isBefore(localDate.minusDays(6)) && date.isBefore(localDate)
            } == true
        }
        val recentIds = recentSnapshots.flatMapTo(mutableSetOf()) { it.songIds }
        val recentForFilter = recentSnapshots.filter { it.filterKey == filterKey }
        val dailySongs = selectDaily(
            songs = dailyCandidates,
            limit = dailySize,
            engagements = engagements,
            favorites = favoriteSongIds,
            artistAffinity = artistAffinity,
            genreAffinity = genreAffinity,
            recentIds = recentIds,
            seed = seed,
            nowMillis = now
        )
        val dailyCover = chooseCoverSong(dailySongs, emptySet())
        val highlightedAlbums = mutableSetOf<Long>()
        dailyCover?.let { highlightedAlbums.add(it.albumId) }
        val results = mutableListOf(
            HomeMixSnapshot(
                id = "daily", kind = HomeMixKind.DAILY, localDate = localDate.toString(),
                songIds = dailySongs.map(Song::id),
                isPersonalized = engagements.isNotEmpty() || favoriteSongIds.isNotEmpty(),
                slot = -1, filterKey = filterKey, catalogFingerprint = catalogFingerprint,
                rotationSlotsGenerated = true,
                coverSongId = dailyCover?.id
            )
        )

        val rotations = mutableListOf<Rotation>()
        val cutoff21 = now - TimeUnit.DAYS.toMillis(21)
        val cutoff60 = now - TimeUnit.DAYS.toMillis(60)
        val underplayed = allSongs.filter { song ->
            val addedAt = addedTimestampMillis(song.dateAdded)
            (engagements[song.id]?.playCount ?: 0) <= 2 && addedAt in 1L until cutoff21
        }
        addRotation(rotations, HomeMixKind.UNDERPLAYED, null, underplayed, recentForFilter)

        val rediscover = allSongs.filter { song ->
            val stats = engagements[song.id]
            val hasHistoryOrFavorite = (stats?.playCount ?: 0) >= 3 || song.id in favoriteSongIds
            hasHistoryOrFavorite && (stats?.lastPlayedTimestamp ?: 0L) in 1L..cutoff60
        }.sortedWith(compareBy<Song> { engagements[it.id]?.lastPlayedTimestamp ?: Long.MAX_VALUE }
            .thenBy { stableHash(seed, it.id) })
        addRotation(rotations, HomeMixKind.REDISCOVER, null, rediscover, recentForFilter)

        val artistOptions = allSongs.asSequence()
            .filter { (engagements[it.id]?.playCount ?: 0) <= 2 }
            .flatMap { song -> artistIds(song).map { it to song } }
            .filter { (artist, _) -> artist.id in artistAffinity }
            .groupBy { (artist, _) -> artist.id }
        artistOptions.forEach { (id, entries) ->
            if (entries.size >= ROTATING_MIN_SIZE) {
                val subject = entries.first().first.name
                val artistSongs = entries.map { it.second }
                addRotation(rotations, HomeMixKind.ARTIST, subject,
                    rankSubset(artistSongs, artistAffinity, genreAffinity, favoriteSongIds, seed + id), recentForFilter)
            }
        }

        allSongs.groupBy { normalizeGenreKey(it.genre) }.forEach { (genre, genreSongs) ->
            if (genre != null && genreSongs.size >= ROTATING_MIN_SIZE) {
                val subject = genreSongs.first().genre?.trim()
                addRotation(rotations, HomeMixKind.GENRE, subject,
                    rankSubset(genreSongs, artistAffinity, genreAffinity, favoriteSongIds, seed + genre.hashCode()), recentForFilter)
            }
        }

        val recentAdded = allSongs.filter { song -> addedTimestampMillis(song.dateAdded) in cutoff21..now }
            .sortedWith(compareByDescending<Song> { addedTimestampMillis(it.dateAdded) }
                .thenBy { engagements[it.id]?.playCount ?: 0 }.thenBy { stableHash(seed, it.id) })
        addRotation(rotations, HomeMixKind.RECENTLY_ADDED, null, recentAdded, recentForFilter)

        val dailyIds = dailySongs.mapTo(mutableSetOf(), Song::id)
        val selectedRotationIds = mutableSetOf<String>()
        val selectedKinds = mutableSetOf<HomeMixKind>()
        val orderedRotations = rotations.sortedWith(
            compareBy<Rotation> { it.priority }.thenBy { stableHash(seed, it.kind.name + it.subject) }
        )
        for (rotation in orderedRotations) {
            if (selectedKinds.size >= ROTATING_COUNT) break
            if (rotation.kind in selectedKinds) continue
            val available = rotation.songs.filterNot { it.id in selectedRotationIds }.distinctBy(Song::id)
            val freshCount = available.count { it.id !in dailyIds }
            val overlapCount = available.size - freshCount
            var targetSize = minOf(ROTATING_TARGET_SIZE, available.size)
            while (targetSize >= ROTATING_MIN_SIZE &&
                freshCount + minOf(overlapCount, targetSize / 4) < targetSize) {
                targetSize--
            }
            if (targetSize < ROTATING_MIN_SIZE) continue
            val selected = selectRotation(
                candidates = available,
                targetSize = targetSize,
                dailyIds = dailyIds,
                recentIds = recentIds,
                seed = seed + results.size
            )
            if (selected.size < ROTATING_MIN_SIZE) continue
            val slot = results.size - 1
            selectedKinds += rotation.kind
            selectedRotationIds.addAll(selected.map(Song::id))
            val cover = chooseCoverSong(selected, highlightedAlbums)
            cover?.let { highlightedAlbums.add(it.albumId) }
            results += HomeMixSnapshot(
                id = rotation.kind.name.lowercase() + rotation.subject?.let { "-" + slug(it) }.orEmpty(),
                kind = rotation.kind, subject = rotation.subject, localDate = localDate.toString(),
                songIds = selected.map(Song::id), isPersonalized = engagements.isNotEmpty() || favoriteSongIds.isNotEmpty(),
                slot = slot, filterKey = filterKey, catalogFingerprint = catalogFingerprint,
                coverSongId = cover?.id
            )
        }
        return results
    }

    private data class Rotation(
        val kind: HomeMixKind,
        val subject: String?,
        val songs: List<Song>,
        val priority: Long
    )

    private fun dailySizeForCatalog(size: Int): Int = when {
        size <= 0 -> 0
        size <= 8 -> size
        size <= 20 -> maxOf(6, (size * 0.6).toInt())
        size <= 40 -> 12
        size <= 80 -> 18
        else -> 24
    }

    private fun artistIds(song: Song) = (song.artists + song.primaryArtist).distinctBy { it.id }
        .filter { it.id >= 0L && it.name.isNotBlank() && !it.name.equals("<unknown>", ignoreCase = true) }

    private fun rankSubset(
        songs: List<Song>, artistAffinity: Map<Long, Double>, genreAffinity: Map<String, Double>,
        favorites: Set<String>, seed: Long
    ): List<Song> = songs.sortedWith(
        compareByDescending<Song> {
            (artistIds(it).sumOf { artist -> artistAffinity[artist.id] ?: 0.0 }) +
                (normalizeGenreKey(it.genre)?.let { genreAffinity[it] ?: 0.0 } ?: 0.0) +
                if (it.id in favorites) 2.0 else 0.0
        }.thenBy { stableHash(seed, it.id) }
    )

    private fun selectDaily(
        songs: List<Song>, limit: Int, engagements: Map<String, SongEngagementStats>, favorites: Set<String>,
        artistAffinity: Map<Long, Double>, genreAffinity: Map<String, Double>, recentIds: Set<String>, seed: Long,
        nowMillis: Long
    ): List<Song> {
        val score: (Song) -> Double = { song ->
            val stats = engagements[song.id]
            val plays = stats?.playCount?.coerceAtLeast(0) ?: 0
            val affinity = artistIds(song).sumOf { artistAffinity[it.id] ?: 0.0 } +
                (normalizeGenreKey(song.genre)?.let { genreAffinity[it] ?: 0.0 } ?: 0.0) +
                if (song.id in favorites) 2.0 else 0.0
            val recency = stats?.lastPlayedTimestamp?.takeIf { it > 0L }
                ?.let { ((nowMillis - it).coerceAtLeast(0L) / TimeUnit.DAYS.toMillis(1)).toDouble() } ?: 365.0
            (1.0 / (1.0 + plays)) * 0.5 + (recency / (recency + 60.0)) * 0.15 +
                (affinity / (affinity + 20.0)) * 0.35
        }
        val order = songs.sortedWith(compareByDescending(score).thenBy { stableHash(seed, it.id) })
        val underplayed = order.filter { (engagements[it.id]?.playCount ?: 0) <= 2 }
        val familiar = order.filter { (engagements[it.id]?.playCount ?: 0) > 2 }
        val knownTarget = limit / 3
        val result = mutableListOf<Song>()
        val used = mutableSetOf<String>()
        val selectedArtists = mutableMapOf<Long, Int>()
        val selectedAlbums = mutableMapOf<Long, Int>()
        fun append(candidates: List<Song>, target: Int) {
            val startSize = result.size
            val ordered = candidates.withIndex().sortedBy { (index, song) ->
                index.toDouble() + (if (song.id in recentIds) 40.0 else 0.0) -
                    ((stableHash(seed, song.id) % 10_000L) / 10_000.0) * 4.0
            }.map { it.value }
            fun addRespectingDiversity() {
                for (song in ordered) {
                    if (result.size - startSize >= target || result.size >= limit || song.id in used) continue
                    val ids = artistIds(song).map { it.id }
                    if (ids.any { (selectedArtists[it] ?: 0) >= 2 } ||
                        (song.albumId >= 0 && (selectedAlbums[song.albumId] ?: 0) >= 2)) continue
                    result += song
                    used += song.id
                    ids.forEach { selectedArtists[it] = (selectedArtists[it] ?: 0) + 1 }
                    if (song.albumId >= 0) selectedAlbums[song.albumId] = (selectedAlbums[song.albumId] ?: 0) + 1
                }
            }
            addRespectingDiversity()
            if (result.size - startSize < target) {
                ordered.forEach { song ->
                    if (result.size - startSize >= target || result.size >= limit) return@forEach
                    if (used.add(song.id)) result += song
                }
            }
        }
        append(underplayed, limit - knownTarget)
        append(familiar, knownTarget)
        append(order, limit - result.size)
        return result.take(limit)
    }

    private fun addRotation(
        target: MutableList<Rotation>, kind: HomeMixKind, subject: String?, songs: List<Song>,
        recentSnapshots: List<HomeMixSnapshot>
    ) {
        if (songs.size < ROTATING_MIN_SIZE) return
        target += Rotation(kind, subject, songs, lastExposureDay(recentSnapshots, kind, subject))
    }

    private fun lastExposureDay(snapshots: List<HomeMixSnapshot>, kind: HomeMixKind, subject: String?): Long =
        snapshots.asSequence()
            .filter { it.kind == kind && it.subject == subject }
            .mapNotNull { runCatching { LocalDate.parse(it.localDate).toEpochDay() }.getOrNull() }
            .maxOrNull() ?: Long.MIN_VALUE

    private fun selectRotation(
        candidates: List<Song>, targetSize: Int, dailyIds: Set<String>,
        recentIds: Set<String>, seed: Long
    ): List<Song> {
        val targetDailyOverlap = targetSize / 4
        val ordered = candidates.distinctBy(Song::id).withIndex().sortedBy { (index, song) ->
            index.toDouble() + (if (song.id in recentIds) 20.0 else 0.0) -
                ((stableHash(seed, song.id) % 10_000L) / 10_000.0) * 2.0
        }.map { it.value }
        val selected = mutableListOf<Song>()
        val artists = mutableMapOf<Long, Int>()
        val albums = mutableMapOf<Long, Int>()
        var dailyOverlap = 0
        fun pick(respectArtistAlbumCaps: Boolean) {
            for (song in ordered) {
                if (selected.size >= targetSize || song in selected) continue
                val overlapsDaily = song.id in dailyIds
                if (overlapsDaily && dailyOverlap >= targetDailyOverlap) continue
                val artistKeys = artistIds(song).map { it.id }
                if (respectArtistAlbumCaps && (artistKeys.any { (artists[it] ?: 0) >= 2 } ||
                        (song.albumId >= 0 && (albums[song.albumId] ?: 0) >= 2))) continue
                selected += song
                if (overlapsDaily) dailyOverlap++
                artistKeys.forEach { artists[it] = (artists[it] ?: 0) + 1 }
                if (song.albumId >= 0) albums[song.albumId] = (albums[song.albumId] ?: 0) + 1
            }
        }
        pick(respectArtistAlbumCaps = true)
        if (selected.size < targetSize) pick(respectArtistAlbumCaps = false)
        return selected
    }

    private fun chooseCoverSong(songs: List<Song>, highlightedAlbums: Set<Long>): Song? =
        songs.firstOrNull { !it.albumArtUriString.isNullOrBlank() && it.albumId !in highlightedAlbums }
            ?: songs.firstOrNull { !it.albumArtUriString.isNullOrBlank() }

    private fun addedTimestampMillis(dateAdded: Long): Long = when {
        dateAdded <= 0L -> 0L
        dateAdded < 10_000_000_000L -> TimeUnit.SECONDS.toMillis(dateAdded)
        else -> dateAdded
    }

    private fun stableHash(seed: Long, value: String): Long =
        ("$seed:$value").hashCode().toLong().and(0xffffffffL)

    private fun slug(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(32)

    private fun normalizeGenreKey(rawGenre: String?): String? {
        val normalized = rawGenre
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return if (normalized.contains("unknown")) {
            null
        } else {
            normalized
        }
    }

    companion object {
        private const val TAG = "DailyMixManager"
        private const val ROTATING_COUNT = 3
        private const val ROTATING_MIN_SIZE = 5
        private const val ROTATING_TARGET_SIZE = 8
        private val SCORE_KEY_CANDIDATES = listOf("score", "count", "value")
    }
}
