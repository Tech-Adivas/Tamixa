package com.tamixa.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.tamixa.network.ktorEngine
import com.tamixa.util.TamixaLog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.UIKit.UIApplication

private const val SUBSCRIPTION_WEB_URL = "https://app.tamixa.com/subscription"

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
    val shareText = if (title.isNotBlank()) "$title\n\n$text" else text
    val activityVC = platform.UIKit.UIActivityViewController(
        activityItems = listOf(shareText),
        applicationActivities = null
    )
    rootVc.presentViewController(activityVC, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun downloadFile(url: String, filename: String, storyTitle: String, mimeType: String) {
    MainScope().launch {
        try {
            val bytes = withContext(Dispatchers.Default) {
                val client = HttpClient(ktorEngine)
                try {
                    client.get(url).body<ByteArray>()
                } finally {
                    client.close()
                }
            }
            val ext = when {
                mimeType.contains("video") -> "mp4"
                mimeType.contains("mp4") || mimeType.contains("x-m4a") -> "m4a"
                mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
                mimeType.contains("aac") -> "aac"
                mimeType.contains("ogg") -> "ogg"
                mimeType.contains("wav") -> "wav"
                mimeType.contains("webm") -> "webm"
                else -> "mp3"
            }
            val safeName = if (filename.contains(".")) "tamixa_$filename" else "tamixa_$filename.$ext"
            val tmpDir = NSTemporaryDirectory() ?: return@launch
            val path = (tmpDir as String).trimEnd('/') + "/$safeName"
            val written = bytes.writeToPath(path)
            if (!written) {
                TamixaLog.w("PlatformIOS", "downloadFile: failed to write to $path")
                return@launch
            }
            withContext(Dispatchers.Main) {
                val fileUrl = NSURL.fileURLWithPath(path)
                val window = UIApplication.sharedApplication.keyWindow
                val rootVc = window?.rootViewController ?: return@withContext
                val activityVC = platform.UIKit.UIActivityViewController(
                    activityItems = listOf(fileUrl),
                    applicationActivities = null
                )
                rootVc.presentViewController(activityVC, animated = true, completion = null)
            }
        } catch (e: Throwable) {
            TamixaLog.w("PlatformIOS", "downloadFile failed: ${e.message}", e)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.writeToPath(path: String): Boolean = usePinned { pin ->
    val file = fopen(path, "wb")
    if (file == null) return@usePinned false
    try {
        val written = fwrite(pin.addressOf(0), 1u, size.toULong(), file)
        written == size.toULong()
    } finally {
        fclose(file)
    }
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
    com.tamixa.ios.component.FamilyVoiceRecordDialogIos(
        onDismiss = onDismiss,
        onRecordingComplete = onRecordingComplete
    )
}

actual fun playSplashRevealSound() {
    // Optional: Add AVAudioPlayer with bundled splash_reveal.mp3 for premium experience
}

@Composable
actual fun AvatarVideoSurface(
    player: Any?,
    modifier: Modifier
) {
    com.tamixa.ios.component.AvatarVideoSurfaceIos(
        player = player,
        modifier = modifier
    )
}
