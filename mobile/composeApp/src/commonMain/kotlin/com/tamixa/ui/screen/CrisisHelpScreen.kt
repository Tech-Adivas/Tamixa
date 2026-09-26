package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.safety.CrisisHelpline
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaDesignTokens
import kotlinx.coroutines.launch

private fun digitsForTel(raw: String): String {
    val t = raw.trim()
    if (t.startsWith("+")) return "+" + t.drop(1).filter { it.isDigit() }
    return t.filter { it.isDigit() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisHelpScreen(
    initialVaultText: String,
    onSaveVault: suspend (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToCrisisNavigator: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var vault by remember { mutableStateOf(initialVaultText) }
    var saveHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialVaultText) { vault = initialVaultText }
    val scroll = rememberScrollState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.crisisHelpTitle(),
                onBack = onBack,
                useTransparentBackground = true,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = TamixaDesignTokens.screenPadding)
                .verticalScroll(scroll)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = Strings.crisisHelpIntro(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            onNavigateToCrisisNavigator?.let { go ->
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = go,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                ) {
                    Text(Strings.crisisNavigatorEntryFromHelp())
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation),
            ) {
                Column(Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                    Text(
                        Strings.crisisHelp1930Title(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Strings.crisisHelpCyberFirstHourSteps(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Strings.crisisHelpDirectorySections().forEach { section ->
                Text(
                    text = section.sectionTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                section.lines.forEach { line ->
                    HelplineCard(line = line, onOpenUrl = onOpenUrl)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                Strings.crisisHelpVaultTitle(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.crisisHelpVaultHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = vault,
                onValueChange = { vault = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text(Strings.crisisHelpVaultPlaceholder()) },
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            onSaveVault(vault.trim())
                            saveHint = Strings.crisisHelpVaultSaved()
                        }
                    },
                ) {
                    Text(Strings.crisisHelpVaultSave())
                }
                saveHint?.let { h ->
                    Text(h, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                Strings.crisisHelpDisclaimer(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelplineCard(
    line: CrisisHelpline,
    onOpenUrl: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = TamixaCardColors.surface(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                line.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            line.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                Spacer(Modifier.height(4.dp))
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            line.note?.takeIf { it.isNotBlank() }?.let { n ->
                Spacer(Modifier.height(6.dp))
                Text(n, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            line.websiteUrl?.takeIf { it.isNotBlank() }?.let { url ->
                TextButton(onClick = { onOpenUrl(url) }) {
                    Text(Strings.crisisHelpOpenWebsite())
                }
            }
            line.phoneNumbers.forEach { raw ->
                val d = digitsForTel(raw)
                if (d.isNotEmpty()) {
                    TextButton(onClick = { onOpenUrl("tel:$d") }) {
                        Text(Strings.crisisHelpCall(raw))
                    }
                }
            }
            line.smsNumber?.takeIf { it.isNotBlank() }?.let { sms ->
                val dest = digitsForTel(sms)
                if (dest.isNotEmpty()) {
                    TextButton(onClick = { onOpenUrl("sms:$dest") }) {
                        Text(Strings.crisisHelpSms(sms))
                    }
                }
            }
        }
    }
}
