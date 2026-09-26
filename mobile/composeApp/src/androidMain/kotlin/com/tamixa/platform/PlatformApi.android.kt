package com.tamixa.platform

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tamixa.android.BuildConfig
import com.tamixa.runtime.ServerEnvironmentCache
import com.tamixa.android.component.FamilyVoiceRecordDialog as AndroidFamilyVoiceRecordDialog
import com.tamixa.android.component.AvatarVideoSurface as AndroidAvatarVideoSurface
import com.tamixa.android.player.synthesizeStoryToFile as androidSynthesizeStoryToFile
import com.tamixa.util.TamixaLog
import java.util.Locale

actual fun openUrl(url: String) {
    val ctx = getApplicationContext()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

actual fun getSubscriptionWebUrl(): String =
    ServerEnvironmentCache.effectiveSubscriptionWebUrl(BuildConfig.SUBSCRIPTION_WEB_URL)

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
        mimeType.contains("video") -> "mp4"
        mimeType.contains("mp4") || mimeType.contains("x-m4a") -> "m4a"
        mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
        mimeType.contains("aac") -> "aac"
        mimeType.contains("ogg") -> "ogg"
        mimeType.contains("wav") -> "wav"
        mimeType.contains("webm") -> "webm"
        else -> "mp3"
    }
    val outName = if (filename.contains(".")) "tamixa_$filename" else "tamixa_$filename.$ext"
    val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
        setTitle(storyTitle)
        setDescription("Tamixa story")
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

actual fun playSplashRevealSound() {
    try {
        val ctx = getApplicationContext()
        val toneGenerator = android.media.ToneGenerator(
            android.media.AudioManager.STREAM_MUSIC,
            60
        )
        toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 180)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                toneGenerator.release()
            } catch (e: Exception) {
                TamixaLog.w("PlatformAndroid", "ToneGenerator.release failed", e)
            }
        }, 250)
    } catch (e: Exception) {
        TamixaLog.w("PlatformAndroid", "playSplashRevealSound failed", e)
    }
}

// Application context holder for platform API calls from non-Composable code
private var _appContext: Context? = null
fun setPlatformAppContext(ctx: Context) {
    _appContext = ctx.applicationContext
}

/** Application context after [setPlatformAppContext]; used for image prefetch and similar. */
fun tamixaApplicationContextOrNull(): Context? = _appContext

private fun getApplicationContext(): Context =
    _appContext ?: error("Platform not initialized: call setPlatformAppContext in Application.onCreate")

private val plainTextTtsMainHandler = Handler(Looper.getMainLooper())
private var plainTextTtsEngine: TextToSpeech? = null

private fun languageCodeToLocaleForPlainTts(code: String): Locale = when (code.lowercase()) {
    "ta" -> Locale("ta", "IN")
    "hi" -> Locale("hi", "IN")
    "te" -> Locale("te", "IN")
    "kn" -> Locale("kn", "IN")
    "ml" -> Locale("ml", "IN")
    "mr" -> Locale("mr", "IN")
    "bn" -> Locale("bn", "IN")
    "en" -> Locale.ENGLISH
    else -> {
        val parts = code.split("-", "_")
        if (parts.size >= 2) Locale(parts[0], parts[1])
        else Locale.forLanguageTag(code)
    }
}

private fun speakPlainTextOnEngine(engine: TextToSpeech, languageCode: String, utterance: String) {
    val locale = languageCodeToLocaleForPlainTts(languageCode)
    val langResult = engine.setLanguage(locale)
    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
        engine.language = Locale.ENGLISH
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }
    engine.setSpeechRate(0.92f)
    engine.setPitch(1.0f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        engine.speak(utterance, TextToSpeech.QUEUE_FLUSH, Bundle(), "tamixa_plain_tts")
    } else {
        @Suppress("DEPRECATION")
        engine.speak(utterance, TextToSpeech.QUEUE_FLUSH, null)
    }
}

actual fun speakPlainText(text: String, languageCode: String) {
    val utterance = text.trim().replace(Regex("\\s+"), " ")
    if (utterance.isEmpty()) return
    val ctx = getApplicationContext()
    plainTextTtsMainHandler.post {
        val existing = plainTextTtsEngine
        if (existing != null) {
            speakPlainTextOnEngine(existing, languageCode, utterance)
            return@post
        }
        plainTextTtsEngine = TextToSpeech(ctx) { status ->
            if (status != TextToSpeech.SUCCESS) {
                TamixaLog.w("PlatformAndroid", "PlainText TTS init failed status=$status")
                return@TextToSpeech
            }
            plainTextTtsMainHandler.post {
                plainTextTtsEngine?.let { speakPlainTextOnEngine(it, languageCode, utterance) }
            }
        }
    }
}

actual fun stopPlainTextSpeech() {
    plainTextTtsMainHandler.post {
        plainTextTtsEngine?.stop()
    }
}
