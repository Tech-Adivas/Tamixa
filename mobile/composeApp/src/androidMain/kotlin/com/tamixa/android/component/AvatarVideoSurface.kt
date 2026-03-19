package com.tamixa.android.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

/**
 * Displays avatar video from ExoPlayer. Use when avatarVideoUrl is present.
 */
@Composable
fun AvatarVideoSurface(
    player: androidx.media3.common.Player?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                controllerHideOnTouch = false
            }
        },
        update = { view -> view.player = player }
    )
}
