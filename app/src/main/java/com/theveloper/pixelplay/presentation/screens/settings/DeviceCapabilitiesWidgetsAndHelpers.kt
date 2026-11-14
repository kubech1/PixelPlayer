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


@Composable
internal fun CapabilityCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    enableTopSpacer: Boolean = false,
    verticalSpacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AbsoluteSmoothCornerShape(28.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIcon(
                    icon = icon,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
            }

            if (enableTopSpacer) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            content()
        }
    }
}

@Composable
internal fun StatusIcon(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun HeroMetricTile(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = 82.dp),
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.76f),
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = 86.dp),
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun FormatSupportTile(
    format: FormatSupportInfo,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        !format.isDecoderAvailable -> MaterialTheme.colorScheme.errorContainer
        format.isHardwareAccelerated -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val statusContentColor = when {
        !format.isDecoderAvailable -> MaterialTheme.colorScheme.onErrorContainer
        format.isHardwareAccelerated -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = 112.dp),
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = format.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = statusColor
                ) {
                    Icon(
                        imageVector = if (format.isDecoderAvailable) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = statusContentColor,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
            Text(
                text = when {
                    !format.isDecoderAvailable -> stringResource(R.string.settings_devcaps_format_unsupported)
                    format.isHardwareAccelerated -> stringResource(R.string.settings_devcaps_format_hardware)
                    else -> stringResource(R.string.settings_devcaps_format_software)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (format.isOffloadSupported) {
                    TonalChip(
                        text = stringResource(R.string.settings_devcaps_format_offload),
                        compact = true
                    )
                }
                if (format.librarySongCount > 0) {
                    TonalChip(
                        text = stringResource(R.string.settings_devcaps_format_library_count, format.librarySongCount),
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProgressReadout(
    label: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.visibleProgress() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
internal fun storagePercentLabel(fraction: Float): String {
    val percent = fraction * 100f
    return when {
        fraction <= 0f -> stringResource(R.string.settings_devcaps_storage_percent, 0)
        percent < 1f -> stringResource(R.string.settings_devcaps_storage_less_than_one_percent)
        else -> stringResource(R.string.settings_devcaps_storage_percent, percent.roundToInt())
    }
}

internal fun Float.visibleProgress(): Float {
    val clamped = coerceIn(0f, 1f)
    return if (clamped > 0f && clamped < 0.01f) 0.01f else clamped
}

internal fun formatDiagnosticsExpiry(epochMs: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(epochMs))

@Composable
internal fun OutputRouteRow(
    name: String,
    category: AudioOutputCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AbsoluteSmoothCornerShape(16.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (category == AudioOutputCategory.BuiltIn) Icons.Rounded.Speaker else Icons.Rounded.Headphones,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = audioOutputCategoryLabel(category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal enum class FindingTone {
    Success,
    Warning,
    Error,
    Info
}

@Composable
internal fun FindingRow(
    icon: ImageVector,
    title: String,
    body: String,
    tone: FindingTone,
    modifier: Modifier = Modifier
) {
    val containerColor = when (tone) {
        FindingTone.Success -> MaterialTheme.colorScheme.primaryContainer
        FindingTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        FindingTone.Error -> MaterialTheme.colorScheme.errorContainer
        FindingTone.Info -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (tone) {
        FindingTone.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        FindingTone.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
        FindingTone.Error -> MaterialTheme.colorScheme.onErrorContainer
        FindingTone.Info -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
internal fun TonalChip(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    compact: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 5.dp else 7.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                )
            }
            Text(
                text = text,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ChipRow(
    chips: List<String>,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    if (chips.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.take(3).forEach {
            TonalChip(text = it)
        }
        if (chips.size > 3) {
            TonalChip(text = stringResource(R.string.settings_devcaps_more_count, chips.size - 3))
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
internal fun yesNo(value: Boolean): String {
    return if (value) {
        stringResource(R.string.settings_devcaps_status_yes)
    } else {
        stringResource(R.string.settings_devcaps_status_no)
    }
}

@Composable
internal fun audioOutputCategoryLabel(category: AudioOutputCategory): String {
    return when (category) {
        AudioOutputCategory.BuiltIn -> stringResource(R.string.settings_devcaps_output_builtin)
        AudioOutputCategory.Bluetooth -> stringResource(R.string.settings_devcaps_output_bluetooth)
        AudioOutputCategory.Usb -> stringResource(R.string.settings_devcaps_output_usb)
        AudioOutputCategory.Wired -> stringResource(R.string.settings_devcaps_output_wired)
        AudioOutputCategory.Cast -> stringResource(R.string.settings_devcaps_output_digital)
        AudioOutputCategory.Other -> stringResource(R.string.settings_devcaps_output_other)
    }
}

@Composable
internal fun localizedDeviceInfoEntries(entries: List<Pair<String, String>>): List<Pair<String, String>> {
    val lManufacturer = stringResource(R.string.settings_devcaps_device_info_manufacturer)
    val lModel = stringResource(R.string.settings_devcaps_device_info_model)
    val lBrand = stringResource(R.string.settings_devcaps_device_info_brand)
    val lDevice = stringResource(R.string.settings_devcaps_device_info_device)
    val lAndroid = stringResource(R.string.settings_devcaps_device_info_android_version)
    val lSdk = stringResource(R.string.settings_devcaps_device_info_sdk_version)
    val lHardware = stringResource(R.string.settings_devcaps_device_info_hardware)

    return entries.map { (key, value) ->
        val label = when (key) {
            "Manufacturer" -> lManufacturer
            "Model" -> lModel
            "Brand" -> lBrand
            "Device" -> lDevice
            "Android Version" -> lAndroid
            "SDK Version" -> lSdk
            "Hardware" -> lHardware
            else -> key
        }
        label to value
    }
}

internal fun orderedDeviceInfoEntries(deviceInfo: Map<String, String>): List<Pair<String, String>> {
    val preferredOrder = listOf(
        "Manufacturer",
        "Model",
        "Brand",
        "Device",
        "Android Version",
        "SDK Version",
        "Hardware"
    )
    val orderedEntries = mutableListOf<Pair<String, String>>()
    val seenKeys = mutableSetOf<String>()

    preferredOrder.forEach { key ->
        deviceInfo[key]?.let { value ->
            orderedEntries += key to value
            seenKeys += key
        }
    }

    deviceInfo.forEach { (key, value) ->
        if (key !in seenKeys) {
            orderedEntries += key to value
        }
    }
    return orderedEntries
}
