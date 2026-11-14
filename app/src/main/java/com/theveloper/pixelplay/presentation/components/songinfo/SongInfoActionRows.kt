package com.theveloper.pixelplay.presentation.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.withFrameNanos


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.subcomps.AutoSizingTextToFill
import com.theveloper.pixelplay.utils.formatDuration
import com.theveloper.pixelplay.utils.shapes.RoundedStarShape
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.theveloper.pixelplay.data.media.CoverArtUpdate
import com.theveloper.pixelplay.ui.theme.MontserratFamily
import com.theveloper.pixelplay.presentation.viewmodel.SongInfoBottomSheetViewModel
import com.theveloper.pixelplay.presentation.viewmodel.SongInfoBottomSheetViewModel.ToneTarget
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.TransformOrigin
import com.theveloper.pixelplay.presentation.screens.TabAnimation
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.utils.AudioMetaUtils
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.theveloper.pixelplay.presentation.components.subcomps.TightWrapText
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState



@Composable
internal fun SongInfoSegmentedListItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    iconDescription: String,
    shape: Shape,
    onClick: (() -> Unit)? = null,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .let { baseModifier ->
            if (onClick != null) {
                baseModifier.clickable(onClick = onClick)
            } else {
                baseModifier
            }
        }

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(headline) },
            supportingContent = { Text(supporting) },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                )
            }
        )
    }
}

@Composable
internal fun Row1Actions(
    songId: String,
    isFavorite: Boolean,
    onPlaySong: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShareClick: () -> Unit,
    playButtonShape: Shape,
    evenCornerRadiusElems: androidx.compose.ui.unit.Dp
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var clickPending by remember { mutableStateOf(false) }

    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    var playVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isPlayPressed) {
        if (isPlayPressed) {
            playVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            playVisualPressed = false
        }
    }

    val shareInteractionSource = remember { MutableInteractionSource() }
    val isSharePressed by shareInteractionSource.collectIsPressedAsState()
    var shareVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isSharePressed) {
        if (isSharePressed) {
            shareVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            shareVisualPressed = false
        }
    }

    val pressSpec = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )

    val pressFractionPlay by animateFloatAsState(
        targetValue = if (playVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "PlayPressFraction"
    )
    val pressFractionShare by animateFloatAsState(
        targetValue = if (shareVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "SharePressFraction"
    )

    val weightPlay = (0.5f + 0.08f * pressFractionPlay - 0.05f * pressFractionShare).coerceAtLeast(0.1f)
    val weightFavorite = (0.25f - 0.04f * pressFractionPlay - 0.03f * pressFractionShare).coerceAtLeast(0.05f)
    val weightShare = (0.25f + 0.08f * pressFractionShare - 0.04f * pressFractionPlay).coerceAtLeast(0.05f)

    val favoriteMotion = com.theveloper.pixelplay.ui.theme.LocalMotionPreferences.current.animationsEnabled
    val favoriteButtonCornerRadius by animateDpAsState(
        targetValue = if (isFavorite) evenCornerRadiusElems else 60.dp,
        animationSpec = if (favoriteMotion) tween(300) else snap(),
        label = "FavoriteCornerAnimation"
    )
    val favoriteButtonContainerColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = if (favoriteMotion) tween(300) else snap(),
        label = "FavoriteContainerColorAnimation"
    )
    val favoriteButtonContentColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = if (favoriteMotion) tween(300) else snap(),
        label = "FavoriteContentColorAnimation"
    )

    val favoriteButtonShape = remember(favoriteButtonCornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = favoriteButtonCornerRadius, smoothnessAsPercentBR = 60, cornerRadiusBR = favoriteButtonCornerRadius,
            smoothnessAsPercentTL = 60, cornerRadiusTL = favoriteButtonCornerRadius, smoothnessAsPercentBL = 60,
            cornerRadiusBL = favoriteButtonCornerRadius, smoothnessAsPercentTR = 60
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            modifier = Modifier
                .weight(weightPlay)
                .heightIn(min = 80.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
            shape = playButtonShape,
            interactionSource = playInteractionSource,
            onClick = {
                if (clickPending) return@FilledTonalButton
                clickPending = true
                playVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row1Actions: Play clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    playVisualPressed = false
                    clickPending = false
                    onPlaySong()
                }
            }
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.song_info_cd_play),
            )
            Spacer(Modifier.width(6.dp))
            TightWrapText(
                text = stringResource(R.string.song_info_action_play),
                modifier = Modifier.padding(end = 4.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                lineHeight = 22.sp,
                style = MaterialTheme.typography.titleLarge
            )
        }

        FavoriteButton(
            songId = songId,
            isFavorite = isFavorite,
            onToggle = onToggleFavorite,
            modifier = Modifier.weight(weightFavorite).fillMaxHeight().zIndex(1f),
            shape = favoriteButtonShape,
            iconSize = FloatingActionButtonDefaults.LargeIconSize,
            containerColor = favoriteButtonContainerColor,
            contentColor = favoriteButtonContentColor,
            style = FavoriteButtonStyle.Full
        )

        FilledTonalIconButton(
            modifier = Modifier
                .weight(weightShare)
                .fillMaxHeight(),
            onClick = {
                if (clickPending) return@FilledTonalIconButton
                clickPending = true
                shareVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row1Actions: Share clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    shareVisualPressed = false
                    clickPending = false
                    onShareClick()
                }
            },
            shape = CircleShape,
            interactionSource = shareInteractionSource
        ) {
            Icon(
                modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.song_info_cd_share_song_file)
            )
        }
    }
}

