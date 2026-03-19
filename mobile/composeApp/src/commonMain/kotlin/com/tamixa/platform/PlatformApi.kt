package com.tamixa.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Opens a URL in the system browser. */
expect fun openUrl(url: String)

/** Returns the subscription management web URL. */
expect fun getSubscriptionWebUrl(): String

/** Shares story title and text via system share sheet. */
expect fun shareStory(title: String, text: String)

/** Downloads a file from URL to device storage. */
expect fun downloadFile(url: String, filename: String, storyTitle: String, mimeType: String)

/** Synthesizes text to audio file. Returns file URI or null. */
expect suspend fun synthesizeStoryToFile(text: String, languageCode: String): String?

/** Registers back handler. Returns unregister function. */
@Composable
expect fun PlatformBackHandler(onBack: () -> Unit)

/** Launches audio file picker. Call launch() to open. */
@Composable
expect fun rememberAudioPickerLauncher(onResult: (ByteArray?, String?) -> Unit): () -> Unit

/** Launches image file picker. Call launch() to open. */
@Composable
expect fun rememberImagePickerLauncher(onResult: (ByteArray?, String?) -> Unit): () -> Unit

/** Family voice recording dialog. */
@Composable
expect fun FamilyVoiceRecordDialog(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
)

/** Avatar video surface. Player type is platform-specific (ExoPlayer on Android, AVPlayer on iOS). */
@Composable
expect fun AvatarVideoSurface(
    player: Any?,
    modifier: Modifier
)

/** Plays a short reveal chime synced with splash logo animation. Call when logo lands (~850ms). */
expect fun playSplashRevealSound()
