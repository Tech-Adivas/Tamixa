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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.domain.VoiceProfile
import com.tamixa.ui.components.StoryCard
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaEmojiDisplay
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceUploadScreen(
    uploadState: UiState<*>,
    profilesState: UiState<List<VoiceProfile>>,
    voiceStories: List<Story>,
    onUpload: (ByteArray, String) -> Unit,
    onPickAudio: () -> Unit,
    onLoadProfiles: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onBack: () -> Unit,
    isPremium: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    apiBaseUrl: String? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToAvatar: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMyVoiceAndAvatarHub: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToShortContent: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var showRecordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onLoadProfiles() }
    LaunchedEffect(uploadState) {
        if (uploadState is UiState.Success) onLoadProfiles()
    }

    if (showRecordDialog) {
        com.tamixa.platform.FamilyVoiceRecordDialog(
            onDismiss = { showRecordDialog = false },
            onRecordingComplete = { bytes ->
                showRecordDialog = false
                onUpload(bytes, "recorded_voice.m4a")
            }
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.tabMyVoiceAndAvatar(),
                onBack = onBack,
                useTransparentBackground = true
            )
        },
        bottomBar = {
            TamixaBottomBar(
                selectedTab = TamixaTab.MyVoiceAndAvatar,
                onHome = onNavigateToHome,
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AppScreenBackground(showClouds = false)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(TamixaDesignTokens.screenPadding)
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "headline") {
                    Text(
                        text = Strings.voiceScreenHeadline(),
                        style = MaterialTheme.typography.titleLarge,
                        color = TamixaColors.cream
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Strings.voiceScreenSubline(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TamixaColors.lavenderGlow.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(20.dp))
                }
                if (!isPremium) {
                    item(key = "premium") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                            colors = TamixaCardColors.primaryContainer(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    Strings.benefitRecordYourVoice(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TamixaContentColors.onPrimaryContainer()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    Strings.voiceUploadPremiumRequired(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TamixaContentColors.onPrimaryContainer()
                                )
                                Spacer(Modifier.height(16.dp))
                                TamixaPrimaryButton(
                                    onClick = onUpgradeClick,
                                    text = Strings.upgrade(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
                item(key = "record") {
            Card(
                modifier = Modifier.fillMaxWidth().alpha(if (isPremium) 1f else 0.75f),
                onClick = if (isPremium) { { showRecordDialog = true } } else ({ }),
                shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = Strings.voiceCtaCloneYourVoice(),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            Strings.voiceCtaCloneYourVoice(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TamixaContentColors.cardPrimary()
                        )
                        Text(
                            Strings.tapToStartRecording(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TamixaContentColors.cardSecondary()
                        )
                    }
                }
            }
                    Spacer(Modifier.height(14.dp))
                }
                item(key = "upload") {
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (isPremium) 1f else 0.75f),
                        onClick = if (isPremium) onPickAudio else ({}),
                        shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = Strings.tapToUploadAudio(),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(Strings.tapToUploadAudio(), style = MaterialTheme.typography.titleMedium, color = TamixaContentColors.cardPrimary())
                                Text(Strings.useVoiceForStories(), style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary())
                            }
                        }
                    }
                }
                when (uploadState) {
                    is UiState.Idle -> {}
                    is UiState.Loading -> item(key = "upload-loading") {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is UiState.Success -> item(key = "upload-success") {
                        val profile = (uploadState as UiState.Success<VoiceProfile>).data
                        if (profile.id >= 0) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            colors = TamixaCardColors.primaryContainer(),
                            shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                        ) {
                            Text(
                                Strings.voiceProfileCreated(),
                                color = TamixaContentColors.onPrimaryContainer(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        if (isPremium) {
                            Spacer(Modifier.height(12.dp))
                            Card(
                                onClick = onNavigateToAvatar,
                                colors = TamixaCardColors.secondaryContainer(),
                                shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = Strings.addAvatarAfterVoice(), tint = TamixaColors.goldAccent, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(Strings.addAvatarAfterVoice(), style = MaterialTheme.typography.bodyMedium, color = TamixaContentColors.onSecondaryContainer())
                                    }
                                }
                            }
                        }
                        }
                    }
                    is UiState.Error -> item(key = "upload-error") {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            colors = TamixaCardColors.errorContainer(),
                            shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                        ) {
                            Text(
                                (uploadState as UiState.Error).message,
                                color = TamixaContentColors.onErrorContainer(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                item(key = "stories-title") {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = Strings.voiceSectionStoriesWithYourVoice(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TamixaColors.cream
                    )
                    Spacer(Modifier.height(12.dp))
                }
                item(key = "stories") {
                    if (voiceStories.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                            colors = TamixaCardColors.surface(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                Strings.voiceSectionNoStoriesYet(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TamixaContentColors.cardSecondary(),
                                modifier = Modifier.padding(20.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(voiceStories.take(10), key = { it.id }) { story ->
                                Box(modifier = Modifier.width(140.dp)) {
                                    StoryCard(
                                        story = story,
                                        onClick = { if (story.status == StoryStatus.READY) onStoryClick(story) },
                                        apiBaseUrl = apiBaseUrl
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
                item(key = "profiles-title") {
                    Text(
                        Strings.yourVoiceProfiles(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TamixaColors.cream
                    )
                    Spacer(Modifier.height(14.dp))
                }
                when (val state = profilesState) {
                    is UiState.Idle -> {}
                    is UiState.Loading -> item(key = "profiles-loading") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(32.dp))
                        }
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item(key = "profiles-empty") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                                    colors = TamixaCardColors.surface()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        TamixaEmojiDisplay(emoji = "🎤", fontSize = 48.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text(Strings.noProfilesYet(), style = MaterialTheme.typography.bodyMedium, color = TamixaContentColors.cardSecondary())
                                        Text(Strings.uploadAudioForFirstProfile(), style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary())
                                    }
                                }
                            }
                        } else {
                            items(state.data, key = { it.id }) { profile ->
                                Card(
                                    shape = RoundedCornerShape(TamixaDesignTokens.inputRadius),
                                    colors = TamixaCardColors.surface(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    ListItem(
                                        headlineContent = { Text(Strings.profileNumber(profile.id), color = TamixaContentColors.cardPrimary()) },
                                        leadingContent = { Icon(Icons.Default.Mic, contentDescription = Strings.voiceCtaCloneYourVoice(), tint = MaterialTheme.colorScheme.primary) }
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                    is UiState.Error -> item(key = "profiles-error") {
                        Card(
                            colors = TamixaCardColors.errorContainer(),
                            shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                        ) {
                            Text(state.message, color = TamixaContentColors.onErrorContainer(), modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }
}
