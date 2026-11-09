package com.theveloper.pixelplay.presentation.components.bottomchrome

import androidx.compose.runtime.*

/** Off-screen pager pages must not react to a reselection of the visible destination. */
val LocalNavigationReselectionActive = compositionLocalOf { true }

@Composable
fun NavigationReselectionEffect(route: String, scrollToTop: suspend () -> Unit) {
    val chrome = LocalBottomChromeState.current
    val active = LocalNavigationReselectionActive.current
    val action by rememberUpdatedState(scrollToTop)
    LaunchedEffect(chrome, route, active) {
        if (active) chrome?.reselectionEvents?.collect { if (it == route) action() }
    }
}
