package com.tamixa.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.ui.components.TamixaFullLogo
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults

private val LoginCardShape = RoundedCornerShape(28.dp)

/**
 * High-contrast “sign-in sheet” on the starfield — warm paper, Storybook Dusk border + accent rail
 * so the palette reads immediately (not just micro-shadow tweaks).
 */
@Composable
private fun LoginThemeCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = LoginCardShape,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = TamixaColors.terracotta.copy(alpha = 0.22f),
            )
            .clip(LoginCardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFDF9),
                        Color(0xFFF2E8DE),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TamixaColors.terracotta.copy(alpha = 0.7f),
                            TamixaColors.deepTeal.copy(alpha = 0.58f),
                        ),
                    ),
                ),
                shape = LoginCardShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(TamixaColors.terracotta, TamixaColors.deepTeal),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/**
 * Login screen with "Welcome to Tamixa", tagline, and app-standard background.
 */
@Composable
fun LoginScreen(
    loginState: UiState<*>,
    otpSentToPhone: String?,
    otpDevCode: String? = null,
    passwordlessCodeSentToEmail: String?,
    passwordlessMagicLinkToken: String? = null,
    onRequestPasswordlessCode: (email: String) -> Unit,
    onVerifyPasswordlessCode: (email: String, code: String, acceptedTerms: Boolean, acceptedPrivacy: Boolean, acceptedParentalAttestation: Boolean) -> Unit,
    onVerifyPasswordlessMagicLink: (loginToken: String, acceptedTerms: Boolean, acceptedPrivacy: Boolean, acceptedParentalAttestation: Boolean) -> Unit = { _, _, _, _ -> },
    onDismissPasswordlessMagicLink: () -> Unit = {},
    onClearPasswordlessState: () -> Unit,
    onSendOtp: (phone: String) -> Unit,
    onVerifyOtp: (phone: String, code: String) -> Unit,
    onClearOtpState: () -> Unit,
    onNavigateToRegister: (() -> Unit)? = null,
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
        AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
        when {
            passwordlessMagicLinkToken != null -> {
                PasswordlessMagicLinkContent(
                    onVerify = { terms, privacy, parental ->
                        onVerifyPasswordlessMagicLink(passwordlessMagicLinkToken, terms, privacy, parental)
                    },
                    onUseOtherMethod = {
                        onDismissPasswordlessMagicLink()
                    },
                    loginState = loginState
                )
            }
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
                    onNavigateToRegister = onNavigateToRegister,
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
                // Do not dismiss or clear fields here: verify is async. On success, email state clears and this dialog
                // is no longer composed; on failure, the user keeps the form and sees the error.
                onVerifyPasswordlessCode(passwordlessCodeSentToEmail, passwordlessCode, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation)
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
    onNavigateToRegister: (() -> Unit)?,
    loginState: UiState<*>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TamixaDesignTokens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(12.dp))
        LoginThemeCard(Modifier.fillMaxWidth()) {
            Text(
                text = Strings.welcomeToTamixa(),
                style = MaterialTheme.typography.headlineMedium,
                color = TamixaColors.appHeading,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Strings.loginWelcomeTagline(),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TamixaColors.appTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
            Spacer(Modifier.height(18.dp))
            TamixaFullLogo(size = 200.dp)
            Spacer(Modifier.height(22.dp))
            PhoneInputField(
                value = phone,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            TamixaPrimaryButton(
                onClick = onGetOtp,
                text = Strings.getOtp(),
                modifier = Modifier.fillMaxWidth(),
                enabled = phone.trim().length >= com.tamixa.util.TamixaConstants.PHONE_NUMBER_MIN_LENGTH,
                loading = loginState is UiState.Loading
            )
            val otpSendError = (loginState as? UiState.Error)?.message?.takeIf { it.isNotBlank() }
            otpSendError?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = err,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = onPasswordless,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = TamixaColors.deepTeal),
            ) {
                Text(Strings.passwordlessLogin(), style = MaterialTheme.typography.bodyMedium)
            }
            onNavigateToRegister?.let { goRegister ->
                TextButton(
                    onClick = goRegister,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = TamixaColors.terracotta),
                ) {
                    Text(Strings.registerWithEmail(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = Strings.termsAndPrivacyDisclaimer(),
            modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
            style = MaterialTheme.typography.bodySmall,
            color = TamixaColors.cream.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
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
    val shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color(0xFF2C2520).copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(shape)
            .border(BorderStroke(1.dp, Color(0xFF2C2520).copy(alpha = 0.08f)), shape),
        shape = shape,
        color = TamixaColors.inputSurface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91",
                style = MaterialTheme.typography.titleMedium,
                color = TamixaColors.onInputSurface
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it.take(com.tamixa.util.TamixaConstants.PHONE_NUMBER_MIN_LENGTH)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.titleMedium.copy(color = TamixaColors.onInputSurface)
            ) { field ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            Strings.enterPhoneNumber(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TamixaColors.onInputSurface.copy(alpha = 0.6f)
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
            .verticalScroll(rememberScrollState())
            .padding(TamixaDesignTokens.screenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TamixaColors.cream)
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        LoginThemeCard(Modifier.fillMaxWidth()) {
            Text(
                text = Strings.enterOtpCode(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TamixaColors.appHeading,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${Strings.otpSentTo()} $phone",
                style = MaterialTheme.typography.bodyMedium,
                color = TamixaColors.appTextSecondary,
            )
            Spacer(Modifier.height(28.dp))
            OtpDigitRow(otpCode = otpCode, onOtpChange = onOtpChange)
            Spacer(Modifier.height(22.dp))
            TamixaPrimaryButton(
                onClick = onVerify,
                text = Strings.verifyOtp(),
                modifier = Modifier.fillMaxWidth(),
                enabled = otpCode.length == 6,
                loading = loginState is UiState.Loading
            )
            otpError?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${Strings.haventReceivedCode()} ${Strings.resendOtpIn(25)}",
                style = MaterialTheme.typography.bodySmall,
                color = TamixaColors.appTextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
                val cellShape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = cellShape,
                            ambientColor = Color(0xFF2C2520).copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.06f),
                        )
                        .clip(cellShape)
                        .border(BorderStroke(1.dp, Color(0xFF2C2520).copy(alpha = 0.08f)), cellShape),
                    shape = cellShape,
                    color = TamixaColors.inputSurface,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TamixaColors.onInputSurface
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
        shape = TamixaDialogDefaults.shape,
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
                    shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = onSend, shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)) {
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
        shape = TamixaDialogDefaults.shape,
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
                    shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
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
                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                enabled = code.length == 6 && acceptedTerms && acceptedPrivacy && acceptedParentalAttestation
            ) {
                Text(Strings.continueWith())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel()) }
        }
    )
}

