package com.theveloper.pixelplay.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetiredAiMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PixelPlayDatabase::class.java)

    @Test
    fun retireAiPreservesPlaylistsSongsCoversAndUnrelatedTables() {
        val name = "retired-ai-migration-test"
        helper.createDatabase(name, 42).apply {
            execSQL("INSERT INTO playlists (id,name,created_at,last_modified,is_ai_generated,is_queue_generated,cover_image_uri,cover_color_argb,source) VALUES ('generated','Saved mix',100,200,1,0,'content://synthetic/cover',42,'AI')")
            execSQL("INSERT INTO playlists (id,name,created_at,last_modified,is_ai_generated,is_queue_generated,source) VALUES ('local','Manual',300,400,0,1,'LOCAL')")
            execSQL("INSERT INTO playlist_songs (playlist_id,song_id,sort_order) VALUES ('generated','cloud:17',0),('generated','23',1),('local','23',0)")
            execSQL("INSERT INTO songs (id,title,artist_name,artist_id,album_name,album_id,content_uri_string,duration,file_path,parent_directory_path,is_favorite,lyrics) VALUES (23,'Fixture track','Fixture artist',1,'Fixture album',2,'content://synthetic/23',20000,'/synthetic/music.mp3','/synthetic',1,'Original / Traducción')")
            execSQL("INSERT INTO favorites (songId,isFavorite,timestamp) VALUES (23,1,500)")
            execSQL("INSERT INTO song_engagements (song_id,play_count,total_play_duration_ms,last_played_timestamp) VALUES ('23',7,90000,600)")
            execSQL("INSERT INTO search_history (id,query,timestamp) VALUES (1,'Fixture search',700)")
            close()
        }
        helper.runMigrationsAndValidate(name, 43, true, PixelPlayDatabase.MIGRATION_42_43).use { db ->
            db.query("SELECT id,name,created_at,last_modified,cover_image_uri,cover_color_argb,source FROM playlists WHERE id='generated'").use {
                assertTrue(it.moveToFirst())
                assertEquals("generated", it.getString(0))
                assertEquals("Saved mix", it.getString(1))
                assertEquals(100L, it.getLong(2))
                assertEquals(200L, it.getLong(3))
                assertEquals("content://synthetic/cover", it.getString(4))
                assertEquals(42, it.getInt(5))
                assertEquals("LOCAL", it.getString(6))
            }
            db.query("SELECT song_id,sort_order FROM playlist_songs WHERE playlist_id='generated' ORDER BY sort_order").use {
                assertTrue(it.moveToNext()); assertEquals("cloud:17", it.getString(0)); assertEquals(0, it.getInt(1))
                assertTrue(it.moveToNext()); assertEquals("23", it.getString(0)); assertEquals(1, it.getInt(1))
                assertFalse(it.moveToNext())
            }
            db.query("SELECT is_queue_generated FROM playlists WHERE id='local'").use {
                assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
            }
            db.query("SELECT title,is_favorite,lyrics FROM songs WHERE id=23").use {
                assertTrue(it.moveToFirst()); assertEquals("Fixture track",it.getString(0))
                assertEquals(1,it.getInt(1)); assertEquals("Original / Traducción",it.getString(2))
            }
            db.query("SELECT isFavorite,timestamp FROM favorites WHERE songId=23").use {
                assertTrue(it.moveToFirst()); assertEquals(1,it.getInt(0)); assertEquals(500L,it.getLong(1))
            }
            db.query("SELECT play_count,total_play_duration_ms,last_played_timestamp FROM song_engagements WHERE song_id='23'").use {
                assertTrue(it.moveToFirst()); assertEquals(7,it.getInt(0)); assertEquals(90000L,it.getLong(1)); assertEquals(600L,it.getLong(2))
            }
            db.query("SELECT query,timestamp FROM search_history WHERE id=1").use {
                assertTrue(it.moveToFirst()); assertEquals("Fixture search",it.getString(0)); assertEquals(700L,it.getLong(1))
            }
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('ai_cache','ai_usage')").use {
                assertEquals(0, it.count)
            }
            db.query("PRAGMA table_info(playlists)").use {
                while (it.moveToNext()) assertNotEquals("is_ai_generated", it.getString(it.getColumnIndexOrThrow("name")))
            }
        }
    }
}