@Composable
internal fun Row2Actions(
    onAddToQueue: () -> Unit,
    onAddNextToQueue: () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var clickPending by remember { mutableStateOf(false) }

    val queueInteractionSource = remember { MutableInteractionSource() }
    val isQueuePressed by queueInteractionSource.collectIsPressedAsState()
    var queueVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isQueuePressed) {
        if (isQueuePressed) {
            queueVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            queueVisualPressed = false
        }
    }

    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()
    var nextVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isNextPressed) {
        if (isNextPressed) {
            nextVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            nextVisualPressed = false
        }
    }

    val pressSpec = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )

    val pressFractionQueue by animateFloatAsState(
        targetValue = if (queueVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "QueuePressFraction"
    )
    val pressFractionNext by animateFloatAsState(
        targetValue = if (nextVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "NextPressFraction"
    )

    val weightQueue = (0.6f + 0.08f * pressFractionQueue - 0.08f * pressFractionNext).coerceAtLeast(0.1f)
    val weightNext = (0.4f + 0.08f * pressFractionNext - 0.08f * pressFractionQueue).coerceAtLeast(0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            modifier = Modifier
                .weight(weightQueue)
                .heightIn(min = 66.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
            shape = CircleShape,
            interactionSource = queueInteractionSource,
            onClick = {
                if (clickPending) return@FilledTonalButton
                clickPending = true
                queueVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row2Actions: AddToQueue clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    queueVisualPressed = false
                    clickPending = false
                    onAddToQueue()
                }
            }
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = stringResource(R.string.song_info_cd_add_to_queue),
            )
            Spacer(Modifier.width(6.dp))
            TightWrapText(
                text = stringResource(R.string.song_info_action_add_to_queue),
                modifier = Modifier.padding(end = 4.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                lineHeight = 20.sp
            )
        }

        FilledTonalButton(
            modifier = Modifier
                .weight(weightNext)
                .heightIn(min = 66.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
            shape = CircleShape,
            interactionSource = nextInteractionSource,
            onClick = {
                if (clickPending) return@FilledTonalButton
                clickPending = true
                nextVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row2Actions: AddNextToQueue clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    nextVisualPressed = false
                    clickPending = false
                    onAddNextToQueue()
                }
            }
        ) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(R.string.song_info_cd_queue_next)
            )
            Spacer(Modifier.width(6.dp))
            TightWrapText(
                text = stringResource(R.string.song_info_action_queue_next),
                modifier = Modifier.padding(end = 4.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
internal fun Row3Actions(
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var clickPending by remember { mutableStateOf(false) }

    val playlistInteractionSource = remember { MutableInteractionSource() }
    val isPlaylistPressed by playlistInteractionSource.collectIsPressedAsState()
    var playlistVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaylistPressed) {
        if (isPlaylistPressed) {
            playlistVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            playlistVisualPressed = false
        }
    }

    val deleteInteractionSource = remember { MutableInteractionSource() }
    val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
    var deleteVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isDeletePressed) {
        if (isDeletePressed) {
            deleteVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            deleteVisualPressed = false
        }
    }

    val pressSpec = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )

    val pressFractionPlaylist by animateFloatAsState(
        targetValue = if (playlistVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "PlaylistPressFraction"
    )
    val pressFractionDelete by animateFloatAsState(
        targetValue = if (deleteVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "DeletePressFraction"
    )

    val weightPlaylist = (0.5f + 0.08f * pressFractionPlaylist - 0.08f * pressFractionDelete).coerceAtLeast(0.1f)
    val weightDelete = (0.5f + 0.08f * pressFractionDelete - 0.08f * pressFractionPlaylist).coerceAtLeast(0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            modifier = Modifier
                .weight(weightPlaylist)
                .heightIn(min = 66.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
            shape = CircleShape,
            interactionSource = playlistInteractionSource,
            onClick = {
                if (clickPending) return@FilledTonalButton
                clickPending = true
                playlistVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row3Actions: AddToPlaylist clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    playlistVisualPressed = false
                    clickPending = false
                    onAddToPlaylist()
                }
            }
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = stringResource(R.string.song_info_cd_add_to_playlist)
            )
            Spacer(Modifier.width(6.dp))
            TightWrapText(
                text = stringResource(R.string.common_playlist),
                modifier = Modifier.padding(end = 4.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                lineHeight = 20.sp
            )
        }

        FilledTonalButton(
            modifier = Modifier
                .weight(weightDelete)
                .heightIn(min = 66.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
            shape = CircleShape,
            interactionSource = deleteInteractionSource,
            onClick = {
                if (clickPending) return@FilledTonalButton
                clickPending = true
                deleteVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row3Actions: Delete clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    deleteVisualPressed = false
                    clickPending = false
                    onDelete()
                }
            }
        ) {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = stringResource(R.string.song_info_action_delete)
            )
            Spacer(Modifier.width(6.dp))
            TightWrapText(
                text = stringResource(R.string.song_info_action_delete),
                modifier = Modifier.padding(end = 4.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                lineHeight = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Row4Actions(
    isPixelPlayWatchAvailable: Boolean,
    isSendingToWatch: Boolean,
    shouldOfferWatchTransfer: Boolean,
    shouldShowWatchTransferLoading: Boolean,
    currentSongTransferPercent: Int,
    currentSongTransferStatus: String?,
    currentSongTransferTotalBytes: Long,
    onRingtoneClick: () -> Unit,
    onSendToWatchClick: () -> Unit,
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var clickPending by remember { mutableStateOf(false) }

    val ringtoneInteractionSource = remember { MutableInteractionSource() }
    val isRingtonePressed by ringtoneInteractionSource.collectIsPressedAsState()
    var ringtoneVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isRingtonePressed) {
        if (isRingtonePressed) {
            ringtoneVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            ringtoneVisualPressed = false
        }
    }

    val watchInteractionSource = remember { MutableInteractionSource() }
    val isWatchPressed by watchInteractionSource.collectIsPressedAsState()
    var watchVisualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isWatchPressed) {
        if (isWatchPressed) {
            watchVisualPressed = true
        } else {
            kotlinx.coroutines.delay(180)
            watchVisualPressed = false
        }
    }

    val pressSpec = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )

    val pressFractionRingtone by animateFloatAsState(
        targetValue = if (ringtoneVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "RingtonePressFraction"
    )
    val pressFractionWatch by animateFloatAsState(
        targetValue = if (watchVisualPressed) 1f else 0f,
        animationSpec = pressSpec,
        label = "WatchPressFraction"
    )

    val weightRingtone = (0.38f + 0.08f * pressFractionRingtone - 0.08f * pressFractionWatch).coerceAtLeast(0.1f)
    val weightWatch = (0.62f + 0.08f * pressFractionWatch - 0.08f * pressFractionRingtone).coerceAtLeast(0.1f)

    val sendToWatchContainerColor by animateColorAsState(
        targetValue = if (isSendingToWatch) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(durationMillis = 250),
        label = "SendToWatchContainerColorAnimation"
    )
    val sendToWatchContentColor by animateColorAsState(
        targetValue = if (isSendingToWatch) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(durationMillis = 250),
        label = "SendToWatchContentColorAnimation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RingtoneActionButton(
            modifier = Modifier
                .weight(weightRingtone)
                .fillMaxHeight(),
            showText = true,
            compactText = true,
            interactionSource = ringtoneInteractionSource,
            onClick = {
                if (clickPending) return@RingtoneActionButton
                clickPending = true
                ringtoneVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row4Actions: Ringtone clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    ringtoneVisualPressed = false
                    clickPending = false
                    onRingtoneClick()
                }
            },
        )

        FilledTonalButton(
            modifier = Modifier
                .weight(weightWatch)
                .fillMaxHeight(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isPixelPlayWatchAvailable) {
                    sendToWatchContainerColor
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isPixelPlayWatchAvailable) {
                    sendToWatchContentColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = CircleShape,
            enabled = shouldOfferWatchTransfer && !isSendingToWatch,
            interactionSource = watchInteractionSource,
            onClick = {
                if (clickPending) return@FilledTonalButton
                clickPending = true
                watchVisualPressed = true
                android.util.Log.d("PixelPlayerDebug", "Row4Actions: SendToWatch clicked")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(180)
                    watchVisualPressed = false
                    clickPending = false
                    onSendToWatchClick()
                }
            }
        ) {
            if (shouldShowWatchTransferLoading) {
                LoadingIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.song_info_checking_watch),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (isSendingToWatch) {
                LoadingIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when {
                        currentSongTransferStatus == com.theveloper.pixelplay.shared.WearTransferProgress.STATUS_TRANSFERRING && currentSongTransferTotalBytes > 0L ->
                            stringResource(
                                R.string.song_info_transferring_percent,
                                currentSongTransferPercent
                            )
                        currentSongTransferStatus == com.theveloper.pixelplay.shared.WearTransferProgress.STATUS_TRANSFERRING ->
                            stringResource(R.string.song_info_transferring_to_watch)
                        else ->
                            stringResource(R.string.song_info_transfer_in_progress)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.rounded_watch_arrow_down_24),
                    contentDescription = stringResource(
                        if (isPixelPlayWatchAvailable) {
                            R.string.song_info_cd_send_to_watch
                        } else {
                            R.string.song_info_watch_unavailable
                        }
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (isPixelPlayWatchAvailable) {
                            R.string.song_info_send_to_watch
                        } else {
                            R.string.song_info_watch_unavailable
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
