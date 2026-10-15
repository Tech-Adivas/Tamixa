package com.tamixa.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.*
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.platform.LocalAvatarImage
import com.tamixa.platform.LocalVoiceRecording
import com.tamixa.platform.rememberImagePickerLauncher
import com.tamixa.platform.rememberLocalFileController
import com.tamixa.util.TamixaLog
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

/**
 * Hub screen for "My voice & Avatar" tab: single entry to voice cloning and avatar upload.
 * Links cloned voice and avatar; after recording voice the flow continues to clone, then optionally upload image (if credits).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVoiceAndAvatarScreen(
    onNavigateToVoice: () -> Unit,
    onNavigateToAvatar: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToShortContent: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onBack: () -> Unit,
    isPremiumForVoice: Boolean = false,
    isPremiumForAvatar: Boolean = false,
    onNavigateToSubscription: () -> Unit = {},
    hasMyVoice: Boolean = false,
    onBackToStory: (() -> Unit)? = null,
    /**
     * Dev-only local recording test: when true, tapping the mic card opens the recorder on this screen
     * instead of the Subscription screen. Set from isLocalVoiceRecordingTestEnabled()
     * (Android dev debug builds only; always false on qa/prod/release and iOS).
     */
    allowLocalRecordingTest: Boolean = false,
    /** Uploads on-device recordings / the active photo that aren't backed up yet (local first, then S3 via the backend). */
    onSyncLocalMedia: suspend () -> Unit = {},
    /** Deletes a saved recording on the device and on the server (see LocalMediaSyncRepository.deleteVoiceRecording). */
    onDeleteVoiceRecording: suspend (LocalVoiceRecording) -> Unit = { com.tamixa.platform.deleteLocalVoiceRecording(it.uri) },
    /** Whether the parent agreed to voice cloning; otherwise the local recorder asks first (recordings get backed up). */
    hasVoiceConsent: Boolean = false,
    /** Records voice cloning consent on the server; false when that failed. */
    onGiveVoiceConsent: suspend () -> Boolean = { false }
) {
    val withVoiceConsent = com.tamixa.ui.components.rememberVoiceConsentGate(hasVoiceConsent, onGiveVoiceConsent)
    val canOpenVoice = isPremiumForVoice || allowLocalRecordingTest

    // Local voice recording: true while the recording dialog is shown on top of this screen.
    var showRecordDialog by remember { mutableStateOf(false) }

    // Mic card tap. Previously non-premium users always went to the static Subscription screen.
    //  - Premium: VoiceUpload screen (record + upload to backend), unchanged.
    //  - Dev local test: open the existing FamilyVoiceRecordDialog right here (record, stop, play,
    //    record again, cancel). The recording is saved on the device first, then backed up to the
    //    backend (S3 + voice cloning) by onSyncLocalMedia.
    //  - Otherwise: Subscription screen, unchanged.
    val onVoiceCardClick: () -> Unit = when {
        isPremiumForVoice -> onNavigateToVoice
        allowLocalRecordingTest -> { { withVoiceConsent { showRecordDialog = true } } }
        else -> onNavigateToSubscription
    }

    // Saved local recordings (app-private storage, survive restarts), newest first. Each Save adds one.
    val scope = rememberCoroutineScope()
    var savedRecordings by remember { mutableStateOf<List<LocalVoiceRecording>>(emptyList()) }
    // Local first, then backup: true while pending recordings / the active photo are being uploaded.
    var isSyncing by remember { mutableStateOf(false) }
    // Recording waiting for the user to confirm its deletion (phone + cloud), or null.
    var recordingToDelete by remember { mutableStateOf<LocalVoiceRecording?>(null) }
    // URI of the recording currently playing, or null. Only one plays at a time.
    var playingUri by remember { mutableStateOf<String?>(null) }
    // Player is mounted only while playing; a null URI releases it. Android MediaPlayer starts on load.
    rememberLocalFileController(
        fileUri = playingUri,
        storyTitle = Strings.playSavedRecording(),
        storyTheme = "",
        scope = scope,
        onProgressChanged = { p -> if (p >= 1f) playingUri = null }
    )

    // Saved local avatar photos (app-private storage, survive restarts), newest first. Each pick adds one.
    var savedPhotos by remember { mutableStateOf<List<LocalAvatarImage>>(emptyList()) }
    // Active profile photo (shown on the Me screen instead of the default person icon).
    var activePhotoUri by remember { mutableStateOf<String?>(null) }
    suspend fun reloadLocalMedia() {
        savedRecordings = com.tamixa.platform.listLocalVoiceRecordings()
        savedPhotos = com.tamixa.platform.listLocalAvatarImages()
        activePhotoUri = com.tamixa.platform.getActiveLocalAvatarImageUri()
    }
    // Uploads whatever is still only on the device, then refreshes the "backed up" labels.
    fun syncLocalMedia() {
        if (!allowLocalRecordingTest || isSyncing) return
        scope.launch {
            isSyncing = true
            try {
                onSyncLocalMedia()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                TamixaLog.w("MyVoiceAndAvatarScreen", "Local media sync failed", e)
            } finally {
                reloadLocalMedia()
                isSyncing = false
            }
        }
    }
    LaunchedEffect(allowLocalRecordingTest) {
        if (allowLocalRecordingTest) {
            reloadLocalMedia()
            syncLocalMedia()
        }
    }
    // Reuses the platform gallery picker (Android: GetContent "image/*"); the photo is copied into app storage.
    val launchPhotoPicker = rememberImagePickerLauncher { bytes, contentType ->
        if (bytes == null) {
            TamixaLog.i("MyVoiceAndAvatarScreen", "No photo picked")
            return@rememberImagePickerLauncher
        }
        scope.launch {
            val saved = com.tamixa.platform.saveLocalAvatarImage(bytes, contentType)
            if (saved != null) {
                savedPhotos = listOf(saved) + savedPhotos
                // A newly picked photo becomes the active profile photo.
                com.tamixa.platform.setActiveLocalAvatarImage(saved.uri)
                activePhotoUri = saved.uri
            }
            TamixaLog.i(
                "MyVoiceAndAvatarScreen",
                "Local photo ${if (saved != null) "saved" else "save failed"} (${bytes.size} bytes); " +
                    "${savedPhotos.size} on device; backing up"
            )
            if (saved != null) syncLocalMedia()
        }
    }

    // Avatar card tap. Previously non-premium users always went to the static Subscription screen.
    //  - Premium with a voice: AvatarUpload screen (upload to backend), unchanged.
    //  - Premium without a voice: no-op (card asks to create a voice first), unchanged.
    //  - Dev local test: open the gallery picker and keep the photo on the device; the active photo is then
    //    backed up as the profile avatar by onSyncLocalMedia.
    //  - Otherwise: Subscription screen, unchanged.
    val onAvatarCardClick: () -> Unit = when {
        isPremiumForAvatar && hasMyVoice -> onNavigateToAvatar
        isPremiumForAvatar -> ({ })
        allowLocalRecordingTest -> launchPhotoPicker
        else -> onNavigateToSubscription
    }

    // Reuses the platform recorder (Android: MediaRecorder + MediaPlayer; mic permission is requested inside).
    if (showRecordDialog) {
        com.tamixa.platform.FamilyVoiceRecordDialog(
            onDismiss = { showRecordDialog = false },
            onRecordingComplete = { bytes ->
                showRecordDialog = false
                // Add the recording to the on-device list (earlier ones are kept), then back it up to the server.
                scope.launch {
                    val saved = com.tamixa.platform.saveLocalVoiceRecording(bytes)
                    if (saved != null) savedRecordings = listOf(saved) + savedRecordings
                    TamixaLog.i(
                        "MyVoiceAndAvatarScreen",
                        "Local recording ${if (saved != null) "saved" else "save failed"} (${bytes.size} bytes); " +
                            "${savedRecordings.size} on device; backing up"
                    )
                    if (saved != null) syncLocalMedia()
                }
            }
        )
    }
    recordingToDelete?.let { recording ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            shape = com.tamixa.ui.theme.TamixaDialogDefaults.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    Strings.deleteRecordingTitle(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    Strings.deleteRecordingMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    recordingToDelete = null
                    if (playingUri == recording.uri) playingUri = null
                    scope.launch {
                        try {
                            onDeleteVoiceRecording(recording)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            TamixaLog.w("MyVoiceAndAvatarScreen", "Deleting recording failed", e)
                        }
                        savedRecordings = com.tamixa.platform.listLocalVoiceRecordings()
                        TamixaLog.i("MyVoiceAndAvatarScreen", "Recording deleted; ${savedRecordings.size} left")
                    }
                }) {
                    Text(Strings.yes(), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { recordingToDelete = null }) {
                    Text(Strings.no())
                }
            }
        )
    }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.tabMyVoiceAndAvatar(),
                onBack = onBackToStory ?: onBack,
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
        val colorScheme = MaterialTheme.colorScheme
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AppScreenBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Scrollable so a long list of saved recordings stays reachable.
                    .verticalScroll(rememberScrollState())
                    .padding(TamixaDesignTokens.screenPadding)
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.voiceScreenHeadline(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = TamixaColors.cream
                )
                Text(
                    text = Strings.voiceScreenSubline(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamixaColors.cream.copy(alpha = 0.9f)
                )
                Card(
                    // Was: if (canOpenVoice) onNavigateToVoice else onNavigateToSubscription
                    onClick = onVoiceCardClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                            vertical = TamixaDesignTokens.smallSpacing,
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = Strings.voiceCtaCloneYourVoice(),
                            tint = TamixaColors.goldAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                        Text(
                            Strings.voiceCtaCloneYourVoice(),
                            style = MaterialTheme.typography.titleLarge,
                            color = TamixaContentColors.cardPrimary()
                        )
                        Text(
                            if (canOpenVoice) Strings.tapToStartRecording() else Strings.uploadVoicePremiumPrompt(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary()
                        )
                    }
                }
                // Saved local recordings: one row each with play / stop and delete. Dev local test only.
                if (allowLocalRecordingTest && savedRecordings.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                                vertical = TamixaDesignTokens.smallSpacing,
                            )
                        ) {
                            BackUpNowButton(
                                visible = savedRecordings.any { !it.backedUp } && !isSyncing,
                                onClick = { syncLocalMedia() }
                            )
                            savedRecordings.forEachIndexed { index, recording ->
                                val isPlaying = playingUri == recording.uri
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { playingUri = if (isPlaying) null else recording.uri }) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) Strings.stopPlayback() else Strings.playSavedRecording(),
                                            tint = TamixaColors.goldAccent
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        // Oldest is "Recording 1"; list is newest first.
                                        Text(
                                            Strings.savedRecordingNumber(savedRecordings.size - index),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TamixaContentColors.cardPrimary()
                                        )
                                        Text(
                                            formatRecordingTime(recording.createdAtMillis),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TamixaContentColors.cardSecondary()
                                        )
                                        LocalMediaBackupLabel(backedUp = recording.backedUp, isSyncing = isSyncing)
                                    }
                                    // Asks first: deleting removes the recording from this phone and from the cloud.
                                    IconButton(onClick = { recordingToDelete = recording }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = Strings.deleteSavedRecording(),
                                            tint = TamixaContentColors.cardSecondary()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Card(
                    // Was: if (premium && hasMyVoice) onNavigateToAvatar else if (!premium) onNavigateToSubscription else no-op
                    onClick = onAvatarCardClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                            vertical = TamixaDesignTokens.smallSpacing,
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📖",
                                fontSize = 48.sp
                            )
                        }
                        Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                        Text(
                            Strings.avatarScreenHeadline(),
                            style = MaterialTheme.typography.titleLarge,
                            color = TamixaContentColors.cardPrimary()
                        )
                        Text(
                            when {
                                !isPremiumForAvatar && allowLocalRecordingTest -> Strings.avatarCtaAddPhoto()
                                !isPremiumForAvatar -> Strings.avatarUploadVoiceAndImageSuperPremium()
                                !hasMyVoice -> Strings.avatarCreateVoiceFirst()
                                else -> Strings.avatarCtaAddPhoto()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary()
                        )
                    }
                }
                // Saved local photos: horizontal thumbnails, each with delete. Dev local test only.
                if (allowLocalRecordingTest && savedPhotos.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(TamixaDesignTokens.smallSpacing),
                            horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
                        ) {
                            savedPhotos.forEach { photo ->
                                val isActive = photo.uri == activePhotoUri
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Tap a thumbnail to make it the active profile photo; the active one is outlined + ✓.
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        // Coil loads the file:// URI from app storage.
                                        coil3.compose.AsyncImage(
                                            model = photo.uri,
                                            contentDescription = Strings.avatarScreenHeadline(),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(96.dp)
                                                .clip(RoundedCornerShape(TamixaDesignTokens.inputRadius))
                                                .border(
                                                    width = if (isActive) 3.dp else 0.dp,
                                                    color = if (isActive) TamixaColors.goldAccent else Color.Transparent,
                                                    shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                                                )
                                                .clickable {
                                                    activePhotoUri = photo.uri
                                                    scope.launch {
                                                        com.tamixa.platform.setActiveLocalAvatarImage(photo.uri)
                                                        // The newly active photo replaces the one on the server.
                                                        syncLocalMedia()
                                                    }
                                                    TamixaLog.i("MyVoiceAndAvatarScreen", "Active profile photo changed")
                                                }
                                        )
                                        if (isActive) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = TamixaColors.goldAccent,
                                                modifier = Modifier.padding(4.dp).size(22.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        formatRecordingTime(photo.createdAtMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TamixaContentColors.cardSecondary()
                                    )
                                    // The server keeps one profile photo, so only the active one is backed up.
                                    if (isActive) LocalMediaBackupLabel(backedUp = photo.backedUp, isSyncing = isSyncing)
                                    IconButton(onClick = {
                                        scope.launch {
                                            if (com.tamixa.platform.deleteLocalAvatarImage(photo.uri)) {
                                                savedPhotos = savedPhotos.filterNot { it.uri == photo.uri }
                                                // If the active photo was deleted, the newest remaining one becomes active.
                                                activePhotoUri = com.tamixa.platform.getActiveLocalAvatarImageUri()
                                                TamixaLog.i("MyVoiceAndAvatarScreen", "Local photo deleted; ${savedPhotos.size} left")
                                            }
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = Strings.deleteSavedPhoto(),
                                            tint = TamixaContentColors.cardSecondary()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Saved" / "Not saved yet" (or "Saving…" while uploading) under a saved item. */
@Composable
private fun LocalMediaBackupLabel(backedUp: Boolean, isSyncing: Boolean) {
    Text(
        text = when {
            backedUp -> Strings.mediaSaved()
            isSyncing -> Strings.mediaSaving()
            else -> Strings.mediaNotSavedYet()
        },
        style = MaterialTheme.typography.labelSmall,
        color = if (backedUp) TamixaColors.goldAccent else TamixaContentColors.cardSecondary()
    )
}

/** Retries the upload of items still only on the device. */
@Composable
private fun BackUpNowButton(visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(Strings.retry(), color = TamixaColors.goldAccent)
    }
}

/** Saved-recording timestamp as local "dd-MM-yyyy HH:mm" (numeric, so it reads the same in every app language). */
private fun formatRecordingTime(epochMillis: Long): String {
    val t = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    fun Int.two() = toString().padStart(2, '0')
    return "${t.dayOfMonth.two()}-${t.monthNumber.two()}-${t.year} ${t.hour.two()}:${t.minute.two()}"
}
