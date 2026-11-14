package com.theveloper.pixelplay.presentation.components

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.Intent
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.theveloper.pixelplay.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.media.MediaRouter
import com.theveloper.pixelplay.presentation.screens.TabAnimation
import com.theveloper.pixelplay.presentation.viewmodel.BluetoothAudioDeviceState
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import com.theveloper.pixelplay.utils.shapes.RoundedStarShape
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


@Composable
internal fun CastSheetContent(
    state: CastSheetUiState,
    onSelectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTurnOnWifi: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefresh: () -> Unit,
    startWithControls: Boolean = true
) {
    val allConnectivityOff = !state.wifiEnabled && !state.isBluetoothEnabled
    val configuration = LocalConfiguration.current
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val maxPagerHeight = (
        configuration.screenHeightDp.dp -
            safeInsets.calculateTopPadding() -
            safeInsets.calculateBottomPadding() -
            212.dp
        ).coerceAtLeast(280.dp)
    val pagerState = rememberPagerState(
        initialPage = if (startWithControls) 0 else 1,
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()
    var heightAnimationEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        heightAnimationEnabled = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = CircleShape
                )
            ) {
                Text(
                    modifier = Modifier.padding(start = 6.dp, end = 8.dp),
                    text = stringResource(R.string.cast_title_connect),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            AnimatedVisibility(
                visible = state.isScanning,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(160)),
                label = "tabScanningIndicator"
            ) {
                BadgeChip(
                    text = stringResource(R.string.cast_scanning_nearby),
                    iconVector = Icons.Filled.Refresh,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxPagerHeight)
                .animateContentSize(
                    animationSpec = if (heightAnimationEnabled) {
                        tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    } else {
                        snap()
                    },
                    alignment = Alignment.TopCenter
                )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) { page ->
                when (page) {
                    0 -> CastControlsTabContent(
                        state = state,
                        allConnectivityOff = allConnectivityOff,
                        onDisconnect = onDisconnect,
                        onVolumeChange = onVolumeChange,
                        onTurnOnWifi = onTurnOnWifi,
                        onOpenBluetoothSettings = onOpenBluetoothSettings,
                        onRefresh = onRefresh,
                        bottomSpacing = 20.dp,
                    )
                    1 -> CastDevicesTabContent(
                        state = state,
                        allConnectivityOff = allConnectivityOff,
                        onSelectDevice = onSelectDevice,
                        onDisconnect = onDisconnect,
                        onTurnOnWifi = onTurnOnWifi,
                        onOpenBluetoothSettings = onOpenBluetoothSettings,
                        onRefresh = onRefresh,
                        maxContentHeight = maxPagerHeight,
                    )
                }
            }
        }

        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(5.dp),
            containerColor = Color.Transparent,
            divider = {},
            indicator = {}
        ) {
            TabAnimation(
                index = 0,
                title = stringResource(R.string.cast_tab_controls),
                selectedIndex = pagerState.currentPage,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                transformOrigin = TransformOrigin(0f, 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Speaker,
                        contentDescription = stringResource(R.string.cast_tab_controls),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "CONTROLS",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TabAnimation(
                index = 1,
                title = stringResource(R.string.cast_tab_devices),
                selectedIndex = pagerState.currentPage,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                transformOrigin = TransformOrigin(1f, 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Devices,
                        contentDescription = stringResource(R.string.cast_tab_devices),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "DEVICES",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun CastControlsTabContent(
    state: CastSheetUiState,
    allConnectivityOff: Boolean,
    onDisconnect: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTurnOnWifi: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefresh: () -> Unit,
    bottomSpacing: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ActiveDeviceHero(
            device = state.activeDevice,
            onDisconnect = onDisconnect,
            onVolumeChange = onVolumeChange
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.cast_connectivity),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (allConnectivityOff) {
                            stringResource(R.string.cast_turn_on_wifi_bt)
                        } else {
                            "Manage active radios and rescan"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cast_cd_refresh_connections))
                }
            }
            QuickSettingsRow(
                wifiOn = state.wifiRadioOn,
                wifiConnected = state.wifiEnabled,
                wifiSsid = state.wifiSsid,
                onWifiClick = onTurnOnWifi,
                bluetoothEnabled = state.isBluetoothEnabled,
                bluetoothName = state.bluetoothName,
                onBluetoothClick = onOpenBluetoothSettings
            )
        }
        Spacer(modifier = Modifier.height(bottomSpacing))
    }
}

