package com.araro.ui.viewmodel

import com.araro.util.AraroLog
import com.araro.domain.AuthTokens
import com.araro.ui.state.UiState
import com.araro.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _loginState = MutableStateFlow<UiState<AuthTokens>>(UiState.Error("", null))
    val loginState: StateFlow<UiState<AuthTokens>> = _loginState.asStateFlow()

    private val _otpSentToPhone = MutableStateFlow<String?>(null)
    val otpSentToPhone: StateFlow<String?> = _otpSentToPhone.asStateFlow()

    private val _otpDevCode = MutableStateFlow<String?>(null)
    val otpDevCode: StateFlow<String?> = _otpDevCode.asStateFlow()

    private val _passwordlessCodeSentToEmail = MutableStateFlow<String?>(null)
    val passwordlessCodeSentToEmail: StateFlow<String?> = _passwordlessCodeSentToEmail.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<AuthTokens>>(UiState.Loading)
    val registerState: StateFlow<UiState<AuthTokens>> = _registerState.asStateFlow()

    private val _currentUser = MutableStateFlow<UiState<com.araro.domain.CurrentUser>>(UiState.Loading)
    val currentUser: StateFlow<UiState<com.araro.domain.CurrentUser>> = _currentUser.asStateFlow()

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun login(email: String, password: String) {
        scope.launch {
            _loginState.value = UiState.Loading
            authRepository.login(email, password)
                .fold(
                    onSuccess = { _loginState.value = UiState.Success(it) },
                    onFailure = {
                        AraroLog.w("AuthViewModel", "login failed", it)
                        _loginState.value = UiState.Error(it.message ?: "Login failed", it)
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
                        AraroLog.w("AuthViewModel", "register failed", it)
                        _registerState.value = UiState.Error(it.message ?: "Registration failed", it)
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
                    onFailure = { _currentUser.value = UiState.Error(it.message ?: "Failed to load user", it) }
                )
        }
    }

    fun requestPasswordlessCode(email: String) {
        scope.launch {
            _passwordlessCodeSentToEmail.value = null
            authRepository.requestPasswordlessCode(email)
                .fold(
                    onSuccess = { sent ->
                        if (sent) _passwordlessCodeSentToEmail.value = email
                        else _loginState.value = UiState.Error("Could not send code. Please check your email and try again.", null)
                    },
                    onFailure = {
                        AraroLog.w("AuthViewModel", "requestPasswordlessCode failed", it)
                        _loginState.value = UiState.Error(it.message ?: "Failed to send code", it)
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
        scope.launch {
            _loginState.value = UiState.Loading
            authRepository.verifyPasswordlessCode(email, code, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation)
                .fold(
                    onSuccess = {
                        _passwordlessCodeSentToEmail.value = null
                        _loginState.value = UiState.Success(it)
                    },
                    onFailure = {
                        AraroLog.w("AuthViewModel", "verifyPasswordlessCode failed", it)
                        _loginState.value = UiState.Error(it.message ?: "Invalid code or consent required", it)
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
                            _loginState.value = UiState.Error("Invalid phone number", null)
                        }
                    },
                    onFailure = {
                        AraroLog.w("AuthViewModel", "sendOtp failed", it)
                        _loginState.value = UiState.Error(it.message ?: "Failed to send OTP", it)
                    }
                )
        }
    }

    fun loginWithOtp(phone: String, code: String) {
        scope.launch {
            _loginState.value = UiState.Loading
            authRepository.loginWithOtp(phone, code)
                .fold(
                    onSuccess = {
                        _otpSentToPhone.value = null
                        _loginState.value = UiState.Success(it)
                    },
                    onFailure = {
                        AraroLog.w("AuthViewModel", "loginWithOtp failed", it)
                        _loginState.value = UiState.Error(it.message ?: "OTP verification failed", it)
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
    }
}
