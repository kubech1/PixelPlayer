package com.theveloper.pixelplay.data.model

import com.theveloper.pixelplay.data.database.isProviderSong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

/** Home recommendation feed for the current local date. */
data class HomeMixFeed(
    val daily: HomeMix?,
    val rotating: ImmutableList<HomeMix>
)

data class HomeMix(
    val id: String,
    val kind: HomeMixKind,
    val subject: String? = null,
    val songs: ImmutableList<Song>,
    val cover: Song?,
    val isPersonalized: Boolean = false
)

internal fun HomeMix.localOnly(): HomeMix? {
    if (songs.none(Song::isProviderSong)) return this
    val localSongs = songs.filterNot(Song::isProviderSong).toImmutableList()
    if (localSongs.isEmpty()) return null
    val localCover = cover?.takeIf { candidate -> localSongs.any { it.id == candidate.id } }
        ?: localSongs.firstOrNull { !it.albumArtUriString.isNullOrBlank() }
        ?: localSongs.first()
    return copy(songs = localSongs, cover = localCover)
}

@Serializable
enum class HomeMixKind {
    DAILY,
    UNDERPLAYED,
    REDISCOVER,
    ARTIST,
    GENRE,
    RECENTLY_ADDED
}

/** Small persisted form; songs are rehydrated from the current library by their canonical IDs. */
@Serializable
data class HomeMixSnapshot(
    val id: String,
    val kind: HomeMixKind,
    val subject: String? = null,
    val localDate: String,
    val songIds: List<String>,
    val isPersonalized: Boolean = false,
    val slot: Int = 0,
    val filterKey: String = "ALL",
    val catalogFingerprint: String = "",
    val rotationSlotsGenerated: Boolean = true,
    val coverSongId: String? = null
)
