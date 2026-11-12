package com.theveloper.pixelplay.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.HomeMix
import com.theveloper.pixelplay.data.model.HomeMixKind
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.subcomps.PlayingEqIcon
import com.theveloper.pixelplay.presentation.model.RecentlyPlayedSongUiModel
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion
import com.theveloper.pixelplay.ui.theme.pillSurface
import com.theveloper.pixelplay.ui.theme.rememberPillSurfaceColors

/** Localized label shared by the Home preview and the mix detail screen. */
@Composable
fun HomeMix.localizedTitle(): String = when (kind) {
    HomeMixKind.DAILY -> stringResource(R.string.home_mix_daily)
    HomeMixKind.UNDERPLAYED -> stringResource(R.string.home_mix_underplayed)
    HomeMixKind.REDISCOVER -> stringResource(R.string.home_mix_rediscover)
    HomeMixKind.ARTIST -> stringResource(R.string.home_mix_artist, subject.orEmpty())
    HomeMixKind.GENRE -> stringResource(R.string.home_mix_genre, subject.orEmpty())
    HomeMixKind.RECENTLY_ADDED -> stringResource(R.string.home_mix_recently_added)
}

@Composable
internal fun HomeMixCarousel(
    mixes: List<HomeMix>,
    onOpen: (HomeMix) -> Unit,
    onPlay: (HomeMix, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeMixSectionTitle(stringResource(R.string.home_explore_library))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = if (LocalDensity.current.fontScale >= 1.5f) {
                (maxWidth * 0.62f).coerceAtMost(196.dp)
            } else {
                (maxWidth * 0.405f).coerceAtMost(176.dp)
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(mixes, key = { it.id }) { mix ->
                    val title = mix.localizedTitle()
                    HomeMixCard(
                        mix = mix,
                        title = title,
                        width = cardWidth,
                        onOpen = { onOpen(mix) },
                        onPlay = { onPlay(mix, title) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMixCard(
    mix: HomeMix,
    title: String,
    width: Dp,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    Box(Modifier.width(width)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpen()
                }
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HomeMixArtwork(mix, Modifier.fillMaxWidth().aspectRatio(1f))
            Text(
                text = title,
                modifier = Modifier.padding(start = 2.dp, top = 3.dp, end = 2.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = GoogleSansRounded),
                color = colors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.home_mix_song_count, mix.songs.size, mix.songs.size),
                modifier = Modifier.padding(horizontal = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onPlay()
            },
            modifier = Modifier.align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = (width - 52.dp).coerceAtLeast(0.dp))
                .size(48.dp),
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(colors.inverseSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    stringResource(R.string.home_mix_play, title),
                    tint = colors.inverseOnSurface,
                )
            }
        }
    }
}

@Composable
fun HomeMixSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(horizontal = 24.dp).semantics { heading() }, fontFamily = GoogleSansRounded,
        fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface)
}

internal data class HomeQuickLink(
    val title: String,
    val route: String,
    @DrawableRes val icon: Int,
    val subtitle: String? = null,
    val artworkUri: String? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeQuickAccess(links: List<HomeQuickLink>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeMixSectionTitle(stringResource(R.string.home_quick_title))
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            if (maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.5f) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    links.forEach { link ->
                        HomeQuickTile(link, { onOpen(link.route) }, Modifier.fillMaxWidth())
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                    links.forEach { link ->
                        HomeQuickTile(link, { onOpen(link.route) }, Modifier.weight(1f).fillMaxRowHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickTile(link: HomeQuickLink, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val colors = rememberPillSurfaceColors()
    Box(modifier.heightIn(min = 56.dp).pillSurface(CircleShape, colors, elevation = 2.dp).clip(CircleShape),
        propagateMinConstraints = true
    ) {
        Row(
            modifier = Modifier.clickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            }.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbnailShape = CircleShape
            val artworkUri = link.artworkUri?.takeIf { it.isNotBlank() }
            if (artworkUri != null) {
                SmartImage(
                    artworkUri, null, Modifier.size(36.dp),
                    shape = thumbnailShape,
                    targetSize = coil.size.Size(108, 108),
                    crossfadeDurationMillis = if (LocalMotionPreferences.current.animationsEnabled) {
                        PixelMotion.QUICK_DURATION_MS
                    } else 0,
                )
            } else {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = thumbnailShape,
                    color = colors.controlColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(link.icon), null, Modifier.size(20.dp),
                            tint = colors.contentColor)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(link.title, style = MaterialTheme.typography.titleSmall, color = colors.contentColor)
                link.subtitle?.let { subtitle ->
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = colors.contentColor.copy(alpha = 0.75f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun HomeRecentlyPlayedSection(
    songs: List<RecentlyPlayedSongUiModel>,
    currentSongId: String?,
    isPlaying: Boolean,
    onPlaySong: (Song) -> Unit,
    onOpenAll: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            HomeMixSectionTitle(stringResource(R.string.home_recently_played), Modifier.weight(1f))
            TextButton(onClick = onOpenAll) {
                Text(stringResource(R.string.home_see_all))
            }
        }
        for (index in 0 until minOf(3, songs.size)) {
            val song = songs[index].song
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                    .clickable(role = Role.Button) { onPlaySong(song) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SmartImage(song.albumArtUriString, null, Modifier.size(48.dp), shape = RoundedCornerShape(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(song.displayArtist, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (currentSongId == song.id) PlayingEqIcon(color = MaterialTheme.colorScheme.primary, isPlaying = isPlaying)
            }
        }
    }
}

@Composable
fun HomeListeningSummaryCard(
    today: String,
    week: String,
    topSong: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_listening_title), modifier = Modifier.weight(1f),
                    fontFamily = GoogleSansRounded, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ListeningMetric(stringResource(R.string.home_listening_today), today, Modifier.weight(1f))
                ListeningMetric(stringResource(R.string.home_listening_week), week, Modifier.weight(1f))
            }
            if (!topSong.isNullOrBlank()) {
                Text(stringResource(R.string.home_listening_top_song, topSong),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ListeningMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, fontFamily = GoogleSansRounded, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
