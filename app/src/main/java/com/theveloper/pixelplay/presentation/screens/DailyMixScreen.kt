package com.theveloper.pixelplay.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.HomeMix
import com.theveloper.pixelplay.presentation.components.HomeMixArtwork
import com.theveloper.pixelplay.presentation.components.PlaylistBottomSheet
import com.theveloper.pixelplay.presentation.components.SongInfoBottomSheet
import com.theveloper.pixelplay.presentation.components.bottomchrome.BottomChromeGradient
import com.theveloper.pixelplay.presentation.components.bottomchrome.LocalBottomChrome
import com.theveloper.pixelplay.presentation.components.localizedTitle
import com.theveloper.pixelplay.presentation.components.rememberMixPosterShine
import com.theveloper.pixelplay.presentation.components.subcomps.EnhancedSongListItem
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.navigation.navigateSafely
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.presentation.viewmodel.PlaylistViewModel
import com.theveloper.pixelplay.utils.formatDuration
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DailyMixScreen(
    playerViewModel: PlayerViewModel,
    navController: NavController,
    mixId: String,
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    val feed by playerViewModel.homeMixFeed.collectAsStateWithLifecycle()
    val availableMix = feed?.let { if (mixId == "daily") it.daily else it.rotating.find { mix -> mix.id == mixId } }
    var displayedMix by remember(mixId) { mutableStateOf<HomeMix?>(null) }
    LaunchedEffect(mixId) {
        if (feed == null) playerViewModel.refreshHomeMixes()
    }
    LaunchedEffect(availableMix) {
        if (displayedMix == null) displayedMix = availableMix
    }
    val mix = displayedMix ?: availableMix
    val shineProgress = rememberMixPosterShine(mix)
    val title = mix?.localizedTitle() ?: stringResource(R.string.daily_mix_title)
    val songs = mix?.songs ?: persistentListOf()

    val playback by remember(playerViewModel) {
        playerViewModel.stablePlayerState
            .map { it.currentSong?.id to it.isPlaying }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null to false)
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val bottomChrome = LocalBottomChrome.current
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    val infoSong = selectedSongForInfo
    if (showSongInfoSheet && infoSong != null) {
        val song = infoSong
        SongInfoBottomSheet(
            song = song,
            isFavorite = favoriteSongIds.contains(song.id),
            onToggleFavorite = { playerViewModel.toggleFavoriteSpecificSong(song) },
            onDismiss = { showSongInfoSheet = false },
            onPlaySong = {
                playerViewModel.playHomeMix(songs, song, title)
            },
            onAddToQueue = {
                playerViewModel.addSongToQueue(song)
            },
            onAddNextToQueue = {
                playerViewModel.addSongNextToQueue(song)
            },
            onAddToPlayList = {
                showPlaylistBottomSheet = true
            },
            onDeleteFromDevice = playerViewModel::deleteFromDevice,
            onNavigateToAlbum = {
                navController.navigateSafely(Screen.AlbumDetail.createRoute(song.albumId))
                showSongInfoSheet = false
            },
            onNavigateToArtist = {
                navController.navigateSafely(Screen.ArtistDetail.createRoute(song.artistId))
                showSongInfoSheet = false
            },
            onNavigateToArtistById = { artistId ->
                navController.navigateSafely(Screen.ArtistDetail.createRoute(artistId))
                showSongInfoSheet = false
            },
            onNavigateToGenre = {
                song.genre?.let {
                    navController.navigateSafely(Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")))
                }
                showSongInfoSheet = false
            },
            onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate ->
                playerViewModel.editSongMetadata(
                    song,
                    newTitle,
                    newArtist,
                    newAlbum,
                    newAlbumArtist,
                    newComposer,
                    newGenre,
                    newLyrics,
                    newTrackNumber,
                    newDiscNumber,
                    replayGainTrackGainDb,
                    replayGainAlbumGainDb,
                    coverArtUpdate
                )
            },
            removeFromListTrigger = {
                val remaining = songs.filterNot { it.id == song.id }.toImmutableList()
                mix?.let {
                    displayedMix = it.copy(
                        songs = remaining,
                        cover = if (it.cover?.id == song.id) {
                            remaining.firstOrNull { item -> !item.albumArtUriString.isNullOrBlank() }
                        } else {
                            it.cover
                        }
                    )
                }
                playerViewModel.refreshHomeMixes()
            }
        )

        if (showPlaylistBottomSheet) {
            val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

            PlaylistBottomSheet(
                playlistUiState = playlistUiState,
                songs = listOf(song),
                onDismiss = { showPlaylistBottomSheet = false },
                bottomBarHeight = bottomChrome.navBarOccupiedHeight,
                playerViewModel = playerViewModel,
            )
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.statusBarsPadding().padding(start = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back))
            }
            when {
                mix == null && feed == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                mix == null || songs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.mix_detail_unavailable),
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomChrome.contentBottomPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "mix_header") {
                        MixDetailHeader(title, mix, shineProgress)
                    }
                    item(key = "play_shuffle_buttons") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { playerViewModel.playHomeMix(songs, songs.first(), title) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.daily_mix_action_play_it))
                            }
                            FilledTonalButton(
                                onClick = {
                                    playerViewModel.playSongsShuffled(
                                        songsToPlay = songs,
                                        queueName = title,
                                        startAtZero = true
                                    )
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Rounded.Shuffle, null, Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.common_shuffle))
                            }
                        }
                    }
                    items(songs, key = { it.id }) { song ->
                        val isCurrentSong = playback.first == song.id
                        EnhancedSongListItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            song = song,
                            isCurrentSong = isCurrentSong,
                            isPlaying = isCurrentSong && playback.second,
                            onClick = { playerViewModel.playHomeMix(songs, song, title) },
                            onMoreOptionsClick = {
                                playerViewModel.selectSongForInfo(song)
                                showSongInfoSheet = true
                            }
                        )
                    }
                }
            }
        }
        BottomChromeGradient(Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MixDetailHeader(title: String, mix: HomeMix, shineProgress: (() -> Float)?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeMixArtwork(mix, Modifier.size(176.dp), shineProgress = shineProgress)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = pluralStringResource(
                R.plurals.daily_mix_songs_dot_duration,
                mix.songs.size,
                mix.songs.size,
                formatDuration(mix.songs.sumOf { it.duration })
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
