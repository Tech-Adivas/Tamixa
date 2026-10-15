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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.domain.VoiceProfile
import com.tamixa.platform.currentTimeMillis
import com.tamixa.platform.rememberLocalFileController
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
import com.tamixa.ui.viewmodel.SavedVoicePlayback
import com.tamixa.util.TamixaLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceUploadScreen(
    uploadState: UiState<*>,
    profilesState: UiState<List<VoiceProfile>>,
    voiceStories: List<Story>,
    onUpload: (ByteArray, String) -> Unit,
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
    onNavigateToProfile: () -> Unit = {},
    savedPlayback: SavedVoicePlayback? = null,
    onPlayProfile: (Long) -> Unit = {},
    onStopProfile: () -> Unit = {},
    /** Dev-only: record and preview on-device without a subscription; recordings are never uploaded. */
    allowLocalRecordingTest: Boolean = false,
    /** Deletes a voice on the server (after the user confirms). */
    onDeleteProfile: (Long) -> Unit = {},
    /** Voices whose delete is in progress. */
    deletingProfileIds: Set<Long> = emptySet(),
    /** Whether the parent agreed to voice cloning; otherwise recording / picking asks first. */
    hasVoiceConsent: Boolean = false,
    /** Records voice cloning consent on the server; false when that failed. */
    onGiveVoiceConsent: suspend () -> Boolean = { false }
) {
    val withVoiceConsent = com.tamixa.ui.components.rememberVoiceConsentGate(hasVoiceConsent, onGiveVoiceConsent)
    var showRecordDialog by remember { mutableStateOf(false) }
    // No new recording or upload while one is being sent, so a double tap can't upload it twice.
    val isUploading = uploadState is UiState.Loading
    val canRecord = (isPremium || allowLocalRecordingTest) && !isUploading
    var profileToDelete by remember { mutableStateOf<Long?>(null) }

    profileToDelete?.let { profileId ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { profileToDelete = null },
            shape = com.tamixa.ui.theme.TamixaDialogDefaults.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(Strings.deleteRecordingTitle(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(Strings.deleteRecordingMessage(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    profileToDelete = null
                    onDeleteProfile(profileId)
                }) {
                    Text(Strings.yes(), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { profileToDelete = null }) {
                    Text(Strings.no())
                }
            }
        )
    }

    // Saved-recording playback: the controller is mounted only while a downloaded file is available.
    val playbackScope = rememberCoroutineScope()
    val savedFileUri = savedPlayback?.fileUri
    val savedController = rememberLocalFileController(
        fileUri = savedFileUri,
        storyTitle = Strings.playSavedRecording(),
        storyTheme = "",
        scope = playbackScope,
        onProgressChanged = { p -> if (p >= 1f) onStopProfile() }
    )
    var savedPlaybackStarted by remember(savedFileUri) { mutableStateOf(false) }
    LaunchedEffect(savedFileUri, savedController.isReady) {
        // Android's MediaPlayer starts on load; AVPlayer may not — start once if it is ready but idle.
        if (savedFileUri != null && savedController.isReady && !savedPlaybackStarted) {
            savedPlaybackStarted = true
            if (!savedController.isPlaying) savedController.playPause()
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { onStopProfile() }
    }

    LaunchedEffect(Unit) { onLoadProfiles() }
    LaunchedEffect(uploadState) {
        if (uploadState is UiState.Success) onLoadProfiles()
    }

    if (showRecordDialog) {
        com.tamixa.platform.FamilyVoiceRecordDialog(
            onDismiss = { showRecordDialog = false },
            onRecordingComplete = { bytes ->
                showRecordDialog = false
                if (isPremium) {
                    // Unique name: the backend stores reference audio under voices/{parentId}/{fileName}.
                    onUpload(bytes, "recorded_voice_${currentTimeMillis()}.m4a")
                } else {
                    // Local recording test: keep upload out of the loop.
                    TamixaLog.i("VoiceUploadScreen", "Local recording test complete (${bytes.size} bytes); not uploaded")
                }
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
                modifier = Modifier.fillMaxWidth().alpha(if (canRecord) 1f else 0.75f),
                onClick = if (canRecord) { { withVoiceConsent { showRecordDialog = true } } } else ({ }),
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
                        val profile = (uploadState as? UiState.Success<*>)?.data as? VoiceProfile
                        if (profile != null && profile.id >= 0) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            colors = TamixaCardColors.primaryContainer(),
                            shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                        ) {
                            Text(
                                Strings.recordingSaved(),
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
                                    modifier = Modifier.fillMaxWidth().padding(TamixaDesignTokens.cardSpacing),
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
                item(key = "profiles-title") {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        Strings.myRecordings(),
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
                                        Text(Strings.noRecordingsYet(), style = MaterialTheme.typography.bodyMedium, color = TamixaContentColors.cardSecondary())
                                        Text(Strings.tapMicToRecordFirst(), style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary())
                                    }
                                }
                            }
                        } else {
                            // "Recording 1" is the oldest; numbers follow the order they were recorded.
                            val recordingNumbers = state.data.sortedBy { it.id }
                                .withIndex().associate { (index, p) -> p.id to index + 1 }
                            items(state.data, key = { it.id }) { profile ->
                                Card(
                                    shape = RoundedCornerShape(TamixaDesignTokens.inputRadius),
                                    colors = TamixaCardColors.surface(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    val isActive = savedPlayback?.profileId == profile.id
                                    ListItem(
                                        headlineContent = { Text(Strings.savedRecordingNumber(recordingNumbers.getValue(profile.id)), color = TamixaContentColors.cardPrimary()) },
                                        supportingContent = savedPlayback?.errorMessage?.takeIf { isActive }?.let { msg ->
                                            { Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                                        },
                                        leadingContent = { Icon(Icons.Default.Mic, contentDescription = Strings.voiceCtaCloneYourVoice(), tint = MaterialTheme.colorScheme.primary) },
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                when {
                                                    isActive && savedPlayback?.isLoading == true ->
                                                        CircularProgressIndicator(Modifier.size(24.dp))
                                                    isActive && savedPlayback?.fileUri != null ->
                                                        IconButton(onClick = onStopProfile) {
                                                            Icon(Icons.Default.Stop, contentDescription = Strings.stopPlayback(), tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    else ->
                                                        IconButton(onClick = { onPlayProfile(profile.id) }) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = Strings.playSavedRecording(), tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                }
                                                if (profile.id in deletingProfileIds) {
                                                    CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp))
                                                } else {
                                                    IconButton(onClick = { profileToDelete = profile.id }) {
                                                        Icon(Icons.Default.Delete, contentDescription = Strings.deleteVoice(), tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
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
            }
        }
    }
}
