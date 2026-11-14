package com.theveloper.pixelplay.presentation.screens

import com.theveloper.pixelplay.presentation.components.topEdgeFade
import com.theveloper.pixelplay.presentation.components.bottomchrome.LocalBottomChrome

import com.theveloper.pixelplay.presentation.components.bottomchrome.NavigationReselectionEffect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.data.model.LibraryTabId
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.StorageFilter
import com.theveloper.pixelplay.data.model.SortOption
import com.theveloper.pixelplay.presentation.components.MiniPlayerHeight
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.presentation.components.subcomps.EnhancedSongListItem
import kotlinx.coroutines.launch
import androidx.paging.compose.LazyPagingItems
import androidx.paging.LoadState
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.ExpressiveScrollBar
import com.theveloper.pixelplay.ui.theme.LocalShowScrollbar
import com.theveloper.pixelplay.presentation.components.songFastScrollLabel
import androidx.compose.ui.text.style.TextOverflow


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibrarySongsTab(
    songs: LazyPagingItems<Song>, // Changed from ImmutableList<Song>
    isLoading: Boolean, // Kept for initial load or other states, though Paging has its own
    playerViewModel: PlayerViewModel,
    bottomBarHeight: Dp,
    onMoreOptionsClick: (Song) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    sortOption: SortOption, // Added sortOption parameter
    // Multi-selection parameters
    isSelectionMode: Boolean = false,
    selectedSongIds: Set<String> = emptySet(),
    onSongLongPress: (Song) -> Unit = {},
    onSongSelectionToggle: (Song) -> Unit = {},
    getSelectionIndex: (String) -> Int? = { null },
    onRegisterLocateCurrentSongAction: ((() -> Unit)?) -> Unit = {},
    storageFilter: StorageFilter = StorageFilter.ALL,
    hasCurrentSong: Boolean = false
) {
    val listState = rememberLazyListState()
    NavigationReselectionEffect(com.theveloper.pixelplay.presentation.navigation.Screen.Library.route) { listState.animateScrollToItem(0) }
    val dummyListState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val songFastScrollLabelProvider = remember(songs, sortOption) {
        { index: Int ->
            songFastScrollLabel(
                song = songs.peek(index),
                sortOption = sortOption
            )
        }
    }
    var lastHandledSongSortKey by remember { mutableStateOf(sortOption.storageKey) }
    var pendingSongSortScrollReset by remember { mutableStateOf(false) }
    var songSortSawRefreshLoading by remember { mutableStateOf(false) }
    RegisterLibraryCenterAction(
        playerViewModel = playerViewModel,
        listState = listState,
        itemCount = songs.itemCount,
        sortOption = sortOption,
        storageFilter = storageFilter,
        favoritesOnly = false,
        onRegister = onRegisterLocateCurrentSongAction,
    )

    LaunchedEffect(sortOption) {
        val currentSortKey = sortOption.storageKey
        if (currentSortKey == lastHandledSongSortKey) return@LaunchedEffect
        lastHandledSongSortKey = currentSortKey
        pendingSongSortScrollReset = true
        songSortSawRefreshLoading = false
        listState.scrollToItem(0)
    }

    // Apply a second reset after paging finishes refresh, to avoid key-anchor jumps.
    LaunchedEffect(songs.loadState.refresh, pendingSongSortScrollReset) {
        if (!pendingSongSortScrollReset) return@LaunchedEffect
        if (songs.loadState.refresh is LoadState.Loading) {
            songSortSawRefreshLoading = true
            return@LaunchedEffect
        }
        if (!songSortSawRefreshLoading) return@LaunchedEffect
        listState.scrollToItem(0)
        pendingSongSortScrollReset = false
    }

    val refreshState = songs.loadState.refresh
    val reachedEndOfPagination = songs.loadState.append.endOfPaginationReached
    val shouldShowInitialLoading = songs.itemCount == 0 && isLoading

    when {
        refreshState is LoadState.Error && songs.itemCount == 0 -> {
            val error = (refreshState as LoadState.Error).error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.library_error_loading_songs), style = MaterialTheme.typography.titleMedium)
                    Text(
                        error.localizedMessage ?: stringResource(R.string.common_error_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { songs.retry() }) {
                        Text(stringResource(R.string.library_action_retry), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        shouldShowInitialLoading -> {
            // Initial loading - show skeleton placeholders
            LazyColumn(
                modifier = Modifier
                    .padding(start = 12.dp, end = 24.dp, bottom = 6.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 26.dp,
                            topEnd = 26.dp,
                            bottomStart = PlayerSheetCollapsedCornerRadius,
                            bottomEnd = PlayerSheetCollapsedCornerRadius
                        )
                    )
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = bottomBarHeight + LocalBottomChrome.current.miniPlayerRestingHeight + ListExtraBottomGap)
            ) {
                items(12, key = { "skeleton_song_$it" }) { // Show 12 skeleton items
                    EnhancedSongListItem(
                        song = Song.emptySong(),
                        isPlaying = false,
                        isLoading = true,
                        isCurrentSong = false,
                        onMoreOptionsClick = {},
                        onClick = {}
                    )
                }
            }
        }
        songs.itemCount == 0 && refreshState is LoadState.NotLoading && reachedEndOfPagination -> {
            LibraryExpressiveEmptyState(
                tabId = LibraryTabId.SONGS,
                storageFilter = storageFilter,
                bottomBarHeight = bottomBarHeight
            )
        }
        else -> {
            // Songs loaded
            Box(modifier = Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val activeListState = if (songs.itemCount > 0) listState else dummyListState
                        LazyColumn(
                            modifier = Modifier
                                .padding(start = 12.dp, end = if (LocalShowScrollbar.current && (activeListState.canScrollForward || activeListState.canScrollBackward)) 22.dp else 12.dp, bottom = 6.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 26.dp,
                                        topEnd = 26.dp,
                                        bottomStart = PlayerSheetCollapsedCornerRadius,
                                        bottomEnd = PlayerSheetCollapsedCornerRadius
                                    )
                                ).topEdgeFade(MaterialTheme.colorScheme.background) { activeListState.canScrollBackward },
                            state = activeListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = bottomBarHeight + LocalBottomChrome.current.miniPlayerRestingHeight + 30.dp)
                        ) {
                            //item(key = "songs_top_spacer") { Spacer(Modifier.height(0.dp)) }

                            items(
                                count = songs.itemCount,
                                key = { index -> songs.peek(index)?.id ?: index },
                                contentType = { "song" }
                            ) { index ->
                                val song = songs[index]
                                
                                if (song != null) {
                                    val isSelected = selectedSongIds.contains(song.id)
                                    
                                    val rememberedOnMoreOptionsClick: (Song) -> Unit = remember(onMoreOptionsClick) {
                                        { songFromListItem -> onMoreOptionsClick(songFromListItem) }
                                    }
                                    
                                    // In selection mode, click toggles selection instead of playing
                                    val rememberedOnClick: () -> Unit = remember(song, isSelectionMode) {
                                        if (isSelectionMode) {
                                            { onSongSelectionToggle(song) }
                                        } else {
                                            { playerViewModel.showAndPlaySongFromLibrary(song) }
                                        }
                                    }
                                    
                                    val rememberedOnLongPress: () -> Unit = remember(song) {
                                        { onSongLongPress(song) }
                                    }

                                    LibraryPlaybackAwareSongItem(
                                        useLibraryPill = true,
                                        song = song,
                                        playerViewModel = playerViewModel,
                                        isSelected = isSelected,
                                        //albumArtSize = 46.dp,
                                        isSelectionMode = isSelectionMode,
                                        selectionIndex = if (isSelectionMode) getSelectionIndex(song.id) else null,
                                        onLongPress = rememberedOnLongPress,
                                        onMoreOptionsClick = rememberedOnMoreOptionsClick,
                                        onClick = rememberedOnClick
                                    )
                                } else {
                                     // Placeholder
                                     EnhancedSongListItem(
                                        song = Song.emptySong(),
                                        isPlaying = false,
                                        isLoading = true,
                                        isCurrentSong = false,
                                        onMoreOptionsClick = {},
                                        onClick = {}
                                     )
                                }
                            }
                        }
                        
                        // ScrollBar Overlay
                        val bottomPadding = if (hasCurrentSong)
                            bottomBarHeight + LocalBottomChrome.current.miniPlayerRestingHeight + 16.dp
                        else 
                            bottomBarHeight + 16.dp

                        ExpressiveScrollBar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp, top = 16.dp, bottom = bottomPadding),
                            listState = activeListState,
                            dragLabelProvider = songFastScrollLabelProvider
                        )
                    }
                }
            }
        }
    }
}
