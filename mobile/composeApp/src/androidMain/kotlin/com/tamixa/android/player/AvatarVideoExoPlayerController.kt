package com.tamixa.android.player

import com.tamixa.player.AvatarVideoControllerResult
import com.tamixa.player.StoryPlaybackController
import com.tamixa.platform.downloadFile
import com.tamixa.platform.shareStory
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

import com.tamixa.util.TamixaConstants

/**
 * ExoPlayer for avatar video (talking-head). Video URL contains both video and audio.
 * Returns controller + player; use player for PlayerView to display video.
 */
@Composable
fun rememberAvatarVideoExoPlayerController(
    videoUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)? = null,
    muteVideoAudio: Boolean = false,
    repeatVideo: Boolean = false
): AvatarVideoControllerResult {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isReady by remember { mutableStateOf(false) }
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }

    val noOp = AvatarVideoControllerResult(
        controller = object : StoryPlaybackController {
            override val isReady get() = false
            override val isPlaying get() = false
            override val progress get() = 0f
            override fun playPause() {}
            override fun rewind() {}
            override fun fastForward() {}
            override fun setSleepTimer(minutes: Int) {}
            override fun share() {}
            override fun download(url: String, filename: String, mimeType: String) {}
        },
        player = null
    )

    DisposableEffect(videoUrl, muteVideoAudio, repeatVideo) {
        if (videoUrl.isNullOrBlank()) {
            onDispose { }
        } else {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
            val exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
            exoPlayer.setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            exoPlayer.setHandleAudioBecomingNoisy(true)
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isReady = true
                            if (!isPlaying) exoPlayer.play()
                        }
                        Player.STATE_ENDED -> {
                            isPlaying = false
                            progress = 1f
                            onProgressChanged(1f)
                        }
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    onPlaybackError?.invoke()
                }
            })
            exoPlayer.volume = if (muteVideoAudio) 0f else 1f
            exoPlayer.repeatMode = if (repeatVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
            exoPlayer.prepare()
            player = exoPlayer
            val progressJob = scope.launch {
                while (true) {
                    delay(500)
                    val p = player
                    if (p != null && p.playbackState != Player.STATE_ENDED) {
                        val dur = p.duration.coerceAtLeast(1L)
                        val pos = p.currentPosition
                        progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
                        onProgressChanged(progress)
                    }
                }
            }
            onDispose {
                progressJob.cancel()
                sleepTimerJob?.cancel()
                exoPlayer.release()
                player = null
            }
        }
    }

    if (videoUrl.isNullOrBlank()) return noOp

    return AvatarVideoControllerResult(
        controller = object : StoryPlaybackController {
            override val isReady get() = isReady
            override val isPlaying get() = isPlaying
            override val progress get() = progress
            override fun playPause() {
                player?.let { if (it.isPlaying) it.pause() else it.play() }
            }
            override fun rewind() {
                player?.let { it.seekTo((it.currentPosition - TamixaConstants.SEEK_MS).coerceAtLeast(0L)) }
            }
            override fun fastForward() {
                player?.let { val d = it.duration.coerceAtLeast(1L); it.seekTo((it.currentPosition + TamixaConstants.SEEK_MS).coerceAtMost(d)) }
            }
            override fun setSleepTimer(minutes: Int) {
                sleepTimerJob?.cancel()
                if (minutes <= 0) return
                sleepTimerJob = scope.launch {
                    delay(minutes * 60 * 1000L)
                    player?.pause()
                    sleepTimerJob = null
                }
            }
            override fun share() {
                shareStory(storyTitle, "$storyTitle - $storyTheme")
            }
            override fun download(url: String, filename: String, mimeType: String) {
                downloadFile(url, filename, storyTitle, mimeType)
            }
        },
        player = player
    )
}
