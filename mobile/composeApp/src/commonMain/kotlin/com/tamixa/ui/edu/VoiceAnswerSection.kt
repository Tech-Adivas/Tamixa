package com.tamixa.ui.edu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.ui.strings.Strings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/** Result of a voice answer from the server: the matched answer id (or null) and what was heard. */
data class VoiceAnswerOutcome(val choiceId: String?, val heardText: String)

private sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data object Thinking : VoiceState
    data class Heard(val text: String) : VoiceState
    data class Retry(val heard: String?) : VoiceState
    data object Denied : VoiceState
    /** Two misses: only tapping is offered so the child is never stuck. */
    data object TapOnly : VoiceState
}

private const val MAX_RECORDING_MS = 6_000L
private const val MIN_RECORDING_MS = 700L
private const val SILENCE_AFTER_SPEECH_MS = 1_200L
private const val SPEECH_LEVEL = 0.08f
private const val MAX_MISSES = 2
private const val SHOW_MATCH_MS = 1_200L

/**
 * 🎤 under an interactive question: the child taps, speaks (recording stops on silence or after 6 s, or on a second
 * tap), the server picks the answer they meant, it is highlighted via [onSelecting] and then chosen via [onChoice].
 * Misses ask again; after two only tapping is offered. Once [answered] (an answer was tapped), recording stops and
 * any pending voice result is dropped, so the tap wins.
 */
@Composable
fun VoiceAnswerSection(
    recorder: VoiceAnswerRecorder,
    choices: List<InteractiveChoice>,
    sendAnswer: suspend (ByteArray) -> VoiceAnswerOutcome?,
    onSelecting: (InteractiveChoice?) -> Unit,
    onChoice: (InteractiveChoice) -> Unit,
    modifier: Modifier = Modifier,
    answered: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<VoiceState>(VoiceState.Idle) }
    var level by remember { mutableFloatStateOf(0f) }
    var misses by remember { mutableIntStateOf(0) }
    var stopRequested by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { job?.cancel(); recorder.cancel() } }
    LaunchedEffect(answered) {
        if (answered) {
            job?.cancel()
            recorder.cancel()
            level = 0f
        }
    }

    fun miss(heard: String?) {
        misses++
        state = if (misses >= MAX_MISSES) VoiceState.TapOnly else VoiceState.Retry(heard?.takeIf { it.isNotBlank() })
    }

    fun listen() {
        if (!recorder.start()) return miss(null)
        stopRequested = false
        state = VoiceState.Listening
        job = scope.launch {
            val started = TimeSource.Monotonic.markNow()
            var lastLoudMs = -1L
            while (true) {
                delay(100)
                val now = started.elapsedNow().inWholeMilliseconds
                level = recorder.level()
                if (level > SPEECH_LEVEL) lastLoudMs = now
                val silentAfterSpeech = lastLoudMs >= 0 && now - lastLoudMs > SILENCE_AFTER_SPEECH_MS
                if (stopRequested || silentAfterSpeech || now >= MAX_RECORDING_MS) break
            }
            level = 0f
            val recordedMs = started.elapsedNow().inWholeMilliseconds
            val audio = recorder.stop()
            if (audio == null || recordedMs < MIN_RECORDING_MS) return@launch miss(null)
            state = VoiceState.Thinking
            val outcome = sendAnswer(audio)
            val chosen = choices.firstOrNull { it.id == outcome?.choiceId }
            if (chosen == null) return@launch miss(outcome?.heardText)
            state = VoiceState.Heard(outcome?.heardText.orEmpty())
            onSelecting(chosen)
            delay(SHOW_MATCH_MS)
            onChoice(chosen)
        }
    }

    fun onMicTap() {
        if (answered) return
        when (state) {
            VoiceState.Listening -> stopRequested = true
            VoiceState.Thinking, is VoiceState.Heard, VoiceState.TapOnly -> Unit
            else -> {
                onSelecting(null)
                if (recorder.hasPermission()) listen()
                else recorder.requestPermission { granted -> if (granted) listen() else state = VoiceState.Denied }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val current = state
        if (current != VoiceState.TapOnly && current != VoiceState.Denied) {
            val listening = current == VoiceState.Listening
            FilledIconButton(
                onClick = ::onMicTap,
                enabled = !answered && current != VoiceState.Thinking && current !is VoiceState.Heard,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (listening) Color(0xFFE5484D) else Color(0xFFC9665A),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        val pulse = if (listening) 1f + (level * 3f).coerceAtMost(0.25f) else 1f
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .semantics { contentDescription = Strings.voiceAnswerTapToSpeak() },
            ) {
                if (current == VoiceState.Thinking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                }
            }
        }
        val status = when (current) {
            VoiceState.Idle -> Strings.voiceAnswerTapToSpeak()
            VoiceState.Listening -> Strings.voiceAnswerListening()
            VoiceState.Thinking -> Strings.voiceAnswerThinking()
            is VoiceState.Heard -> Strings.voiceAnswerHeard(current.text)
            is VoiceState.Retry -> listOfNotNull(current.heard?.let { Strings.voiceAnswerHeard(it) }, Strings.voiceAnswerTryAgain())
                .joinToString("\n")
            VoiceState.Denied -> Strings.voiceAnswerMicDenied()
            VoiceState.TapOnly -> Strings.voiceAnswerPleaseTap()
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}