@Composable
private fun PasswordlessMagicLinkContent(
    onVerify: (acceptedTerms: Boolean, acceptedPrivacy: Boolean, acceptedParentalAttestation: Boolean) -> Unit,
    onUseOtherMethod: () -> Unit,
    loginState: UiState<*>
) {
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var acceptedParentalAttestation by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TamixaFullLogo()
        Spacer(Modifier.height(20.dp))
        LoginThemeCard {
            Text(
                Strings.finishEmailLinkSignIn(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TamixaColors.deepTeal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                Strings.passwordlessLogin(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { acceptedTerms = !acceptedTerms },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                Text(Strings.agreeTerms(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { acceptedPrivacy = !acceptedPrivacy },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = acceptedPrivacy, onCheckedChange = { acceptedPrivacy = it })
                Text(Strings.agreePrivacy(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { acceptedParentalAttestation = !acceptedParentalAttestation },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = acceptedParentalAttestation, onCheckedChange = { acceptedParentalAttestation = it })
                Text(Strings.parentalAttestation(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            }
            (loginState as? UiState.Error)?.message?.takeIf { it.isNotBlank() }?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            TamixaPrimaryButton(
                onClick = { onVerify(acceptedTerms, acceptedPrivacy, acceptedParentalAttestation) },
                text = Strings.continueWith(),
                enabled = acceptedTerms && acceptedPrivacy && acceptedParentalAttestation && loginState !is UiState.Loading,
                loading = loginState is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onUseOtherMethod, modifier = Modifier.fillMaxWidth()) {
                Text(Strings.chooseSignIn())
            }
        }
    }
}
