package com.example.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared in-memory state tracking whether screen blackout is currently active.
 */
object EcoState {
    private val _isScreenOffActive = MutableStateFlow(false)
    val isScreenOffActive = _isScreenOffActive.asStateFlow()

    fun setScreenOffActive(active: Boolean) {
        _isScreenOffActive.value = active
    }
}
