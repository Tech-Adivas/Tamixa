package com.araro.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.araro.player.AvatarVideoControllerResult
import com.araro.player.StoryPlaybackController
import com.araro.util.AraroConstants
import com.araro.util.AraroLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC
import androidx.compose.ui.Modifier
import com.araro.ios.component.AvatarVideoSurfaceIos

private fun noOpController(): StoryPlaybackController = object : StoryPlaybackController {
    override val isPlaying get() = false
    override val progress get() = 0f
    override fun playPause() {}
    override fun rewind() {}
    override fun fastForward() {}
    override fun setSleepTimer(minutes: Int) {}
    override fun share() {}
    override fun download(url: String, filename: String, mimeType: String) {}
}

@OptIn(ExperimentalForeignApi::class)
private fun createAvPlayerController(
    mediaUrl: String,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    isPlayingState: androidx.compose.runtime.MutableState<Boolean>,
    progressState: androidx.compose.runtime.MutableState<Float>,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?
): Pair<AVPlayer?, StoryPlaybackController> {
    AraroLog.d("PlayerIOS", "createAvPlayerController urlType=${AraroLog.maskUrl(mediaUrl)}")
    val nsUrl = NSURL.URLWithString(mediaUrl)
        ?: (if (mediaUrl.startsWith("file://")) NSURL.fileURLWithPath(mediaUrl.removePrefix("file://")) else null)
        ?: run {
            AraroLog.w("PlayerIOS", "Invalid media URL (null NSURL)")
            return null to noOpController()
        }
    val item = AVPlayerItem(uRL = nsUrl)
    val avPlayer = AVPlayer()
    avPlayer.replaceCurrentItemWithPlayerItem(item)
    AraroLog.d("PlayerIOS", "AVPlayer created and item set")
    var sleepTimerJob: Job? = null
    scope.launch {
        while (true) {
            delay(500)
            val currentItem = avPlayer.currentItem ?: break
            // AVPlayerItemStatusFailed = 2
            if (currentItem.status.toLong() == 2L) {
                val err = currentItem.error
                val msg = err?.localizedDescription ?: err?.description ?: "unknown"
                AraroLog.w("PlayerIOS", "AVPlayerItem failed: $msg")
                onPlaybackError?.invoke()
                break
            }
            val currentTime = CMTimeGetSeconds(avPlayer.currentTime())
            val duration = currentItem.duration?.let { d -> CMTimeGetSeconds(d) } ?: 0.0
            if (duration > 0 && !duration.isNaN()) {
                val p = (currentTime / duration).toFloat().coerceIn(0f, 1f)
                progressState.value = p
                onProgressChanged(p)
            }
            isPlayingState.value = avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
        }
    }
    val ctrl = object : StoryPlaybackController {
        override val isPlaying: Boolean get() = isPlayingState.value
        override val progress: Float get() = progressState.value
        override fun playPause() {
            if (avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying) {
                avPlayer.pause()
                isPlayingState.value = false
            } else {
                avPlayer.play()
                isPlayingState.value = true
            }
        }
        override fun rewind() {
            val t = (CMTimeGetSeconds(avPlayer.currentTime()) - AraroConstants.SEEK_MS / 1000.0).coerceAtLeast(0.0)
            avPlayer.currentItem?.seekToTime(CMTimeMakeWithSeconds(t, NSEC_PER_SEC.toInt()), null)
        }
        override fun fastForward() {
            val dur = avPlayer.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
            val maxT = if (dur.isNaN()) 0.0 else dur
            val t = (CMTimeGetSeconds(avPlayer.currentTime()) + AraroConstants.SEEK_MS / 1000.0).coerceAtMost(maxT)
            avPlayer.currentItem?.seekToTime(CMTimeMakeWithSeconds(t, NSEC_PER_SEC.toInt()), null)
        }
        override fun setSleepTimer(minutes: Int) {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            if (minutes <= 0) return
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                avPlayer.pause()
                isPlayingState.value = false
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
    avPlayer.play()
    isPlayingState.value = true
    return avPlayer to ctrl
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberStreamingController(
    streamUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?
): StoryPlaybackController {
    val isPlayingState = remember(streamUrl) { mutableStateOf(false) }
    val progressState = remember(streamUrl) { mutableFloatStateOf(0f) }
    var controller by remember(streamUrl) { mutableStateOf<StoryPlaybackController>(noOpController()) }
    var avPlayer by remember(streamUrl) { mutableStateOf<AVPlayer?>(null) }
    DisposableEffect(streamUrl) {
        AraroLog.d("PlayerIOS", "rememberStreamingController streamUrl=${AraroLog.maskUrl(streamUrl)}")
        if (!streamUrl.isNullOrBlank()) {
            val (player, ctrl) = createAvPlayerController(
                streamUrl,
                storyTitle,
                storyTheme,
                scope,
                isPlayingState,
                progressState,
                onProgressChanged,
                onPlaybackError
            )
            avPlayer = player
            controller = ctrl
        }
        onDispose {
            avPlayer?.pause()
            avPlayer = null
            controller = noOpController()
        }
    }
    return controller
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberLocalFileController(
    fileUri: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit
): StoryPlaybackController {
    val isPlayingState = remember(fileUri) { mutableStateOf(false) }
    val progressState = remember(fileUri) { mutableFloatStateOf(0f) }
    var controller by remember(fileUri) { mutableStateOf<StoryPlaybackController>(noOpController()) }
    var avPlayer by remember(fileUri) { mutableStateOf<AVPlayer?>(null) }
    DisposableEffect(fileUri) {
        if (!fileUri.isNullOrBlank()) {
            val (player, ctrl) = createAvPlayerController(
                fileUri,
                storyTitle,
                storyTheme,
                scope,
                isPlayingState,
                progressState,
                onProgressChanged,
                null
            )
            avPlayer = player
            controller = ctrl
        }
        onDispose {
            avPlayer?.pause()
            avPlayer = null
            controller = noOpController()
        }
    }
    return controller
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberAvatarVideoController(
    videoUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?
): AvatarVideoControllerResult {
    val isPlayingState = remember(videoUrl) { mutableStateOf(false) }
    val progressState = remember(videoUrl) { mutableFloatStateOf(0f) }
    var result by remember(videoUrl) {
        mutableStateOf(
            AvatarVideoControllerResult(controller = noOpController(), player = null)
        )
    }
    DisposableEffect(videoUrl) {
        if (!videoUrl.isNullOrBlank()) {
            val (player, ctrl) = createAvPlayerController(
                videoUrl,
                storyTitle,
                storyTheme,
                scope,
                isPlayingState,
                progressState,
                onProgressChanged,
                onPlaybackError
            )
            result = AvatarVideoControllerResult(controller = ctrl, player = player)
            AraroLog.d("PlayerIOS", "rememberAvatarVideoController player=${if (player != null) "non-null" else "null"}")
        }
        onDispose {
            (result.player as? AVPlayer)?.pause()
            result = AvatarVideoControllerResult(controller = noOpController(), player = null)
        }
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CoverVideoSurface(
    videoUrl: String?,
    modifier: Modifier
) {
    if (videoUrl.isNullOrBlank()) return
    val nsUrl = NSURL.URLWithString(videoUrl)
        ?: (if (videoUrl.startsWith("file://")) NSURL.fileURLWithPath(videoUrl.removePrefix("file://")) else null)
    if (nsUrl == null) return
    val item = AVPlayerItem(uRL = nsUrl)
    val avPlayer = remember(videoUrl) {
        AVPlayer().apply {
            replaceCurrentItemWithPlayerItem(item)
            setMuted(true)
            play()
        }
    }
    DisposableEffect(videoUrl) {
        val observer = platform.Foundation.NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = null
        ) {
            avPlayer.seekToTime(platform.CoreMedia.CMTimeMakeWithSeconds(0.0, NSEC_PER_SEC.toInt()))
            avPlayer.play()
        }
        onDispose {
            platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(observer)
            avPlayer.pause()
        }
    }
    AvatarVideoSurfaceIos(player = avPlayer, modifier = modifier)
}
