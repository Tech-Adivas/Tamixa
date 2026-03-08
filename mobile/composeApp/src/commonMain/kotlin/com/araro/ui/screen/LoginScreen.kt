package com.araro.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.araro.ui.components.AraroLanguageLogo
import com.araro.ui.state.UiState
import com.araro.ui.strings.Strings
import com.araro.ui.components.AraroPrimaryButton
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.theme.AraroDialogDefaults

@Composable
fun LoginScreen(
    loginState: UiState<*>,
    otpSentToPhone: String?,
    otpDevCode: String?,
    passwordlessCodeSentToEmail: String?,
    onRequestPasswordlessCode: (email: String) -> Unit,
    onVerifyPasswordlessCode: (email: String, code: String, acceptedTerms: Boolean, acceptedPrivacy: Boolean, acceptedParentalAttestation: Boolean) -> Unit,
    onClearPasswordlessState: () -> Unit,
    onSendOtp: (phone: String) -> Unit,
    onVerifyOtp: (phone: String, code: String) -> Unit,
    onClearOtpState: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPasswordlessDialog by remember { mutableStateOf(false) }
    var showPasswordlessCodeDialog by remember { mutableStateOf(false) }
    var showMobileSheet by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var passwordlessCode by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var acceptedParentalAttestation by remember { mutableStateOf(false) }

    LaunchedEffect(otpDevCode) {
        if (otpDevCode != null && otpSentToPhone != null) otpCode = otpDevCode
    }

    LaunchedEffect(passwordlessCodeSentToEmail) {
        if (passwordlessCodeSentToEmail != null) {
            showPasswordlessDialog = false
            showPasswordlessCodeDialog = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        StarryNightBackground()
        when {
            otpSentToPhone != null -> {
                OtpVerificationContent(
                    phone = otpSentToPhone,
                    otpCode = otpCode,
                    onOtpChange = { otpCode = it },
                    onVerify = { onVerifyOtp(otpSentToPhone, otpCode) },
                    onBack = { onClearOtpState() },
                    loginState = loginState,
                    otpError = (loginState as? UiState.Error)?.message
                )
            }
            else -> {
                WelcomeContent(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    onGetOtp = { onSendOtp(phone.trim()) },
                    onPasswordless = { showPasswordlessDialog = true },
                    loginState = loginState
                )
            }
        }
    }

    if (showPasswordlessDialog) {
        PasswordlessEmailDialog(
            email = email,
            onEmailChange = { email = it },
            onSend = { onRequestPasswordlessCode(email) },
            onDismiss = { showPasswordlessDialog = false },
            error = (loginState as? UiState.Error)?.message
        )
    }

    if (showPasswordlessCodeDialog && passwordlessCodeSentToEmail != null) {
        PasswordlessCodeDialog(
            email = passwordlessCodeSentToEmail,
            code = passwordlessCode,
            onCodeChange = { passwordlessCode = it },
            acceptedTerms = acceptedTerms,
            acceptedPrivacy = acceptedPrivacy,
            acceptedParentalAttestation = acceptedParentalAttestation,
            onTermsChange = { acceptedTerms = it },
            onPrivacyChange = { acceptedPrivacy = it },
            onParentalAttestationChange = { acceptedParentalAttestation = it },
            onVerify = {
                onVerifyPasswordlessCode(passwordlessCodeSentToEmail, passwordlessCode, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation)
                showPasswordlessCodeDialog = false
                passwordlessCode = ""
                acceptedTerms = false
                acceptedPrivacy = false
                acceptedParentalAttestation = false
            },
            onDismiss = { showPasswordlessCodeDialog = false; onClearPasswordlessState() },
            error = (loginState as? UiState.Error)?.message
        )
    }
}

@Composable
private fun WelcomeContent(
    phone: String,
    onPhoneChange: (String) -> Unit,
    onGetOtp: () -> Unit,
    onPasswordless: () -> Unit,
    loginState: UiState<*>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(24.dp))
        AraroLanguageLogo(languageCode = com.araro.util.AraroConstants.DEFAULT_LANGUAGE, size = 120.dp)
        Spacer(Modifier.height(32.dp))
        PhoneInputField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        AraroPrimaryButton(
            onClick = onGetOtp,
            text = Strings.getOtp(),
            modifier = Modifier.fillMaxWidth(),
            enabled = phone.trim().length >= com.araro.util.AraroConstants.PHONE_NUMBER_MIN_LENGTH,
            loading = loginState is UiState.Loading
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onPasswordless, modifier = Modifier.fillMaxWidth()) {
            Text(
                Strings.passwordlessLogin(),
                style = MaterialTheme.typography.bodySmall,
                color = AraroColors.cream.copy(alpha = 0.9f)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = Strings.termsAndPrivacyDisclaimer(),
            style = MaterialTheme.typography.bodySmall,
            color = AraroColors.cream.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AraroDesignTokens.inputRadius),
        color = AraroColors.inputSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91",
                style = MaterialTheme.typography.titleMedium,
                color = AraroColors.onInputSurface
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it.take(com.araro.util.AraroConstants.PHONE_NUMBER_MIN_LENGTH)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.titleMedium.copy(color = AraroColors.onInputSurface)
            ) { field ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            Strings.enterPhoneNumber(),
                            style = MaterialTheme.typography.titleMedium,
                            color = AraroColors.onInputSurface.copy(alpha = 0.6f)
                        )
                    }
                    field()
                }
            }
        }
    }
}

