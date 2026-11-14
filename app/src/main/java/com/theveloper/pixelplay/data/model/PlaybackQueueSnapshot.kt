package com.theveloper.pixelplay.data.model

import com.theveloper.pixelplay.data.database.isProviderMusicReference
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackQueueItemSnapshot(
    val mediaId: String,
    val uri: String,
    val title: String? = null,
    val artist: String? = null,
    val albumTitle: String? = null,
    val artworkUri: String? = null,
    val durationMs: Long? = null,
    val providerMusic: Boolean? = null,
)

@Serializable
data class PlaybackQueueSnapshot(
    val items: List<PlaybackQueueItemSnapshot>,
    val currentMediaId: String? = null,
    val currentIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val playWhenReady: Boolean = false,
    val repeatMode: Int = 0,
    val shuffleEnabled: Boolean = false,
    val savedAtEpochMs: Long = System.currentTimeMillis(),
)

/** Retains the selected occurrence, even when a song ID appears more than once. */
internal fun PlaybackQueueSnapshot.localOnly(legacyProviderIds: Set<String>): PlaybackQueueSnapshot {
    val activeIndex = currentIndex.takeIf { index ->
        index in items.indices && (currentMediaId == null || items[index].mediaId == currentMediaId)
    } ?: items.indexOfFirst { it.mediaId == currentMediaId }.takeIf { it >= 0 }
        ?: currentIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))

    val local = items.withIndex().filterNot { (_, item) ->
        item.providerMusic == true || isProviderMusicReference(item.mediaId, item.uri) ||
            (item.providerMusic == null && item.mediaId in legacyProviderIds)
    }
    if (local.size == items.size) return this

    val activeLocalIndex = local.indexOfFirst { it.index == activeIndex }
    val nextIndex = activeLocalIndex.takeIf { it >= 0 }
        ?: local.indexOfFirst { it.index > activeIndex }.takeIf { it >= 0 }
        ?: local.lastIndex.coerceAtLeast(0)
    return copy(
        items = local.map { it.value },
        currentMediaId = local.getOrNull(nextIndex)?.value?.mediaId,
        currentIndex = nextIndex,
        currentPositionMs = if (activeLocalIndex >= 0) currentPositionMs else 0L,
        playWhenReady = activeLocalIndex >= 0 && playWhenReady,
    )
}
