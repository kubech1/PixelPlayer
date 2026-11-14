package com.theveloper.pixelplay.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.graphics.ColorUtils

val LocalPixelPlayDarkTheme = staticCompositionLocalOf { false }
val LocalShowScrollbar = staticCompositionLocalOf { true }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
@Composable
fun PixelPlayStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        run {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                run {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF292929),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFF242424),
    secondary = Color(0xFFD5D5D5),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF313131),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFC8C8C8),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF383838),
    onTertiaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF282828),
    onSurfaceVariant = Color.White,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF2F2F2),
    inverseOnSurface = Color.Black,
    outline = Color(0xFF858585),
    outlineVariant = Color(0xFF3F3F3F),
    surfaceBright = Color(0xFF3B3B3B),
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF101010),
    surfaceContainer = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF212121),
    surfaceContainerHighest = Color(0xFF2B2B2B),
    primaryFixed = Color(0xFFE8E8E8),
    primaryFixedDim = Color(0xFFCECECE),
    onPrimaryFixed = Color.Black,
    onPrimaryFixedVariant = Color(0xFF272727),
    secondaryFixed = Color(0xFFDEDEDE),
    secondaryFixedDim = Color(0xFFC6C6C6),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color(0xFF303030),
    tertiaryFixed = Color(0xFFD4D4D4),
    tertiaryFixedDim = Color(0xFFBEBEBE),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color(0xFF383838)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF242424),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E9E9),
    onPrimaryContainer = Color.Black,
    inversePrimary = Color(0xFFF2F2F2),
    secondary = Color(0xFF414141),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF555555),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E0E0),
    onTertiaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color.Black,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF242424),
    inverseOnSurface = Color.White,
    outline = Color(0xFF747474),
    outlineVariant = Color(0xFFD8D8D8),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDCDCDC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF1F1F1),
    surfaceContainerHigh = Color(0xFFEBEBEB),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    primaryFixed = Color(0xFFE8E8E8),
    primaryFixedDim = Color(0xFFCECECE),
    onPrimaryFixed = Color.Black,
    onPrimaryFixedVariant = Color(0xFF272727),
    secondaryFixed = Color(0xFFDEDEDE),
    secondaryFixedDim = Color(0xFFC6C6C6),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color(0xFF303030),
    tertiaryFixed = Color(0xFFD4D4D4),
    tertiaryFixedDim = Color(0xFFBEBEBE),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color(0xFF383838)
)

@Composable
fun PixelPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val finalColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    PixelPlayStatusBarStyle(
        color = finalColorScheme.background,
        navigationColor = finalColorScheme.background
    )

    CompositionLocalProvider(LocalPixelPlayDarkTheme provides darkTheme) {
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        MaterialTheme(
            colorScheme = finalColorScheme,
            motionScheme = PixelPlayMotionScheme,
            shapes = Shapes,
            typography = Typography,
            content = content
        )
    }
}
