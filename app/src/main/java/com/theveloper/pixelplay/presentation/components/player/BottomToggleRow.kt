package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.FavoriteButton
import com.theveloper.pixelplay.presentation.components.FavoriteButtonStyle
import com.theveloper.pixelplay.presentation.components.ToggleSegmentButton
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

internal val PlayerControlOuterCorner = 24.dp
internal val PlayerControlInnerCorner = 12.dp
internal val InactiveFavoriteSegmentShape = RoundedCornerShape(
    topStart = PlayerControlInnerCorner, topEnd = PlayerControlOuterCorner,
    bottomEnd = PlayerControlOuterCorner, bottomStart = PlayerControlInnerCorner
)

@Composable
fun BottomToggleRow(
    modifier: Modifier,
    songId: String?,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavoriteProvider: () -> Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    activeColorMain: Color = MaterialTheme.colorScheme.primary,
    activeColorSecondary: Color = MaterialTheme.colorScheme.secondary,
    activeColorTertiary: Color = MaterialTheme.colorScheme.tertiary,
    onActiveColorMain: Color = MaterialTheme.colorScheme.onPrimary,
    onActiveColorSecondary: Color = MaterialTheme.colorScheme.onSecondary,
    onActiveColorTertiary: Color = MaterialTheme.colorScheme.onTertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    val isFavorite = isFavoriteProvider()
    val rowCorners = 60.dp

    Box(
        modifier = modifier.background(
            color = containerColor,
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusBL = rowCorners,
                smoothnessAsPercentTR = 60,
                cornerRadiusBR = rowCorners,
                smoothnessAsPercentBL = 60,
                cornerRadiusTL = rowCorners,
                smoothnessAsPercentBR = 60,
                cornerRadiusTR = rowCorners,
                smoothnessAsPercentTL = 60
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val commonModifier = Modifier.weight(1f)

            ToggleSegmentButton(
                modifier = commonModifier,
                active = isShuffleEnabled,
                activeColor = activeColorMain,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorMain,
                inactiveColor = inactiveColor,
                inactiveContentColor = inactiveContentColor,
                onClick = onShuffleToggle,
                iconId = R.drawable.rounded_shuffle_24,
                rotateOnActivation = true,
                inactiveCornerRadius = PlayerControlInnerCorner,
                inactiveStartCornerRadius = PlayerControlOuterCorner,
                contentDesc = androidx.compose.ui.res.stringResource(R.string.common_shuffle)
            )
            val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.rounded_repeat_one_24
                Player.REPEAT_MODE_ALL -> R.drawable.rounded_repeat_24
                else -> R.drawable.rounded_repeat_24
            }
            ToggleSegmentButton(
                modifier = commonModifier,
                active = repeatActive,
                activeColor = activeColorSecondary,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorSecondary,
                inactiveColor = inactiveColor,
                inactiveContentColor = inactiveContentColor,
                onClick = onRepeatToggle,
                iconId = repeatIcon,
                crossfadeIcon = true,
                inactiveCornerRadius = PlayerControlInnerCorner,
                contentDesc = androidx.compose.ui.res.stringResource(R.string.common_repeat)
            )
            FavoriteButton(
                songId = songId,
                isFavorite = isFavorite,
                onToggle = onFavoriteToggle,
                modifier = commonModifier.fillMaxHeight(),
                shape = if (isFavorite) RoundedCornerShape(rowCorners) else InactiveFavoriteSegmentShape,
                containerColor = if (isFavorite) activeColorTertiary else inactiveColor,
                contentColor = if (isFavorite) onActiveColorTertiary else inactiveContentColor,
                style = FavoriteButtonStyle.Full
            )
        }
    }
}
