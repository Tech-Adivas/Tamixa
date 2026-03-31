package com.tamixa.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tamixa.player.AvatarVideoControllerResult
import com.tamixa.player.StoryPlaybackController
import kotlinx.coroutines.CoroutineScope

/** Streaming audio controller (ExoPlayer on Android, AVPlayer on iOS). */
@Composable
expect fun rememberStreamingController(
    streamUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?
): StoryPlaybackController

/** Local file controller (MediaPlayer on Android, AVPlayer on iOS). */
@Composable
expect fun rememberLocalFileController(
    fileUri: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit
): StoryPlaybackController

/** Avatar video controller (ExoPlayer on Android, AVPlayer on iOS). Returns controller + player for video surface. */
@Composable
expect fun rememberAvatarVideoController(
    videoUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?,
    /** When true, video audio is silent (e.g. Phase 4 host clip while story audio plays). */
    muteVideoAudio: Boolean = false,
    /** When true, loop the clip (host story bumper). */
    repeatVideo: Boolean = false
): AvatarVideoControllerResult

/** Looping, muted video for story cover. Use when coverVideoUrl is present; image remains as fallback behind. */
@Composable
expect fun CoverVideoSurface(
    videoUrl: String?,
    modifier: Modifier
)
