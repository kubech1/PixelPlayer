package com.theveloper.pixelplay.presentation.utils

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/** Observes system animator duration scale so reduced-motion users receive instant state transitions. */
@Composable
fun rememberSystemAnimationsEnabled(): State<Boolean> {
    val context = LocalContext.current
    val animationsEnabled = remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animationsEnabled.value = ValueAnimator.areAnimatorsEnabled()
            }
        }
        animationsEnabled.value = ValueAnimator.areAnimatorsEnabled()
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return animationsEnabled
}

/** Observes TalkBack / touch exploration state to disable automatic chrome compaction when active. */
@Composable
fun rememberTouchExplorationEnabled(): State<Boolean> {
    val context = LocalContext.current
    val accessibilityManager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    val touchExplorationEnabled = remember(accessibilityManager) {
        mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true)
    }
    DisposableEffect(accessibilityManager) {
        if (accessibilityManager == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
            touchExplorationEnabled.value = enabled
        }
        touchExplorationEnabled.value = accessibilityManager.isTouchExplorationEnabled
        accessibilityManager.addTouchExplorationStateChangeListener(listener)
        onDispose {
            accessibilityManager.removeTouchExplorationStateChangeListener(listener)
        }
    }
    return touchExplorationEnabled
}

/** Decorative loops require visible bounds, a resumed UI and enabled system animations. */
@Composable
internal fun visualAnimationActivity(): Pair<Boolean, Modifier> {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var inWindow by remember { mutableStateOf(false) }
    val animationsEnabled by rememberSystemAnimationsEnabled()
    return (inWindow && animationsEnabled && lifecycle.isAtLeast(Lifecycle.State.RESUMED)) to
        Modifier.onGloballyPositioned { inWindow = it.isAttached && !it.boundsInWindow().isEmpty }
}
