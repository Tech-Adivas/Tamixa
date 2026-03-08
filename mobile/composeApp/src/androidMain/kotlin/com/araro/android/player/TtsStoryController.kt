package com.araro.android.player

import com.araro.player.StoryPlaybackController
import com.araro.platform.shareStory
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * TTS-based story playback when no audio URL is available.
 * Reads story content aloud like a real bedtime storytelling experience.
 */
@Composable
fun rememberTtsStoryController(
    storyContent: String?,
    storyTitle: String,
    storyTheme: String,
    languageCode: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit
): StoryPlaybackController {
    val context = LocalContext.current
    var tts by mutableStateOf<TextToSpeech?>(null)
    var isPlaying by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var sleepTimerJob by mutableStateOf<Job?>(null)
    var currentChunkIndex by mutableStateOf(0)
    var chunks by mutableStateOf<List<String>>(emptyList())
    var isInitialized by mutableStateOf(false)
    val noOpController = object : StoryPlaybackController {
        override val isPlaying: Boolean get() = false
        override val progress: Float get() = 0f
        override fun playPause() {}
        override fun rewind() {}
        override fun fastForward() {}
        override fun setSleepTimer(minutes: Int) {}
        override fun share() {}
        override fun download(url: String, filename: String, mimeType: String) {}
    }

    DisposableEffect(storyContent, languageCode) {
        if (storyContent.isNullOrBlank()) {
            onDispose { }
        } else {
            val text = storyContent.trim()
            val splitChunks = splitIntoSpeakableChunks(text)
            chunks = splitChunks

            val engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Handler(Looper.getMainLooper()).post {
                        val engine = tts ?: return@post
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            engine.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                            )
                        }
                        val locale = languageCodeToLocale(languageCode)
                        val result = engine.setLanguage(locale)
                        val useLocale = when (result) {
                            TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE -> locale
                            else -> Locale.ENGLISH // Fallback when language pack not installed
                        }
                        engine.language = useLocale
                        engine.setSpeechRate(0.88f)
                        engine.setPitch(1.0f)
                        isInitialized = true
                    }
                }
            }
            tts = engine

            onDispose {
                sleepTimerJob?.cancel()
                tts?.stop()
                tts?.shutdown()
                tts = null
                isInitialized = false
            }
        }
    }

    if (storyContent.isNullOrBlank() || !isInitialized || chunks.isEmpty()) return noOpController

    val mainHandler = Handler(Looper.getMainLooper())
    fun speakNextChunk() {
        val engine = tts ?: return
        if (currentChunkIndex >= chunks.size) return
        engine.setSpeechRate(0.88f)
        val chunk = chunks[currentChunkIndex]
        val idx = currentChunkIndex
        val utteranceId = "araro_$idx"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (utteranceId == "araro_$idx") {
                        currentChunkIndex++
                        val total = chunks.size
                        if (currentChunkIndex >= total) {
                            progress = 1f
                            onProgressChanged(1f)
                            isPlaying = false
                        } else {
                            progress = if (total > 0) (currentChunkIndex.toFloat() / total).coerceIn(0f, 1f) else 1f
                            onProgressChanged(progress)
                            speakNextChunk()
                        }
                    }
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                mainHandler.post { isPlaying = false }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post { isPlaying = false }
            }
        })
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        engine.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    return object : StoryPlaybackController {
        override val isPlaying: Boolean get() = isPlaying
        override val progress: Float get() = progress

        override fun playPause() {
            val engine = tts ?: return
            if (isPlaying) {
                engine.stop()
                isPlaying = false
            } else {
                isPlaying = true
                if (currentChunkIndex >= chunks.size) {
                    currentChunkIndex = 0
                    progress = 0f
                    onProgressChanged(0f)
                }
                speakNextChunk()
            }
        }

        override fun rewind() {
            if (chunks.isEmpty()) return
            currentChunkIndex = (currentChunkIndex - 1).coerceAtLeast(0)
            val total = chunks.size
            progress = if (total > 0) (currentChunkIndex.toFloat() / total).coerceIn(0f, 1f) else 0f
            onProgressChanged(progress)
            if (isPlaying) {
                tts?.stop()
                speakNextChunk()
            }
        }

        override fun fastForward() {
            if (chunks.isEmpty()) return
            currentChunkIndex = (currentChunkIndex + 1).coerceAtMost(chunks.size)
            val total = chunks.size
            progress = if (total > 0) (currentChunkIndex.toFloat() / total).coerceIn(0f, 1f) else 1f
            onProgressChanged(progress)
            if (currentChunkIndex >= chunks.size) {
                isPlaying = false
            } else if (isPlaying) {
                tts?.stop()
                speakNextChunk()
            }
        }

        override fun setSleepTimer(minutes: Int) {
            sleepTimerJob?.cancel()
            if (minutes <= 0) return
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                tts?.stop()
                isPlaying = false
                sleepTimerJob = null
            }
        }

        override fun share() {
            com.araro.platform.shareStory(storyTitle, "$storyTitle - $storyTheme")
        }

        override fun download(url: String, filename: String, mimeType: String) {
            // TTS has no audio file to download
        }
    }
}

/** Split story text into chunks for natural TTS pacing. Splits by paragraph, then sentence. */
private fun splitIntoSpeakableChunks(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val paragraphs = text.split("\n\n", "\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val chunks = mutableListOf<String>()
    for (para in paragraphs) {
        if (para.length <= 350) {
            chunks.add(para)
        } else {
            // Split long paragraphs by sentence
            val sentences = para.split(Regex("(?<=[.!?])\\s+"))
            var current = StringBuilder()
            for (s in sentences) {
                if (current.length + s.length > 350 && current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current = StringBuilder()
                }
                if (current.isNotEmpty()) current.append(" ")
                current.append(s)
            }
            if (current.isNotEmpty()) chunks.add(current.toString().trim())
        }
    }
    return chunks.ifEmpty { listOf(text) }
}

private fun languageCodeToLocale(code: String): Locale {
    return when (code.lowercase()) {
        "ta" -> Locale("ta", "IN")  // Tamil
        "hi" -> Locale("hi", "IN")  // Hindi
        "te" -> Locale("te", "IN")  // Telugu
        "kn" -> Locale("kn", "IN")  // Kannada
        "ml" -> Locale("ml", "IN")  // Malayalam
        "mr" -> Locale("mr", "IN")  // Marathi
        "bn" -> Locale("bn", "IN")  // Bengali
        "en" -> Locale.ENGLISH
        else -> {
            val parts = code.split("-", "_")
            if (parts.size >= 2) Locale(parts[0], parts[1])
            else Locale.forLanguageTag(code)
        }
    }
}

