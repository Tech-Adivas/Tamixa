package com.tamixa.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.tamixa.domain.Story
import com.tamixa.network.WordTiming
import com.tamixa.util.NarrationTextUtils
import com.tamixa.util.TamixaConstants
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.MagicLoadingOverlay
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults
import com.tamixa.ui.components.NarrativeSceneOverlay
import com.tamixa.ui.components.activeNarrativeScene
import com.tamixa.ui.components.StoryCoverImage
import com.tamixa.ui.components.narrativeLayerWorthShowing
import com.tamixa.ui.components.platformIsReduceMotionEnabled
import com.tamixa.ui.strings.Strings
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow

private val PlayerScreenInk = TamixaColors.cream
private val PlayerScreenMuted = TamixaColors.grayText
private val PlayerTrackMuted = Color.White.copy(alpha = 0.3f)
private val PlayerCardStroke = TamixaColors.cream.copy(alpha = 0.34f)

/** Circular hero chrome — uses [clickable] so taps work above video/cover layers. */
@Composable
private fun PlayerHeroCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(
                elevation = 3.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF2C2520).copy(alpha = 0.14f),
                spotColor = Color.White.copy(alpha = 0.1f),
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.97f))
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Back control above loading / error / empty states (hero uses its own pinned control). */
@Composable
private fun AudioPlayerOverlayBack(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlayerHeroCircleButton(onClick = onBack, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = Strings.back(),
            tint = TamixaColors.onInputSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AudioPlayerStoryContentHost(
    story: Story?,
    reduceMotion: Boolean,
    content: @Composable (Story?) -> Unit
) {
    if (reduceMotion) {
        key(story?.id) {
            content(story)
        }
    } else {
        Crossfade(targetState = story, modifier = Modifier.fillMaxSize(), label = "playerContent") { s ->
            content(s)
        }
    }
}

private fun formatPlaybackClock(totalSec: Int): String =
    "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"

/** Callbacks for story retention analytics. Optional; no-op if null. */
data class StoryAnalyticsCallbacks(
    val onStoryStarted: (Story, Int) -> Unit = { _, _ -> },
    val onProgress: (Story, Float, Int) -> Unit = { _, _, _ -> },
    val onCompleted: (Story, Int) -> Unit = { _, _ -> },
    val onStoppedEarly: (Story, Int) -> Unit = { _, _ -> }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    story: Story?,
    isPlayerReady: Boolean = true,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onBack: () -> Unit,
    progress: Float = 0f,
    isLoading: Boolean = false,
    /** Optional message shown during loading (e.g. "Preparing audio..."). */
    loadingMessage: String? = null,
    /** Error message when audio failed to load; shown with Retry. */
    loadError: String? = null,
    onRetryLoad: (() -> Unit)? = null,
    onRewind: (() -> Unit)? = null,
    onFastForward: (() -> Unit)? = null,
    onSleepTimer: () -> Unit = {},
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onRemix: ((Long, String) -> Unit)? = null,
    analytics: StoryAnalyticsCallbacks? = null,
    selectedVoice: String = com.tamixa.util.TamixaConstants.VOICE_PROFILE_DEFAULT,
    onVoiceAndModeChange: ((String, String) -> Unit)? = null,
    availableVoices: List<com.tamixa.network.VoiceOptionDto> = emptyList(),
    onNavigateToVoiceUpload: (() -> Unit)? = null,
    onNavigateToMyVoiceAndAvatar: (() -> Unit)? = null,
    hasAvatarAvailable: Boolean = false,
    avatarStatus: String? = null,
    playbackMode: String = "default",
    isPremiumForVoice: Boolean = false,
    onNavigateToSubscription: (() -> Unit)? = null,
    apiBaseUrl: String? = null,
    storytellingAvatarUrl: String? = null,
    storytellingAvatarVideoContent: (@Composable () -> Unit)? = null,
    hostStoryClipVideoContent: (@Composable () -> Unit)? = null,
    wordTimings: List<com.tamixa.network.WordTiming>? = null,
    /** Actual audio duration in seconds from backend. Use for progress so display matches real playback. */
    durationSeconds: Int? = null,
    narrativeScenes: List<com.tamixa.network.NarrativeSceneVisual>? = null,
    /** Phase 3: parent opted in to future story-art personalization (disclosure in Settings). */
    storyArtPersonalizationOptIn: Boolean = false,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onAddToList: (() -> Unit)? = null,
    onVolumeClick: (() -> Unit)? = null,
    bottomNavSelectedTab: TamixaTab = TamixaTab.Home,
    onBottomNavHome: () -> Unit = {},
    onBottomNavLibrary: () -> Unit = {},
    onBottomNavFunAndLearn: () -> Unit = {},
    onBottomNavProfile: () -> Unit = {},
    /** Optional: open story quiz (e.g. when story is linked to a child). */
    onOpenQuiz: (() -> Unit)? = null,
    /** Optional line under the title (Learn cue or theme for generated). */
    playbackSubtitle: String? = null,
    /** Library interactive graph: small parent-facing cue near the title. */
    showInteractivePracticeChip: Boolean = false,
    /** Current interactive segment narration; when set, transcript matches segment audio instead of [Story.content]. */
    interactiveSceneTranscript: String? = null,
    /** On-device TTS file playback — no server word timings; hide voice-synced highlight. */
    playbackUsesDeviceTts: Boolean = false,
    /** Playback speed (0.75x, 1x, 1.25x, 1.5x) */
    playbackSpeed: Float = 1.0f,
    /** Callback when speed is changed */
    onSpeedChange: ((Float) -> Unit)? = null,
    /** Related stories for recommendations */
    relatedStories: List<Story> = emptyList(),
    /** Callback when related story is tapped */
    onRelatedStoryClick: ((Story) -> Unit)? = null,
) {
    var showVoicePremiumDialog by remember { mutableStateOf(false) }
    var playMenuExpanded by remember { mutableStateOf(false) }
    val reduceStoryMotion = platformIsReduceMotionEnabled()
    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenBackground(
            modifier = Modifier.fillMaxSize(),
            showStars = true,
            showClouds = true,
            animateStars = true,
            ambientPresence = true
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AudioPlayerStoryContentHost(story = story, reduceMotion = reduceStoryMotion) { currentStory ->
                    when {
                        loadError != null && currentStory != null -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        Strings.audioLoadFailed(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = PlayerScreenInk,
                                        textAlign = TextAlign.Center
                                    )
                                    if (onRetryLoad != null) {
                                        Button(
                                            onClick = onRetryLoad,
                                            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                                        ) {
                                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(Strings.retry())
                                        }
                                    }
                                }
                                AudioPlayerOverlayBack(
                                    onBack = onBack,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .statusBarsPadding()
                                        .padding(start = 8.dp, top = 8.dp)
                                        .zIndex(4f)
                                )
                            }
                        }
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                MagicLoadingOverlay(
                                    progressColor = com.tamixa.ui.theme.TamixaColors.goldAccent,
                                    message = loadingMessage
                                )
                                AudioPlayerOverlayBack(
                                    onBack = onBack,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .statusBarsPadding()
                                        .padding(start = 8.dp, top = 8.dp)
                                        .zIndex(4f)
                                )
                            }
                        }
                        currentStory == null -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    "No story",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PlayerScreenMuted,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                AudioPlayerOverlayBack(
                                    onBack = onBack,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .statusBarsPadding()
                                        .padding(start = 8.dp, top = 8.dp)
                                        .zIndex(4f)
                                )
                            }
                        }
                        else -> {
                            PlayerContent(
                                story = currentStory,
                                isPlayerReady = isPlayerReady,
                                isPlaying = isPlaying,
                                progress = progress,
                                onPlayPause = onPlayPause,
                                onRewind = onRewind,
                                onFastForward = onFastForward,
                                onRemix = onRemix,
                                analytics = analytics,
                                apiBaseUrl = apiBaseUrl,
                                storytellingAvatarUrl = storytellingAvatarUrl,
                                storytellingAvatarVideoContent = storytellingAvatarVideoContent,
                                hostStoryClipVideoContent = hostStoryClipVideoContent,
                                wordTimings = wordTimings,
                                durationSeconds = durationSeconds,
                                narrativeScenes = narrativeScenes,
                                storyArtPersonalizationOptIn = storyArtPersonalizationOptIn,
                                onBack = onBack,
                                onOpenVoiceMenu = onVoiceAndModeChange?.let { { playMenuExpanded = true } },
                                onOpenQuiz = onOpenQuiz,
                                isFavorite = isFavorite,
                                onFavoriteToggle = onFavoriteToggle,
                                onAddToList = onAddToList,
                                onVolumeClick = onVolumeClick,
                                onSleepTimer = onSleepTimer,
                                onDownload = onDownload,
                                playbackSubtitle = playbackSubtitle,
                                showInteractivePracticeChip = showInteractivePracticeChip,
                                interactiveSceneTranscript = interactiveSceneTranscript,
                                playbackUsesDeviceTts = playbackUsesDeviceTts,
                                onShare = onShare,
                                playbackSpeed = playbackSpeed,
                                onSpeedChange = onSpeedChange,
                                relatedStories = relatedStories,
                                onRelatedStoryClick = onRelatedStoryClick,
                            )
                        }
                    }
                }
            }
            TamixaBottomBar(
                selectedTab = bottomNavSelectedTab,
                onHome = onBottomNavHome,
                onLibrary = onBottomNavLibrary,
                onFunAndLearn = onBottomNavFunAndLearn,
                onProfile = onBottomNavProfile
            )
            if (showVoicePremiumDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showVoicePremiumDialog = false },
                    title = { Text(Strings.premiumVoiceTitle()) },
                    text = { Text(Strings.uploadVoicePremiumPrompt()) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showVoicePremiumDialog = false
                                onNavigateToSubscription?.invoke()
                            },
                            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                        ) { Text(Strings.upgrade()) }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showVoicePremiumDialog = false }) {
                            Text(Strings.cancel())
                        }
                    }
                )
            }
            if (playMenuExpanded && onVoiceAndModeChange != null) {
                AudioPlayerVoiceMenuSheet(
                    onDismiss = { playMenuExpanded = false },
                    onVoiceAndModeChange = onVoiceAndModeChange,
                    selectedVoice = selectedVoice,
                    playbackMode = playbackMode,
                    avatarStatus = avatarStatus,
                    availableVoices = availableVoices,
                    hasAvatarAvailable = hasAvatarAvailable,
                    isPremiumForVoice = isPremiumForVoice,
                    onNavigateToVoiceUpload = onNavigateToVoiceUpload,
                    onNavigateToMyVoiceAndAvatar = onNavigateToMyVoiceAndAvatar,
                    onNavigateToSubscription = onNavigateToSubscription,
                    onRequestPremiumDialog = { showVoicePremiumDialog = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioPlayerVoiceMenuSheet(
    onDismiss: () -> Unit,
    onVoiceAndModeChange: (String, String) -> Unit,
    selectedVoice: String,
    playbackMode: String,
    avatarStatus: String?,
    availableVoices: List<com.tamixa.network.VoiceOptionDto>,
    hasAvatarAvailable: Boolean,
    isPremiumForVoice: Boolean,
    onNavigateToVoiceUpload: (() -> Unit)?,
    onNavigateToMyVoiceAndAvatar: (() -> Unit)?,
    onNavigateToSubscription: (() -> Unit)?,
    onRequestPremiumDialog: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clonedVoices = remember(availableVoices) {
        availableVoices.filter { it.voiceProfile.startsWith("cloned:", ignoreCase = true) }
    }
    val familyVoice = remember(availableVoices) {
        availableVoices.find { it.voiceProfile.equals("family", ignoreCase = true) }
    }
    val myVoiceOptions = remember(clonedVoices, familyVoice) {
        if (clonedVoices.isNotEmpty()) clonedVoices
        else if (familyVoice != null) listOf(familyVoice)
        else emptyList()
    }
    val hasMyVoice = myVoiceOptions.isNotEmpty()
    val firstMyVoiceProfile = myVoiceOptions.firstOrNull()?.voiceProfile
    val isDefaultSelected = playbackMode == "default"
    val isMyVoiceSelected = playbackMode == "my_voice"
    val isAvatarSelected = playbackMode == "avatar"
    val isAvatarGenerating = avatarStatus == "VIDEO_GENERATING"
    val isAvatarFailed = avatarStatus == "VIDEO_FAILED"
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TamixaDesignTokens.screenPadding)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = Strings.playerVoiceAndPlayMode(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TamixaDesignTokens.cardRadius))
                    .clickable {
                        onDismiss()
                        onVoiceAndModeChange(TamixaConstants.VOICE_PROFILE_DEFAULT, "default")
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    Strings.playScreenDefaultVoice(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isDefaultSelected) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = Strings.playScreenYourVoices(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            myVoiceOptions.forEach { voiceOpt ->
                val label = voiceOpt.displayLabel?.takeIf { it.isNotBlank() } ?: Strings.playScreenMyVoice()
                val isThisSelected = isMyVoiceSelected && voiceOpt.voiceProfile.equals(selectedVoice, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TamixaDesignTokens.cardRadius))
                        .clickable {
                            onDismiss()
                            onVoiceAndModeChange(voiceOpt.voiceProfile, "my_voice")
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isThisSelected) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (clonedVoices.size < 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TamixaDesignTokens.cardRadius))
                        .clickable {
                            onDismiss()
                            when {
                                isPremiumForVoice && onNavigateToVoiceUpload != null -> onNavigateToVoiceUpload()
                                onNavigateToSubscription != null -> onRequestPremiumDialog()
                                onNavigateToVoiceUpload != null -> onNavigateToVoiceUpload()
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (hasMyVoice) Strings.playScreenUploadRecordAnotherVoice()
                        else Strings.playScreenUploadRecordVoice(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = Strings.playScreenYourAvatars(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            val avatarCount = if (hasAvatarAvailable) 1 else 0
            if (hasAvatarAvailable) {
                val voiceForAvatar = if (isMyVoiceSelected && myVoiceOptions.any { it.voiceProfile.equals(selectedVoice, ignoreCase = true) }) {
                    selectedVoice
                } else {
                    firstMyVoiceProfile
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TamixaDesignTokens.cardRadius))
                        .clickable {
                            onDismiss()
                            if (voiceForAvatar != null) onVoiceAndModeChange(voiceForAvatar, "avatar")
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        when {
                            isAvatarGenerating -> Strings.playScreenAvatarGenerating()
                            isAvatarFailed -> Strings.playScreenAvatarTrouble()
                            else -> Strings.playScreenAvatar()
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isAvatarSelected) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (avatarCount < 5 && onNavigateToMyVoiceAndAvatar != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TamixaDesignTokens.cardRadius))
                        .clickable {
                            onDismiss()
                            onNavigateToMyVoiceAndAvatar()
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        Strings.playScreenUploadAvatar(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerQuickAction(
    icon: @Composable () -> Unit,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = TamixaColors.nightSkySurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, PlayerCardStroke),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                icon()
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) PlayerScreenMuted else PlayerScreenMuted.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun PlayerContent(
    story: com.tamixa.domain.Story,
    isPlayerReady: Boolean,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onRewind: (() -> Unit)?,
    onFastForward: (() -> Unit)?,
    onRemix: ((Long, String) -> Unit)?,
    analytics: StoryAnalyticsCallbacks? = null,
    apiBaseUrl: String? = null,
    storytellingAvatarUrl: String? = null,
    storytellingAvatarVideoContent: (@Composable () -> Unit)? = null,
    hostStoryClipVideoContent: (@Composable () -> Unit)? = null,
    wordTimings: List<com.tamixa.network.WordTiming>? = null,
    durationSeconds: Int? = null,
    narrativeScenes: List<com.tamixa.network.NarrativeSceneVisual>? = null,
    storyArtPersonalizationOptIn: Boolean = false,
    onBack: () -> Unit,
    onOpenVoiceMenu: (() -> Unit)? = null,
    onOpenQuiz: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onAddToList: (() -> Unit)? = null,
    onVolumeClick: (() -> Unit)? = null,
    onSleepTimer: () -> Unit = {},
    onDownload: (() -> Unit)? = null,
    playbackSubtitle: String? = null,
    showInteractivePracticeChip: Boolean = false,
    interactiveSceneTranscript: String? = null,
    playbackUsesDeviceTts: Boolean = false,
    onShare: (() -> Unit)? = null,
    playbackSpeed: Float = 1.0f,
    onSpeedChange: ((Float) -> Unit)? = null,
    relatedStories: List<Story> = emptyList(),
    onRelatedStoryClick: ((Story) -> Unit)? = null,
) {
    val storyTextForDisplay = remember(story.id, story.content) {
        NarrationTextUtils.stripRemainingMarkers(story.content)
    }
    val sceneTranscriptStripped = remember(interactiveSceneTranscript) {
        interactiveSceneTranscript?.trim()?.takeIf { it.isNotBlank() }?.let {
            NarrationTextUtils.stripRemainingMarkers(it)
        }.orEmpty()
    }
    val transcriptForDisplay =
        if (showInteractivePracticeChip && sceneTranscriptStripped.isNotBlank()) {
            sceneTranscriptStripped
        } else {
            storyTextForDisplay
        }
    // Interactive episodes: no server word timings; we approximate read-along from segment text + estimated duration
    // while [progress] is driven by the real segment MP3 (see interactiveApproximateWordTimings).
    val showSyncedReadAlong =
        storyTextForDisplay.isNotBlank() && !playbackUsesDeviceTts && !showInteractivePracticeChip
    val totalSec = remember(durationSeconds, showInteractivePracticeChip, sceneTranscriptStripped, story.readingTimeMinutes) {
        when {
            durationSeconds != null && durationSeconds > 0 -> durationSeconds
            showInteractivePracticeChip && sceneTranscriptStripped.isNotBlank() -> {
                val words = sceneTranscriptStripped.split(Regex("\\s+")).count { it.isNotBlank() }
                (words * 3 + 8).coerceIn(12, 180)
            }
            else -> (story.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
        }
    }
    /** Approximate word windows for interactive segment MP3s (no server timings); spaced evenly over [totalSec]. */
    val interactiveApproximateWordTimings = remember(
        transcriptForDisplay,
        totalSec,
        showInteractivePracticeChip,
        sceneTranscriptStripped,
    ) {
        if (showInteractivePracticeChip && sceneTranscriptStripped.isNotBlank() && totalSec > 0) {
            buildUniformWordTimingsForReadAlong(transcriptForDisplay, totalSec)
        } else {
            null
        }
    }
    val accent = com.tamixa.ui.theme.TamixaColors.goldAccent
    var showRemixDialog by remember(story.id) { mutableStateOf(false) }
    var remixInstruction by remember(story.id) { mutableStateOf("") }
    val remixHandler = onRemix?.takeIf { story.parentId != 0L }
    var playerOverflowOpen by remember(story.id) { mutableStateOf(false) }
    LaunchedEffect(story.id) {
        analytics?.onStoryStarted?.invoke(story, 0)
    }
    LaunchedEffect(progress) {
        val positionSec = (progress * totalSec).toInt()
        analytics?.onProgress?.invoke(story, progress, positionSec)
        if (progress >= 0.99f) analytics?.onCompleted?.invoke(story, totalSec)
    }
    val scrollState = rememberScrollState()
    val posSec = (progress * totalSec).toInt()
    val seekSec = (TamixaConstants.SEEK_MS / 1000).toInt()
    val snippetSurface = TamixaColors.nightSkySurface
    val heroControlTint = TamixaColors.onInputSurface
    val narrativeVisuals = narrativeScenes.orEmpty()
    val chapterTicks = remember(narrativeVisuals) {
        narrativeVisuals.map { it.startProgress }.filter { it in 0.02f..0.98f }.distinct().sorted()
    }
    val learnStory = story.theme.contains("Learn", ignoreCase = true) ||
        (story.category?.contains("Learn", ignoreCase = true) == true)
    val activeSceneIndex = activeNarrativeScene(progress, narrativeVisuals)?.sceneIndex
    var prevSceneIndexForCue by remember(story.id) { mutableStateOf<Int?>(null) }
    var reflectionCueVisible by remember(story.id) { mutableStateOf(false) }
    LaunchedEffect(activeSceneIndex, learnStory, narrativeVisuals.size) {
        val cur = activeSceneIndex
        if (!learnStory || narrativeVisuals.size < 2 || cur == null) {
            prevSceneIndexForCue = cur
            return@LaunchedEffect
        }
        if (prevSceneIndexForCue != null && prevSceneIndexForCue != cur) {
            reflectionCueVisible = true
            delay(4200)
            reflectionCueVisible = false
        }
        prevSceneIndexForCue = cur
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
        val heroHasAvatarMedia =
            storytellingAvatarVideoContent != null || storytellingAvatarUrl != null
        val heroShape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = TamixaDesignTokens.storyIllustrationFrameRadius,
            bottomEnd = TamixaDesignTokens.storyIllustrationFrameRadius
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .height(300.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .clip(heroShape)
            ) {
                StoryCoverImage(
                    story = story,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    apiBaseUrl = apiBaseUrl
                )
                if (!heroHasAvatarMedia) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.42f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.28f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )
                }
                if (narrativeLayerWorthShowing(narrativeVisuals)) {
                    NarrativeSceneOverlay(
                        progress = progress,
                        scenes = narrativeVisuals,
                        apiBaseUrl = apiBaseUrl,
                        reduceMotion = platformIsReduceMotionEnabled(),
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0.5f)
                    )
                }
                if (storytellingAvatarVideoContent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0.65f)
                    ) {
                        storytellingAvatarVideoContent()
                    }
                } else if (storytellingAvatarUrl != null) {
                    coil3.compose.AsyncImage(
                        model = storytellingAvatarUrl,
                        contentDescription = Strings.yourAvatar(),
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0.65f),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .zIndex(100f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerHeroCircleButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Strings.back(),
                        tint = heroControlTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                val showOverflowMenu = onOpenVoiceMenu != null || onOpenQuiz != null
                if (showOverflowMenu) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        PlayerHeroCircleButton(
                            onClick = { playerOverflowOpen = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = Strings.playerMoreActions(),
                                tint = heroControlTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = playerOverflowOpen,
                            onDismissRequest = { playerOverflowOpen = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            onOpenQuiz?.let { openQuiz ->
                                DropdownMenuItem(
                                    text = { Text(Strings.storyQuiz()) },
                                    onClick = {
                                        playerOverflowOpen = false
                                        openQuiz()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                                    }
                                )
                            }
                            onOpenVoiceMenu?.let { openVoice ->
                                DropdownMenuItem(
                                    text = { Text(Strings.playerVoiceAndPlayMode()) },
                                    onClick = {
                                        playerOverflowOpen = false
                                        openVoice()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Mic, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }
        }

        if (hostStoryClipVideoContent != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = Strings.hostStoryClipSectionLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = PlayerScreenMuted,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                hostStoryClipVideoContent()
            }
            Spacer(Modifier.height(8.dp))
        }

        if (storyArtPersonalizationOptIn) {
            Text(
                text = Strings.storyArtPersonalizationPlayerNote(),
                style = MaterialTheme.typography.labelSmall,
                color = PlayerScreenMuted,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = story.title?.takeIf { it.isNotBlank() } ?: story.theme,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = PlayerScreenInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            playbackSubtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PlayerScreenMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showInteractivePracticeChip) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = Strings.playerInteractivePracticeChip(),
                        modifier = Modifier
                            .shadow(
                                4.dp,
                                RoundedCornerShape(10.dp),
                                ambientColor = Color.Black.copy(alpha = 0.12f),
                                spotColor = Color.White.copy(alpha = 0.08f),
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        TamixaColors.deepTeal.copy(alpha = 0.92f),
                                        TamixaColors.terracotta.copy(alpha = 0.88f),
                                    ),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                    )
                }
            }
            if (showSyncedReadAlong) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = Strings.playerListenAlongTitle(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PlayerScreenInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                StoryTranscriptSection(
                    content = storyTextForDisplay,
                    progress = progress,
                    wordTimings = wordTimings,
                    durationSeconds = totalSec,
                )
            } else if (showInteractivePracticeChip && transcriptForDisplay.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (sceneTranscriptStripped.isNotBlank()) {
                        Strings.playerInteractiveSceneTranscriptTitle()
                    } else {
                        Strings.transcript()
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PlayerScreenInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                StoryTranscriptSection(
                    content = transcriptForDisplay,
                    progress = progress,
                    wordTimings = interactiveApproximateWordTimings,
                    durationSeconds = totalSec,
                    wordWeightedHeuristic = interactiveApproximateWordTimings == null && sceneTranscriptStripped.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (playbackUsesDeviceTts && storyTextForDisplay.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = Strings.playerDeviceReadAloudHint(),
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerScreenMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = snippetSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PlayerCardStroke),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = storyTextForDisplay,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PlayerScreenInk.copy(alpha = 0.92f),
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    )
                }
            }
            val hasParentExtras = story.parentId == 0L && (
                !story.parentContentNote.isNullOrBlank() ||
                    !story.speakAlongPrompt.isNullOrBlank() ||
                    story.parentDiscussionPrompts?.any { it.isNotBlank() } == true
                )
            if (hasParentExtras) {
                Spacer(Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = snippetSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PlayerCardStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            Strings.forParentsSectionTitle(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PlayerScreenInk,
                        )
                        story.parentContentNote?.takeIf { it.isNotBlank() }?.let { note ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                Strings.parentContentNoteLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = PlayerScreenMuted,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PlayerScreenInk.copy(alpha = 0.92f),
                            )
                        }
                        story.speakAlongPrompt?.takeIf { it.isNotBlank() }?.let { prompt ->
                            Spacer(Modifier.height(10.dp))
                            Text(
                                Strings.speakAlongLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = PlayerScreenMuted,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PlayerScreenInk.copy(alpha = 0.92f),
                            )
                        }
                        val prompts = story.parentDiscussionPrompts?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                        if (prompts.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                Strings.discussionPromptsLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = PlayerScreenMuted,
                            )
                            Spacer(Modifier.height(6.dp))
                            prompts.forEach { p ->
                                Text(
                                    "• $p",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PlayerScreenInk.copy(alpha = 0.92f),
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
            
            // Related Stories Section
            if (relatedStories.isNotEmpty() && onRelatedStoryClick != null) {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = Strings.playerRelatedStories(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PlayerScreenInk,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    relatedStories.take(3).forEach { relatedStory ->
                        Surface(
                            onClick = { onRelatedStoryClick(relatedStory) },
                            shape = RoundedCornerShape(TamixaDesignTokens.cardRadius),
                            color = snippetSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlayerCardStroke),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover image
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TamixaColors.warmSurfaceVariant)
                                ) {
                                    StoryCoverImage(
                                        story = relatedStory,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        apiBaseUrl = apiBaseUrl
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = relatedStory.title?.takeIf { it.isNotBlank() } ?: relatedStory.theme,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PlayerScreenInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        relatedStory.category?.let { category ->
                                            Text(
                                                text = category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PlayerScreenMuted
                                            )
                                        }
                                        if (relatedStory.readingTimeMinutes > 0) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PlayerScreenMuted
                                            )
                                            Text(
                                                text = "${relatedStory.readingTimeMinutes} min",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PlayerScreenMuted
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = Strings.play(),
                                    tint = accent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                PlayerQuickAction(
                    icon = {
                        Icon(
                            if (isFavorite) Icons.Default.Check else Icons.AutoMirrored.Outlined.PlaylistAdd,
                            contentDescription = null,
                            tint = if (isFavorite) accent else PlayerScreenInk,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = if (isFavorite) Strings.playerInList() else Strings.playerAddToList(),
                    enabled = onAddToList != null,
                    onClick = { onAddToList?.invoke() }
                )
                PlayerQuickAction(
                    icon = {
                        Icon(Icons.Default.Download, null, tint = PlayerScreenInk, modifier = Modifier.size(22.dp))
                    },
                    label = Strings.download(),
                    enabled = onDownload != null,
                    onClick = { onDownload?.invoke() }
                )
                PlayerQuickAction(
                    icon = {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null,
                            tint = if (isFavorite) accent else PlayerScreenInk,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = Strings.playerLike(),
                    enabled = onFavoriteToggle != null,
                    onClick = { onFavoriteToggle?.invoke() }
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        }

        Surface(
            color = TamixaColors.nightSkyBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPlaybackClock(posSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = PlayerScreenMuted,
                        modifier = Modifier.width(44.dp),
                        maxLines = 1
                    )
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp)
                            .padding(horizontal = 6.dp)
                    ) {
                        val frac = progress.coerceIn(0f, 1f)
                        val thumb = 14.dp
                        if (chapterTicks.isNotEmpty()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                                    .height(10.dp)
                            ) {
                                val tickColor = PlayerScreenInk.copy(alpha = 0.45f)
                                val h = size.height
                                chapterTicks.forEach { t ->
                                    val x = size.width * t.coerceIn(0f, 1f)
                                    drawRect(
                                        color = tickColor,
                                        topLeft = Offset(x - 1f, h * 0.15f),
                                        size = Size(2f, h * 0.7f),
                                    )
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accent,
                            trackColor = PlayerTrackMuted
                        )
                        val offsetX = (maxWidth - thumb) * frac
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = offsetX)
                                .size(thumb)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF2C2520).copy(alpha = 0.2f),
                                    spotColor = accent.copy(alpha = 0.35f),
                                )
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                    Text(
                        text = formatPlaybackClock(totalSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = PlayerScreenMuted,
                        modifier = Modifier.width(44.dp),
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }
                if (reflectionCueVisible) {
                    Text(
                        text = Strings.playerReflectionCue(),
                        style = MaterialTheme.typography.bodySmall,
                        color = PlayerScreenInk.copy(alpha = 0.88f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onVolumeClick != null) {
                        IconButton(
                            onClick = onVolumeClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Volume", tint = PlayerScreenInk, modifier = Modifier.size(22.dp))
                        }
                    } else {
                        Spacer(Modifier.width(40.dp))
                    }
                    IconButton(
                        onClick = { onRewind?.invoke() },
                        enabled = isPlayerReady,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = Strings.playerSkipSeconds(seekSec), tint = PlayerScreenInk, modifier = Modifier.size(22.dp))
                    }
                    Surface(
                        onClick = { if (isPlayerReady) onPlayPause() },
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier
                            .size(58.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = Color(0xFF2C2520).copy(alpha = 0.22f),
                                spotColor = accent.copy(alpha = 0.28f),
                            ),
                        shadowElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isPlayerReady) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) Strings.pause() else Strings.play(),
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { onFastForward?.invoke() },
                        enabled = isPlayerReady,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = Strings.playerSkipSeconds(seekSec), tint = PlayerScreenInk, modifier = Modifier.size(22.dp))
                    }
                    if (onShare != null) {
                        IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = Strings.playerShare(),
                                tint = PlayerScreenInk,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    IconButton(onClick = onSleepTimer, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Schedule, contentDescription = Strings.sleepTimer(), tint = PlayerScreenInk, modifier = Modifier.size(22.dp))
                    }
                    if (remixHandler != null) {
                        IconButton(
                            onClick = { showRemixDialog = true },
                            enabled = !isPlaying,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = Strings.editStory(),
                                tint = if (isPlaying) PlayerScreenMuted.copy(alpha = 0.4f) else PlayerScreenInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                
                // Speed control
                if (onSpeedChange != null) {
                    Spacer(Modifier.height(12.dp))
                    var speedMenuExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { speedMenuExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            color = TamixaColors.nightSkySurfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlayerCardStroke),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = Strings.playerSpeed(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PlayerScreenMuted
                                )
                                Text(
                                    text = "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = PlayerScreenInk
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = PlayerScreenInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${speed}x")
                                            if (speed == playbackSpeed) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSpeedChange(speed)
                                        speedMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                if (showRemixDialog && remixHandler != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            showRemixDialog = false
                            remixInstruction = ""
                        },
                        shape = TamixaDialogDefaults.shape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                Strings.editStory(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = remixInstruction,
                                onValueChange = { remixInstruction = it },
                                placeholder = { Text(Strings.remixInstruction()) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (remixInstruction.isNotBlank()) {
                                        remixHandler(story.id, remixInstruction.trim())
                                        showRemixDialog = false
                                        remixInstruction = ""
                                    }
                                },
                                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                                enabled = remixInstruction.isNotBlank()
                            ) { Text(Strings.continueWith()) }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showRemixDialog = false
                                remixInstruction = ""
                            }) { Text(Strings.cancel()) }
                        }
                    )
                }
            }
        }
    }
}

/** Nudge highlight earlier so it tracks perceived speech (output / decoder latency). */
private const val READ_ALONG_TIMING_OFFSET_SEC = 0.08

/**
 * Evenly spaces [WordTiming] across [durationSeconds] using the same tokenization as [StoryTranscriptSection].
 * Used for interactive segment clips without server-side alignment; [progress] still comes from real audio duration.
 */
private fun buildUniformWordTimingsForReadAlong(content: String, durationSeconds: Int): List<WordTiming>? {
    if (durationSeconds <= 0 || content.isBlank()) return null
    val sentences = splitIntoSentences(content.trim())
    val words = sentences.flatMap { splitIntoWords(it) }
    if (words.isEmpty()) return null
    val dur = durationSeconds.toDouble()
    val n = words.size
    return words.mapIndexed { i, w ->
        val start = dur * i / n
        val end = dur * (i + 1) / n
        WordTiming(word = w, startSec = start, endSec = end)
    }
}

private fun splitIntoSentences(text: String): List<String> {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return emptyList()
    return trimmed
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun splitIntoWords(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    return text.split(Regex("\\s+")).filter { it.isNotBlank() }
}

private fun stripReadAlongEdgePunctuation(word: String): String {
    var w = word.trim()
    val trailing = ".,!?;:\"')」】』›»］)].…।॥"
    val leading = "(「【『‹«（［"
    while (w.isNotEmpty() && w.last() in trailing) w = w.dropLast(1)
    while (w.isNotEmpty() && w.first() in leading) w = w.drop(1)
    return w.trim()
}

private fun readAlongTokensMatch(timingWord: String, storyWord: String): Boolean {
    val a = stripReadAlongEdgePunctuation(timingWord)
    val b = stripReadAlongEdgePunctuation(storyWord)
    if (a == b) return true
    val asciiA = a.isNotEmpty() && a.all { it.code < 128 }
    val asciiB = b.isNotEmpty() && b.all { it.code < 128 }
    if (asciiA && asciiB && a.equals(b, ignoreCase = true)) return true
    return false
}

/**
 * Maps each [WordTiming] index to a flat word index in the story (all sentences concatenated).
 * Greedy sequential match tolerates small punctuation / tokenization differences vs assuming 1:1 counts.
 */
private fun alignWordTimingsToFlatStoryIndices(
    flatStoryWords: List<String>,
    timings: List<WordTiming>,
): IntArray {
    val out = IntArray(timings.size) { -1 }
    if (flatStoryWords.isEmpty() || timings.isEmpty()) return out
    var s = 0
    val maxLookahead = 8
    for (t in timings.indices) {
        val needle = timings[t].word
        if (stripReadAlongEdgePunctuation(needle).isEmpty()) {
            if (t > 0 && out[t - 1] >= 0) out[t] = out[t - 1]
            continue
        }
        var matched = false
        val scanEnd = kotlin.math.min(flatStoryWords.size, s + maxLookahead)
        for (tryS in s until scanEnd) {
            if (readAlongTokensMatch(needle, flatStoryWords[tryS])) {
                out[t] = tryS
                s = tryS + 1
                matched = true
                break
            }
        }
        if (!matched) {
            // Keep search window moving so later timings can re-sync.
            if (s < flatStoryWords.size) s++
        }
    }
    var carry = 0
    for (i in out.indices) {
        if (out[i] >= 0) carry = out[i]
        else out[i] = carry
    }
    carry = out.lastOrNull { it >= 0 } ?: 0
    for (i in out.indices.reversed()) {
        if (out[i] >= 0) carry = out[i]
        else out[i] = carry
    }
    return out
}

private fun flatStoryWordIndexToSentenceWord(
    flatIndex: Int,
    sentences: List<String>,
    startIndices: List<Int>,
): Pair<Int, Int> {
    if (sentences.isEmpty()) return 0 to 0
    val f = flatIndex.coerceAtLeast(0)
    val sentIdx = sentences.indices.lastOrNull { startIndices[it] <= f } ?: 0
    val start = startIndices[sentIdx]
    val endExclusive = startIndices.getOrElse(sentIdx + 1) { start }
    val wCount = (endExclusive - start).coerceAtLeast(1)
    val wordInSent = (f - start).coerceIn(0, wCount - 1)
    return sentIdx to wordInSent
}

/** Cumulative word count per sentence: sentenceStartWordIndex[i] = words in sentences 0..i-1. */
private fun sentenceStartWordIndices(sentences: List<String>): List<Int> {
    val indices = mutableListOf(0)
    for (s in sentences) {
        indices.add(indices.last() + splitIntoWords(s).size)
    }
    return indices
}

/** Delay (in word fraction) for heuristic-only sync when server word timings are absent. */
private const val TRANSCRIPT_HIGHLIGHT_DELAY = 0.42f

/**
 * Maps 0..1 playback progress to (sentenceIndex, fractionWithinSentence 0..1) using word counts per sentence.
 * Better for interactive segment clips than equal time per sentence.
 */
private fun progressToSentenceFractionByWordWeight(
    progress: Float,
    sentences: List<String>,
): Pair<Int, Float> {
    if (sentences.isEmpty()) return 0 to 0f
    val wordCounts = sentences.map { splitIntoWords(it).size.coerceAtLeast(1) }
    val total = wordCounts.sum().coerceAtLeast(1)
    val target = progress.coerceIn(0f, 1f) * total
    var acc = 0f
    for ((i, c) in wordCounts.withIndex()) {
        val end = acc + c
        if (target < end || i == wordCounts.lastIndex) {
            val frac = ((target - acc) / c.toFloat()).coerceIn(0f, 1f)
            return i to frac
        }
        acc = end
    }
    return sentences.lastIndex.coerceAtLeast(0) to 1f
}

/**
 * Karaoke-style transcript: one sentence at a time, word-by-word highlight synced to playback.
 * When [wordTimings] and [durationSeconds] are provided (from backend/voice transcription),
 * highlight is driven by current time for exact sync. Otherwise uses sentence-based heuristic.
 * When [wordWeightedHeuristic] is true (interactive segment, no timings), progress is mapped by word count per sentence.
 */
@Composable
private fun StoryTranscriptSection(
    content: String?,
    progress: Float,
    wordTimings: List<WordTiming>? = null,
    durationSeconds: Int = 0,
    wordWeightedHeuristic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val surfaceColor = com.tamixa.ui.theme.TamixaColors.nightSkySurfaceVariant.copy(alpha = 0.6f)
    val textColor = com.tamixa.ui.theme.TamixaColors.cream
    val highlightColor = com.tamixa.ui.theme.TamixaColors.goldAccent

    val sentences = remember(content) { content?.let { splitIntoSentences(it) } ?: emptyList() }
    val startIndices = remember(sentences) { sentenceStartWordIndices(sentences) }
    val sentenceCount = sentences.size
    val flatStoryWords = remember(sentences) { sentences.flatMap { splitIntoWords(it) } }
    val timingToFlat = remember(sentences, wordTimings, content) {
        if (wordTimings.isNullOrEmpty() || flatStoryWords.isEmpty()) null
        else alignWordTimingsToFlatStoryIndices(flatStoryWords, wordTimings)
    }

    val (currentSentenceIndex, highlightWordIndex) = if (
        !wordTimings.isNullOrEmpty() && durationSeconds > 0
    ) {
        val currentTimeSec = (progress * durationSeconds.toFloat() + READ_ALONG_TIMING_OFFSET_SEC)
            .toDouble()
            .coerceIn(0.0, durationSeconds.toDouble())
        val wordIndex = wordTimings.indexOfFirst { currentTimeSec >= it.startSec && currentTimeSec < it.endSec }
            .takeIf { it >= 0 }
            ?: wordTimings.indexOfLast { it.endSec <= currentTimeSec }.takeIf { it >= 0 }
            ?: 0
        val timingIdx = wordIndex.coerceIn(0, wordTimings.size - 1)
        val map = timingToFlat
        if (map != null && map.size == wordTimings.size) {
            val flatIdx = map[timingIdx].coerceIn(0, (flatStoryWords.size - 1).coerceAtLeast(0))
            flatStoryWordIndexToSentenceWord(flatIdx, sentences, startIndices)
        } else {
            val globalWordIndex = timingIdx
            val sentIdx = startIndices.indexOfFirst { it > globalWordIndex }.let { idx ->
                if (idx < 0) (startIndices.size - 2).coerceAtLeast(0) else (idx - 1).coerceAtLeast(0)
            }
            val wordInSent = (globalWordIndex - startIndices.getOrElse(sentIdx) { 0 }).coerceIn(0, Int.MAX_VALUE)
            sentIdx to wordInSent
        }
    } else {
        val (sentIdx, progressInSentence) =
            if (wordWeightedHeuristic && sentenceCount > 0) {
                progressToSentenceFractionByWordWeight(progress, sentences)
            } else {
                val sentenceProgress =
                    if (sentenceCount > 0) {
                        (progress * sentenceCount).toFloat().coerceIn(0f, (sentenceCount - 0.001f))
                    } else {
                        0f
                    }
                val si = sentenceProgress.toInt().coerceIn(0, (sentenceCount - 1).coerceAtLeast(0))
                val pis = (sentenceProgress - si).coerceIn(0f, 1f)
                si to pis
            }
        val sentenceWords = splitIntoWords(sentences.getOrNull(sentIdx) ?: "")
        val wordCountInSentence = sentenceWords.size
        val wordProgressInSentence = if (wordCountInSentence > 0) progressInSentence * wordCountInSentence else 0f
        val hi = if (wordCountInSentence > 0) {
            (wordProgressInSentence - TRANSCRIPT_HIGHLIGHT_DELAY).toInt().coerceIn(0, wordCountInSentence - 1)
        } else {
            0
        }
        sentIdx to hi
    }

    val currentSentence = sentences.getOrNull(currentSentenceIndex) ?: ""
    val sentenceWords = remember(currentSentence) { splitIntoWords(currentSentence) }
    val safeHighlightIndex = highlightWordIndex.coerceIn(0, (sentenceWords.size - 1).coerceAtLeast(0))

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(TamixaDesignTokens.inputRadius),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 108.dp, max = 288.dp)
        ) {
            if (content.isNullOrBlank() || currentSentence.isBlank()) {
                Text(
                    text = Strings.transcriptUnavailable(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.padding(TamixaDesignTokens.cardSpacing)
                )
            } else {
                val nextSentence = sentences.getOrNull(currentSentenceIndex + 1)
                val annotated = buildAnnotatedString {
                    var searchStart = 0
                    sentenceWords.forEachIndexed { wi, word ->
                        val wordStart = currentSentence.indexOf(word, searchStart).coerceIn(0, currentSentence.length)
                        val wordEnd = (wordStart + word.length).coerceAtMost(currentSentence.length)
                        if (wordStart > searchStart) append(currentSentence.substring(searchStart, wordStart))
                        when {
                            wi < safeHighlightIndex -> {
                                pushStyle(SpanStyle(color = textColor.copy(alpha = 0.45f)))
                                append(currentSentence.substring(wordStart, wordEnd))
                                pop()
                            }
                            wi == safeHighlightIndex -> {
                                pushStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold))
                                append(currentSentence.substring(wordStart, wordEnd))
                                pop()
                            }
                            else -> {
                                pushStyle(SpanStyle(color = textColor.copy(alpha = 0.7f)))
                                append(currentSentence.substring(wordStart, wordEnd))
                                pop()
                            }
                        }
                        searchStart = wordEnd
                        if (searchStart < currentSentence.length && currentSentence[searchStart].isWhitespace()) {
                            append(currentSentence[searchStart].toString())
                            searchStart++
                        }
                    }
                    if (searchStart < currentSentence.length) append(currentSentence.substring(searchStart))
                    if (nextSentence != null && nextSentence.isNotBlank()) {
                        append("\n\n")
                        pushStyle(SpanStyle(color = textColor.copy(alpha = 0.5f)))
                        append(nextSentence)
                        pop()
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textColor,
                            lineHeight = 28.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryCoverWithFallback(story: Story, apiBaseUrl: String? = null) {
    val baseUrl = apiBaseUrl
    val infiniteTransition = rememberInfiniteTransition(label = "coverScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        // Theme-based soft glow behind cover
        Box(
            modifier = Modifier
                .size(216.dp)
                .scale(scale * 1.02f)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                    CircleShape
                )
        )
        Surface(
            modifier = Modifier
                .size(200.dp)
                .scale(scale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp
        ) {
            StoryCoverImage(
                story = story,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
                apiBaseUrl = baseUrl
            )
        }
    }
}

@Composable
fun AudioPlayerScreenPreview() {
    com.tamixa.ui.theme.TamixaTheme(darkTheme = true) {
        AudioPlayerScreen(
            story = com.tamixa.ui.data.SampleData.sampleStories().first(),
            isPlaying = false,
            onPlayPause = {},
            onBack = {},
            progress = 0.3f,
            onRewind = {},
            onFastForward = {},
            onSleepTimer = {},
            onDownload = {},
            onShare = {}
        )
    }
}
