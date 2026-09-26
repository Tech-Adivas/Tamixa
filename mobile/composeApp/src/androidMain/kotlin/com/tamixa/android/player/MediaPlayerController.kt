package com.tamixa.android.player

import android.content.Context
import com.tamixa.player.StoryPlaybackController
import com.tamixa.platform.shareStory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

import com.tamixa.util.TamixaConstants

/**
 * MediaPlayer-based controller for local file:// URIs (TTS output).
 * More reliable than ExoPlayer for local WAV playback on many devices.
 */
@Composable
fun rememberMediaPlayerController(
    fileUri: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit
): StoryPlaybackController {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMillisState by remember(fileUri) { mutableLongStateOf(0L) }
    var isReady by remember { mutableStateOf(false) }
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }

    val noOpController = object : StoryPlaybackController {
        override val isReady: Boolean get() = false
        override val isPlaying: Boolean get() = false
        override val progress: Float get() = 0f
        override val durationMillis: Long get() = 0L
        override fun playPause() {}
        override fun rewind() {}
        override fun fastForward() {}
        override fun setSleepTimer(minutes: Int) {}
        override fun share() {}
        override fun download(url: String, filename: String, mimeType: String) {}
    }

    DisposableEffect(fileUri) {
        val path = if (fileUri.isNullOrBlank() || !fileUri.startsWith("file://")) null
        else try { android.net.Uri.parse(fileUri).path } catch (e: Exception) { null }
        val file = path?.let { File(it) }
        val isValid = path != null && file != null && file.exists() && file.length() > 0L

        var progressJob: kotlinx.coroutines.Job? = null
        var mp: MediaPlayer? = null
        durationMillisState = 0L
        if (isValid) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val focusGranted = am?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attrs)
                        .build()
                    it.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                } else {
                    @Suppress("DEPRECATION")
                    it.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                }
            } ?: true
            mp = MediaPlayer().apply {
                setAudioAttributes(attrs)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                setDataSource(path!!)
                setOnCompletionListener {
                    isPlaying = false
                    progress = 1f
                    onProgressChanged(1f)
                }
                setOnErrorListener { _, what, extra ->
                    android.util.Log.w("Tamixa", "MediaPlayer error: what=$what extra=$extra path=$path")
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            isReady = true
            isPlaying = mp.isPlaying
            progressJob = scope.launch {
                while (true) {
                    delay(300)
                    mediaPlayer?.let { p ->
                        val raw = p.duration
                        if (raw > 0) durationMillisState = raw.toLong()
                        val dur = when {
                            raw > 0 -> raw
                            durationMillisState > 0L -> durationMillisState.toInt().coerceAtLeast(1)
                            else -> return@let
                        }
                        val pos = p.currentPosition
                        progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
                        onProgressChanged(progress)
                    }
                }
            }
        }

        onDispose {
            progressJob?.cancel()
            sleepTimerJob?.cancel()
            mp?.let {
                try {
                    it.stop()
                    it.release()
                } catch (e: Exception) {
                    android.util.Log.w("Tamixa", "MediaPlayer dispose: ${e.message}", e)
                }
            }
            mediaPlayer = null
        }
    }

    if (fileUri.isNullOrBlank() || !fileUri.startsWith("file://")) return noOpController

    return object : StoryPlaybackController {
        override val isReady: Boolean get() = isReady
        override val isPlaying: Boolean get() = isPlaying
        override val progress: Float get() = progress
        override val durationMillis: Long get() = durationMillisState

        override fun playPause() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    isPlaying = false
                } else {
                    mp.start()
                    isPlaying = true
                }
            }
        }

        override fun rewind() {
            mediaPlayer?.let { mp ->
                val seekMs = TamixaConstants.SEEK_MS.toInt()
                val newPos = (mp.currentPosition - seekMs).coerceAtLeast(0)
                mp.seekTo(newPos)
            }
        }

        override fun fastForward() {
            mediaPlayer?.let { mp ->
                val seekMs = TamixaConstants.SEEK_MS.toInt()
                val dur = mp.duration.takeIf { it > 0 } ?: durationMillisState.toInt().takeIf { it > 0 } ?: return@let
                val newPos = (mp.currentPosition + seekMs).coerceAtMost(dur)
                mp.seekTo(newPos)
            }
        }

        override fun setSleepTimer(minutes: Int) {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            if (minutes <= 0) return
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                mediaPlayer?.pause()
                isPlaying = false
                sleepTimerJob = null
            }
        }

        override fun share() {
            shareStory(storyTitle, "$storyTitle - $storyTheme")
        }

        override fun download(url: String, filename: String, mimeType: String) {
            // TTS files are local - no download needed
        }
    }
}
