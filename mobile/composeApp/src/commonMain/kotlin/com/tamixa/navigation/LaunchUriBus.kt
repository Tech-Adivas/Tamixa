package com.tamixa.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-platform entry for VIEW / universal-link URLs (Android MainActivity, iOS onOpenURL).
 * [TamixaNavHost] collects and routes passwordless tokens and story deep links.
 */
object LaunchUriBus {
    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(uri: String) {
        val trimmed = uri.trim()
        if (trimmed.isNotEmpty()) {
            _events.tryEmit(trimmed)
        }
    }
}
