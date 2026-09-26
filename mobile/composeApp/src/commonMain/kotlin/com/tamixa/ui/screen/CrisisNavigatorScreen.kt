package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamixa.platform.speakPlainText
import com.tamixa.platform.stopPlainTextSpeech
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.safety.CrisisHelpline
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaDesignTokens

private enum class CrisisWorkflow {
    MoneyCyber,
    Police,
    Medical,
}

private fun spokenScriptFor(workflow: CrisisWorkflow): String {
    val titleBody = when (workflow) {
        CrisisWorkflow.MoneyCyber ->
            Strings.crisisNavigatorScriptMoneyTitle() + ". " + Strings.crisisNavigatorScriptMoneyBody()
        CrisisWorkflow.Police ->
            Strings.crisisNavigatorScriptPoliceTitle() + ". " + Strings.crisisNavigatorScriptPoliceBody()
        CrisisWorkflow.Medical ->
            Strings.crisisNavigatorScriptMedicalTitle() + ". " + Strings.crisisNavigatorScriptMedicalBody()
    }
    return titleBody.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
}

private fun digitsForTel(raw: String): String {
    val t = raw.trim()
    if (t.startsWith("+")) return "+" + t.drop(1).filter { it.isDigit() }
    return t.filter { it.isDigit() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisNavigatorScreen(
    onBack: () -> Unit,
    onOpenFullDirectory: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onTrackScreenView: () -> Unit = {},
) {
    LaunchedEffect(Unit) { onTrackScreenView() }
    var selected by remember { mutableStateOf<CrisisWorkflow?>(null) }
    val scroll = rememberScrollState()
    DisposableEffect(Unit) {
        onDispose { stopPlainTextSpeech() }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.crisisNavigatorTitle(),
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
                text = Strings.crisisNavigatorIntro(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenFullDirectory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
            ) {
                Text(Strings.crisisNavigatorOpenHelplines())
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = Strings.crisisNavigatorChoosePrompt(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selected == CrisisWorkflow.MoneyCyber,
                    onClick = { selected = CrisisWorkflow.MoneyCyber },
                    label = { Text(Strings.crisisNavigatorWorkflowMoney()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                FilterChip(
                    selected = selected == CrisisWorkflow.Police,
                    onClick = { selected = CrisisWorkflow.Police },
                    label = { Text(Strings.crisisNavigatorWorkflowPolice()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                FilterChip(
                    selected = selected == CrisisWorkflow.Medical,
                    onClick = { selected = CrisisWorkflow.Medical },
                    label = { Text(Strings.crisisNavigatorWorkflowMedical()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(18.dp))
            when (selected) {
                CrisisWorkflow.MoneyCyber -> WorkflowScriptCard(
                    title = Strings.crisisNavigatorScriptMoneyTitle(),
                    body = Strings.crisisNavigatorScriptMoneyBody(),
                )
                CrisisWorkflow.Police -> WorkflowScriptCard(
                    title = Strings.crisisNavigatorScriptPoliceTitle(),
                    body = Strings.crisisNavigatorScriptPoliceBody(),
                )
                CrisisWorkflow.Medical -> WorkflowScriptCard(
                    title = Strings.crisisNavigatorScriptMedicalTitle(),
                    body = Strings.crisisNavigatorScriptMedicalBody(),
                )
                null -> { }
            }
            if (selected != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val w = selected ?: return@OutlinedButton
                            val script = spokenScriptFor(w).trim()
                            if (script.isNotEmpty()) {
                                speakPlainText(script, Strings.languageCode)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                    ) {
                        Text(Strings.crisisNavigatorListenAloud())
                    }
                    OutlinedButton(
                        onClick = { stopPlainTextSpeech() },
                        shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                    ) {
                        Text(Strings.crisisNavigatorStopSpeaking())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = Strings.crisisHelpDisclaimer(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = Strings.crisisNavigatorExpertsTitle(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Strings.crisisNavigatorExpertsIntro(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Strings.crisisNavigatorExpertResources().forEach { line ->
                ExpertHelplineCard(line = line, onOpenUrl = onOpenUrl)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun WorkflowScriptCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
        colors = TamixaCardColors.surface(),
        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation),
    ) {
        Column(Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ExpertHelplineCard(
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
        }
    }
}
