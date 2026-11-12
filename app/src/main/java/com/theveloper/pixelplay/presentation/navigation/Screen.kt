package com.theveloper.pixelplay.presentation.navigation

import androidx.compose.runtime.Immutable
import com.theveloper.pixelplay.data.model.LibraryTabId


@Immutable
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library") {
        const val TAB_ARGUMENT = "tab"
        val pattern = "$route?$TAB_ARGUMENT={$TAB_ARGUMENT}"

        fun createRoute(tab: LibraryTabId) = "$route?$TAB_ARGUMENT=${tab.storageKey}"
    }
    object Settings : Screen("settings")
    object Accounts : Screen("settings_accounts")
    object SettingsCategory : Screen("settings_category/{categoryId}") {
        fun createRoute(categoryId: String) = "settings_category/$categoryId"
    }
    object PaletteStyle : Screen("palette_style_settings")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }

    object DailyMixScreen : Screen("daily_mix?mixId={mixId}") {
        fun createRoute(mixId: String) = "daily_mix?mixId=${android.net.Uri.encode(mixId)}"
    }
    object RecentlyPlayed : Screen("recently_played")
    object Stats : Screen("stats")
    object GenreDetail : Screen("genre_detail/{genreId}") { // New screen
        fun createRoute(genreId: String) = "genre_detail/$genreId"
    }
    object DJSpace : Screen("dj_space")
    // La ruta base es "album_detail". La ruta completa con el argumento se define en AppNavigation.
    object AlbumDetail : Screen("album_detail/{albumId}") {
        // Función de ayuda para construir la ruta de navegación con el ID del álbum.
        fun createRoute(albumId: Long) = "album_detail/$albumId"
    }

    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: Long) = "artist_detail/$artistId"
    }

    object EditTransition : Screen("edit_transition?playlistId={playlistId}") {
        fun createRoute(playlistId: String?) =
            if (playlistId != null) "edit_transition?playlistId=$playlistId" else "edit_transition"
    }

    object About : Screen("about")
    object OpenSourceLicenses : Screen("open_source_licenses")
    object EasterEgg : Screen("easter_egg")

    object ArtistSettings : Screen("artist_settings")
    object DelimiterConfig : Screen("delimiter_config")
    object WordDelimiterConfig : Screen("word_delimiter_config")
    object Equalizer : Screen("equalizer")
    object DeviceCapabilities : Screen("device_capabilities")
    object NeteaseDashboard : Screen("netease_dashboard")
    object QqMusicDashboard : Screen("qqmusic_dashboard")
    object NavidromeDashboard : Screen("navidrome_dashboard")
    object JellyfinDashboard : Screen("jellyfin_dashboard")

}
