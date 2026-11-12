package com.theveloper.pixelplay.data

import android.content.Context
import com.theveloper.pixelplay.data.database.EngagementDao
import com.theveloper.pixelplay.data.database.SongEngagementEntity
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.data.model.HomeMixSnapshot
import com.theveloper.pixelplay.data.model.Song
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DailyMixManagerTest {
    @Test
    fun `home feed is stable discovery led and respects the rotation promises`() = runTest {
        val directory = Files.createTempDirectory("home-mix-test")
        try {
            val context = mockk<Context>()
            every { context.filesDir } returns directory.toFile()
            val engagements = mockk<EngagementDao>()
            val date = LocalDate.of(2026, 11, 11)
            val now = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
            val playedAt = now - TimeUnit.DAYS.toMillis(3)
            coEvery { engagements.getAllEngagements() } returns (1..24).map { id ->
                val lastPlayed = if (id <= 5) playedAt - TimeUnit.DAYS.toMillis(90) else playedAt
                val playCount = if (id == 6) 1 else 12
                val effectiveLastPlayed = if (id == 6) playedAt - TimeUnit.DAYS.toMillis(90) else lastPlayed
                SongEngagementEntity(id.toString(), playCount = playCount, totalPlayDurationMs = 20_000_000, lastPlayedTimestamp = effectiveLastPlayed)
            }
            val manager = DailyMixManager(context, engagements)
            val songs = (1..72).map { index ->
                Song.emptySong().copy(
                    id = index.toString(), title = "Song $index", artist = "Artist ${index % 6}", artistId = (index % 6).toLong(),
                    albumId = (index % 12).toLong(), album = "Album ${index % 12}",
                    genre = "Genre ${index % 3}", dateAdded = now - TimeUnit.DAYS.toMillis(index.toLong())
                )
            }
            val favorites = setOf("6")
            val initial = manager.generateHomeMixSnapshots(songs, favorites, date, emptyList(), zoneId = ZoneOffset.UTC)
            val sameDay = manager.generateHomeMixSnapshots(songs, favorites, date, emptyList(), zoneId = ZoneOffset.UTC)
            assertEquals(initial, sameDay)
            assertEquals(initial, Json.decodeFromString<List<HomeMixSnapshot>>(Json.encodeToString(initial)))
            assertEquals(4, initial.size)
            assertEquals(HomeMixKind.DAILY, initial.first().kind)
            val daily = initial.first()
            assertEquals(daily.songIds.size, daily.songIds.distinct().size)
            assertTrue(daily.songIds.size in 12..24)
            assertTrue(daily.songIds.count { it.toInt() in 1..5 || it.toInt() in 7..24 } >= daily.songIds.size / 3)
            assertTrue(daily.songIds.count { it.toInt() == 6 || it.toInt() > 24 } >= daily.songIds.size * 2 / 3)
            assertTrue(initial.drop(1).all { it.localDate == date.toString() && it.songIds.size in 5..10 })
            assertTrue(initial.drop(1).map { it.kind }.distinct().size == initial.drop(1).size)
            val priorMixIds = mutableSetOf<String>()
            initial.drop(1).forEach { mix ->
                assertTrue(mix.songIds.intersect(daily.songIds.toSet()).size <= mix.songIds.size / 4)
                assertTrue(mix.songIds.none { it in priorMixIds })
                priorMixIds += mix.songIds
                val tracks = mix.songIds.map { id -> songs.first { it.id == id } }
                when (mix.kind) {
                    HomeMixKind.UNDERPLAYED -> assertTrue(tracks.all { (it.id.toInt() > 24) })
                    HomeMixKind.REDISCOVER -> assertTrue(tracks.all { it.id.toInt() in 1..6 })
                    HomeMixKind.ARTIST -> assertTrue(tracks.all { it.artist == mix.subject })
                    HomeMixKind.GENRE -> assertTrue(tracks.all { it.genre == mix.subject })
                    HomeMixKind.RECENTLY_ADDED -> assertTrue(tracks.all { now - it.dateAdded <= TimeUnit.DAYS.toMillis(21) })
                    HomeMixKind.DAILY -> fail("Daily kind cannot be a rotating mix")
                }
            }

            val manualDaily = manager.generateHomeMixSnapshots(
                songs, favorites, date, emptyList(), variantSeed = 987654321L,
                zoneId = ZoneOffset.UTC, dailyExcludedSongIds = priorMixIds
            ).first()
            assertNotEquals(daily.songIds, manualDaily.songIds)
            assertTrue(manualDaily.songIds.none { it in priorMixIds })

            val history = HomeMixSnapshot("daily", HomeMixKind.DAILY, localDate = date.minusDays(1).toString(), songIds = daily.songIds)
            val nextDay = manager.generateHomeMixSnapshots(songs, favorites, date.plusDays(1), listOf(history), zoneId = ZoneOffset.UTC)
            assertEquals(date.plusDays(1).toString(), nextDay.first().localDate)
        } finally {
            directory.toFile().delete()
        }
    }
}
