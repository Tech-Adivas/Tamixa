package com.tamixa.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tamixa.ui.screen.LoginScreen
import com.tamixa.ui.state.UiState
import com.tamixa.ui.theme.TamixaTheme

@Preview(name = "Login light", showBackground = true)
@Composable
private fun LoginScreenPreview() {
    TamixaTheme(darkTheme = false) {
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
    TamixaTheme(darkTheme = true) {
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
