package com.theveloper.pixelplay.presentation.viewmodel

import com.theveloper.pixelplay.data.DailyMixManager
import com.theveloper.pixelplay.data.model.HomeMix
import com.theveloper.pixelplay.data.model.HomeMixFeed
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.data.model.HomeMixSnapshot
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.StorageFilter
import com.theveloper.pixelplay.data.database.isProviderSong
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import com.theveloper.pixelplay.data.repository.MusicRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class DailyMixStateHolder @Inject constructor(
    private val dailyMixManager: DailyMixManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val musicRepository: MusicRepository
) {
    private var scope: CoroutineScope? = null
    private var refreshJob: Job? = null
    private val generation = AtomicLong(0L)
    private val _homeMixFeed = MutableStateFlow<HomeMixFeed?>(null)
    val homeMixFeed: StateFlow<HomeMixFeed?> = _homeMixFeed.asStateFlow()

    fun initialize(coroutineScope: CoroutineScope) {
        scope = coroutineScope
    }

    fun refreshHomeMixes(favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>, dailyOnly: Boolean = false) {
        val currentScope = scope ?: return
        val request = generation.incrementAndGet()
        refreshJob?.cancel()
        refreshJob = currentScope.launch {
            try {
                val zoneId = ZoneId.systemDefault()
                val today = LocalDate.now(zoneId)
                val stored = withContext(Dispatchers.IO) { userPreferencesRepository.homeMixSnapshotsFlow.first() }
                val visible = withContext(Dispatchers.IO) { loadVisibleSongs() }
                if (request != generation.get()) return@launch
                val allSongs = visible.songs
                if (allSongs.isEmpty()) {
                    publishFeed(HomeMixFeed(daily = null, rotating = persistentListOf()))
                    return@launch
                }
                val todaySnapshots = stored.filter {
                    it.localDate == today.toString() && it.filterKey == visible.filterKey
                }
                val dailyCached = todaySnapshots.firstOrNull { it.id == DAILY_ID }
                val rotationsCached = todaySnapshots.filter { it.id != DAILY_ID }.sortedBy(HomeMixSnapshot::slot)
                val catalogFingerprint = visible.catalogFingerprint
                val validIds = withContext(Dispatchers.Default) { allSongs.mapTo(HashSet(allSongs.size), Song::id) }
                val snapshotsMatchCatalog = todaySnapshots.all {
                    it.filterKey == visible.filterKey && it.catalogFingerprint == catalogFingerprint
                }
                val canReuseDaily = dailyCached != null && dailyCached.rotationSlotsGenerated &&
                    dailyCached.songIds.all(validIds::contains)
                val canReuseRotations = rotationsCached.size <= ROTATING_COUNT &&
                    rotationsCached.all { snapshot -> snapshot.songIds.all(validIds::contains) }

                suspend fun generate(variantSeed: Long = 0L, excluded: Set<String> = emptySet()) = withContext(Dispatchers.Default) {
                    dailyMixManager.generateHomeMixSnapshots(
                        allSongs, favoriteSongIdsFlow.first(), today, stored, DAILY_LIMIT,
                        variantSeed = variantSeed,
                        filterKey = visible.filterKey,
                        catalogFingerprint = catalogFingerprint,
                        zoneId = zoneId,
                        dailyExcludedSongIds = excluded
                    )
                }

                val snapshots = when {
                    dailyOnly && dailyCached != null && canReuseRotations -> {
                        val rotatingIds = rotationsCached.flatMapTo(mutableSetOf()) { it.songIds }
                        val regenerated = generate(variantSeed = System.currentTimeMillis(), excluded = rotatingIds)
                            .firstOrNull { it.id == DAILY_ID }
                        listOfNotNull(regenerated) + rotationsCached.map { rotation ->
                            rotation.copy(
                                songIds = rotation.songIds.filter(validIds::contains),
                                filterKey = visible.filterKey,
                                catalogFingerprint = catalogFingerprint
                            )
                        }
                    }
                    !dailyOnly && snapshotsMatchCatalog && canReuseDaily && canReuseRotations -> todaySnapshots
                    else -> generate()
                }
                if (request != generation.get()) return@launch

                val retained = stored.filter { snapshot ->
                    runCatching { LocalDate.parse(snapshot.localDate) }.getOrNull()?.let { date ->
                        !date.isBefore(today.minusDays(EXPOSURE_DAYS - 1)) && !date.isAfter(today) &&
                            !(date == today && snapshot.filterKey == visible.filterKey)
                    } == true
                }
                val completeSnapshots = (retained + snapshots)
                    .distinctBy { Triple(it.localDate, it.filterKey, it.id) }
                    .sortedWith(compareBy<HomeMixSnapshot> { it.localDate }.thenBy { it.filterKey }.thenBy { it.slot })
                if (completeSnapshots != stored) {
                    withContext(Dispatchers.IO) { userPreferencesRepository.saveHomeMixSnapshots(completeSnapshots) }
                }
                if (request != generation.get()) return@launch
                val feed = withContext(Dispatchers.Default) { snapshots.sortedBy(HomeMixSnapshot::slot).toFeed(allSongs) }
                if (request == generation.get()) publishFeed(feed)
            } catch (throwable: Exception) {
                if (throwable is kotlinx.coroutines.CancellationException) throw throwable
                Timber.e(throwable, "Failed to refresh home mix recommendations")
                if (request == generation.get()) publishFeed(HomeMixFeed(daily = null, rotating = persistentListOf()))
            }
        }
    }

    fun forceUpdate(favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>) =
        refreshHomeMixes(favoriteSongIdsFlow, dailyOnly = true)

    fun onCleared() {
        generation.incrementAndGet()
        refreshJob?.cancel()
        scope = null
    }

    private fun publishFeed(feed: HomeMixFeed) {
        _homeMixFeed.value = feed
    }

    private data class VisibleCatalog(val songs: List<Song>, val filterKey: String, val catalogFingerprint: String)

    private suspend fun loadVisibleSongs(): VisibleCatalog {
        val all = musicRepository.getAllSongsOnce()
        val effectiveFilter = userPreferencesRepository.effectiveStorageFilterFlow.first()
        val songs = all.filter { song ->
            val online = song.isProviderSong()
            song.contentUriString.isNotBlank() && when (effectiveFilter) {
                StorageFilter.ALL -> true
                StorageFilter.ONLINE -> online
                StorageFilter.OFFLINE -> !online
            }
        }
        val filterKey = effectiveFilter.name
        val catalogDescription = songs.sortedBy(Song::id).joinToString("\n") { song ->
            listOf(song.id, song.artistId, song.albumId, song.genre.orEmpty(), song.dateAdded, song.contentUriString).joinToString("|")
        }
        val fingerprint = fingerprint(catalogDescription)
        return VisibleCatalog(songs, filterKey, fingerprint)
    }

    private fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun List<HomeMixSnapshot>.toFeed(allSongs: List<Song>): HomeMixFeed {
        val songsById = allSongs.associateBy(Song::id)
        fun HomeMixSnapshot.toHomeMix(): HomeMix? {
            val mixSongs = songIds.mapNotNull(songsById::get).toImmutableList()
            if (mixSongs.isEmpty()) return null
            val cover = coverSongId?.let(songsById::get)
                ?: mixSongs.firstOrNull { !it.albumArtUriString.isNullOrBlank() }
                ?: mixSongs.firstOrNull()
            return HomeMix(id, kind, subject, mixSongs, cover, isPersonalized)
        }
        val daily = firstOrNull { it.id == DAILY_ID }?.toHomeMix()
        val rotations = filter { it.id != DAILY_ID }.sortedBy(HomeMixSnapshot::slot)
            .mapNotNull { it.toHomeMix() }.toImmutableList()
        return HomeMixFeed(daily, rotations)
    }

    companion object {
        private const val DAILY_ID = "daily"
        private const val DAILY_LIMIT = 24
        private const val ROTATING_COUNT = 3
        private const val EXPOSURE_DAYS = 7L
    }
}
