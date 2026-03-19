package com.tamixa.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Notifier for app-wide messages. Use showError() for API/network failures to display
 * a common error dialog with a generic message (no technical details shown to users).
 */
class AppMessageNotifier {
    private val _message = MutableStateFlow<AppMessage?>(null)
    val message: StateFlow<AppMessage?> = _message.asStateFlow()

    private val _showErrorDialog = MutableStateFlow(false)
    val showErrorDialog: StateFlow<Boolean> = _showErrorDialog.asStateFlow()

    /** Show a common error dialog with generic message. Use for all API/network/server failures. */
    fun showError() {
        _showErrorDialog.value = true
    }

    fun dismissErrorDialog() {
        _showErrorDialog.value = false
    }

    fun show(text: String, tag: String = "default") {
        _message.value = AppMessage(text = text, tag = tag)
    }

    fun clear() {
        _message.value = null
    }
}

data class AppMessage(val text: String, val tag: String = "default")
