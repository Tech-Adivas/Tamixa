package com.araro.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.araro.ui.screen.LoginScreen
import com.araro.ui.state.UiState
import com.araro.ui.theme.AraroTheme

@Preview(name = "Login light", showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AraroTheme(darkTheme = false) {
        LoginScreen(
            loginState = UiState.Error("", null),
            otpSentToPhone = null,
            passwordlessCodeSentToEmail = null,
            onRequestPasswordlessCode = {},
            onVerifyPasswordlessCode = { _, _, _, _, _ -> },
            onClearPasswordlessState = {},
            onSendOtp = {},
            onVerifyOtp = { _, _ -> },
            onClearOtpState = {}
        )
    }
}

@Preview(name = "Login dark", showBackground = true)
@Composable
private fun LoginScreenDarkPreview() {
    AraroTheme(darkTheme = true) {
        LoginScreen(
            loginState = UiState.Error("", null),
            otpSentToPhone = null,
            passwordlessCodeSentToEmail = null,
            onRequestPasswordlessCode = {},
            onVerifyPasswordlessCode = { _, _, _, _, _ -> },
            onClearPasswordlessState = {},
            onSendOtp = {},
            onVerifyOtp = { _, _ -> },
            onClearOtpState = {}
        )
    }
}