@Composable
internal fun CastDevicesTabContent(
    state: CastSheetUiState,
    allConnectivityOff: Boolean,
    onSelectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onTurnOnWifi: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefresh: () -> Unit,
    maxContentHeight: Dp,
) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxContentHeight)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        if (!allConnectivityOff) {
            item(key = "deviceSectionHeader") {
                DeviceSectionHeader(
                    modifier = Modifier.fillMaxWidth(),
                    hasDevices = state.devices.isNotEmpty(),
                        onRefresh = onRefresh
                )
            }

            item(key = "refreshIndicator") {
                AnimatedVisibility(
                    visible = state.isRefreshing,
                    enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(180)),
                    label = "refreshIndicator"
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp)),
                        color = colors.primary,
                        trackColor = colors.primary.copy(alpha = 0.12f)
                    )
                }
            }
        }

        if (state.isScanning && state.devices.isEmpty()) {
            item(key = "scanningPlaceholder") {
                ScanningPlaceholderList()
            }
        } else if (state.devices.isEmpty()) {
            item(key = "emptyDevices") {
                EmptyDeviceState()
            }
        } else {
            items(state.devices, key = { it.id }) { device ->
                CastDeviceRow(
                    device = device,
                    onSelect = { onSelectDevice(device.id) },
                    onDisconnect = onDisconnect
                )
            }
        }
    }
}

