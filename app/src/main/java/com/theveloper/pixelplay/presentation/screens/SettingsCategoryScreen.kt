package com.theveloper.pixelplay.presentation.screens

import com.theveloper.pixelplay.presentation.navigation.navigateSafely
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.preferences.AlbumArtQuality
import com.theveloper.pixelplay.data.preferences.AppLanguage
import com.theveloper.pixelplay.data.preferences.AppThemeMode
import com.theveloper.pixelplay.data.preferences.CollagePattern
import com.theveloper.pixelplay.data.preferences.CarouselStyle
import com.theveloper.pixelplay.data.preferences.LaunchTab
import com.theveloper.pixelplay.data.preferences.LibraryNavigationMode
import com.theveloper.pixelplay.data.preferences.NavBarStyle
import com.theveloper.pixelplay.data.preferences.ThemePreference
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.LyricsSourcePreference
import com.theveloper.pixelplay.presentation.components.CollapsibleCommonTopBar
import com.theveloper.pixelplay.presentation.components.ExpressiveTopBarContent
import com.theveloper.pixelplay.presentation.components.FileExplorerDialog
import com.theveloper.pixelplay.presentation.components.MiniPlayerHeight
import com.theveloper.pixelplay.presentation.model.SettingsCategory
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.viewmodel.LyricsRefreshProgress
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.presentation.viewmodel.SettingsViewModel
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsCategoryScreen(
    categoryId: String,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    statsViewModel: com.theveloper.pixelplay.presentation.viewmodel.StatsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val category = SettingsCategory.fromId(categoryId) ?: return
    val context = LocalContext.current

    // Hoisted string resources (for use inside lambdas/callbacks)
    val toastLibrarySyncFinished = stringResource(R.string.settings_toast_library_sync_finished)
    val syncFullRescanLabel = stringResource(R.string.settings_label_sync_full_rescan)
    val toastFullRescanStarted = stringResource(R.string.settings_toast_full_rescan_started)
    val toastBatteryAlreadyDisabled = stringResource(R.string.settings_toast_battery_already_disabled)
    val toastBatterySettingsUnavailable = stringResource(R.string.settings_toast_battery_settings_unavailable)
    val dialogPaletteRegeneratedFormat = stringResource(R.string.settings_dialog_palette_regenerated)
    val dialogPaletteRegenerateFailedFormat = stringResource(R.string.settings_dialog_palette_regenerate_failed)
    val toastRegeneratedPalettesAllFormat = stringResource(R.string.settings_toast_regenerated_palettes_all)
    val toastRegeneratedPalettesPartialFormat = stringResource(R.string.settings_toast_regenerated_palettes_partial)
    val syncIndicatorRebuilding = stringResource(R.string.settings_label_sync_rebuilding)
    val toastRebuildingDatabase = stringResource(R.string.settings_toast_rebuilding_database)
    val toastDailyMixRegenerationStarted = stringResource(R.string.settings_toast_daily_mix_regeneration_started)
    val toastStatsRegenerationStarted = stringResource(R.string.settings_toast_stats_regeneration_started)
    
    // State Collection (Duplicated from SettingsScreen for now to ensure functionality)
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val currentPath by settingsViewModel.currentPath.collectAsStateWithLifecycle()
    val directoryChildren by settingsViewModel.currentDirectoryChildren.collectAsStateWithLifecycle()
    val availableStorages by settingsViewModel.availableStorages.collectAsStateWithLifecycle()
    val selectedStorageIndex by settingsViewModel.selectedStorageIndex.collectAsStateWithLifecycle()
    val isLoadingDirectories by settingsViewModel.isLoadingDirectories.collectAsStateWithLifecycle()
    val isExplorerPriming by settingsViewModel.isExplorerPriming.collectAsStateWithLifecycle()
    val isExplorerReady by settingsViewModel.isExplorerReady.collectAsStateWithLifecycle()
    val isCurrentDirectoryResolved by settingsViewModel.isCurrentDirectoryResolved.collectAsStateWithLifecycle()
    val isSyncing by settingsViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by settingsViewModel.syncProgress.collectAsStateWithLifecycle()
    val paletteRegenerateTargets by playerViewModel.paletteRegenerationTargets.collectAsStateWithLifecycle()
    val explorerRoot = settingsViewModel.explorerRoot()

    // Local State
    var showExplorerSheet by remember { mutableStateOf(false) }
    var refreshRequested by remember { mutableStateOf(false) }
    var syncRequestObservedRunning by remember { mutableStateOf(false) }
    var syncIndicatorLabel by remember { mutableStateOf<String?>(null) }
    var showClearLyricsDialog by remember { mutableStateOf(false) }
    var showRebuildDatabaseWarning by remember { mutableStateOf(false) }
    var showRegenerateDailyMixDialog by remember { mutableStateOf(false) }
    var showRegenerateStatsDialog by remember { mutableStateOf(false) }
    var showRegenerateAllPalettesDialog by remember { mutableStateOf(false) }
    var minSongDurationDraft by remember(uiState.minSongDuration) {
        mutableStateOf(uiState.minSongDuration.toFloat())
    }
    var minTracksPerAlbumDraft by remember(uiState.minTracksPerAlbum) {
        mutableStateOf(uiState.minTracksPerAlbum.toFloat())
    }
    var albumArtCacheLimitDraft by remember(uiState.albumArtCacheLimitMb) {
        mutableStateOf(uiState.albumArtCacheLimitMb.toFloat())
    }

    LaunchedEffect(isSyncing, refreshRequested) {
        if (!refreshRequested) return@LaunchedEffect

        if (isSyncing) {
            syncRequestObservedRunning = true
        } else if (syncRequestObservedRunning) {
            Toast.makeText(context, toastLibrarySyncFinished, Toast.LENGTH_SHORT).show()
            refreshRequested = false
            syncRequestObservedRunning = false
            syncIndicatorLabel = null
        }
    }

    var showPaletteRegenerateSheet by remember { mutableStateOf(false) }
    var isPaletteRegenerateRunning by remember { mutableStateOf(false) }
    var isPaletteBulkRegenerateRunning by remember { mutableStateOf(false) }
    var paletteBulkCompletedCount by remember { mutableStateOf(0) }
    var paletteBulkTotalCount by remember { mutableStateOf(0) }
    var paletteSongSearchQuery by remember { mutableStateOf("") }
    val paletteRegenerateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isAnyPaletteRegenerateRunning = isPaletteRegenerateRunning || isPaletteBulkRegenerateRunning
    val filteredPaletteSongs = remember(paletteRegenerateTargets, paletteSongSearchQuery) {
        val query = paletteSongSearchQuery.trim()
        if (query.isBlank()) {
            paletteRegenerateTargets
        } else {
            paletteRegenerateTargets.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                    song.displayArtist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            }
        }
    }


    // TopBar Animations (identical to SettingsScreen)
    // TopBar Animations (identical to SettingsScreen)
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    
    val categoryTitle = stringResource(category.titleRes)
    val isLongTitle = categoryTitle.length > 13
    
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = if (isLongTitle) 200.dp else 180.dp //for 2 lines use 220 and make text use \n

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    
    val titleMaxLines = if (isLongTitle) 2 else 1

    val topBarHeight = remember(maxTopBarHeightPx) { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value, maxTopBarHeightPx) {
        collapseFraction =
                1f -
                        ((topBarHeight.value - minTopBarHeightPx) /
                                        (maxTopBarHeightPx - minTopBarHeightPx))
                                .coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (!isScrollingDown &&
                                (lazyListState.firstVisibleItemIndex > 0 ||
                                        lazyListState.firstVisibleItemScrollOffset > 0)
                ) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight =
                        (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand =
                    lazyListState.firstVisibleItemIndex == 0 &&
                            lazyListState.firstVisibleItemScrollOffset == 0

            val targetValue =
                    if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    Box(
        modifier =
            Modifier.nestedScroll(nestedScrollConnection).fillMaxSize()
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
        
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = currentTopBarHeightDp + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            item {
               // Use a simple Column for now, or ExpressiveSettingsGroup if preferred strictly for items
               Column(
                    modifier = Modifier.background(Color.Transparent)
               ) {
                    when (category) {
                        SettingsCategory.LIBRARY -> {
                            SettingsSubsection(title = stringResource(R.string.settings_library_structure_section)) {
                                SettingsItem(
                                    title = stringResource(R.string.settings_excluded_directories_title),
                                    subtitle = stringResource(R.string.settings_excluded_directories_subtitle),
                                    leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.secondary) },
                                    trailingIcon = { Icon(Icons.Rounded.ChevronRight, stringResource(R.string.settings_cd_open), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = {
                                        showExplorerSheet = true
                                        settingsViewModel.openExplorer()
                                    }
                                )
                                SettingsItem(
                                    title = stringResource(R.string.settings_artists_title),
                                    subtitle = stringResource(R.string.settings_artists_subtitle),
                                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.secondary) },
                                    trailingIcon = { Icon(Icons.Rounded.ChevronRight, stringResource(R.string.settings_cd_open), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = { navController.navigateSafely(Screen.ArtistSettings.route) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_filtering_section)) {
                                SliderSettingsItem(
                                    label = stringResource(R.string.settings_min_song_duration),
                                    value = minSongDurationDraft,
                                    valueRange = 0f..120000f,
                                    steps = 23, // 0, 5, 10, 15, ... 120 seconds (24 positions, 23 steps)
                                    onValueChange = { minSongDurationDraft = it },
                                    onValueChangeFinished = {
                                        val selectedDuration = minSongDurationDraft.toInt()
                                        if (selectedDuration != uiState.minSongDuration) {
                                            settingsViewModel.setMinSongDuration(selectedDuration)
                                        }
                                    },
                                    valueText = { value -> "${(value / 1000).toInt()}s" }
                                )
                                SliderSettingsItem(
                                    label = stringResource(R.string.settings_min_tracks_per_album),
                                    value = minTracksPerAlbumDraft,
                                    valueRange = 1f..5f,
                                    steps = 3, // 1, 2, 3, 4, 5
                                    onValueChange = { minTracksPerAlbumDraft = it },
                                    onValueChangeFinished = {
                                        val selectedTracks = minTracksPerAlbumDraft.toInt()
                                        if (selectedTracks != uiState.minTracksPerAlbum) {
                                            settingsViewModel.setMinTracksPerAlbum(selectedTracks)
                                        }
                                    },
                                    valueText = { value -> "${value.toInt()}" }
                                )
                                SliderSettingsItem(
                                    label = stringResource(R.string.settings_album_art_cache_limit),
                                    value = albumArtCacheLimitDraft,
                                    valueRange = 50f..1500f,
                                    steps = 28, // 50, 100, 150, ... 1500 (30 stops)
                                    onValueChange = { albumArtCacheLimitDraft = it },
                                    onValueChangeFinished = {
                                        val selectedLimit = albumArtCacheLimitDraft.toInt()
                                        if (selectedLimit != uiState.albumArtCacheLimitMb) {
                                            settingsViewModel.setAlbumArtCacheLimitMb(selectedLimit)
                                        }
                                    },
                                    valueText = { value -> "${value.toInt()} MB" }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_sync_scanning_section)) {
                                RefreshLibraryItem(
                                    isSyncing = isSyncing,
                                    syncProgress = syncProgress,
                                    activeOperationLabel = if (isSyncing) syncIndicatorLabel else null,
                                    onFullSync = {
                                        if (isSyncing) return@RefreshLibraryItem
                                        refreshRequested = true
                                        syncRequestObservedRunning = false
                                        syncIndicatorLabel = syncFullRescanLabel
                                        Toast.makeText(context, toastFullRescanStarted, Toast.LENGTH_SHORT).show()
                                        settingsViewModel.fullSyncLibrary()
                                    },
                                    onRebuild = {
                                        if (isSyncing) return@RefreshLibraryItem
                                        showRebuildDatabaseWarning = true
                                    }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_auto_scan_lrc_title),
                                    subtitle = stringResource(R.string.settings_auto_scan_lrc_subtitle),
                                    checked = uiState.autoScanLrcFiles,
                                    onCheckedChange = { settingsViewModel.setAutoScanLrcFiles(it) },
                                    leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(
                                title = stringResource(R.string.settings_lyrics_management_section)
                            ) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_lyrics_source_priority_title),
                                    description = stringResource(R.string.settings_lyrics_source_priority_subtitle),
                                    options = mapOf(
                                        LyricsSourcePreference.EMBEDDED_FIRST.name to stringResource(R.string.settings_lyrics_embedded_first),
                                        LyricsSourcePreference.API_FIRST.name to stringResource(R.string.settings_lyrics_online_first),
                                        LyricsSourcePreference.LOCAL_FIRST.name to stringResource(R.string.settings_lyrics_local_first)
                                    ),
                                    selectedKey = uiState.lyricsSourcePreference.name,
                                    onSelectionChanged = { key ->
                                        settingsViewModel.setLyricsSourcePreference(
                                            LyricsSourcePreference.fromName(key)
                                        )
                                    },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_lyrics_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SettingsItem(
                                    title = stringResource(R.string.settings_reset_imported_lyrics_title),
                                    subtitle = stringResource(R.string.settings_reset_imported_lyrics_subtitle),
                                    leadingIcon = { Icon(Icons.Outlined.ClearAll, null, tint = MaterialTheme.colorScheme.secondary) },
                                    onClick = { showClearLyricsDialog = true }
                                )
                            }

                            SettingsSubsection(
                                title = stringResource(R.string.settings_maintenance_section),
                                addBottomSpace = false
                            ) {
                                ActionSettingsItem(
                                    title = stringResource(R.string.settings_force_daily_mix_title),
                                    subtitle = stringResource(R.string.settings_force_daily_mix_subtitle),
                                    icon = { Icon(painterResource(R.drawable.rounded_instant_mix_24), null, tint = MaterialTheme.colorScheme.secondary) },
                                    primaryActionLabel = stringResource(R.string.settings_action_regenerate_daily_mix),
                                    onPrimaryAction = { showRegenerateDailyMixDialog = true }
                                )
                                ActionSettingsItem(
                                    title = stringResource(R.string.settings_force_stats_title),
                                    subtitle = stringResource(R.string.settings_force_stats_subtitle),
                                    icon = { Icon(painterResource(R.drawable.rounded_monitoring_24), null, tint = MaterialTheme.colorScheme.secondary) },
                                    primaryActionLabel = stringResource(R.string.settings_action_regenerate_stats),
                                    onPrimaryAction = { showRegenerateStatsDialog = true }
                                )
                                ActionSettingsItem(
                                    title = stringResource(R.string.settings_force_palette_title),
                                    subtitle = if (paletteRegenerateTargets.isEmpty()) {
                                        stringResource(R.string.settings_force_palette_empty)
                                    } else {
                                        stringResource(R.string.settings_force_palette_subtitle)
                                    },
                                    icon = { Icon(Icons.Outlined.Style, null, tint = MaterialTheme.colorScheme.secondary) },
                                    primaryActionLabel = if (isPaletteBulkRegenerateRunning) stringResource(R.string.settings_regenerating) else stringResource(R.string.settings_action_regenerate_all),
                                    onPrimaryAction = { showRegenerateAllPalettesDialog = true },
                                    secondaryActionLabel = stringResource(R.string.settings_action_choose_song),
                                    onSecondaryAction = { showPaletteRegenerateSheet = true },
                                    enabled = paletteRegenerateTargets.isNotEmpty() && !isAnyPaletteRegenerateRunning
                                )
                            }
                        }
                        SettingsCategory.APPEARANCE -> {
                            val useSmoothCorners by settingsViewModel.useSmoothCorners.collectAsStateWithLifecycle()

                            SettingsSubsection(title = stringResource(R.string.settings_global_theme_section)) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_app_language_title),
                                    description = stringResource(R.string.settings_app_language_subtitle),
                                    options = AppLanguage.getLanguageOptions(context),
                                    selectedKey = uiState.appLanguageTag,
                                    onSelectionChanged = {
                                        settingsViewModel.setAppLanguage(it)
                                        (context as? Activity)?.recreate()
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_app_theme_title),
                                    description = stringResource(R.string.settings_app_theme_subtitle),
                                    options = mapOf(
                                        AppThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                                        AppThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                                        AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.settings_theme_follow_system)
                                    ),
                                    selectedKey = uiState.appThemeMode,
                                    onSelectionChanged = { settingsViewModel.setAppThemeMode(it) },
                                    leadingIcon = { Icon(Icons.Outlined.LightMode, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_smooth_corners_title),
                                    subtitle = stringResource(R.string.settings_smooth_corners_subtitle),
                                    checked = useSmoothCorners,
                                    onCheckedChange = settingsViewModel::setUseSmoothCorners,
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_rounded_corner_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_disable_blur_all_over_title),
                                    subtitle = stringResource(R.string.settings_disable_blur_all_over_subtitle),
                                    checked = uiState.disableBlurAllOver,
                                    onCheckedChange = { settingsViewModel.setDisableBlurAllOver(it) },
                                    leadingIcon = { Icon(Icons.Rounded.BlurOff, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_show_scrollbar_title),
                                    subtitle = stringResource(R.string.settings_show_scrollbar_subtitle),
                                    checked = uiState.showScrollbar,
                                    onCheckedChange = { settingsViewModel.setShowScrollbar(it) },
                                    leadingIcon = { Icon(Icons.Rounded.UnfoldMore, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_now_playing_section)) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_player_theme_title),
                                    description = stringResource(R.string.settings_player_theme_subtitle),
                                    options = mapOf(
                                        ThemePreference.ALBUM_ART to stringResource(R.string.settings_player_theme_album_art),
                                        ThemePreference.DYNAMIC to stringResource(R.string.settings_player_theme_dynamic)
                                    ),
                                    selectedKey = uiState.playerThemePreference,
                                    onSelectionChanged = { settingsViewModel.setPlayerThemePreference(it) },
                                    leadingIcon = { Icon(Icons.Outlined.PlayCircle, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_show_player_file_info_title),
                                    subtitle = stringResource(R.string.settings_show_player_file_info_subtitle),
                                    checked = uiState.showPlayerFileInfo,
                                    onCheckedChange = { settingsViewModel.setShowPlayerFileInfo(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_attach_file_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SettingsItem(
                                    title = stringResource(R.string.settings_album_art_palette_title),
                                    subtitle = stringResource(R.string.settings_album_art_palette_subtitle, uiState.albumArtPaletteStyle.label),
                                    leadingIcon = { Icon(Icons.Outlined.Style, null, tint = MaterialTheme.colorScheme.secondary) },
                                    trailingIcon = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = { navController.navigateSafely(Screen.PaletteStyle.route) }
                                )
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_carousel_style_title),
                                    description = stringResource(R.string.settings_carousel_style_subtitle),
                                    options = mapOf(
                                        CarouselStyle.NO_PEEK to stringResource(R.string.settings_carousel_no_peek),
                                        CarouselStyle.ONE_PEEK to stringResource(R.string.settings_carousel_one_peek)
                                    ),
                                    selectedKey = uiState.carouselStyle,
                                    onSelectionChanged = { settingsViewModel.setCarouselStyle(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_view_carousel_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_exp_album_art_resolution),
                                    description = stringResource(R.string.settings_exp_visual_quality),
                                    options = AlbumArtQuality.entries.associate { it.name to it.label },
                                    selectedKey = uiState.albumArtQuality.name,
                                    onSelectionChanged = { settingsViewModel.setAlbumArtQuality(AlbumArtQuality.valueOf(it)) },
                                    leadingIcon = { Icon(painterResource(R.drawable.outline_high_quality_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_home_collage_section)) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_collage_pattern_title),
                                    description = stringResource(R.string.settings_collage_pattern_subtitle),
                                    options = CollagePattern.entries.associate { it.storageKey to it.label },
                                    selectedKey = uiState.collagePattern.storageKey,
                                    onSelectionChanged = { key ->
                                        settingsViewModel.setCollagePattern(CollagePattern.fromStorageKey(key))
                                    },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_view_column_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_auto_rotate_patterns_title),
                                    subtitle = stringResource(R.string.settings_auto_rotate_patterns_subtitle),
                                    checked = uiState.collageAutoRotate,
                                    onCheckedChange = { settingsViewModel.setCollageAutoRotate(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_shuffle_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_navigation_bar_section)) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_navbar_style_title),
                                    description = stringResource(R.string.settings_navbar_style_subtitle),
                                    options = mapOf(
                                        NavBarStyle.DEFAULT to stringResource(R.string.settings_navbar_style_default),
                                        NavBarStyle.FULL_WIDTH to stringResource(R.string.settings_navbar_style_full_width)
                                    ),
                                    selectedKey = uiState.navBarStyle,
                                    onSelectionChanged = { settingsViewModel.setNavBarStyle(it) },
                                    leadingIcon = { Icon(Icons.Outlined.Style, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_compact_mode_title),
                                    subtitle = stringResource(R.string.settings_compact_mode_subtitle),
                                    checked = uiState.navBarCompactMode,
                                    onCheckedChange = { settingsViewModel.setNavBarCompactMode(it) },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.rounded_view_week_24),
                                            null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                )
                                SettingsItem(
                                    title = stringResource(R.string.settings_navbar_corner_title),
                                    subtitle = stringResource(R.string.settings_navbar_corner_subtitle),
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_rounded_corner_24), null, tint = MaterialTheme.colorScheme.secondary) },
                                    trailingIcon = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = { navController.navigateSafely("nav_bar_corner_radius") }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_lyrics_screen_section)) {
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_immersive_lyrics_title),
                                    subtitle = stringResource(R.string.settings_immersive_lyrics_subtitle),
                                    checked = uiState.immersiveLyricsEnabled,
                                    onCheckedChange = { settingsViewModel.setImmersiveLyricsEnabled(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_lyrics_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )

                                if (uiState.immersiveLyricsEnabled) {
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_auto_hide_delay_title),
                                        description = stringResource(R.string.settings_auto_hide_delay_subtitle),
                                        options = mapOf(
                                            "3000" to stringResource(R.string.settings_auto_hide_delay_3s),
                                            "4000" to stringResource(R.string.settings_auto_hide_delay_4s),
                                            "5000" to stringResource(R.string.settings_auto_hide_delay_5s),
                                            "6000" to stringResource(R.string.settings_auto_hide_delay_6s)
                                        ),
                                        selectedKey = uiState.immersiveLyricsTimeout.toString(),
                                        onSelectionChanged = { settingsViewModel.setImmersiveLyricsTimeout(it.toLong()) },
                                        leadingIcon = { Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.secondary) }
                                    )
                                }

                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_exp_animated_lyrics_title),
                                    subtitle = stringResource(R.string.settings_exp_animated_lyrics_subtitle),
                                    checked = uiState.useAnimatedLyrics,
                                    onCheckedChange = { settingsViewModel.setUseAnimatedLyrics(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_lyrics_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )

                                if (uiState.useAnimatedLyrics) {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.settings_exp_lyrics_blur_title),
                                        subtitle = stringResource(R.string.settings_exp_lyrics_blur_subtitle),
                                        checked = uiState.animatedLyricsBlurEnabled,
                                        onCheckedChange = { settingsViewModel.setAnimatedLyricsBlurEnabled(it) },
                                        leadingIcon = { Icon(Icons.Rounded.BlurOff, null, tint = MaterialTheme.colorScheme.secondary) }
                                    )

                                    if (uiState.animatedLyricsBlurEnabled) {
                                        SliderSettingsItem(
                                            label = stringResource(R.string.settings_exp_lyrics_blur_strength_title),
                                            value = uiState.animatedLyricsBlurStrength,
                                            valueRange = 1f..4f,
                                            steps = 2,
                                            onValueChange = { settingsViewModel.setAnimatedLyricsBlurStrength(it) },
                                            valueText = { value -> "${"%.1f".format(value)}x" }
                                        )
                                    }
                                }
                            }

                            SettingsSubsection(
                                title = stringResource(R.string.settings_app_navigation_section),
                                addBottomSpace = false
                            ) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_default_tab_title),
                                    description = stringResource(R.string.settings_default_tab_subtitle),
                                    options = mapOf(
                                        LaunchTab.HOME to stringResource(R.string.settings_default_tab_home),
                                        LaunchTab.SEARCH to stringResource(R.string.common_search),
                                        LaunchTab.LIBRARY to stringResource(R.string.settings_default_tab_library),
                                    ),
                                    selectedKey = uiState.launchTab,
                                    onSelectionChanged = { settingsViewModel.setLaunchTab(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.tab_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_library_navigation_title),
                                    description = stringResource(R.string.settings_library_navigation_subtitle),
                                    options = mapOf(
                                        LibraryNavigationMode.TAB_ROW to stringResource(R.string.settings_library_nav_tab_row),
                                        LibraryNavigationMode.COMPACT_PILL to stringResource(R.string.settings_library_nav_compact_pill)
                                    ),
                                    selectedKey = uiState.libraryNavigationMode,
                                    onSelectionChanged = { settingsViewModel.setLibraryNavigationMode(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_library_music_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }
                        }
                        SettingsCategory.PLAYBACK -> {
                            SettingsSubsection(title = stringResource(R.string.settings_background_playback_section)) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_keep_playing_title),
                                    description = stringResource(R.string.settings_keep_playing_subtitle),
                                    options = mapOf("true" to stringResource(R.string.settings_label_on), "false" to stringResource(R.string.settings_label_off)),
                                    selectedKey = if (uiState.keepPlayingInBackground) "true" else "false",
                                    onSelectionChanged = { settingsViewModel.setKeepPlayingInBackground(it.toBoolean()) },
                                    leadingIcon = { Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SettingsItem(
                                    title = stringResource(R.string.settings_battery_optimization_title),
                                    subtitle = stringResource(R.string.settings_battery_optimization_subtitle),
                                    onClick = {
                                        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                                        if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                            Toast.makeText(context, toastBatteryAlreadyDisabled, Toast.LENGTH_SHORT).show()
                                            return@SettingsItem
                                        }
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = "package:${context.packageName}".toUri()
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                context.startActivity(fallbackIntent)
                                            } catch (e2: Exception) {
                                                Toast.makeText(context, toastBatterySettingsUnavailable, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_all_inclusive_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_replaygain_section)) {
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_replaygain_enable_title),
                                    subtitle = stringResource(R.string.settings_replaygain_enable_subtitle),
                                    checked = uiState.replayGainEnabled,
                                    onCheckedChange = { settingsViewModel.setReplayGainEnabled(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_volume_down_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                AnimatedVisibility(
                                    visible = uiState.replayGainEnabled,
                                    enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(animationSpec = spring(stiffness = 400f)),
                                    exit = shrinkVertically(animationSpec = spring(stiffness = 500f)) + fadeOut(animationSpec = spring(stiffness = 500f))
                                ) {
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_gain_mode_title),
                                        description = stringResource(R.string.settings_gain_mode_subtitle),
                                        options = mapOf("track" to stringResource(R.string.settings_gain_mode_track), "album" to stringResource(R.string.settings_gain_mode_album)),
                                        selectedKey = if (uiState.replayGainUseAlbumGain) "album" else "track",
                                        onSelectionChanged = { settingsViewModel.setReplayGainUseAlbumGain(it == "album") },
                                        leadingIcon = { Icon(painterResource(R.drawable.rounded_volume_down_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                    )
                                }
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_cast_section)) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_cast_autoplay_title),
                                    description = stringResource(R.string.settings_cast_autoplay_subtitle),
                                    options = mapOf("false" to stringResource(R.string.settings_label_enabled), "true" to stringResource(R.string.settings_label_disabled)),
                                    selectedKey = if (uiState.disableCastAutoplay) "true" else "false",
                                    onSelectionChanged = { settingsViewModel.setDisableCastAutoplay(it.toBoolean()) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_cast_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_volume_section)) {
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_pause_on_volume_zero),
                                    subtitle = stringResource(R.string.settings_pause_on_volume_zero_desc),
                                    checked = uiState.pauseOnVolumeZero,
                                    onCheckedChange = { settingsViewModel.setPauseOnVolumeZero(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_volume_down_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_headphones_section)) {
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_headphones_resume_title),
                                    subtitle = stringResource(R.string.settings_headphones_resume_subtitle),
                                    checked = uiState.resumeOnHeadsetReconnect,
                                    onCheckedChange = { settingsViewModel.setResumeOnHeadsetReconnect(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_headphones_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                            SettingsSubsection(title = stringResource(R.string.settings_queue_transitions_section)) {
                                ThemeSelectorItem(
                                    label = stringResource(R.string.settings_crossfade_title),
                                    description = stringResource(R.string.settings_crossfade_subtitle),
                                    options = mapOf("true" to stringResource(R.string.settings_label_enabled), "false" to stringResource(R.string.settings_label_disabled)),
                                    selectedKey = if (uiState.isCrossfadeEnabled) "true" else "false",
                                    onSelectionChanged = { settingsViewModel.setCrossfadeEnabled(it.toBoolean()) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_align_justify_space_even_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                if (uiState.isCrossfadeEnabled) {
                                    SliderSettingsItem(
                                        label = stringResource(R.string.settings_crossfade_duration_title),
                                        value = uiState.crossfadeDuration.toFloat(),
                                        valueRange = 1000f..12000f,
                                        steps= 10,
                                        onValueChange = { settingsViewModel.setCrossfadeDuration(it.toInt()) },
                                        valueText = { value -> "${(value / 1000).toInt()}s" }
                                    )
                                }
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_hifi_mode_title),
                                    subtitle = if (uiState.hiFiModeDeviceSupported)
                                        stringResource(R.string.settings_hifi_mode_supported_subtitle)
                                    else
                                        stringResource(R.string.settings_hifi_mode_unsupported_subtitle),
                                    checked = uiState.hiFiModeEnabled,
                                    onCheckedChange = { settingsViewModel.setHiFiModeEnabled(it) },
                                    enabled = uiState.hiFiModeDeviceSupported,
                                    leadingIcon = { Icon(painterResource(R.drawable.outline_high_quality_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_persistent_shuffle_title),
                                    subtitle = stringResource(R.string.settings_persistent_shuffle_subtitle),
                                    checked = uiState.persistentShuffleEnabled,
                                    onCheckedChange = { settingsViewModel.setPersistentShuffleEnabled(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_shuffle_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_show_queue_history_title),
                                    subtitle = stringResource(R.string.settings_show_queue_history_subtitle),
                                    checked = uiState.showQueueHistory,
                                    onCheckedChange = { settingsViewModel.setShowQueueHistory(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_queue_music_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }

                        }
                        SettingsCategory.BEHAVIOR -> {
                            SettingsSubsection(
                                title = stringResource(R.string.settings_folders_section)
                            ) {
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_folder_back_gesture_title),
                                    subtitle = stringResource(R.string.settings_folder_back_gesture_subtitle),
                                    checked = uiState.folderBackGestureNavigation,
                                    onCheckedChange = { settingsViewModel.setFolderBackGestureNavigation(it) },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.rounded_touch_app_24),
                                            null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                )
                            }
                            SettingsSubsection(
                                title = stringResource(R.string.settings_player_gestures_section)
                            ) {
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_tap_bg_closes_title),
                                    subtitle = stringResource(R.string.settings_tap_bg_closes_subtitle),
                                    checked = uiState.tapBackgroundClosesPlayer,
                                    onCheckedChange = { settingsViewModel.setTapBackgroundClosesPlayer(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_touch_app_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                            }
                            SettingsSubsection(
                                title = stringResource(R.string.settings_haptics_section),
                                addBottomSpace = false
                            ) {
                                SwitchSettingItem(
                                    title = stringResource(R.string.settings_haptic_feedback_title),
                                    subtitle = stringResource(R.string.settings_haptic_feedback_subtitle),
                                    checked = uiState.hapticsEnabled,
                                    onCheckedChange = { settingsViewModel.setHapticsEnabled(it) },
                                    leadingIcon = { Icon(painterResource(R.drawable.rounded_touch_app_24), null, tint = MaterialTheme.colorScheme.secondary) }
                                )
                                HapticIntensitySettings(
                                    intensity = uiState.hapticIntensity,
                                    enabled = uiState.hapticsEnabled,
                                    onSelect = settingsViewModel::setHapticIntensity
                                )
                            }
                        }
                        SettingsCategory.ABOUT -> {
                            // About has its own screen
                        }
                        SettingsCategory.EQUALIZER -> {
                            // Equalizer has its own screen, so this block is unreachable via standard navigation
                            // but required for exhaustiveness.
                        }
                        SettingsCategory.DEVICE_CAPABILITIES -> {
                            // Device Capabilities has its own screen
                        }

                    }
               }
            }

            item {
                // Spacer handled by contentPadding
                Spacer(Modifier.height(1.dp))
            }
        }

        CollapsibleCommonTopBar(
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = onBackClick,
            title = categoryTitle,
            maxLines = titleMaxLines
        )

        // Block interaction during transition
        var isTransitioning by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(com.theveloper.pixelplay.presentation.navigation.TRANSITION_DURATION.toLong())
            isTransitioning = false
        }
        
        if (isTransitioning) {
            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                   awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }
            )
        }
    }

    // Dialogs
    FileExplorerDialog(
        visible = showExplorerSheet,
        currentPath = currentPath,
        directoryChildren = directoryChildren,
        availableStorages = availableStorages,
        selectedStorageIndex = selectedStorageIndex,
        isLoading = isLoadingDirectories,
        isPriming = isExplorerPriming,
        isReady = isExplorerReady,
        isCurrentDirectoryResolved = isCurrentDirectoryResolved,
        isAtRoot = settingsViewModel.isAtRoot(),
        rootDirectory = explorerRoot,
        onNavigateTo = settingsViewModel::loadDirectory,
        onNavigateUp = settingsViewModel::navigateUp,
        onNavigateHome = { settingsViewModel.loadDirectory(explorerRoot) },
        onToggleAllowed = settingsViewModel::toggleDirectoryAllowed,
        onRefresh = settingsViewModel::refreshExplorer,
        onStorageSelected = settingsViewModel::selectStorage,
        onDone = {
            settingsViewModel.applyPendingDirectoryRuleChanges()
            showExplorerSheet = false
        },
        onDismiss = {
            settingsViewModel.applyPendingDirectoryRuleChanges()
            showExplorerSheet = false
        }
    )

    if (showPaletteRegenerateSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isAnyPaletteRegenerateRunning) {
                    showPaletteRegenerateSheet = false
                    paletteSongSearchQuery = ""
                }
            },
            sheetState = paletteRegenerateSheetState
        ) {
            PaletteRegenerateSongSheetContent(
                songs = filteredPaletteSongs,
                isRunning = isPaletteRegenerateRunning,
                searchQuery = paletteSongSearchQuery,
                onSearchQueryChange = { paletteSongSearchQuery = it },
                onClearSearch = { paletteSongSearchQuery = "" },
                onSongClick = { song ->
                    if (isAnyPaletteRegenerateRunning) return@PaletteRegenerateSongSheetContent
                    isPaletteRegenerateRunning = true
                    coroutineScope.launch {
                        val success = playerViewModel.forceRegenerateAlbumPaletteForSong(song)
                        isPaletteRegenerateRunning = false
                        if (success) {
                            showPaletteRegenerateSheet = false
                            paletteSongSearchQuery = ""
                            Toast.makeText(
                                context,
                                dialogPaletteRegeneratedFormat.format(song.title),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                dialogPaletteRegenerateFailedFormat.format(song.title),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }

    if (showRegenerateAllPalettesDialog) {
        AlertDialog(
            icon = {
                Icon(
                    Icons.Outlined.Style,
                    null,
                    tint = if (isPaletteBulkRegenerateRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            },
            title = {
                Text(
                    if (isPaletteBulkRegenerateRunning) {
                        stringResource(R.string.settings_dialog_regenerating_palettes_title)
                    } else {
                        stringResource(R.string.settings_dialog_regenerate_palettes_title)
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isPaletteBulkRegenerateRunning) {
                            stringResource(R.string.settings_dialog_regenerating_palettes_body, paletteBulkTotalCount)
                        } else {
                            stringResource(R.string.settings_dialog_regenerate_palettes_body, paletteRegenerateTargets.size)
                        }
                    )

                    if (isPaletteBulkRegenerateRunning) {
                        val progress = if (paletteBulkTotalCount > 0) {
                            paletteBulkCompletedCount.toFloat() / paletteBulkTotalCount.toFloat()
                        } else {
                            0f
                        }

                        LinearWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Text(
                            text = stringResource(R.string.settings_dialog_palette_progress_format, paletteBulkCompletedCount, paletteBulkTotalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            onDismissRequest = {
                if (!isPaletteBulkRegenerateRunning) {
                    showRegenerateAllPalettesDialog = false
                }
            },
            confirmButton = {
                if (isPaletteBulkRegenerateRunning) {
                    TextButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Text(stringResource(R.string.settings_working_label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    TextButton(
                        onClick = {
                            isPaletteBulkRegenerateRunning = true
                            paletteBulkCompletedCount = 0
                            paletteBulkTotalCount = paletteRegenerateTargets.size

                            coroutineScope.launch {
                                var successCount = 0
                                paletteRegenerateTargets.forEachIndexed { index, song ->
                                    if (playerViewModel.forceRegenerateAlbumPaletteForSong(song)) {
                                        successCount++
                                    }
                                    paletteBulkCompletedCount = index + 1
                                }

                                isPaletteBulkRegenerateRunning = false
                                showRegenerateAllPalettesDialog = false

                                val totalCount = paletteRegenerateTargets.size
                                Toast.makeText(
                                    context,
                                    if (successCount == totalCount) {
                                        toastRegeneratedPalettesAllFormat.format(successCount)
                                    } else {
                                        toastRegeneratedPalettesPartialFormat.format(successCount, totalCount)
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.settings_action_regenerate))
                    }
                }
            },
            dismissButton = {
                if (!isPaletteBulkRegenerateRunning) {
                    TextButton(
                        onClick = { showRegenerateAllPalettesDialog = false }
                    ) {
                        Text(stringResource(R.string.common_cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        )
    }
    
     // Dialogs logic (copied)
    if (showClearLyricsDialog) {
        AlertDialog(
            icon = { Icon(Icons.Outlined.Warning, null) },
            title = { Text(stringResource(R.string.settings_dialog_reset_imported_lyrics_title)) },
            text = { Text(stringResource(R.string.settings_dialog_reset_imported_lyrics_body)) },
            onDismissRequest = { showClearLyricsDialog = false },
            confirmButton = { TextButton(onClick = { showClearLyricsDialog = false; playerViewModel.resetAllLyrics() }) { Text(stringResource(R.string.common_confirm), maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            dismissButton = { TextButton(onClick = { showClearLyricsDialog = false }) { Text(stringResource(R.string.common_cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        )
    }

    
    if (showRebuildDatabaseWarning) {
        val syncIndicatorRebuilding = stringResource(R.string.settings_label_sync_rebuilding)
        val toastRebuildingDatabase = stringResource(R.string.settings_toast_rebuilding_database)
        AlertDialog(
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.settings_dialog_rebuild_database_title)) },
            text = { Text(stringResource(R.string.settings_dialog_rebuild_database_body)) },
            onDismissRequest = { showRebuildDatabaseWarning = false },
            confirmButton = { 
                TextButton(
                    onClick = { 
                        showRebuildDatabaseWarning = false
                        refreshRequested = true
                        syncRequestObservedRunning = false
                        syncIndicatorLabel = syncIndicatorRebuilding
                        Toast.makeText(context, toastRebuildingDatabase, Toast.LENGTH_SHORT).show()
                        settingsViewModel.rebuildDatabase() 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { 
                    Text(stringResource(R.string.settings_action_rebuild)) 
                } 
            },
            dismissButton = { TextButton(onClick = { showRebuildDatabaseWarning = false }) { Text(stringResource(R.string.common_cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        )
    }

    if (showRegenerateDailyMixDialog) {
        val toastDailyMixRegenerationStarted = stringResource(R.string.settings_toast_daily_mix_regeneration_started)
        AlertDialog(
            icon = { Icon(painterResource(R.drawable.rounded_instant_mix_24), null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.settings_dialog_regenerate_daily_mix_title)) },
            text = { Text(stringResource(R.string.settings_dialog_regenerate_daily_mix_body)) },
            onDismissRequest = { showRegenerateDailyMixDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateDailyMixDialog = false
                        playerViewModel.forceUpdateDailyMix()
                        Toast.makeText(context, toastDailyMixRegenerationStarted, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.settings_action_regenerate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = { TextButton(onClick = { showRegenerateDailyMixDialog = false }) { Text(stringResource(R.string.common_cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        )
    }

    if (showRegenerateStatsDialog) {
        val toastStatsRegenerationStarted = stringResource(R.string.settings_toast_stats_regeneration_started)
        AlertDialog(
            icon = { Icon(painterResource(R.drawable.rounded_monitoring_24), null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.settings_dialog_regenerate_stats_title)) },
            text = { Text(stringResource(R.string.settings_dialog_regenerate_stats_body)) },
            onDismissRequest = { showRegenerateStatsDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateStatsDialog = false
                        statsViewModel.resetAllStats()
                        Toast.makeText(context, toastStatsRegenerationStarted, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.settings_action_regenerate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = { TextButton(onClick = { showRegenerateStatsDialog = false }) { Text(stringResource(R.string.common_cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        )
    }
}

@Composable
private fun PaletteRegenerateSongSheetContent(
    songs: List<Song>,
    isRunning: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_force_palette_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_force_palette_partial_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isRunning,
            placeholder = { Text(stringResource(R.string.settings_search_title_artist_album_placeholder)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = onClearSearch,
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Outlined.ClearAll, contentDescription = stringResource(R.string.common_clear_search))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        if (isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.settings_palette_regenerating_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (songs.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_search_no_songs_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(songs, key = { it.id }) { song ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRunning) { onSongClick(song) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.displayArtist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (song.album.isNotBlank()) {
                                Text(
                                    text = song.album,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSubsectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsSubsection(
    title: String,
    addBottomSpace: Boolean = true,
    content: @Composable () -> Unit
) {
    SettingsSubsectionHeader(title)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        content()
    }
    if (addBottomSpace) {
        Spacer(modifier = Modifier.height(10.dp))
    }
}
