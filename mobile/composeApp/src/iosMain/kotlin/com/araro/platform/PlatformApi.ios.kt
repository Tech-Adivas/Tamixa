package com.araro.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication

private const val SUBSCRIPTION_WEB_URL = "https://app.araro.com/subscription"

@OptIn(ExperimentalForeignApi::class)
actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

actual fun getSubscriptionWebUrl(): String = SUBSCRIPTION_WEB_URL

@OptIn(ExperimentalForeignApi::class)
actual fun shareStory(title: String, text: String) {
    val window = UIApplication.sharedApplication.keyWindow
    val rootVc = window?.rootViewController ?: return
    val activityVC = platform.UIKit.UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    rootVc.presentViewController(activityVC, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun downloadFile(url: String, filename: String, storyTitle: String, mimeType: String) {
    // TODO: NSURLSession.dataTaskWithURL completion handler API differs in Kotlin/Native bindings.
    // Consider using Ktor HttpClient or custom cinterop for async download to Documents directory.
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun synthesizeStoryToFile(text: String, languageCode: String): String? {
    // AVSpeechSynthesizer.speakUtterance outputs to speaker, not file.
    // AVSpeechSynthesizer.write() (iOS 13+) writes to buffer - would require AVAudioFile setup.
    // For now return null; app falls back to streaming audio when available.
    return null
}

private fun languageCodeToLocaleId(code: String): String? = when (code.lowercase()) {
    "ta" -> "ta-IN"
    "hi" -> "hi-IN"
    "te" -> "te-IN"
    "kn" -> "kn-IN"
    "ml" -> "ml-IN"
    "mr" -> "mr-IN"
    "bn" -> "bn-IN"
    "en" -> "en-US"
    else -> {
        val parts = code.split("-", "_")
        if (parts.size >= 2) "${parts[0]}-${parts[1].uppercase()}"
        else code
    }
}

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}

@Composable
actual fun rememberAudioPickerLauncher(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    return { /* TODO: UIDocumentPickerViewController - requires UIViewController */ }
}

@Composable
actual fun rememberImagePickerLauncher(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    return { /* TODO: PHPickerViewController - requires UIViewController */ }
}

@Composable
actual fun FamilyVoiceRecordDialog(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
) {
    com.araro.ios.component.FamilyVoiceRecordDialogIos(
        onDismiss = onDismiss,
        onRecordingComplete = onRecordingComplete
    )
}

@Composable
actual fun AvatarVideoSurface(
    player: Any?,
    modifier: Modifier
) {
    com.araro.ios.component.AvatarVideoSurfaceIos(
        player = player,
        modifier = modifier
    )
}
