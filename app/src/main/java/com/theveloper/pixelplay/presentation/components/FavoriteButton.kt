package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.ui.theme.LocalMotionPreferences
import com.theveloper.pixelplay.ui.theme.PixelMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

internal enum class FavoriteButtonStyle { Full, Reduced }

private class FavoriteClick(val adding: Boolean)

@Composable
internal fun FavoriteButton(
    songId: String?,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    iconSize: Dp = 24.dp,
    style: FavoriteButtonStyle = FavoriteButtonStyle.Reduced,
    palette: ColorScheme = MaterialTheme.colorScheme
) {
    val animated = LocalMotionPreferences.current.animationsEnabled
    val haptics = LocalHapticFeedback.current
    val interactions = remember(songId) { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale = remember(songId) { Animatable(1f) }
    val burst = remember(songId) { Animatable(1f) }
    var click by remember(songId, animated) { mutableStateOf<FavoriteClick?>(null) }

    LaunchedEffect(click, animated, scale) {
        val action = click
        if (!animated) {
            scale.snapTo(1f)
            burst.snapTo(1f)
            return@LaunchedEffect
        }
        if (action == null) return@LaunchedEffect
        coroutineScope {
            if (style == FavoriteButtonStyle.Full && action.adding) {
                launch {
                    burst.snapTo(0f)
                    burst.animateTo(1f, tween(400, easing = LinearEasing))
                }
                scale.animateTo(1.2f, tween(120, easing = PixelMotion.EnterEasing))
            } else {
                burst.snapTo(1f)
            }
            scale.animateTo(1f, if (action.adding) PixelMotion.feedback() else PixelMotion.structural())
        }
        click = null
    }
    LaunchedEffect(pressed, click, animated, scale) {
        if (animated && click == null) {
            if (pressed) scale.animateTo(0.85f, tween(90))
            else scale.animateTo(1f, PixelMotion.feedback())
        }
    }

    val label = stringResource(R.string.common_favorite)
    val state = stringResource(if (isFavorite) R.string.favorite_state_added else R.string.favorite_state_removed)
    val actionLabel = stringResource(
        if (isFavorite) R.string.song_info_cd_remove_from_favorites else R.string.song_info_cd_add_to_favorites
    )
    val toggle = {
        val adding = !isFavorite
        if (click?.adding != adding) {
            if (animated) click = FavoriteClick(adding)
            haptics.performHapticFeedback(
                if (adding) HapticFeedbackType.Confirm else HapticFeedbackType.ToggleOff
            )
            onToggle()
        }
    }
    Box(
        modifier = modifier
            .drawWithCache {
                val iconPx = iconSize.toPx()
                val stroke = 2.dp.toPx()
                val dot = 2.5.dp.toPx()
                val directions = List(7) { index ->
                    val angle = index * (2.0 * Math.PI / 7) + if (index % 2 == 0) 0.12 else -0.12
                    Offset(cos(angle).toFloat(), sin(angle).toFloat())
                }
                onDrawWithContent {
                    drawContent()
                    val progress = burst.value
                    if (animated && click?.adding == true && progress < 1f) {
                        val ring = (progress * 400f / 350f).coerceAtMost(1f)
                        if (ring < 1f) {
                            drawCircle(
                                color = palette.primary.copy(alpha = 0.6f * (1f - ring)),
                                radius = iconPx * lerp(0.5f, 1.6f, ring),
                                style = Stroke(stroke * (1f - ring))
                            )
                        }
                        directions.forEachIndexed { index, direction ->
                            drawCircle(
                                color = if (index % 2 == 0) palette.tertiary else palette.primary,
                                radius = dot * (1f - progress),
                                center = center + direction * (iconPx * lerp(0.6f, 1.5f, progress))
                            )
                        }
                    }
                }
            }
            .clip(shape)
            .background(containerColor)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = state
                role = Role.Switch
                toggleableState = ToggleableState(isFavorite)
                onClick(label = actionLabel) {
                    toggle()
                    true
                }
            }
            .toggleable(
                value = isFavorite,
                interactionSource = interactions,
                indication = androidx.compose.material3.ripple(),
                role = Role.Switch,
                onValueChange = { toggle() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize).graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        )
    }
}
