package com.tamixa.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tamixa.player.AvatarVideoControllerResult
import com.tamixa.player.StoryPlaybackController
import com.tamixa.util.TamixaConstants
import com.tamixa.util.TamixaLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol
import platform.Foundation.NSError
import platform.darwin.NSEC_PER_SEC
import androidx.compose.ui.Modifier
import com.tamixa.ios.component.AvatarVideoSurfaceIos

private fun noOpController(): StoryPlaybackController = object : StoryPlaybackController {
    override val isReady get() = false
    override val isPlaying get() = false
    override val progress get() = 0f
    override val durationMillis get() = 0L
    override fun playPause() {}
    override fun rewind() {}
    override fun fastForward() {}
    override fun setSleepTimer(minutes: Int) {}
    override fun share() {}
    override fun download(url: String, filename: String, mimeType: String) {}
}

// AVPlayerItem.Status: unknown = 0, readyToPlay = 1, failed = 2
private const val AVPlayerItemStatusReadyToPlay = 1L
private const val AVPlayerItemStatusFailed = 2L

@OptIn(ExperimentalForeignApi::class)
private fun configureAudioSessionForBackgroundPlayback() {
    val session = AVAudioSession.sharedInstance()
    session.setCategory(AVAudioSessionCategoryPlayback, null)
    // Session activates implicitly when AVPlayer starts. Explicit setActive:error: has
    // interop issues in Kotlin/Native (method name/signature varies by platform lib version).
}

