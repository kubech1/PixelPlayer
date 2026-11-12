package com.theveloper.pixelplay.presentation.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder

private fun NavController.isReadyForNavigation(): Boolean {
    return runCatching {
        // We allow navigation if the current entry is at least STARTED.
        // This is safer than strictly RESUMED as transitions can sometimes delay RESUMED state.
        currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
    }.getOrDefault(false)
}

fun NavController.navigateSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
    }
    return true
}

fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
        builder()
    }
    return true
}

fun NavController.navigateSafelyReplacing(
    route: String,
    patternToPop: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = false
        popUpTo(patternToPop) {
            inclusive = true
        }
        builder()
    }
    return true
}

fun NavController.navigateToTopLevelSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false

    // Reuse an existing root entry directly. Restoring a saved top-level stack
    // after popping to the start destination can otherwise bring back the stack
    // we just left (notably Library after opening a Library tab from Home).
    val returnedToExistingDestination = runCatching {
        popBackStack(route, inclusive = false, saveState = false)
    }.getOrDefault(false)
    if (returnedToExistingDestination) return true

    val startDestinationId = runCatching { graph.findStartDestination().id }.getOrNull() ?: return false
    navigate(route) {
        popUpTo(startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    return true
}
