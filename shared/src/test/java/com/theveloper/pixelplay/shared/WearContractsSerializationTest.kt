package com.theveloper.pixelplay.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearContractsSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun `WearPlayerState round-trip preserves fields and defaults`() {
        val original = WearPlayerState(
            songId = "song_42",
            songTitle = "Midnight City",
            artistName = "M83",
            albumName = "Hurry Up, We're Dreaming",
            isPlaying = true,
            currentPositionMs = 12_345L,
            totalDurationMs = 243_000L,
            isFavorite = true,
            repeatMode = 2,
            queueRevision = "rev-7",
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WearPlayerState>(encoded)

        assertEquals(original, decoded)
        assertFalse(decoded.isEmpty)

        val emptyDecoded = json.decodeFromString<WearPlayerState>("{}")
        assertTrue(emptyDecoded.isEmpty)
        assertNull(emptyDecoded.themePalette)
        assertNull(emptyDecoded.lyrics)
    }

    @Test
    fun `WearPlaybackCommand ignores unknown fields from newer peers`() {
        val raw = """{"action":"play_item","songId":"abc","futureField":123}"""
        val decoded = json.decodeFromString<WearPlaybackCommand>(raw)

        assertEquals(WearPlaybackCommand.PLAY_ITEM, decoded.action)
        assertEquals("abc", decoded.songId)
        assertNull(decoded.requestId)
        assertNull(decoded.queueIndex)
    }

    @Test
    fun `WearVolumeState and WearVolumeCommand round-trip`() {
        val state = WearVolumeState(level = 7, max = 15, routeType = WearVolumeState.ROUTE_TYPE_BLUETOOTH, routeName = "Buds")
        val stateDecoded = json.decodeFromString<WearVolumeState>(json.encodeToString(state))
        assertEquals(state, stateDecoded)

        val cmd = WearVolumeCommand(direction = WearVolumeCommand.UP, value = 8)
        val cmdDecoded = json.decodeFromString<WearVolumeCommand>(json.encodeToString(cmd))
        assertEquals(cmd, cmdDecoded)
    }
}
