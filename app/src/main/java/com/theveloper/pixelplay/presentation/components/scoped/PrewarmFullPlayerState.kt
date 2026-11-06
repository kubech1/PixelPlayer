package com.theveloper.pixelplay.presentation.components.scoped

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
internal fun rememberPrewarmFullPlayer(currentSongId: String?): Boolean {
    // FullPlayerCompositionPolicy now keeps the real FullPlayerContent warm inside
    // UnifiedPlayerMiniAndFullLayers across song changes, avoiding duplicate 32ms
    // throwaway FullPlayerContent compositions in a separate tree branch.
    return false
}
