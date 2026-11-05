package com.theveloper.pixelplay.presentation.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process UI visibility is independent from the media service's lifetime. */
object AppVisibility {
    private val mutable = MutableStateFlow(false)
    val visible = mutable.asStateFlow()
    fun setVisible(visible: Boolean) { mutable.value = visible }
}
