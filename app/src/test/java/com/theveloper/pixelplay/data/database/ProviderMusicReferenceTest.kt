package com.theveloper.pixelplay.data.database

import com.theveloper.pixelplay.data.model.HomeMix
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.localOnly
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProviderMusicReferenceTest {
    @Test
    fun `provider origin survives a cached file URI`() {
        assertTrue(isProviderMusicReference("navidrome_42", "file:///cache/track.flac"))
        assertTrue(isProviderMusicReference("-10042_18", "file:///cache/track.mp3"))
        assertTrue(isProviderMusicReference("12", "https://example.test/audio"))
        assertTrue(Song.emptySong().copy(
            id = "12",
            contentUriString = "file:///cache/track.mp3",
            telegramFileId = 18,
        ).isProviderSong())
        assertFalse(isProviderMusicReference("12", "content://media/external/audio/media/12"))
        assertFalse(isProviderMusicReference("external:fixture", "file:///Music/track.flac"))
    }

    @Test
    fun `local projection retains order and replaces a provider cover`() {
        val cloud = Song.emptySong().copy(id = "jellyfin_8", contentUriString = "file:///cache/track.flac")
        val local = Song.emptySong().copy(id = "11", contentUriString = "content://media/11")
        val localWithArt = local.copy(id = "12", albumArtUriString = "content://art/12")
        val mix = HomeMix(
            id = "daily",
            kind = HomeMixKind.DAILY,
            songs = persistentListOf(local, cloud, localWithArt),
            cover = cloud,
        )

        val visible = requireNotNull(mix.localOnly())
        assertEquals(listOf(local.id, localWithArt.id), visible.songs.map(Song::id))
        assertEquals(localWithArt.id, visible.cover?.id)
        assertNull(mix.copy(songs = persistentListOf(cloud)).localOnly())
    }
}
