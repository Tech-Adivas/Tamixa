package com.araro.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.araro.domain.CreateChildRequest
import com.araro.ui.components.AraroEmojiDisplay
import com.araro.ui.components.AraroPrimaryButton
import com.araro.ui.state.UiState
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildCreateScreen(
    createState: UiState<*>,
    onCreate: (CreateChildRequest) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var languagePreference by remember { mutableStateOf("ta") }
    var interests by remember { mutableStateOf("") }
    var childProfileConsent by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            com.araro.ui.components.AraroScreenTopBar(
                title = Strings.addChild(),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AraroEmojiDisplay(emoji = "👶🌈", fontSize = 48.sp)
            }
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        Strings.childDetails(),
                        style = MaterialTheme.typography.titleMedium,
                        color = AraroColors.maroonPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.name()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = { Text(Strings.dateOfBirth()) },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("YYYY-MM-DD") },
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = languagePreference,
                        onValueChange = { languagePreference = it },
                        label = { Text(Strings.languagePreference()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = interests,
                        onValueChange = { interests = it },
                        label = { Text(Strings.interests()) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. dinosaurs, space, animals") },
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                colors = CardDefaults.cardColors(containerColor = AraroColors.warmSurfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = childProfileConsent,
                        onCheckedChange = { childProfileConsent = it }
                    )
                    Text(
                        Strings.childConsentMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            when (createState) {
                is UiState.Loading -> {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                    }
                }
                is UiState.Error -> {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                    ) {
                        Text(
                            (createState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                else -> {}
            }
            Spacer(Modifier.height(24.dp))
            AraroPrimaryButton(
                onClick = {
                    onCreate(
                        CreateChildRequest(
                            name = name,
                            dateOfBirth = dateOfBirth,
                            languagePreference = languagePreference.ifBlank { null },
                            interests = interests.ifBlank { null },
                            childProfileConsent = childProfileConsent
                        )
                    )
                },
                text = Strings.addChild(),
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && dateOfBirth.isNotBlank() && childProfileConsent,
                loading = createState is UiState.Loading
            )
        }
    }
}
