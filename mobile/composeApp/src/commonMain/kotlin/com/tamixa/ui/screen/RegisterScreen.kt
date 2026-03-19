package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaHeroIllustration
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import androidx.compose.ui.graphics.Color
import com.tamixa.ui.theme.TamixaDesignTokens

@Composable
fun RegisterScreen(
    registerState: UiState<*>,
    onRegister: (email: String, password: String, acceptedTerms: Boolean, acceptedPrivacy: Boolean, acceptedParentalAttestation: Boolean) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var acceptedParentalAttestation by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
        Scaffold(
            topBar = {
                com.tamixa.ui.components.TamixaScreenTopBar(
                    title = Strings.register(),
                    onBack = onNavigateToLogin,
                    useTransparentBackground = true
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(TamixaDesignTokens.screenPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val colorScheme = MaterialTheme.colorScheme
            Spacer(modifier = Modifier.height(8.dp))
            TamixaHeroIllustration(size = 100.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.appName(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = TamixaColors.cream
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Strings.createAccountSafe(),
                style = MaterialTheme.typography.bodySmall,
                color = TamixaColors.cream.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
            ) {
                Column(
                    modifier = Modifier.padding(TamixaDesignTokens.screenPadding)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(Strings.email()) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(Strings.password()) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = { Text(Strings.minCharacters()) },
                        shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it }
                        )
                        Text(
                            Strings.acceptTerms(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedPrivacy,
                            onCheckedChange = { acceptedPrivacy = it }
                        )
                        Text(
                            Strings.acceptPrivacy(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedParentalAttestation,
                            onCheckedChange = { acceptedParentalAttestation = it }
                        )
                        Text(
                            Strings.parentalAttestation(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            when (val state = registerState) {
                is UiState.Loading -> {
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(32.dp)
                            .then(Modifier.semantics { contentDescription = Strings.loading() })
                    )
                }
                is UiState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = TamixaCardColors.errorContainer(),
                        shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                else -> {}
            }
            Spacer(modifier = Modifier.height(24.dp))
            TamixaPrimaryButton(
                onClick = { onRegister(email, password, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation) },
                text = Strings.register(),
                modifier = Modifier.fillMaxWidth(),
                enabled = acceptedTerms && acceptedPrivacy && acceptedParentalAttestation,
                loading = registerState is UiState.Loading
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    Strings.login(),
                    color = TamixaColors.goldAccent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}
