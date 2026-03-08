package com.araro.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.araro.android.BuildConfig
import com.araro.android.component.FamilyVoiceRecordDialog as AndroidFamilyVoiceRecordDialog
import com.araro.android.component.AvatarVideoSurface as AndroidAvatarVideoSurface
import com.araro.android.player.synthesizeStoryToFile as androidSynthesizeStoryToFile

actual fun openUrl(url: String) {
    val ctx = getApplicationContext()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

actual fun getSubscriptionWebUrl(): String = BuildConfig.SUBSCRIPTION_WEB_URL

actual fun shareStory(title: String, text: String) {
    val ctx = getApplicationContext()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title)
    }
    ctx.startActivity(Intent.createChooser(intent, null))
}

actual fun downloadFile(url: String, filename: String, storyTitle: String, mimeType: String) {
    val ctx = getApplicationContext()
    val ext = when {
        mimeType.contains("mp4") -> "mp4"
        mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
        else -> "mp3"
    }
    val outName = if (filename.contains(".")) "araro_$filename" else "araro_$filename.$ext"
    val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
        setTitle(storyTitle)
        setDescription("Araro story")
        setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalFilesDir(ctx, android.os.Environment.DIRECTORY_DOWNLOADS, outName)
    }
    (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager)?.enqueue(request)
}

actual suspend fun synthesizeStoryToFile(text: String, languageCode: String): String? {
    return androidSynthesizeStoryToFile(getApplicationContext(), text, languageCode)
}

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}

@Composable
actual fun rememberAudioPickerLauncher(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { u ->
            context.contentResolver.openInputStream(u)?.use { stream ->
                val bytes = stream.readBytes()
                val name = u.lastPathSegment?.substringAfterLast('/') ?: "audio"
                onResult(bytes, name)
            } ?: onResult(null, null)
        } ?: onResult(null, null)
    }
    return remember { { launcher.launch("audio/*") } }
}

@Composable
actual fun rememberImagePickerLauncher(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { u ->
            context.contentResolver.openInputStream(u)?.use { stream ->
                val bytes = stream.readBytes()
                val contentType = context.contentResolver.getType(u) ?: "image/jpeg"
                onResult(bytes, contentType)
            } ?: onResult(null, null)
        } ?: onResult(null, null)
    }
    return remember { { launcher.launch("image/*") } }
}

@Composable
actual fun FamilyVoiceRecordDialog(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
) {
    AndroidFamilyVoiceRecordDialog(onDismiss = onDismiss, onRecordingComplete = onRecordingComplete)
}

@Composable
actual fun AvatarVideoSurface(
    player: Any?,
    modifier: Modifier
) {
    AndroidAvatarVideoSurface(player = player as? androidx.media3.common.Player, modifier = modifier)
}

// Application context holder for platform API calls from non-Composable code
private var _appContext: Context? = null
fun setPlatformAppContext(ctx: Context) {
    _appContext = ctx.applicationContext
}
private fun getApplicationContext(): Context =
    _appContext ?: error("Platform not initialized: call setPlatformAppContext in Application.onCreate")
