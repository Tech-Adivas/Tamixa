package com.tamixa.android.player

import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tamixa.player.StoryPlaybackController
import com.tamixa.platform.downloadFile
import com.tamixa.platform.shareStory
import com.tamixa.util.TamixaConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Streaming audio controller that uses [AudioPlaybackService] and [MediaController]
 * so playback continues when the user navigates away from the app (background audio).
 */
@Composable
fun rememberBackgroundStreamingController(
    streamUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)? = null
): StoryPlaybackController {
    val context = LocalContext.current
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMillisState by remember(streamUrl) { mutableLongStateOf(0L) }
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

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNullOrBlank()) return@LaunchedEffect
        durationMillisState = 0L
        context.startForegroundService(Intent(context, AudioPlaybackService::class.java))
        val token = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                val item = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder().setTitle(storyTitle).build()
                    )
                    .build()
                controller.setMediaItem(item)
                controller.prepare()
                controller.play()
                controller.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> isReady = true
                            Player.STATE_ENDED -> {
                                isPlaying = false
                                progress = 1f
                                onProgressChanged(1f)
                            }
                        }
                    }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        onPlaybackError?.invoke()
                    }
                })
                mediaController = controller
            } catch (_: Exception) {
                onPlaybackError?.invoke()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val progressJob = mediaController
    LaunchedEffect(progressJob) {
        val c = progressJob ?: return@LaunchedEffect
        while (true) {
            delay(500)
            val controller = mediaController ?: break
            if (controller.playbackState != Player.STATE_ENDED) {
                val raw = controller.duration
                val dur = when {
                    raw > 0L && raw != C.TIME_UNSET -> raw.also { durationMillisState = it }
                    durationMillisState > 0L -> durationMillisState
                    else -> 0L
                }
                if (dur > 0L) {
                    val pos = controller.currentPosition
                    val prog = (pos.toFloat() / dur).coerceIn(0f, 1f)
                    progress = prog
                    onProgressChanged(prog)
                }
            }
        }
    }

    DisposableEffect(streamUrl) {
        onDispose {
            sleepTimerJob?.cancel()
            mediaController?.let { controller ->
                controller.stop()
                controller.release()
            }
            mediaController = null
            isReady = false
            isPlaying = false
            progress = 0f
            durationMillisState = 0L
            context.stopService(Intent(context, AudioPlaybackService::class.java))
        }
    }

    if (streamUrl.isNullOrBlank()) return noOpController

    return object : StoryPlaybackController {
        override val isReady: Boolean get() = isReady
        override val isPlaying: Boolean get() = isPlaying
        override val progress: Float get() = progress
        override val durationMillis: Long get() = durationMillisState

        override fun playPause() {
            mediaController?.let { c ->
                if (c.isPlaying) c.pause() else c.play()
            }
        }

        override fun rewind() {
            mediaController?.let { c ->
                val newPos = (c.currentPosition - TamixaConstants.SEEK_MS).coerceAtLeast(0L)
                c.seekTo(newPos)
            }
        }

        override fun fastForward() {
            mediaController?.let { c ->
                val raw = c.duration
                val dur = when {
                    raw > 0L && raw != C.TIME_UNSET -> raw
                    durationMillisState > 0L -> durationMillisState
                    else -> return@let
                }
                val newPos = (c.currentPosition + TamixaConstants.SEEK_MS).coerceAtMost(dur)
                c.seekTo(newPos)
            }
        }

        override fun setSleepTimer(minutes: Int) {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            if (minutes <= 0) return
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                mediaController?.pause()
                isPlaying = false
                sleepTimerJob = null
            }
        }

        override fun share() {
            shareStory(storyTitle, "$storyTitle - $storyTheme")
        }

        override fun download(url: String, filename: String, mimeType: String) {
            downloadFile(url, filename, storyTitle, mimeType)
        }
    }
}
