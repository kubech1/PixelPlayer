package com.theveloper.pixelplay.presentation.screens

import com.theveloper.pixelplay.presentation.components.bottomchrome.BottomChromeGradient

import com.theveloper.pixelplay.presentation.components.bottomchrome.NavigationReselectionEffect

import com.theveloper.pixelplay.presentation.navigation.navigateSafely
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.data.model.LibraryTabId
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.netease.dashboard.NeteaseDashboardViewModel
import com.theveloper.pixelplay.presentation.jellyfin.dashboard.JellyfinDashboardViewModel
import com.theveloper.pixelplay.presentation.navidrome.dashboard.NavidromeDashboardViewModel
import com.theveloper.pixelplay.presentation.qqmusic.dashboard.QqMusicDashboardViewModel
import com.theveloper.pixelplay.presentation.components.HomeListeningSummaryCard
import com.theveloper.pixelplay.presentation.components.HomeMixCarousel
import com.theveloper.pixelplay.presentation.components.HomeQuickAccess
import com.theveloper.pixelplay.presentation.components.HomeQuickLink
import com.theveloper.pixelplay.presentation.components.HomeRecentlyPlayedSection
import com.theveloper.pixelplay.presentation.components.localizedTitle
import com.theveloper.pixelplay.presentation.components.HomeGradientTopBar
import com.theveloper.pixelplay.presentation.components.bottomchrome.LocalBottomChrome
import com.theveloper.pixelplay.presentation.model.collectRecentlyPlayedSongIds
import com.theveloper.pixelplay.presentation.model.mapRecentlyPlayedSongs
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.components.StreamingProviderSheet
import com.theveloper.pixelplay.data.database.isProviderSong
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.presentation.viewmodel.SettingsViewModel
import com.theveloper.pixelplay.presentation.viewmodel.StatsViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.ui.res.stringResource

