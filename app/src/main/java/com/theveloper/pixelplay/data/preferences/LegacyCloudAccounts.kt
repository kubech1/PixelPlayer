package com.theveloper.pixelplay.data.preferences

import com.theveloper.pixelplay.data.database.TelegramDao
import com.theveloper.pixelplay.data.gdrive.GDriveRepository
import com.theveloper.pixelplay.data.jellyfin.JellyfinRepository
import com.theveloper.pixelplay.data.navidrome.NavidromeRepository
import com.theveloper.pixelplay.data.netease.NeteaseRepository
import com.theveloper.pixelplay.data.qqmusic.QqMusicRepository
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Uses local account state to initialize the preference for existing installations. */
class LegacyCloudAccounts @Inject constructor(
    private val telegramDao: TelegramDao,
    private val navidrome: Provider<NavidromeRepository>,
    private val jellyfin: Provider<JellyfinRepository>,
    private val netease: Provider<NeteaseRepository>,
    private val qqMusic: Provider<QqMusicRepository>,
    private val drive: Provider<GDriveRepository>
) {
    suspend fun hasConfiguredAccount(): Boolean = withContext(Dispatchers.IO) {
        telegramDao.getAllChannels().first().isNotEmpty() ||
            navidrome.get().isLoggedIn ||
            jellyfin.get().isLoggedIn ||
            netease.get().isLoggedIn ||
            qqMusic.get().isLoggedIn ||
            drive.get().isLoggedIn
    }
}
