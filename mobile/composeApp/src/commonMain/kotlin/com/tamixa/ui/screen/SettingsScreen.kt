package com.tamixa.ui.screen

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.network.ConsentRecordDto
import com.tamixa.network.ExportJobDto
import com.tamixa.network.ListeningProgressDto
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaLanguageLogo
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    languageCode: String,
    useSystemTheme: Boolean = true,
    darkMode: Boolean,
    preferredVoiceProfile: String = com.tamixa.util.TamixaConstants.VOICE_PROFILE_DEFAULT,
    consentRecords: List<ConsentRecordDto>,
    exportJobs: List<ExportJobDto>,
    listeningProgress: ListeningProgressDto?,
    settingsLoading: Boolean,
    settingsLoadError: String? = null,
    exporting: Boolean,
    onLanguageChange: (String) -> Unit,
    onUseSystemThemeChange: (Boolean) -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit,
    onPreferredVoiceChange: (String) -> Unit = {},
    onLoadSettings: () -> Unit,
    onRequestDataExport: () -> Unit,
    onOpenUrl: (String) -> Unit = {},
    onLogout: () -> Unit,
    onDeleteAccount: (() -> Unit)? = null,
    onBack: () -> Unit,
    onNavigateToVoiceUpload: (() -> Unit)? = null,
    onNavigateToAvatarUpload: (() -> Unit)? = null,
    onNavigateToSubscription: (() -> Unit)? = null,
    isPremiumForAvatar: Boolean = false,
    isPremiumForVoice: Boolean = false,
    onNavigateToHome: () -> Unit = {},
    onNavigateToAvatar: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMyVoiceAndAvatar: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToShortContent: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    storyArtPersonalizationOptIn: Boolean = false,
    onStoryArtPersonalizationOptInChange: (Boolean) -> Unit = {},
    apiBaseUrlOverride: String = "",
    subscriptionWebUrlOverride: String = "",
    serverEnvironmentMessage: String? = null,
    serverEnvironmentError: String? = null,
    onApiBaseUrlOverrideChange: (String) -> Unit = {},
    onSubscriptionWebUrlOverrideChange: (String) -> Unit = {},
    onSaveServerEnvironment: () -> Unit = {},
    onClearServerEnvironment: () -> Unit = {},
    onDismissServerEnvironmentMessage: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.settings(),
                onBack = onBack,
                useTransparentBackground = true
            )
        },
        bottomBar = {
            TamixaBottomBar(
                selectedTab = TamixaTab.Settings,
                onHome = onNavigateToHome,
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AppScreenBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(TamixaDesignTokens.screenPadding)
                    .verticalScroll(scrollState)
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav)
            ) {
            if (settingsLoadError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                        Text(
                            text = settingsLoadError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onLoadSettings) {
                            Text(Strings.retry())
                        }
                    }
                }
                Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                TamixaLanguageLogo(
                    languageCode = languageCode,
                    size = 120.dp
                )
            }
            Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation + 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)
                ) {
                    Text(
                        Strings.selectLanguage(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = languageCode == "ta",
                                onClick = { onLanguageChange("ta") },
                                label = { Text(Strings.tamil(), style = MaterialTheme.typography.labelLarge) }
                            )
                            FilterChip(
                                selected = languageCode == "hi",
                                onClick = { onLanguageChange("hi") },
                                label = { Text(Strings.hindi(), style = MaterialTheme.typography.labelLarge) }
                            )
                            FilterChip(
                                selected = languageCode == "en",
                                onClick = { onLanguageChange("en") },
                                label = { Text(Strings.english(), style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = languageCode == "te",
                                onClick = { onLanguageChange("te") },
                                label = { Text(Strings.telugu(), style = MaterialTheme.typography.labelLarge) }
                            )
                            FilterChip(
                                selected = languageCode == "kn",
                                onClick = { onLanguageChange("kn") },
                                label = { Text(Strings.kannada(), style = MaterialTheme.typography.labelLarge) }
                            )
                            FilterChip(
                                selected = languageCode == "ml",
                                onClick = { onLanguageChange("ml") },
                                label = { Text(Strings.malayalam(), style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation + 2.dp)
            ) {
                Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                    Text(
                        Strings.preferredNarrationVoice(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Strings.useVoiceGloballyHint(),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = TamixaContentColors.cardSecondary()
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = preferredVoiceProfile == com.tamixa.util.TamixaConstants.VOICE_PROFILE_DEFAULT,
                            onClick = { onPreferredVoiceChange(com.tamixa.util.TamixaConstants.VOICE_PROFILE_DEFAULT) },
                            label = { Text(com.tamixa.ui.strings.Strings.voiceLabel("default", false)) }
                        )
                        FilterChip(
                            selected = preferredVoiceProfile == "calm",
                            onClick = { onPreferredVoiceChange("calm") },
                            label = { Text(com.tamixa.ui.strings.Strings.voiceLabel("calm", true)) }
                        )
                        FilterChip(
                            selected = preferredVoiceProfile == "family",
                            onClick = { onPreferredVoiceChange("family") },
                            label = { Text(com.tamixa.ui.strings.Strings.voiceLabel("family", false)) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation + 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TamixaDesignTokens.cardContentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            Strings.storyArtPersonalizationSettingTitle(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            Strings.storyArtPersonalizationSettingSummary(),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                            color = TamixaContentColors.cardSecondary()
                        )
                    }
                    Switch(
                        checked = storyArtPersonalizationOptIn,
                        onCheckedChange = onStoryArtPersonalizationOptInChange
                    )
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
            onNavigateToVoiceUpload?.let { onVoice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPremiumForVoice || onNavigateToSubscription == null) {
                                onVoice()
                            } else {
                                onNavigateToSubscription()
                            }
                        },
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surface()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TamixaDesignTokens.cardContentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                Strings.voiceUpload(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Strings.useVoiceForStories(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TamixaContentColors.cardSecondary()
                            )
                        }
                        if (isPremiumForVoice) {
                            TextButton(onClick = { onVoice() }) {
                                Text(
                                    Strings.voiceUpload(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            TextButton(onClick = { onNavigateToSubscription?.invoke() }) {
                                Text(
                                    Strings.upgrade(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
            }
            onNavigateToAvatarUpload?.let { onAvatar ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isPremiumForAvatar) { onAvatar() },
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surface()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TamixaDesignTokens.cardContentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            Strings.avatarUpload(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TamixaContentColors.cardPrimary()
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
                Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation + 2.dp)
            ) {
                Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            Strings.useSystemTheme(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                            color = TamixaContentColors.cardPrimary()
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
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = TamixaContentColors.cardSecondary()
                            )
                            Switch(
                                checked = darkMode,
                                onCheckedChange = onDarkModeChange
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                colors = TamixaCardColors.surfaceVariant(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
            ) {
                Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                    Text(
                        Strings.serverEnvironmentTitle(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Strings.serverEnvironmentDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TamixaContentColors.cardSecondary()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiBaseUrlOverride,
                        onValueChange = onApiBaseUrlOverrideChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(Strings.apiBaseUrlHint()) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = subscriptionWebUrlOverride,
                        onValueChange = onSubscriptionWebUrlOverrideChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(Strings.subscriptionWebUrlHint()) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onSaveServerEnvironment) {
                            Text(Strings.saveServerEnvironment())
                        }
                        OutlinedButton(onClick = onClearServerEnvironment) {
                            Text(Strings.clearServerEnvironment())
                        }
                    }
                    serverEnvironmentError?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    serverEnvironmentMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = TamixaContentColors.cardSecondary()
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onDismissServerEnvironmentMessage) {
                            Text(Strings.close())
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
                    Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surfaceVariant(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                    Text(
                        Strings.listeningProgress(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                            Spacer(Modifier.height(12.dp))
                            Text("${Strings.storiesStarted()}: ${p.storiesStarted}", style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                            Text("${Strings.storiesCompleted()}: ${p.storiesCompleted}", style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                            Text("${Strings.completionRate()}: ${(p.completionRate * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                        }
                    }
                }

                Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surfaceVariant()
                ) {
                    Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
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

                Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surfaceVariant()
                ) {
                    Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
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
                            shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
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

            if (onDeleteAccount != null) {
                Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surfaceVariant(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                        Text(
                            Strings.deleteAccount(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(Strings.deleteAccountDescription(), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showDeleteAccountDialog = true },
                            shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(Strings.deleteAccount())
                        }
                    }
                }
                if (showDeleteAccountDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAccountDialog = false },
                        shape = TamixaDialogDefaults.shape,
                        title = { Text(Strings.deleteAccount()) },
                        text = { Text(Strings.deleteAccountDescription()) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteAccountDialog = false
                                    onDeleteAccount()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(Strings.deleteAccountConfirm())
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteAccountDialog = false }) {
                                Text(Strings.cancel())
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadius),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(Strings.logout())
            }
            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    shape = TamixaDialogDefaults.shape,
                    title = { Text(Strings.logoutConfirmTitle()) },
                    text = { Text(Strings.logoutConfirmMessage()) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutConfirm = false
                                onLogout()
                            },
                            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                        ) {
                            Text(Strings.logout())
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) {
                            Text(Strings.cancel())
                        }
                    }
                )
            }
        }
        }
    }
}
