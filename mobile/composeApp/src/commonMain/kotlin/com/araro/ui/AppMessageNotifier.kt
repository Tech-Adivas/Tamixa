package com.araro.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple notifier for app-wide snackbar/toast messages.
 * Inject via Koin; NavHost collects and displays.
 */
class AppMessageNotifier {
    private val _message = MutableStateFlow<AppMessage?>(null)
    val message: StateFlow<AppMessage?> = _message.asStateFlow()

    fun show(text: String, tag: String = "default") {
        _message.value = AppMessage(text = text, tag = tag)
    }

    fun clear() {
        _message.value = null
    }
}

data class AppMessage(val text: String, val tag: String = "default")