@Composable
internal fun CastSheetContainer(
    onDismiss: () -> Unit,
    onExpansionChanged: (Float) -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    val hiddenOffsetPx = remember { mutableFloatStateOf(0f) }
    val sheetOffset = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    var pendingOpenAnimation by remember { mutableStateOf(false) }
    var dragOffsetCache by remember { mutableStateOf<Float?>(null) }
    var pendingDragTarget by remember { mutableStateOf<Float?>(null) }
    var dragSnapJob by remember { mutableStateOf<Job?>(null) }

    fun resetDragPipeline() {
        dragOffsetCache = null
        pendingDragTarget = null
        dragSnapJob?.cancel()
        dragSnapJob = null
    }

    fun launchDragSnapLoopIfNeeded() {
        if (dragSnapJob?.isActive == true) return
        dragSnapJob = scope.launch {
            while (isActive) {
                val target = pendingDragTarget ?: break
                pendingDragTarget = null
                sheetOffset.snapTo(target)
                withFrameNanos { }
            }
            dragSnapJob = null
        }
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.45f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "scrimAlpha"
    )

    LaunchedEffect(sheetHeightPx) {
        if (sheetHeightPx == 0f) return@LaunchedEffect
        hiddenOffsetPx.floatValue = sheetHeightPx

        if (!isVisible) {
            sheetOffset.snapTo(sheetHeightPx)
            // Once we are snapped to the hidden position, make content visible (alpha 1)
            // so we can see it slide in.
            contentAlpha.snapTo(1f)
            isVisible = true
            onExpansionChanged(1f)
            pendingOpenAnimation = true
        }

        if (!isDismissing && sheetOffset.value > 0.5f) {
            resetDragPipeline()
            if (pendingOpenAnimation) {
                withFrameNanos { }
                pendingOpenAnimation = false
            }
            val hidden = hiddenOffsetPx.floatValue
            val travelFraction = if (hidden > 0f) {
                (abs(sheetOffset.value) / hidden).coerceIn(0f, 1f)
            } else {
                1f
            }
            val durationMillis = (220f + (130f * travelFraction)).toInt()
            sheetOffset.animateTo(0f, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
        }
    }

    suspend fun animateToRest() {
        val hidden = hiddenOffsetPx.floatValue
        val travelFraction = if (hidden > 0f) {
            (abs(sheetOffset.value) / hidden).coerceIn(0f, 1f)
        } else {
            1f
        }
        val durationMillis = (170f + (120f * travelFraction)).toInt()
        sheetOffset.animateTo(0f, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
    }

    fun dismissSheet(velocity: Float = 0f) {
        if (isDismissing) return
        isDismissing = true
        resetDragPipeline()
        val targetOffset = when {
            hiddenOffsetPx.floatValue > 0f -> hiddenOffsetPx.floatValue
            sheetHeightPx > 0f -> sheetHeightPx
            else -> sheetOffset.value + 1f // Ensure a movement path exists
        }
        scope.launch {
            isVisible = false
            onExpansionChanged(0f)
            try {
                sheetOffset.animateTo(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    initialVelocity = velocity
                )
            } finally {
                onDismiss()
            }
        }
    }

    val dragThreshold = with(density) { 72.dp.toPx() }

    // Shared logic for manual dragging (from header or nested scroll)
    fun onDrag(dragAmount: Float) {
        if (isDismissing) return
        val current = dragOffsetCache ?: sheetOffset.value
        val target = (current + dragAmount).coerceIn(0f, hiddenOffsetPx.floatValue)
        dragOffsetCache = target
        pendingDragTarget = target
        launchDragSnapLoopIfNeeded()
    }

    fun onDragEnd(velocity: Float) {
        if (isDismissing) return
        val settledOffset = pendingDragTarget ?: dragOffsetCache ?: sheetOffset.value
        resetDragPipeline()
        if (settledOffset > dragThreshold || velocity > 1400f) {
            dismissSheet(velocity)
        } else {
            scope.launch { animateToRest() }
        }
    }

    // Drag modifier for non-scrollable areas (e.g. Header)
    val sheetDragModifier = Modifier.pointerInput(dragThreshold, hiddenOffsetPx.floatValue) {
        val velocityTracker = VelocityTracker()
        detectVerticalDragGestures(
            onDragStart = { velocityTracker.resetTracking() },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                onDrag(dragAmount)
            },
            onDragEnd = {
                val velocity = velocityTracker.calculateVelocity().y
                onDragEnd(velocity)
            },
            onDragCancel = {
                resetDragPipeline()
                scope.launch { animateToRest() }
            }
        )
    }

    // Nested scroll connection for the list area
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (sheetOffset.value > 0f) {
                    // Sheet is moving (dragging up/down while partially open).
                    // We consume all vertical delta to move the sheet.
                    val delta = available.y
                    // Dragging up (delta < 0) reduces offset (moves sheet up towards 0).
                    // Dragging down (delta > 0) increases offset (moves sheet down).
                    // Logic in onDrag handles addition correctly.
                    onDrag(delta)
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                // If list reached top and user drags down (available.y > 0)
                if (available.y > 0f) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sheetOffset.value > 0f) {
                    onDragEnd(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    BackHandler(enabled = isVisible && !isDismissing) { dismissSheet() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { dismissSheet() }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                .onGloballyPositioned {
                    val height = it.size.height.toFloat()
                    if (height != sheetHeightPx) sheetHeightPx = height
                }
                // Transform-only motion keeps frame budget predictable during open/close animations.
                .graphicsLayer {
                    translationY = sheetOffset.value
                    alpha = contentAlpha.value
                }
                .then(sheetDragModifier)
                .nestedScroll(nestedScrollConnection),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 12.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(modifier = Modifier.padding(bottom = 18.dp)) {
                content()
            }
        }
    }

    // Cast overlay fraction is handled at state-level to avoid frame-by-frame parent recomposition.
}

@Composable
internal fun CollapsibleCastTopBar(
    modifier: Modifier = Modifier,
    collapseFraction: Float,
    isScanning: Boolean,
    wifiOn: Boolean,
    wifiConnected: Boolean,
    wifiSsid: String?,
    onWifiClick: () -> Unit,
    isBluetoothEnabled: Boolean,
    bluetoothName: String?,
    onBluetoothClick: () -> Unit,
    maxHeight: Dp
) {
    val contentAlpha by animateFloatAsState(
        targetValue = 1f - collapseFraction,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "topBarAlpha"
    )
    val translationYOffset by animateDpAsState(
        targetValue = (-12).dp * collapseFraction,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "topBarTranslation"
    )
    val collapsedTitleAlpha by animateFloatAsState(
        targetValue = collapseFraction,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "collapsedTitle"
    )

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .heightIn(min = 0.dp, max = maxHeight)
            .clipToBounds()
    ) {
        //Ch
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .padding(bottom = 20.dp, start = 4.dp)
//                .graphicsLayer{
//                    alpha = (collapsedTitleAlpha)
//                }
//                .background(
//                    color = MaterialTheme.colorScheme.surfaceContainerLow,
//                    shape = CircleShape
//                )
//        ) {
//            Text(
//                text = stringResource(R.string.cast_title_connect),
//                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .padding(horizontal = 10.dp, vertical = 6.dp)
//                    .graphicsLayer { alpha = (collapsedTitleAlpha) },
//                maxLines = 1
//            )
//        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(bottom = 12.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = with(density) { translationYOffset.toPx() }
                },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = CircleShape
                    )
            ) {
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = stringResource(R.string.cast_title_connect),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(160)),
                label = "scanningIndicator"
            ) {
                BadgeChip(
                    text = stringResource(R.string.cast_scanning_nearby),
                    iconVector = Icons.Filled.Refresh,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }

            QuickSettingsRow(
                wifiOn = wifiOn,
                wifiConnected = wifiConnected,
                wifiSsid = wifiSsid,
                onWifiClick = onWifiClick,
                bluetoothEnabled = isBluetoothEnabled,
                bluetoothName = bluetoothName,
                onBluetoothClick = onBluetoothClick
            )
        }
    }
}