@Composable
private fun OtpVerificationContent(
    phone: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    loginState: UiState<*>,
    otpError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AraroColors.cream)
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = Strings.enterOtpCode(),
            style = MaterialTheme.typography.headlineMedium,
            color = AraroColors.cream
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${Strings.otpSentTo()} $phone",
            style = MaterialTheme.typography.bodyMedium,
            color = AraroColors.cream.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(32.dp))
        OtpDigitRow(otpCode = otpCode, onOtpChange = onOtpChange)
        Spacer(Modifier.height(24.dp))
        AraroPrimaryButton(
            onClick = onVerify,
            text = Strings.verifyOtp(),
            modifier = Modifier.fillMaxWidth(),
            enabled = otpCode.length == 6,
            loading = loginState is UiState.Loading
        )
        otpError?.let { err ->
            Spacer(Modifier.height(12.dp))
            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "${Strings.haventReceivedCode()} ${Strings.resendOtpIn(25)}",
            style = MaterialTheme.typography.bodySmall,
            color = AraroColors.cream.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OtpDigitRow(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(6) { index ->
                val char = otpCode.getOrNull(index)?.toString() ?: ""
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AraroColors.inputSurface
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineSmall,
                            color = AraroColors.onInputSurface
                        )
                    }
                }
            }
        }
        BasicTextField(
            value = otpCode,
            onValueChange = { onOtpChange(it.filter { c -> c.isDigit() }.take(6)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(0.02f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = androidx.compose.ui.graphics.Color.Transparent,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun PasswordlessEmailDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    error: String?
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = AraroDialogDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                Strings.passwordlessLogin(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text(Strings.email()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                    shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = onSend, shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)) {
                Text(Strings.sendCode())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel()) }
        }
    )
}

@Composable
private fun PasswordlessCodeDialog(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    acceptedTerms: Boolean,
    acceptedPrivacy: Boolean,
    acceptedParentalAttestation: Boolean,
    onTermsChange: (Boolean) -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
    onParentalAttestationChange: (Boolean) -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
    error: String?
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = AraroDialogDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                Strings.enterCode(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(Strings.codeSentTo(email), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text(Strings.code()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTermsChange(!acceptedTerms) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = acceptedTerms, onCheckedChange = onTermsChange)
                    Text(Strings.agreeTerms(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onPrivacyChange(!acceptedPrivacy) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = acceptedPrivacy, onCheckedChange = onPrivacyChange)
                    Text(Strings.agreePrivacy(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onParentalAttestationChange(!acceptedParentalAttestation) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = acceptedParentalAttestation, onCheckedChange = onParentalAttestationChange)
                    Text(Strings.parentalAttestation(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = onVerify,
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                enabled = code.length >= 4 && acceptedTerms && acceptedPrivacy && acceptedParentalAttestation
            ) {
                Text(Strings.continueWith())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel()) }
        }
    )
}
