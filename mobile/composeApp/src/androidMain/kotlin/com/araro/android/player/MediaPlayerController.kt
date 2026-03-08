package com.araro.android.player

import android.content.Context
import com.araro.player.StoryPlaybackController
import com.araro.platform.shareStory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

import com.araro.util.AraroConstants

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
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }

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

    DisposableEffect(fileUri) {
        val path = if (fileUri.isNullOrBlank() || !fileUri.startsWith("file://")) null
        else try { android.net.Uri.parse(fileUri).path } catch (e: Exception) { null }
        val file = path?.let { File(it) }
        val isValid = path != null && file != null && file.exists() && file.length() > 0L

        var progressJob: kotlinx.coroutines.Job? = null
        var mp: MediaPlayer? = null
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
                    android.util.Log.w("Araro", "MediaPlayer error: what=$what extra=$extra path=$path")
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            isPlaying = mp.isPlaying
            progressJob = scope.launch {
                while (true) {
                    delay(300)
                    mediaPlayer?.let { p ->
                        val dur = p.duration.coerceAtLeast(1)
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
                    android.util.Log.w("Araro", "MediaPlayer dispose: ${e.message}", e)
                }
            }
            mediaPlayer = null
        }
    }

    if (fileUri.isNullOrBlank() || !fileUri.startsWith("file://")) return noOpController

    return object : StoryPlaybackController {
        override val isPlaying: Boolean get() = isPlaying
        override val progress: Float get() = progress

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
                val seekMs = AraroConstants.SEEK_MS.toInt()
                val newPos = (mp.currentPosition - seekMs).coerceAtLeast(0)
                mp.seekTo(newPos)
            }
        }

        override fun fastForward() {
            mediaPlayer?.let { mp ->
                val seekMs = AraroConstants.SEEK_MS.toInt()
                val newPos = (mp.currentPosition + seekMs).coerceAtMost(mp.duration)
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
