package com.araro.android.player

import com.araro.player.StoryPlaybackController
import com.araro.platform.downloadFile
import com.araro.platform.shareStory
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.araro.util.AraroConstants

/**
 * Holds ExoPlayer state and callbacks for audio playback in the Audio Player screen.
 * Call [dispose] when leaving the screen.
 */
@Composable
fun rememberExoPlayerController(
    streamUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)? = null
): StoryPlaybackController {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
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

    DisposableEffect(streamUrl) {
        if (streamUrl.isNullOrBlank()) {
            onDispose { }
        } else {
        // Local file:// and backend /audio/ URLs: skip cache to always play latest after regeneration.
        // Cache can serve stale flat audio when backend has new conversational narration.
        val isLocalFile = streamUrl.startsWith("file://")
        val isBackendAudio = "/audio/" in streamUrl
        val dataSourceFactory = if (isLocalFile || isBackendAudio) {
            DefaultDataSource.Factory(context)
        } else {
            AudioCache.getCacheDataSourceFactory(context)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        val contentType = if (isLocalFile) C.AUDIO_CONTENT_TYPE_SPEECH else C.AUDIO_CONTENT_TYPE_MUSIC
        val exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
        exoPlayer.setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(contentType)
                .build(),
            true /* handleAudioFocus */
        )
        exoPlayer.setHandleAudioBecomingNoisy(true)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (!isPlaying) exoPlayer.play()
                    }
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
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        player = exoPlayer
        val progressJob = scope.launch {
            while (true) {
                delay(500)
                val p = player
                if (p != null && p.playbackState != Player.STATE_ENDED) {
                    val dur = p.duration.coerceAtLeast(1L)
                    val pos = p.currentPosition
                    val prog = (pos.toFloat() / dur).coerceIn(0f, 1f)
                    progress = prog
                    onProgressChanged(prog)
                }
            }
        }
        onDispose {
            progressJob.cancel()
            sleepTimerJob?.cancel()
            player?.release()
            player = null
        }
        }
    }

    if (streamUrl.isNullOrBlank()) return noOpController

    return object : StoryPlaybackController {
        override val isPlaying: Boolean get() = isPlaying
        override val progress: Float get() = progress

        override fun playPause() {
            player?.let { p ->
                if (p.isPlaying) {
                    p.pause()
                } else {
                    p.play()
                }
            }
        }

        override fun rewind() {
            player?.let { p ->
                val newPos = (p.currentPosition - AraroConstants.SEEK_MS).coerceAtLeast(0L)
                p.seekTo(newPos)
            }
        }

        override fun fastForward() {
            player?.let { p ->
                val dur = p.duration.coerceAtLeast(1L)
                val newPos = (p.currentPosition + AraroConstants.SEEK_MS).coerceAtMost(dur)
                p.seekTo(newPos)
            }
        }

        override fun setSleepTimer(minutes: Int) {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            if (minutes <= 0) return
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                player?.pause()
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

// StoryPlaybackController interface is in TtsStoryController.kt
