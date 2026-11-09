package com.theveloper.pixelplay.presentation.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.theveloper.pixelplay.data.preferences.VisualEffectsLevel

@Immutable
data class EffectiveVisualEffects(
    val configuredLevel: VisualEffectsLevel = VisualEffectsLevel.DEFAULT,
    val isPowerSaveMode: Boolean = false
) {
    val effectiveLevel: VisualEffectsLevel =
        if (isPowerSaveMode) configuredLevel.downgradeOneLevel() else configuredLevel

    val isMinimal: Boolean
        get() = effectiveLevel == VisualEffectsLevel.MINIMAL

    val allowsDepthEffects: Boolean
        get() = effectiveLevel != VisualEffectsLevel.MINIMAL

    val allowsRealtimeBlur: Boolean
        get() = effectiveLevel == VisualEffectsLevel.FULL
}

val LocalVisualEffects = compositionLocalOf { EffectiveVisualEffects() }

/** Observes Android Battery Saver state so visual effects automatically step down one level. */
@Composable
fun rememberPowerSaveMode(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    val isPowerSave = remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }

    DisposableEffect(context, powerManager, lifecycleOwner) {
        if (powerManager == null) {
            return@DisposableEffect onDispose { }
        }
        val refresh = {
            isPowerSave.value = powerManager.isPowerSaveMode
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    refresh()
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }

        refresh()
        context.registerReceiver(
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            Context.RECEIVER_NOT_EXPORTED
        )
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return isPowerSave
}
