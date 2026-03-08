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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.araro.network.ConsentRecordDto
import com.araro.network.ExportJobDto
import com.araro.network.ListeningProgressDto
import com.araro.ui.components.AraroLanguageLogo
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    languageCode: String,
    useSystemTheme: Boolean = true,
    darkMode: Boolean,
    preferredVoiceProfile: String = com.araro.util.AraroConstants.VOICE_PROFILE_DEFAULT,
    consentRecords: List<ConsentRecordDto>,
    exportJobs: List<ExportJobDto>,
    listeningProgress: ListeningProgressDto?,
    settingsLoading: Boolean,
    exporting: Boolean,
    onLanguageChange: (String) -> Unit,
    onUseSystemThemeChange: (Boolean) -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit,
    onPreferredVoiceChange: (String) -> Unit = {},
    onLoadSettings: () -> Unit,
    onRequestDataExport: () -> Unit,
    onOpenUrl: (String) -> Unit = {},
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onNavigateToVoiceUpload: (() -> Unit)? = null,
    onNavigateToAvatarUpload: (() -> Unit)? = null,
    isPremiumForAvatar: Boolean = false
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            com.araro.ui.components.AraroScreenTopBar(
                title = Strings.settings(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            StarryNightBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(AraroDesignTokens.screenPadding)
                    .verticalScroll(scrollState)
            ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AraroLanguageLogo(
                    languageCode = languageCode,
                    size = 72.dp
                )
            }
            Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        Strings.selectLanguage(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = languageCode == "ta",
                                onClick = { onLanguageChange("ta") },
                                label = { Text(Strings.tamil()) }
                            )
                            FilterChip(
                                selected = languageCode == "hi",
                                onClick = { onLanguageChange("hi") },
                                label = { Text(Strings.hindi()) }
                            )
                            FilterChip(
                                selected = languageCode == "en",
                                onClick = { onLanguageChange("en") },
                                label = { Text(Strings.english()) }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = languageCode == "te",
                                onClick = { onLanguageChange("te") },
                                label = { Text(Strings.telugu()) }
                            )
                            FilterChip(
                                selected = languageCode == "kn",
                                onClick = { onLanguageChange("kn") },
                                label = { Text(Strings.kannada()) }
                            )
                            FilterChip(
                                selected = languageCode == "ml",
                                onClick = { onLanguageChange("ml") },
                                label = { Text(Strings.malayalam()) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        Strings.preferredNarrationVoice(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        Strings.useVoiceGloballyHint(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = preferredVoiceProfile == com.araro.util.AraroConstants.VOICE_PROFILE_DEFAULT,
                            onClick = { onPreferredVoiceChange(com.araro.util.AraroConstants.VOICE_PROFILE_DEFAULT) },
                            label = { Text(com.araro.ui.strings.Strings.voiceLabel("default", false)) }
                        )
                        FilterChip(
                            selected = preferredVoiceProfile == "calm",
                            onClick = { onPreferredVoiceChange("calm") },
                            label = { Text(com.araro.ui.strings.Strings.voiceLabel("calm", true)) }
                        )
                        FilterChip(
                            selected = preferredVoiceProfile == "family",
                            onClick = { onPreferredVoiceChange("family") },
                            label = { Text(com.araro.ui.strings.Strings.voiceLabel("family", false)) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
            onNavigateToVoiceUpload?.let { onVoice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVoice() },
                    shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            Strings.useVoiceForStories(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
            }
            onNavigateToAvatarUpload?.let { onAvatar ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isPremiumForAvatar) { onAvatar() },
                    shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            Strings.avatarUpload(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isPremiumForAvatar) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isPremiumForAvatar) {
                            Text(
                                Strings.upgrade(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            Strings.useSystemTheme(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = useSystemTheme,
                            onCheckedChange = onUseSystemThemeChange
                        )
                    }
                    if (!useSystemTheme) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                Strings.darkMode(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = darkMode,
                                onCheckedChange = onDarkModeChange
                            )
                        }
                    }
                }
            }

            if (settingsLoading) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                listeningProgress?.let { p ->
                    Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        Strings.listeningProgress(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                            Spacer(Modifier.height(12.dp))
                            Text("${Strings.storiesStarted()}: ${p.storiesStarted}", style = MaterialTheme.typography.bodyMedium)
                            Text("${Strings.storiesCompleted()}: ${p.storiesCompleted}", style = MaterialTheme.typography.bodyMedium)
                            Text("${Strings.completionRate()}: ${(p.completionRate * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            Strings.consentHistory(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        if (consentRecords.isEmpty()) {
                            Text(Strings.noConsentRecords(), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            consentRecords.forEachIndexed { i, c ->
                                Text("${c.consentType} (v${c.version}) — ${c.grantedAt}", style = MaterialTheme.typography.bodySmall)
                                if (i < consentRecords.size - 1) Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(AraroDesignTokens.cardSpacing))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            Strings.dataExport(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(Strings.dataExportDescription(), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onRequestDataExport,
                            enabled = !exporting,
                            shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)
                        ) {
                            Text(if (exporting) Strings.requesting() else Strings.requestDataExport())
                        }
                        if (exportJobs.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            exportJobs.forEach { j ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Job #${j.id}: ${j.status} — ${j.requestedAt}", style = MaterialTheme.typography.bodySmall)
                                    if (j.downloadUrl != null) {
                                        Spacer(Modifier.padding(8.dp))
                                        TextButton(onClick = { onOpenUrl(j.downloadUrl) }) {
                                            Text(Strings.download())
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(AraroDesignTokens.sectionSpacing))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(Strings.logout())
            }
        }
        }
    }
}
