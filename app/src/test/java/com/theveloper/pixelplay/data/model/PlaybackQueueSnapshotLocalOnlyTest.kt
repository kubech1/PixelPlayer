package com.theveloper.pixelplay.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueSnapshotLocalOnlyTest {
    @Test
    fun preservesSelectedLocalOccurrenceAndPositionAfterFiltering() {
        val local = item("7")
        val snapshot = PlaybackQueueSnapshot(
            items = listOf(local, item("navidrome_42", provider = true), local),
            currentMediaId = local.mediaId,
            currentIndex = 2,
            currentPositionMs = 730L,
            playWhenReady = true,
        )

        val filtered = snapshot.localOnly(emptySet())

        assertEquals(listOf(local, local), filtered.items)
        assertEquals(1, filtered.currentIndex)
        assertEquals(730L, filtered.currentPositionMs)
        assertTrue(filtered.playWhenReady)
    }

    @Test
    fun cachedProviderSelectionRestoresNextLocalTrackPaused() {
        val first = item("3")
        val next = item("9")
        val snapshot = PlaybackQueueSnapshot(
            items = listOf(first, item("81"), next),
            currentMediaId = "81",
            currentIndex = 1,
            currentPositionMs = 1_200L,
            playWhenReady = true,
        )

        val filtered = snapshot.localOnly(setOf("81"))

        assertEquals(listOf(first, next), filtered.items)
        assertEquals(next.mediaId, filtered.currentMediaId)
        assertEquals(1, filtered.currentIndex)
        assertEquals(0L, filtered.currentPositionMs)
        assertFalse(filtered.playWhenReady)
    }

    @Test
    fun providerOnlyQueueRestoresNothing() {
        val snapshot = PlaybackQueueSnapshot(
            items = listOf(item("31", provider = true), item("-10042_18")),
            currentMediaId = "31",
            playWhenReady = true,
        )

        val filtered = snapshot.localOnly(emptySet())

        assertTrue(filtered.items.isEmpty())
        assertNull(filtered.currentMediaId)
        assertFalse(filtered.playWhenReady)
    }

    private fun item(id: String, provider: Boolean? = null) =
        PlaybackQueueItemSnapshot(id, "file:///audio/$id.mp3", providerMusic = provider)
}
