package com.theveloper.pixelplay.presentation.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.SortOption
import com.theveloper.pixelplay.data.model.StorageFilter
import com.theveloper.pixelplay.presentation.components.bottomchrome.LocalNavigationReselectionActive
import com.theveloper.pixelplay.presentation.components.subcomps.EnhancedSongListItem
import com.theveloper.pixelplay.presentation.utils.LocalVisualEffects
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.LocalPixelPlayDarkTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

@Immutable
internal data class LibrarySongPlaybackUiState(
    val isCurrentSong: Boolean = false,
    val isPlaying: Boolean = false
)

@OptIn(UnstableApi::class)
@Composable
internal fun LibraryPlaybackAwareSongItem(
    song: Song,
    playerViewModel: PlayerViewModel,
    albumArtSize: Dp = 50.dp,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    isSelectionMode: Boolean = false,
    useLibraryPill: Boolean = false,
    onLongPress: () -> Unit = {},
    onMoreOptionsClick: (Song) -> Unit,
    onClick: () -> Unit
) {
    val playbackUiState by remember(song.id, song.contentUriString, playerViewModel) {
        playerViewModel.stablePlayerState
            .map { state ->
                val current = state.currentSong
                val isCurrentSong = current?.id == song.id ||
                    (song.contentUriString.isNotBlank() && current?.contentUriString == song.contentUriString)
                LibrarySongPlaybackUiState(
                    isCurrentSong = isCurrentSong,
                    isPlaying = isCurrentSong && state.isPlaying
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = LibrarySongPlaybackUiState())

    val artworkUri = song.albumArtUriString
    val artworkColors = if (
        useLibraryPill && playbackUiState.isCurrentSong &&
        !artworkUri.isNullOrBlank() && LocalVisualEffects.current.allowsDepthEffects
    ) {
        val schemePair by remember(playerViewModel, artworkUri) {
            playerViewModel.themeStateHolder.getAlbumColorSchemeFlow(artworkUri)
        }.collectAsStateWithLifecycle()
        val scheme = if (LocalPixelPlayDarkTheme.current) schemePair?.dark else schemePair?.light
        scheme?.let { it.primary to it.tertiary }
    } else null

    EnhancedSongListItem(
        song = song,
        isPlaying = playbackUiState.isPlaying,
        isCurrentSong = playbackUiState.isCurrentSong,
        isLoading = false,
        albumArtSize = albumArtSize,
        isSelected = isSelected,
        selectionIndex = selectionIndex,
        isSelectionMode = isSelectionMode,
        useLibraryPill = useLibraryPill,
        artworkColors = artworkColors,
        onLongPress = onLongPress,
        onMoreOptionsClick = onMoreOptionsClick,
        onClick = onClick
    )
}

@OptIn(UnstableApi::class)
@Composable
internal fun RegisterLibraryCenterAction(
    playerViewModel: PlayerViewModel,
    listState: LazyListState,
    itemCount: Int,
    sortOption: SortOption,
    storageFilter: StorageFilter,
    favoritesOnly: Boolean,
    onRegister: ((() -> Unit)?) -> Unit
) {
    val active = LocalNavigationReselectionActive.current
    val scope = rememberCoroutineScope()
    val animated = LocalMotionPreferences.current.animationsEnabled
    val register by rememberUpdatedState(onRegister)
    val songId by remember(playerViewModel) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isFavorite = if (favoritesOnly) {
        val favorites by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
        songId in favorites
    } else true

    var index by remember(active, songId, itemCount, sortOption, storageFilter, isFavorite) {
        mutableStateOf<Int?>(null)
    }
    LaunchedEffect(active, songId, itemCount, sortOption, storageFilter, isFavorite) {
        if (active && itemCount > 0) {
            index = songId?.let {
                playerViewModel.findLibrarySongIndex(it, sortOption, storageFilter, favoritesOnly)
            }?.takeIf { it in 0 until itemCount }
        }
    }

    val action = remember(index, listState, animated) {
        index?.let { target ->
            { scope.launch { listState.centerLibraryItem(target, animated) }; Unit }
        }
    }
    DisposableEffect(active, action) {
        if (active) register(action)
        onDispose { if (active) register(null) }
    }
}

private suspend fun LazyListState.centerLibraryItem(index: Int, animated: Boolean) {
    if (index !in 0 until layoutInfo.totalItemsCount) return
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
        if (animated && abs(index - firstVisibleItemIndex) <= 20) {
            animateScrollToItem(index)
        } else {
            scrollToItem(index)
        }
    }

    val layout = layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportCenter =
        (layout.viewportStartOffset + layout.viewportEndOffset - layout.afterContentPadding) / 2f
    val delta = item.offset + item.size / 2f - viewportCenter
    if (animated) animateScrollBy(delta) else scrollBy(delta)
}
