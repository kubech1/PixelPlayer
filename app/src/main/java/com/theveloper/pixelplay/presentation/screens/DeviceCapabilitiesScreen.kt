package com.theveloper.pixelplay.presentation.screens

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.CollapsibleCommonTopBar
import com.theveloper.pixelplay.presentation.components.collapsedTopBarHeight
import com.theveloper.pixelplay.presentation.components.rememberCollapsibleTopBarHeight
import com.theveloper.pixelplay.presentation.components.MiniPlayerHeight
import com.theveloper.pixelplay.presentation.viewmodel.AudioCapabilities
import com.theveloper.pixelplay.presentation.viewmodel.AudioOutputCategory
import com.theveloper.pixelplay.presentation.viewmodel.DeviceCapabilitiesState
import com.theveloper.pixelplay.presentation.viewmodel.DeviceCapabilitiesViewModel
import com.theveloper.pixelplay.presentation.viewmodel.ExoPlayerInfo
import com.theveloper.pixelplay.presentation.viewmodel.FormatSupportInfo
import com.theveloper.pixelplay.presentation.viewmodel.LocalMusicStorageSummary
import com.theveloper.pixelplay.presentation.viewmodel.MemorySummary
import com.theveloper.pixelplay.presentation.viewmodel.PlaybackCompatibilitySummary
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.roundToInt


@OptIn(UnstableApi::class)
@Composable
fun DeviceCapabilitiesScreen(
    navController: NavController,
    viewModel: DeviceCapabilitiesViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAudioRoute()
    }


    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val minTopBarHeight = collapsedTopBarHeight()
    val maxTopBarHeight = 188.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = rememberCollapsibleTopBarHeight(minTopBarHeightPx, maxTopBarHeightPx)
    val collapseFraction by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        derivedStateOf {
            1f - ((topBarHeight.value - minTopBarHeightPx) /
                (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
        }
    }

    val nestedScrollConnection = remember(topBarHeight, minTopBarHeightPx, maxTopBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }

                return if (consumed.roundToInt() != 0) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeight.value != targetValue) {
                coroutineScope.launch { topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium)) }
            }
        }
    }

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = currentTopBarHeightDp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            DeviceCapabilitiesContent(
                state = state,
                lazyListState = lazyListState,
                topPadding = currentTopBarHeightDp,
                onGenerateReport = viewModel::generatePerformanceReport,
                onAdvancedDiagnosticsChange = viewModel::setAdvancedPerformanceDiagnosticsEnabled,
                onMarkLagNow = viewModel::markLagNow,
                modifier = Modifier.fillMaxSize()
            )
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.settings_category_device_capabilities_title),
            collapseFraction = { collapseFraction },
            headerHeight = currentTopBarHeightDp,
            onBackClick = { navController.popBackStack() },
            expandedTitleStartPadding = 20.dp,
            maxLines = 2
        )
    }
}

@Composable
private fun DeviceCapabilitiesContent(
    state: DeviceCapabilitiesState,
    lazyListState: LazyListState,
    topPadding: Dp,
    onGenerateReport: () -> Unit,
    onAdvancedDiagnosticsChange: (Boolean) -> Unit,
    onMarkLagNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            top = topPadding + 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        item {
            PlaybackReadinessCard(
                deviceInfo = state.deviceInfo,
                audioCapabilities = state.audioCapabilities,
                storageSummary = state.storageSummary,
                playbackCompatibility = state.playbackCompatibility
            )
        }

        item { AudioRouteStatusCard(state.audioOutput) }

        state.storageSummary?.let { storage ->
            item {
                LocalMusicStorageCard(storageSummary = storage)
            }
        }

        state.audioCapabilities?.let { audio ->
            item {
                PlaybackPathCard(
                    audioCapabilities = audio,
                    memorySummary = state.memorySummary,
                    exoPlayerInfo = state.exoPlayerInfo
                )
            }
        }

        if (state.formatSupport.isNotEmpty()) {
            item {
                FormatCompatibilityCard(
                    formats = state.formatSupport,
                    playbackCompatibility = state.playbackCompatibility
                )
            }
        }

        state.playbackCompatibility?.let { compatibility ->
            item {
                PlaybackFindingsCard(compatibility = compatibility)
            }
        }

        item {
            DeviceInfoPanel(deviceInfo = state.deviceInfo)
        }

        item {
            PerformanceReportCard(
                report = state.performanceReport,
                isGenerating = state.isGeneratingReport,
                advancedDiagnosticsEnabled = state.advancedDiagnosticsEnabled,
                advancedDiagnosticsExpiresAtEpochMs = state.advancedDiagnosticsExpiresAtEpochMs,
                onGenerate = onGenerateReport,
                onAdvancedDiagnosticsChange = onAdvancedDiagnosticsChange,
                onMarkLagNow = onMarkLagNow
            )
        }
    }
}

@Composable
private fun PerformanceReportCard(
    report: String?,
    isGenerating: Boolean,
    advancedDiagnosticsEnabled: Boolean,
    advancedDiagnosticsExpiresAtEpochMs: Long?,
    onGenerate: () -> Unit,
    onAdvancedDiagnosticsChange: (Boolean) -> Unit,
    onMarkLagNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.settings_devcaps_report_copied)
    val shareTitle = stringResource(R.string.settings_devcaps_report_share_title)
    val lagMarkedMessage = stringResource(R.string.settings_devcaps_advanced_diagnostics_marked)

    CapabilityCard(
        title = stringResource(R.string.settings_devcaps_report_title),
        icon = Icons.Rounded.Assessment,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.settings_devcaps_report_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AdvancedDiagnosticsToggleRow(
            enabled = advancedDiagnosticsEnabled,
            expiresAtEpochMs = advancedDiagnosticsExpiresAtEpochMs,
            onEnabledChange = onAdvancedDiagnosticsChange
        )

        if (advancedDiagnosticsEnabled) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onMarkLagNow()
                    Toast.makeText(context, lagMarkedMessage, Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(stringResource(R.string.settings_devcaps_advanced_diagnostics_mark_lag))
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onGenerate,
            enabled = !isGenerating
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = stringResource(
                        if (report == null) R.string.settings_devcaps_report_generate
                        else R.string.settings_devcaps_report_regenerate
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (report != null) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(report))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_devcaps_report_copy))
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, shareTitle))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_devcaps_report_share))
                }
            }
        }

        if (report != null) {
            Surface(
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = report,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedDiagnosticsToggleRow(
    enabled: Boolean,
    expiresAtEpochMs: Long?,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_devcaps_advanced_diagnostics_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (enabled && expiresAtEpochMs != null) {
                        stringResource(
                            R.string.settings_devcaps_advanced_diagnostics_expires,
                            formatDiagnosticsExpiry(expiresAtEpochMs)
                        )
                    } else {
                        stringResource(R.string.settings_devcaps_advanced_diagnostics_description)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