@Composable
private fun formatListeningDuration(durationMs: Long): String {
    if (durationMs <= 0L) return stringResource(R.string.home_minutes, 0)
    val minutes = durationMs / 60_000L
    return if (minutes == 0L) stringResource(R.string.home_less_than_minute)
    else stringResource(R.string.home_minutes, minutes.toInt())
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    paddingValuesParent: PaddingValues,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onOpenSidebar: () -> Unit
) {
    val context = LocalContext.current
    // DETECTAR MODO BENCHMARK
    val isBenchmarkMode = remember {
        (context as? android.app.Activity)?.intent?.getBooleanExtra("is_benchmark", false) ?: false
    }
    val statsViewModel: StatsViewModel = hiltViewModel()
    val homeMixFeed by playerViewModel.homeMixFeed.collectAsStateWithLifecycle()
    val cloudMusicEnabled by playerViewModel.cloudMusicEnabled.collectAsStateWithLifecycle()
    val playbackHistory by playerViewModel.playbackHistory.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val recentSongIds = remember(playbackHistory) {
        collectRecentlyPlayedSongIds(
            playbackHistory = playbackHistory,
            maxItems = 64
        )
    }
    val recentlyPlayedSourceSongsInitialValue = remember(recentSongIds) {
        if (recentSongIds.isEmpty()) persistentListOf<Song>() else null
    }
    val recentlyPlayedSourceSongs by remember(recentSongIds, playerViewModel) {
        playerViewModel.observeSongs(recentSongIds)
            .map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(initialValue = recentlyPlayedSourceSongsInitialValue)
    val latestRecentlyPlayedSongs = remember(playbackHistory, recentlyPlayedSourceSongs) {
        val sourceSongs = recentlyPlayedSourceSongs ?: return@remember emptyList()
        mapRecentlyPlayedSongs(
            playbackHistory = playbackHistory,
            songs = sourceSongs,
            maxItems = 64
        )
    }
    // Keep the visible Home snapshot stable and only refresh it once the screen is off-screen.
    var recentlyPlayedSongs by rememberSaveable { mutableStateOf(latestRecentlyPlayedSongs) }
    val latestRecentlyPlayedSongsState = rememberUpdatedState(latestRecentlyPlayedSongs)

    LaunchedEffect(latestRecentlyPlayedSongs, lifecycleOwner) {
        val isHomeVisible = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (recentlyPlayedSongs.isEmpty() || !isHomeVisible) {
            recentlyPlayedSongs = latestRecentlyPlayedSongs
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    playerViewModel.refreshHomeMixes()
                    statsViewModel.refreshHomeListening()
                }
                Lifecycle.Event.ON_STOP -> recentlyPlayedSongs = latestRecentlyPlayedSongsState.value
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val visibleRecentSongs = remember(recentlyPlayedSongs, cloudMusicEnabled) {
        if (cloudMusicEnabled) recentlyPlayedSongs else recentlyPlayedSongs.filterNot { it.song.isProviderSong() }
    }
    val recentlyPlayedQueue = remember(visibleRecentSongs) {
        visibleRecentSongs.map { it.song }.toImmutableList()
    }

    ReportDrawnWhen {
        homeMixFeed != null || isBenchmarkMode
    }

    // 2) Observar sólo el currentSong (o null) para saber si mostrar padding
    val currentSong by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong }
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    // Padding inferior si hay canción en reproducción

    var showStreamingProviderSheet by remember { mutableStateOf(false) }
    LaunchedEffect(cloudMusicEnabled) {
        if (!cloudMusicEnabled) showStreamingProviderSheet = false
    }
    LocalContext.current

    val homeListening by statsViewModel.homeListening.collectAsStateWithLifecycle()

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    NavigationReselectionEffect(Screen.Home.route) { listState.animateScrollToItem(0) }
    val density = LocalDensity.current
    val scrollThresholdPx = remember(density) { with(density) { 32.dp.roundToPx() } }
    val isScrolledPastThreshold = remember(listState, scrollThresholdPx) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > scrollThresholdPx }
    }

    // Persist the scroll position across navigation away/back. The Stats card and other
    // conditional sections can shift indices while data re-emits when returning, which
    // would otherwise leave the list scrolled to the wrong place or jump to the top.
    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    var needsScrollRestore by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, listState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                savedScrollIndex = listState.firstVisibleItemIndex
                savedScrollOffset = listState.firstVisibleItemScrollOffset
                needsScrollRestore = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        needsScrollRestore,
        homeMixFeed,
        visibleRecentSongs.size,
        homeListening
    ) {
        if (!needsScrollRestore) return@LaunchedEffect
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems == 0) return@LaunchedEffect
        val targetIndex = savedScrollIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
        listState.scrollToItem(targetIndex, savedScrollOffset)
        needsScrollRestore = false
    }

    val libraryTabs by playerViewModel.libraryTabsFlow.collectAsStateWithLifecycle()
    val unknownAlbum = stringResource(R.string.common_unknown_album)
    val lastAlbumSong = remember(visibleRecentSongs, unknownAlbum) {
        visibleRecentSongs.firstOrNull {
            val song = it.song
            song.albumId != 0L && song.albumId != -1L && song.album.isNotBlank() &&
                !song.album.equals(unknownAlbum, ignoreCase = true) &&
                !song.album.equals("Unknown Album", ignoreCase = true) && song.album != "<unknown>"
        }?.song
    }
    val recentlyAdded = if (lastAlbumSong == null) {
        homeMixFeed?.rotating?.firstOrNull { it.kind == HomeMixKind.RECENTLY_ADDED }
    } else null
    val mixes = remember(homeMixFeed, recentlyAdded) {
        val rotations = homeMixFeed?.rotating.orEmpty().filter { it.id != recentlyAdded?.id }.take(3)
        (listOfNotNull(homeMixFeed?.daily) + rotations).filter { it.songs.isNotEmpty() }
    }
    val libraryLink = HomeQuickLink(
        title = stringResource(R.string.nav_bar_library),
        route = Screen.Library.route,
        icon = R.drawable.rounded_library_music_24,
    )
    val quickLinks = buildList {
        add(
            when {
                lastAlbumSong != null -> HomeQuickLink(
                    title = stringResource(R.string.home_quick_recent_album),
                    route = Screen.AlbumDetail.createRoute(lastAlbumSong.albumId),
                    icon = R.drawable.rounded_album_24,
                    subtitle = lastAlbumSong.album,
                    artworkUri = lastAlbumSong.albumArtUriString,
                )
                recentlyAdded != null -> HomeQuickLink(
                    title = recentlyAdded.localizedTitle(),
                    route = Screen.DailyMixScreen.createRoute(recentlyAdded.id),
                    icon = R.drawable.rounded_library_music_24,
                )
                else -> libraryLink
            }
        )
        if (LibraryTabId.LIKED.storageKey in libraryTabs) {
            add(HomeQuickLink(
                title = stringResource(R.string.library_tab_liked),
                route = Screen.Library.createRoute(LibraryTabId.LIKED),
                icon = R.drawable.rounded_favorite_24,
            ))
        } else if (first() != libraryLink) {
            add(libraryLink)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HomeGradientTopBar(
                    onNavigationIconClick = {
                        navController.navigateSafely(Screen.Settings.route)
                    },
                    onCloudClick = if (cloudMusicEnabled) ({
                        showStreamingProviderSheet = true
                    }) else null,
                    onMenuClick = {
                        // onOpenSidebar() // Disabled
                    },
                    isScrolled = isScrolledPastThreshold.value
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = maxOf(
                        paddingValuesParent.calculateBottomPadding(),
                        LocalBottomChrome.current.contentBottomPadding
                    )
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(key = "home_quick_access", contentType = "home_quick_access") {
                    HomeQuickAccess(quickLinks) { route -> navController.navigateSafely(route) }
                }
                if (mixes.isNotEmpty()) {
                    item(key = "home_mix_carousel", contentType = "home_mix_carousel") {
                        HomeMixCarousel(
                            mixes = mixes,
                            onOpen = { mix -> navController.navigateSafely(Screen.DailyMixScreen.createRoute(mix.id)) },
                            onPlay = { mix, title ->
                                playerViewModel.playHomeMix(mix.songs, mix.songs.first(), title)
                            },
                        )
                    }
                } else if (homeMixFeed == null) {
                    item(key = "home_mix_loading", contentType = "home_mix_loading") { HomeMixLoadingPlaceholder() }
                } else {
                    item(key = "home_mix_empty", contentType = "home_mix_empty") {
                        HomeMixEmptyPlaceholder(onRefresh = {
                            settingsViewModel.refreshLibrary()
                            playerViewModel.forceUpdateDailyMix()
                        })
                    }
                }

                if (visibleRecentSongs.isNotEmpty()) {
                    item(key = "recently_played_section", contentType = "recently_played_section") {
                        HomeRecentlyPlayedSection(
                            songs = visibleRecentSongs,
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            onPlaySong = { song ->
                                if (recentlyPlayedQueue.isNotEmpty()) playerViewModel.playSongs(
                                    songsToPlay = recentlyPlayedQueue, startSong = song, queueName = "Recently Played"
                                )
                            },
                            onOpenAll = { navController.navigateSafely(Screen.RecentlyPlayed.route) }
                        )
                    }
                }

                homeListening?.let { summary ->
                    item(key = "home_listening_summary", contentType = "home_listening_summary") {
                        HomeListeningSummaryCard(
                            today = formatListeningDuration(summary.todayDurationMs),
                            week = formatListeningDuration(summary.weekDurationMs),
                            topSong = summary.topSongTitle,
                            onClick = { navController.navigateSafely(Screen.Stats.route) }
                        )
                    }
                }
            }
        }
        BottomChromeGradient(
                Modifier.align(Alignment.BottomCenter)
            )
    }
    if (cloudMusicEnabled && showStreamingProviderSheet) {
        val neteaseViewModel: NeteaseDashboardViewModel = hiltViewModel()
        val qqMusicViewModel: QqMusicDashboardViewModel = hiltViewModel()
        val navidromeViewModel: NavidromeDashboardViewModel = hiltViewModel()
        val jellyfinViewModel: JellyfinDashboardViewModel = hiltViewModel()
        val isNeteaseLoggedIn by neteaseViewModel.isLoggedIn.collectAsStateWithLifecycle()
        val isQqMusicLoggedIn by qqMusicViewModel.isLoggedIn.collectAsStateWithLifecycle()
        val isNavidromeLoggedIn by navidromeViewModel.isLoggedIn.collectAsStateWithLifecycle()
        val isJellyfinLoggedIn by jellyfinViewModel.isLoggedIn.collectAsStateWithLifecycle()
        StreamingProviderSheet(
            onDismissRequest = { showStreamingProviderSheet = false },
            isNeteaseLoggedIn = isNeteaseLoggedIn,
            onNavigateToNeteaseDashboard = {
                navController.navigateSafely(Screen.NeteaseDashboard.route)
            },
            isQqMusicLoggedIn = isQqMusicLoggedIn,
            onNavigateToQqMusicDashboard = {
                navController.navigateSafely(Screen.QqMusicDashboard.route)
            },
            isNavidromeLoggedIn = isNavidromeLoggedIn,
            onNavigateToNavidromeDashboard = {
                navController.navigateSafely(Screen.NavidromeDashboard.route)
            },
            isJellyfinLoggedIn = isJellyfinLoggedIn,
            onNavigateToJellyfinDashboard = {
                navController.navigateSafely(Screen.JellyfinDashboard.route)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeMixLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun HomeMixEmptyPlaceholder(
    onRefresh: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 256.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 28.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusBR = 28.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusBL = 28.dp,
                    smoothnessAsPercentBR = 60,
                    cornerRadiusTR = 28.dp,
                    smoothnessAsPercentBL = 60,
                ),
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_empty_placeholder_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.home_empty_placeholder_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FilledTonalButton(
                onClick = onRefresh,
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 22.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusBR = 22.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusBL = 22.dp,
                    smoothnessAsPercentBR = 60,
                    cornerRadiusTR = 22.dp,
                    smoothnessAsPercentBL = 60,
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.home_empty_placeholder_refresh))
            }
        }
    }
}
