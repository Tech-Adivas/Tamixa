package com.araro.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import com.araro.domain.Story
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.theme.AraroDialogDefaults
import com.araro.ui.components.StoryCoverImage
import com.araro.ui.strings.Strings

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
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onBack: () -> Unit,
    progress: Float = 0f,
    isLoading: Boolean = false,
    onRewind: (() -> Unit)? = null,
    onFastForward: (() -> Unit)? = null,
    onSleepTimer: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onMoralClick: (() -> Unit)? = null,
    onRemix: ((Long, String) -> Unit)? = null,
    analytics: StoryAnalyticsCallbacks? = null,
    selectedVoice: String = com.araro.util.AraroConstants.VOICE_PROFILE_DEFAULT,
    onVoiceChange: ((String) -> Unit)? = null,
    availableVoices: List<com.araro.network.VoiceOptionDto> = emptyList(),
    onRecordFamilyVoice: (() -> Unit)? = null,
    apiBaseUrl: String? = null,
    storytellingAvatarUrl: String? = null,
    storytellingAvatarVideoContent: (@Composable () -> Unit)? = null,
    wordTimings: List<com.araro.network.WordTiming>? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        StarryNightBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Strings.back(),
                        tint = com.araro.ui.theme.AraroColors.cream
                    )
                }
                Spacer(Modifier.weight(1f))
                if ((availableVoices.isNotEmpty() || onRecordFamilyVoice != null) && onVoiceChange != null) {
                    var voiceMenuExpanded by remember { mutableStateOf(false) }
                    val hasFamilyVoice = availableVoices.any { it.voiceProfile.equals("family", ignoreCase = true) }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { voiceMenuExpanded = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = com.araro.ui.strings.Strings.voiceLabel(selectedVoice, availableVoices.find { it.voiceProfile == selectedVoice }?.isPremium == true),
                                style = MaterialTheme.typography.bodySmall,
                                color = com.araro.ui.theme.AraroColors.cream
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = com.araro.ui.theme.AraroColors.cream)
                        }
                        DropdownMenu(
                            expanded = voiceMenuExpanded,
                            onDismissRequest = { voiceMenuExpanded = false }
                        ) {
                            availableVoices.forEach { v ->
                                DropdownMenuItem(
                                    text = {
                                        Text(com.araro.ui.strings.Strings.voiceLabel(v.voiceProfile, v.isPremium))
                                    },
                                    onClick = {
                                        onVoiceChange(v.voiceProfile)
                                        voiceMenuExpanded = false
                                    }
                                )
                            }
                            if (!hasFamilyVoice && onRecordFamilyVoice != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(com.araro.ui.strings.Strings.recordYourVoiceNow())
                                    },
                                    onClick = {
                                        voiceMenuExpanded = false
                                        onRecordFamilyVoice()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = story, modifier = Modifier.fillMaxSize(), label = "playerContent") { currentStory ->
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        currentStory == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No story",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        else -> {
                            PlayerContent(
                                story = currentStory,
                                isPlaying = isPlaying,
                                progress = progress,
                                onPlayPause = onPlayPause,
                                onRewind = onRewind,
                                onFastForward = onFastForward,
                                onSleepTimer = onSleepTimer,
                                onDownload = onDownload,
                                onShare = onShare,
                                onMoralClick = onMoralClick,
                                onRemix = onRemix,
                                analytics = analytics,
                                apiBaseUrl = apiBaseUrl,
                                storytellingAvatarUrl = storytellingAvatarUrl,
                                storytellingAvatarVideoContent = storytellingAvatarVideoContent,
                                wordTimings = wordTimings
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerContent(
    story: com.araro.domain.Story,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onRewind: (() -> Unit)?,
    onFastForward: (() -> Unit)?,
    onSleepTimer: (() -> Unit)?,
    onDownload: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onMoralClick: (() -> Unit)?,
    onRemix: ((Long, String) -> Unit)?,
    analytics: StoryAnalyticsCallbacks? = null,
    apiBaseUrl: String? = null,
    storytellingAvatarUrl: String? = null,
    storytellingAvatarVideoContent: (@Composable () -> Unit)? = null,
    wordTimings: List<com.araro.network.WordTiming>? = null
) {
    LaunchedEffect(story.id) {
        analytics?.onStoryStarted?.invoke(story, 0)
    }
    LaunchedEffect(progress) {
        val totalSec = (story.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
        val positionSec = (progress * totalSec).toInt()
        analytics?.onProgress?.invoke(story, progress, positionSec)
        if (progress >= 0.99f) analytics?.onCompleted?.invoke(story, totalSec)
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Spacer(Modifier.height(16.dp))
            when {
                storytellingAvatarVideoContent != null -> {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        storytellingAvatarVideoContent()
                    }
                    Spacer(Modifier.height(12.dp))
                }
                storytellingAvatarUrl != null -> {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        coil3.compose.AsyncImage(
                            model = storytellingAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            StoryCoverWithFallback(story = story, apiBaseUrl = apiBaseUrl)
            Spacer(Modifier.height(16.dp))
            if (SHOW_TRANSCRIPT) {
                StoryTranscriptSection(
                    content = story.content,
                    progress = progress,
                    wordTimings = wordTimings,
                    durationSeconds = (story.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
                )
                Spacer(Modifier.height(24.dp))
            }
            Text(
                text = story.title?.takeIf { it.isNotBlank() } ?: story.theme,
                style = MaterialTheme.typography.headlineSmall,
                color = com.araro.ui.theme.AraroColors.cream,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            val totalSec = (story.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
            val posSec = (progress * totalSec).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${posSec / 60}:${(posSec % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onRewind?.invoke() }) {
                    Icon(Icons.Default.Replay, contentDescription = "Rewind", tint = MaterialTheme.colorScheme.onSurface)
                }
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) Strings.pause() else Strings.play(),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = { onFastForward?.invoke() }) {
                    Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onMoralClick?.let { onMoral ->
                    Button(
                        onClick = onMoral,
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(Strings.moralOfStory())
                    }
                }
                onRemix?.takeIf { story.parentId != 0L }?.let { remix ->
                    var showRemixDialog by remember { mutableStateOf(false) }
                    var remixInstruction by remember { mutableStateOf("") }
                    IconButton(
                        onClick = { showRemixDialog = true },
                        enabled = !isPlaying
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = Strings.editStory(),
                            tint = if (isPlaying) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showRemixDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showRemixDialog = false },
                            shape = AraroDialogDefaults.shape,
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
                                    shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (remixInstruction.isNotBlank()) {
                                            remix(story.id, remixInstruction.trim())
                                            showRemixDialog = false
                                            remixInstruction = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
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
            Spacer(Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onSleepTimer?.let { onTimer ->
                    IconButton(onClick = onTimer) {
                        Icon(Icons.Default.Timer, contentDescription = "Sleep timer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                onDownload?.let { onDown ->
                    IconButton(onClick = onDown) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                onShare?.let { onShare ->
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
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

/** Cumulative word count per sentence: sentenceStartWordIndex[i] = words in sentences 0..i-1. */
private fun sentenceStartWordIndices(sentences: List<String>): List<Int> {
    val indices = mutableListOf(0)
    for (s in sentences) {
        indices.add(indices.last() + splitIntoWords(s).size)
    }
    return indices
}

/** Delay (in word fraction) so highlight stays on each word longer and lags the voice. */
private const val TRANSCRIPT_HIGHLIGHT_DELAY = 0.58f

/** Set to true when transcript content matches TTS (no SSML/expressive collapse) or word timings are available. */
private const val SHOW_TRANSCRIPT = false

/**
 * Karaoke-style transcript: one sentence at a time, word-by-word highlight synced to playback.
 * When [wordTimings] and [durationSeconds] are provided (from backend/voice transcription),
 * highlight is driven by current time for exact sync. Otherwise uses sentence-based heuristic.
 */
@Composable
private fun StoryTranscriptSection(
    content: String?,
    progress: Float,
    wordTimings: List<com.araro.network.WordTiming>? = null,
    durationSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val surfaceColor = com.araro.ui.theme.AraroColors.nightSkySurfaceVariant.copy(alpha = 0.6f)
    val textColor = com.araro.ui.theme.AraroColors.cream
    val highlightColor = com.araro.ui.theme.AraroColors.goldAccent

    val sentences = remember(content) { content?.let { splitIntoSentences(it) } ?: emptyList() }
    val startIndices = remember(sentences) { sentenceStartWordIndices(sentences) }
    val sentenceCount = sentences.size

    val (currentSentenceIndex, highlightWordIndex) = if (
        !wordTimings.isNullOrEmpty() && durationSeconds > 0
    ) {
        val currentTimeSec = progress * durationSeconds
        val wordIndex = wordTimings.indexOfFirst { currentTimeSec >= it.startSec && currentTimeSec <= it.endSec }
            .takeIf { it >= 0 }
            ?: wordTimings.indexOfLast { it.endSec <= currentTimeSec }.takeIf { it >= 0 }
            ?: 0
        val globalWordIndex = wordIndex.coerceIn(0, wordTimings.size - 1)
        val sentIdx = startIndices.indexOfFirst { it > globalWordIndex }.let { idx ->
            if (idx < 0) (startIndices.size - 1).coerceAtLeast(0) else (idx - 1).coerceAtLeast(0)
        }
        val wordInSent = (globalWordIndex - startIndices.getOrElse(sentIdx) { 0 }).coerceIn(0, Int.MAX_VALUE)
        sentIdx to wordInSent
    } else {
        val sentenceProgress = if (sentenceCount > 0) (progress * sentenceCount).toFloat().coerceIn(0f, (sentenceCount - 0.001f)) else 0f
        val sentIdx = sentenceProgress.toInt().coerceIn(0, (sentenceCount - 1).coerceAtLeast(0))
        val progressInSentence = (sentenceProgress - sentIdx).coerceIn(0f, 1f)
        val sentenceWords = splitIntoWords(sentences.getOrNull(sentIdx) ?: "")
        val wordCountInSentence = sentenceWords.size
        val wordProgressInSentence = if (wordCountInSentence > 0) progressInSentence * wordCountInSentence else 0f
        val hi = if (wordCountInSentence > 0) {
            (wordProgressInSentence - TRANSCRIPT_HIGHLIGHT_DELAY).toInt().coerceIn(0, wordCountInSentence - 1)
        } else 0
        sentIdx to hi
    }

    val currentSentence = sentences.getOrNull(currentSentenceIndex) ?: ""
    val sentenceWords = remember(currentSentence) { splitIntoWords(currentSentence) }
    val safeHighlightIndex = highlightWordIndex.coerceIn(0, (sentenceWords.size - 1).coerceAtLeast(0))

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(AraroDesignTokens.inputRadius),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 240.dp)
        ) {
            if (content.isNullOrBlank() || currentSentence.isBlank()) {
                Text(
                    text = Strings.transcriptUnavailable(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.padding(16.dp)
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
    com.araro.ui.theme.AraroTheme(darkTheme = true) {
        AudioPlayerScreen(
            story = com.araro.ui.data.SampleData.sampleStories().first(),
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
