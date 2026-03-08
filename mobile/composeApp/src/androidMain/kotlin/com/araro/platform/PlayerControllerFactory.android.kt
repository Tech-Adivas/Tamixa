package com.araro.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.araro.android.player.rememberAvatarVideoExoPlayerController
import com.araro.android.player.rememberExoPlayerController
import com.araro.android.player.rememberMediaPlayerController
import com.araro.player.AvatarVideoControllerResult
import com.araro.player.StoryPlaybackController
import kotlinx.coroutines.CoroutineScope

@Composable
actual fun rememberStreamingController(
    streamUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?
): StoryPlaybackController = rememberExoPlayerController(
    streamUrl = streamUrl,
    storyTitle = storyTitle,
    storyTheme = storyTheme,
    scope = scope,
    onProgressChanged = onProgressChanged,
    onPlaybackError = onPlaybackError
)

@Composable
actual fun rememberLocalFileController(
    fileUri: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit
): StoryPlaybackController = rememberMediaPlayerController(
    fileUri = fileUri,
    storyTitle = storyTitle,
    storyTheme = storyTheme,
    scope = scope,
    onProgressChanged = onProgressChanged
)

@Composable
actual fun rememberAvatarVideoController(
    videoUrl: String?,
    storyTitle: String,
    storyTheme: String,
    scope: CoroutineScope,
    onProgressChanged: (Float) -> Unit,
    onPlaybackError: (() -> Unit)?
): AvatarVideoControllerResult = rememberAvatarVideoExoPlayerController(
    videoUrl = videoUrl,
    storyTitle = storyTitle,
    storyTheme = storyTheme,
    scope = scope,
    onProgressChanged = onProgressChanged,
    onPlaybackError = onPlaybackError
)

@Composable
actual fun CoverVideoSurface(
    videoUrl: String?,
    modifier: Modifier
) {
    if (videoUrl.isNullOrBlank()) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(videoUrl) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
            }
        },
        update = { view -> view.player = player }
    )
}