@OptIn(ExperimentalForeignApi::class)
private fun createAvPlayerController(
    mediaUrl: String,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    isReadyState: androidx.compose.runtime.MutableState<Boolean>,
    isPlayingState: androidx.compose.runtime.MutableState<Boolean>,
    progressState: androidx.compose.runtime.MutableState<Float>,
    durationMillisState: MutableState<Long>,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?,
    muteVideoAudio: Boolean = false
): Pair<AVPlayer?, StoryPlaybackController> {
    TamixaLog.d("PlayerIOS", "createAvPlayerController urlType=${TamixaLog.maskUrl(mediaUrl)}")
    configureAudioSessionForBackgroundPlayback()
    val nsUrl = NSURL.URLWithString(mediaUrl)
        ?: (if (mediaUrl.startsWith("file://")) NSURL.fileURLWithPath(mediaUrl.removePrefix("file://")) else null)
        ?: run {
            TamixaLog.w("PlayerIOS", "Invalid media URL (null NSURL)")
            return null to noOpController()
        }
    val item = AVPlayerItem(uRL = nsUrl)
    val avPlayer = AVPlayer()
    avPlayer.replaceCurrentItemWithPlayerItem(item)
    avPlayer.setMuted(muteVideoAudio)
    TamixaLog.d("PlayerIOS", "AVPlayer created and item set")
    var sleepTimerJob: Job? = null
    scope.launch {
        while (true) {
            delay(500)
            val currentItem = avPlayer.currentItem ?: break
            val status = currentItem.status.toLong()
            if (status == AVPlayerItemStatusFailed) {
                val err = currentItem.error as? NSError
                val code = err?.code
                val msg = err?.localizedDescription ?: err?.description ?: "unknown"
                TamixaLog.w("PlayerIOS", "AVPlayerItem failed code=$code domain=${err?.domain} $msg")
                if (code != null && code.toLong() == -12939L) {
                    TamixaLog.w("PlayerIOS", "Hint: -12939 = server must support HTTP Range (206 Partial Content)")
                }
                onPlaybackError?.invoke()
                break
            }
            if (status == AVPlayerItemStatusReadyToPlay) {
                isReadyState.value = true
            }
            val currentTime = CMTimeGetSeconds(avPlayer.currentTime())
            val duration = currentItem.duration?.let { d -> CMTimeGetSeconds(d) } ?: 0.0
            if (duration > 0 && !duration.isNaN() && !duration.isInfinite()) {
                durationMillisState.value = (duration * 1000.0).toLong()
                val p = (currentTime / duration).toFloat().coerceIn(0f, 1f)
                progressState.value = p
                onProgressChanged(p)
            }
            isPlayingState.value = avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
        }
    }
    val ctrl = object : StoryPlaybackController {
        override val isReady: Boolean get() = isReadyState.value
        override val isPlaying: Boolean get() = isPlayingState.value
        override val progress: Float get() = progressState.value
        override val durationMillis: Long get() = durationMillisState.value
        override fun playPause() {
            if (!isReadyState.value) return
            if (avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying) {
                avPlayer.pause()
                isPlayingState.value = false
            } else {
                avPlayer.play()
                isPlayingState.value = true
            }
        }
        override fun rewind() {
            if (!isReadyState.value) return
            val t = (CMTimeGetSeconds(avPlayer.currentTime()) - TamixaConstants.SEEK_MS / 1000.0).coerceAtLeast(0.0)
            avPlayer.currentItem?.seekToTime(CMTimeMakeWithSeconds(t, NSEC_PER_SEC.toInt()), null)
        }
        override fun fastForward() {
            if (!isReadyState.value) return
            val dur = avPlayer.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
            val maxT = if (dur.isNaN()) 0.0 else dur
            val t = (CMTimeGetSeconds(avPlayer.currentTime()) + TamixaConstants.SEEK_MS / 1000.0).coerceAtMost(maxT)
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
    // Do not call play() here — wait until isReady is true; NavHost will auto-play when ready.
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
    val isReadyState = remember(streamUrl) { mutableStateOf(false) }
    val isPlayingState = remember(streamUrl) { mutableStateOf(false) }
    val progressState = remember(streamUrl) { mutableFloatStateOf(0f) }
    val durationMillisState = remember(streamUrl) { mutableStateOf(0L) }
    var controller by remember(streamUrl) { mutableStateOf<StoryPlaybackController>(noOpController()) }
    var avPlayer by remember(streamUrl) { mutableStateOf<AVPlayer?>(null) }
    DisposableEffect(streamUrl) {
        TamixaLog.d("PlayerIOS", "rememberStreamingController streamUrl=${TamixaLog.maskUrl(streamUrl)}")
        if (!streamUrl.isNullOrBlank()) {
            val (player, ctrl) = createAvPlayerController(
                streamUrl,
                storyTitle,
                storyTheme,
                scope,
                isReadyState,
                isPlayingState,
                progressState,
                durationMillisState,
                onProgressChanged,
                onPlaybackError,
                muteVideoAudio = false
            )
            avPlayer = player
            controller = ctrl
        }
        onDispose {
            avPlayer?.replaceCurrentItemWithPlayerItem(null)
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
    val isReadyState = remember(fileUri) { mutableStateOf(false) }
    val isPlayingState = remember(fileUri) { mutableStateOf(false) }
    val progressState = remember(fileUri) { mutableFloatStateOf(0f) }
    val durationMillisState = remember(fileUri) { mutableStateOf(0L) }
    var controller by remember(fileUri) { mutableStateOf<StoryPlaybackController>(noOpController()) }
    var avPlayer by remember(fileUri) { mutableStateOf<AVPlayer?>(null) }
    DisposableEffect(fileUri) {
        if (!fileUri.isNullOrBlank()) {
            val (player, ctrl) = createAvPlayerController(
                fileUri,
                storyTitle,
                storyTheme,
                scope,
                isReadyState,
                isPlayingState,
                progressState,
                durationMillisState,
                onProgressChanged,
                null,
                muteVideoAudio = false
            )
            avPlayer = player
            controller = ctrl
        }
        onDispose {
            avPlayer?.replaceCurrentItemWithPlayerItem(null)
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
    onPlaybackError: (() -> Unit)?,
    muteVideoAudio: Boolean,
    repeatVideo: Boolean
): AvatarVideoControllerResult {
    val isReadyState = remember(videoUrl, muteVideoAudio, repeatVideo) { mutableStateOf(false) }
    val isPlayingState = remember(videoUrl, muteVideoAudio, repeatVideo) { mutableStateOf(false) }
    val progressState = remember(videoUrl, muteVideoAudio, repeatVideo) { mutableFloatStateOf(0f) }
    val durationMillisState = remember(videoUrl, muteVideoAudio, repeatVideo) { mutableStateOf(0L) }
    var result by remember(videoUrl, muteVideoAudio, repeatVideo) {
        mutableStateOf(
            AvatarVideoControllerResult(controller = noOpController(), player = null)
        )
    }
    DisposableEffect(videoUrl, muteVideoAudio, repeatVideo) {
        var endObserver: NSObjectProtocol? = null
        var createdPlayer: AVPlayer? = null
        if (!videoUrl.isNullOrBlank()) {
            val (player, ctrl) = createAvPlayerController(
                videoUrl,
                storyTitle,
                storyTheme,
                scope,
                isReadyState,
                isPlayingState,
                progressState,
                durationMillisState,
                onProgressChanged,
                onPlaybackError,
                muteVideoAudio = muteVideoAudio
            )
            createdPlayer = player
            if (repeatVideo && player != null) {
                player.currentItem?.let { item ->
                    endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                        name = AVPlayerItemDidPlayToEndTimeNotification,
                        `object` = item,
                        queue = null
                    ) { _ ->
                        player.seekToTime(CMTimeMakeWithSeconds(0.0, NSEC_PER_SEC.toInt()))
                        player.play()
                    }
                }
            }
            result = AvatarVideoControllerResult(controller = ctrl, player = player)
            TamixaLog.d("PlayerIOS", "rememberAvatarVideoController player=${if (player != null) "non-null" else "null"}")
        }
        onDispose {
            endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            val p = createdPlayer ?: (result.player as? AVPlayer)
            p?.replaceCurrentItemWithPlayerItem(null)
            p?.pause()
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
