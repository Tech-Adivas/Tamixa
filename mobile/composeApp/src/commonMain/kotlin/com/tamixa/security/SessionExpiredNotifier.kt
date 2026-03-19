package com.tamixa.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Notifies the app when the session has expired (e.g. refresh token failed).
 * NavHost observes [sessionExpired] and navigates to Login, then calls [clear].
 */
class SessionExpiredNotifier {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    fun notifySessionExpired() {
        _sessionExpired.value = true
    }

    fun clear() {
        _sessionExpired.value = false
    }
}
