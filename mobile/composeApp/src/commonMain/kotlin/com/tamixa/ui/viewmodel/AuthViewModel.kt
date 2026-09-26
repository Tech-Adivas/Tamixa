package com.tamixa.ui.viewmodel

import com.tamixa.util.AuthValidation
import com.tamixa.util.TamixaLog
import com.tamixa.domain.AuthTokens
import com.tamixa.ui.errorMessageForUser
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import com.tamixa.repository.AuthRepository
import com.tamixa.security.SessionExpiredNotifier
import com.tamixa.ui.AppMessageNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val sessionExpiredNotifier: SessionExpiredNotifier,
    private val authRepository: AuthRepository,
    private val appMessageNotifier: AppMessageNotifier,
    private val scope: CoroutineScope
) {
    val sessionExpired = sessionExpiredNotifier.sessionExpired

    fun clearSessionExpired() {
        sessionExpiredNotifier.clear()
    }

    private val _loginState = MutableStateFlow<UiState<AuthTokens>>(UiState.Error("", null))
    val loginState: StateFlow<UiState<AuthTokens>> = _loginState.asStateFlow()

    private val _otpSentToPhone = MutableStateFlow<String?>(null)
    val otpSentToPhone: StateFlow<String?> = _otpSentToPhone.asStateFlow()

    // Dev code is intentionally NOT exposed as a public StateFlow to prevent
    // it from being rendered in UI or leaked via state inspection.
    // Only used internally to auto-fill OTP in debug builds.
    private val _otpDevCode = MutableStateFlow<String?>(null)
    val otpDevCode: StateFlow<String?> = _otpDevCode.asStateFlow()

    private val _passwordlessCodeSentToEmail = MutableStateFlow<String?>(null)
    val passwordlessCodeSentToEmail: StateFlow<String?> = _passwordlessCodeSentToEmail.asStateFlow()

    /** Magic-link token from email (e.g. Android VIEW intent). Consumed after successful verify or on clear. */
    private val _pendingPasswordlessMagicLinkToken = MutableStateFlow<String?>(null)
    val pendingPasswordlessMagicLinkToken: StateFlow<String?> = _pendingPasswordlessMagicLinkToken.asStateFlow()

    fun setPendingPasswordlessMagicLinkToken(token: String?) {
        _pendingPasswordlessMagicLinkToken.value = token?.trim()?.lowercase()?.takeIf { it.length == 32 }
    }

    fun clearPendingPasswordlessMagicLinkToken() {
        _pendingPasswordlessMagicLinkToken.value = null
    }

    private val _registerState = MutableStateFlow<UiState<AuthTokens>>(UiState.Loading)
    val registerState: StateFlow<UiState<AuthTokens>> = _registerState.asStateFlow()

    private val _currentUser = MutableStateFlow<UiState<com.tamixa.domain.CurrentUser>>(UiState.Loading)
    val currentUser: StateFlow<UiState<com.tamixa.domain.CurrentUser>> = _currentUser.asStateFlow()

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun login(email: String, password: String) {
        scope.launch {
            _loginState.value = UiState.Loading
            authRepository.login(email, password)
                .fold(
                    onSuccess = { _loginState.value = UiState.Success(it) },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "login failed", it)
                        val msg = errorMessageForUser(it)
                        _loginState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun register(
        email: String,
        password: String,
        acceptedTerms: Boolean = false,
        acceptedPrivacy: Boolean = false,
        acceptedParentalAttestation: Boolean = false
    ) {
        scope.launch {
            _registerState.value = UiState.Loading
            authRepository.register(email, password, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation)
                .fold(
                    onSuccess = { _registerState.value = UiState.Success(it) },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "register failed", it)
                        val msg = errorMessageForUser(it)
                        _registerState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun loadCurrentUser() {
        scope.launch {
            _currentUser.value = UiState.Loading
            authRepository.me()
                .fold(
                    onSuccess = { _currentUser.value = UiState.Success(it) },
                    onFailure = {
                        val msg = errorMessageForUser(it)
                        _currentUser.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun updateProfile(nickname: String?, displayName: String?) {
        scope.launch {
            authRepository.updateProfile(nickname, displayName)
                .fold(
                    onSuccess = {
                        loadCurrentUser()
                        appMessageNotifier.show(Strings.profileUpdated())
                    },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "updateProfile failed", it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun requestPasswordlessCode(email: String) {
        if (!AuthValidation.isEmailValid(email)) {
            _loginState.value = UiState.Error(Strings.invalidEmail(), null)
            return
        }
        val normalized = email.trim().lowercase()
        scope.launch {
            _passwordlessCodeSentToEmail.value = null
            authRepository.requestPasswordlessCode(normalized)
                .fold(
                    onSuccess = { sent ->
                        if (sent) _passwordlessCodeSentToEmail.value = normalized
                        else _loginState.value = UiState.Error("Could not send code. Please check your email and try again.", null)
                    },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "requestPasswordlessCode failed", it)
                        val msg = errorMessageForUser(it)
                        _loginState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun verifyPasswordlessCode(
        email: String,
        code: String,
        acceptedTerms: Boolean = true,
        acceptedPrivacy: Boolean = true,
        acceptedParentalAttestation: Boolean = false
    ) {
        if (!AuthValidation.isPasswordlessEmailCodeValid(code)) {
            _loginState.value = UiState.Error(Strings.invalidCode(), null)
            return
        }
        scope.launch {
            _loginState.value = UiState.Loading
            authRepository.verifyPasswordlessCode(
                email.trim().lowercase(),
                code.trim(),
                acceptedTerms,
                acceptedPrivacy,
                acceptedParentalAttestation
            )
                .fold(
                    onSuccess = {
                        _passwordlessCodeSentToEmail.value = null
                        _pendingPasswordlessMagicLinkToken.value = null
                        _loginState.value = UiState.Success(it)
                    },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "verifyPasswordlessCode failed", it)
                        val msg = errorMessageForUser(it)
                        _loginState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun verifyPasswordlessMagicLink(
        loginToken: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
        acceptedParentalAttestation: Boolean
    ) {
        val normalized = loginToken.trim().lowercase()
        if (normalized.length != 32 || !normalized.all { it in '0'..'9' || it in 'a'..'f' }) {
            _loginState.value = UiState.Error(Strings.invalidCode(), null)
            return
        }
        scope.launch {
            _loginState.value = UiState.Loading
            authRepository.verifyPasswordlessMagicLink(
                normalized,
                acceptedTerms,
                acceptedPrivacy,
                acceptedParentalAttestation
            )
                .fold(
                    onSuccess = {
                        _pendingPasswordlessMagicLinkToken.value = null
                        _passwordlessCodeSentToEmail.value = null
                        _loginState.value = UiState.Success(it)
                    },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "verifyPasswordlessMagicLink failed", it)
                        val msg = errorMessageForUser(it)
                        _loginState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun clearPasswordlessState() {
        _passwordlessCodeSentToEmail.value = null
    }

    fun sendOtp(phone: String) {
        scope.launch {
            _otpSentToPhone.value = null
            _otpDevCode.value = null
            authRepository.sendOtp(phone)
                .fold(
                    onSuccess = { result ->
                        if (result.sent) {
                            _loginState.value = UiState.Error("", null)
                            _otpSentToPhone.value = phone
                            _otpDevCode.value = result.devCode
                        } else {
                            _loginState.value = UiState.Error(Strings.otpSendFailed(), null)
                            appMessageNotifier.showError()
                        }
                    },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "sendOtp failed", it)
                        val msg = errorMessageForUser(it)
                        _loginState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun loginWithOtp(phone: String, code: String) {
        if (!AuthValidation.isOtpCodeValid(code)) {
            _loginState.value = UiState.Error(Strings.invalidCode(), null)
            return
        }
        scope.launch {
            _loginState.value = UiState.Loading
            authRepository.loginWithOtp(phone, code.trim())
                .fold(
                    onSuccess = {
                        _otpSentToPhone.value = null
                        _loginState.value = UiState.Success(it)
                    },
                    onFailure = {
                        TamixaLog.w("AuthViewModel", "loginWithOtp failed", it)
                        val msg = errorMessageForUser(it)
                        _loginState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun clearOtpState() {
        _otpSentToPhone.value = null
        _otpDevCode.value = null
    }

    fun logout() {
        authRepository.logout()
        // Reset auth UI state so Login/Register screens don't treat stale Success as "just logged in" and auto-navigate to Dashboard
        _loginState.value = UiState.Error("", null)
        _registerState.value = UiState.Loading
        _passwordlessCodeSentToEmail.value = null
        _pendingPasswordlessMagicLinkToken.value = null
        _otpSentToPhone.value = null
        _otpDevCode.value = null
    }

    suspend fun deleteAccount(): Result<Unit> = authRepository.deleteAccount()
}
