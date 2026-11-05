package com.theveloper.pixelplay.data

import android.content.Context
import com.theveloper.pixelplay.data.database.EngagementDao
import com.theveloper.pixelplay.data.model.Song
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class DailyMixManagerTest {
    @Test
    fun `local mixes still produce bounded unique selections without AI configuration`() = runTest {
        val directory = Files.createTempDirectory("local-mix-test")
        try {
            val context = mockk<Context>()
            every { context.filesDir } returns directory.toFile()
            val engagements = mockk<EngagementDao>()
            coEvery { engagements.getAllEngagements() } returns emptyList()
            val manager = DailyMixManager(context, engagements)
            val songs = (1..5).map { index ->
                Song.emptySong().copy(id = "source:$index", title = "Song $index", artistId = index.toLong())
            }
            val favorites = setOf("source:1")
            val daily = manager.generateDailyMix(songs, favorites, limit = 3)
            val personal = manager.generateYourMix(songs, favorites, limit = 3)
            for (mix in listOf(daily, personal)) {
                assertEquals(3, mix.size)
                assertEquals(mix.size, mix.map { it.id }.distinct().size)
                assertTrue(mix.all { candidate -> songs.any { it.id == candidate.id } })
            }
            assertTrue(personal.any { it.id in favorites })
            assertTrue(manager.generateDailyMix(emptyList()).isEmpty())
            assertTrue(manager.generateYourMix(emptyList()).isEmpty())
        } finally {
            directory.toFile().delete()
        }
    }
}
