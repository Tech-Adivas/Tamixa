package com.tamixa.ui.edu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.ui.components.platformIsReduceMotionEnabled
import com.tamixa.ui.strings.Strings

private fun normalizeOverlayStyle(raw: String?): String =
    raw?.trim()?.uppercase().orEmpty()

/** Lets the child answer a question by voice: the platform [recorder] and the call that sends the recording. */
class VoiceAnswerInput(
    val recorder: VoiceAnswerRecorder,
    val send: suspend (ByteArray) -> VoiceAnswerOutcome?,
)

/** Answer chosen by voice, outlined for a moment before the story continues. */
private fun Modifier.voiceHighlight(selected: Boolean): Modifier =
    if (selected) border(3.dp, Color(0xFFFFC857), RoundedCornerShape(18.dp)) else this

/** WhatsApp-chat-style choice bubbles for scam / safety pilots; [overlayStyle] from interactive graph JSON. */
@Composable
fun InteractiveChoiceOverlay(
    choices: List<InteractiveChoice>,
    onChoice: (InteractiveChoice) -> Unit,
    modifier: Modifier = Modifier,
    overlayStyle: String? = null,
    voice: VoiceAnswerInput? = null,
) {
    var voiceSelected by remember { mutableStateOf<String?>(null) }
    // Tap and voice are both live: only the first answer counts (a double tap or a tap landing with the voice
    // result would otherwise record the choice twice).
    var answered by remember(choices) { mutableStateOf(false) }
    val choose: (InteractiveChoice) -> Unit = { c ->
        if (!answered) {
            answered = true
            onChoice(c)
        }
    }
    val style = normalizeOverlayStyle(overlayStyle).ifBlank { "WHATSAPP_CHAT" }
    val scrimAlpha = if (style == "RESEARCH_CARDS" || style == "CARDS") 0.62f else 0.55f
    val bubbleColor =
        if (style == "RESEARCH_CARDS" || style == "CARDS") Color(0xFFE8EEF5) else Color(0xFFDCF8C6)
    val bubbleText = if (style == "RESEARCH_CARDS" || style == "CARDS") Color(0xFF0D1B2A) else Color(0xFF111111)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = Strings.interactiveStoryChoose(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            choices.forEach { c ->
                Surface(
                    shape = RoundedCornerShape(if (style == "RESEARCH_CARDS" || style == "CARDS") 12.dp else 18.dp),
                    color = bubbleColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .voiceHighlight(voiceSelected == c.id)
                        .clickable(enabled = !answered) { choose(c) },
                ) {
                    Text(
                        text = c.label,
                        modifier = Modifier.padding(14.dp),
                        color = bubbleText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            voice?.let { v ->
                VoiceAnswerSection(
                    recorder = v.recorder,
                    choices = choices,
                    sendAnswer = v.send,
                    onSelecting = { voiceSelected = it?.id },
                    onChoice = choose,
                    answered = answered,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * Question popup for a scene that has a picture: the picture, the question, then the answers. Pops in with a short
 * fade + scale (skipped under reduce motion). If the picture fails to load it is left out.
 */
@Composable
fun InteractiveQuestionCard(
    question: String?,
    imageUrl: String,
    choices: List<InteractiveChoice>,
    onChoice: (InteractiveChoice) -> Unit,
    modifier: Modifier = Modifier,
    voice: VoiceAnswerInput? = null,
) {
    var voiceSelected by remember { mutableStateOf<String?>(null) }
    // Tap and voice are both live: only the first answer counts (a double tap or a tap landing with the voice
    // result would otherwise record the choice twice).
    var answered by remember(choices) { mutableStateOf(false) }
    val choose: (InteractiveChoice) -> Unit = { c ->
        if (!answered) {
            answered = true
            onChoice(c)
        }
    }
    val reduceMotion = platformIsReduceMotionEnabled()
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) appear.animateTo(1f, tween(durationMillis = 280)) }
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E2A32),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = appear.value
                    val scale = 0.92f + 0.08f * appear.value
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!imageFailed) {
                    coil3.compose.SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = question,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        onError = { imageFailed = true },
                    )
                }
                Text(
                    text = question?.takeIf { it.isNotBlank() } ?: Strings.interactiveStoryChoose(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                choices.forEach { c ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE8EEF5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .voiceHighlight(voiceSelected == c.id)
                            .clickable(enabled = !answered) { choose(c) },
                    ) {
                        Text(
                            text = c.label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            color = Color(0xFF0D1B2A),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                voice?.let { v ->
                    VoiceAnswerSection(
                        recorder = v.recorder,
                        choices = choices,
                        sendAnswer = v.send,
                        onSelecting = { voiceSelected = it?.id },
                        onChoice = choose,
                        answered = answered,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MissionCardOverlay(
    missionText: String?,
    resourceUrl: String?,
    onDismiss: () -> Unit,
    onOpenResource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E2A32),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = Strings.familyMissionTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = Strings.missionCardChallengeHint(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll),
                ) {
                    missionText?.trim()?.takeIf { it.isNotBlank() }?.let { t ->
                        Text(
                            text = t,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                resourceUrl?.trim()?.takeIf { it.isNotBlank() }?.let { url ->
                    Button(
                        onClick = { onOpenResource(url) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(Strings.openLinkedResource())
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(Strings.missionCardDone())
                }
            }
        }
    }
}
