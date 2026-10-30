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

actual suspend fun writeTempAudioFile(bytes: ByteArray, fileName: String): String? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "audio.m4a" }
            val file = java.io.File(getApplicationContext().cacheDir, safeName)
            file.writeBytes(bytes)
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "writeTempAudioFile failed: ${e.message}", e)
            null
        }
    }

actual suspend fun deleteTempAudioFile(fileName: String) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "audio.m4a" }
        try {
            val file = java.io.File(getApplicationContext().cacheDir, safeName)
            if (file.exists() && !file.delete()) {
                TamixaLog.w("PlatformAndroid", "deleteTempAudioFile could not delete $safeName")
            }
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "deleteTempAudioFile failed for $safeName: ${e.message}", e)
        }
    }
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
        if (uri == null) {
            TamixaLog.i("PlatformAndroid", "Image picker returned no image (cancelled)")
            onResult(null, null)
            return@rememberLauncherForActivityResult
        }
        // Never fail silently: a provider error or missing stream is logged and reported as "no image".
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "Image picker: reading picked image failed: ${e.message}", e)
            null
        }
        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        TamixaLog.i("PlatformAndroid", "Image picker result: ${bytes?.size ?: "no"} bytes, $contentType")
        onResult(bytes, contentType)
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

actual fun isLocalVoiceRecordingTestEnabled(): Boolean =
    BuildConfig.DEBUG && BuildConfig.TAMIXA_ENVIRONMENT == "dev"

// Local test media (voice recordings, avatar photos) lives in filesDir (not cacheDir, which Android may clear),
// one file per save: <prefix><epochMillis>.<ext>. The timestamp in the name is the creation time.
private const val LOCAL_VOICE_DIR = "voice_recordings"
private const val LOCAL_VOICE_PREFIX = "my_voice_"
private const val LOCAL_AVATAR_DIR = "avatar_images"
private const val LOCAL_AVATAR_PREFIX = "my_photo_"

private fun localMediaDir(dirName: String): java.io.File =
    java.io.File(getApplicationContext().filesDir, dirName)

/** Writes [bytes] as a new file; temp file + rename so a failed write never leaves a half-written file. */
private suspend fun saveLocalMediaFile(dirName: String, prefix: String, extension: String, bytes: ByteArray): java.io.File? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (bytes.isEmpty()) return@withContext null
        try {
            val dir = localMediaDir(dirName).apply { mkdirs() }
            val file = java.io.File(dir, "$prefix${System.currentTimeMillis()}.$extension")
            val tmp = java.io.File(dir, "${file.name}.tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(file)) {
                tmp.delete()
                return@withContext null
            }
            TamixaLog.i("PlatformAndroid", "Local media saved in $dirName (${bytes.size} bytes)")
            file
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "saveLocalMediaFile($dirName) failed: ${e.message}", e)
            null
        }
    }

/** Saved files in [dirName], newest first, as (file, createdAtMillis). */
private suspend fun listLocalMediaFiles(dirName: String, prefix: String): List<Pair<java.io.File, Long>> =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        localMediaDir(dirName)
            .listFiles { f -> f.isFile && f.name.startsWith(prefix) && !f.name.endsWith(".tmp") }
            .orEmpty()
            .map { f -> f to (f.name.removePrefix(prefix).substringBefore('.').toLongOrNull() ?: f.lastModified()) }
            .sortedByDescending { it.second }
    }

private suspend fun deleteLocalMediaFile(dirName: String, uri: String): Boolean =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val file = java.io.File(uri.removePrefix("file://"))
        // Only ever delete files inside our own media folder.
        if (file.parentFile?.canonicalPath != localMediaDir(dirName).canonicalPath) return@withContext false
        file.delete()
    }

actual suspend fun saveLocalVoiceRecording(bytes: ByteArray): LocalVoiceRecording? =
    saveLocalMediaFile(LOCAL_VOICE_DIR, LOCAL_VOICE_PREFIX, "m4a", bytes)?.let { f ->
        LocalVoiceRecording("file://${f.absolutePath}", f.name.removePrefix(LOCAL_VOICE_PREFIX).substringBefore('.').toLong(), f.length())
    }

actual suspend fun listLocalVoiceRecordings(): List<LocalVoiceRecording> {
    val backedUp = readBackedUp(LOCAL_VOICE_DIR)
    return listLocalMediaFiles(LOCAL_VOICE_DIR, LOCAL_VOICE_PREFIX)
        .map { (f, created) ->
            LocalVoiceRecording("file://${f.absolutePath}", created, f.length(), f.name in backedUp, backedUp[f.name])
        }
}

actual suspend fun deleteLocalVoiceRecording(uri: String): Boolean {
    val deleted = deleteLocalMediaFile(LOCAL_VOICE_DIR, uri)
    // Forget its "backed up" entry too, so the marker only lists recordings that still exist.
    if (deleted) writeBackedUp(LOCAL_VOICE_DIR, readBackedUp(LOCAL_VOICE_DIR) - java.io.File(uri.removePrefix("file://")).name)
    return deleted
}

actual suspend fun saveLocalAvatarImage(bytes: ByteArray, contentType: String?): LocalAvatarImage? {
    val extension = when (contentType?.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
    return saveLocalMediaFile(LOCAL_AVATAR_DIR, LOCAL_AVATAR_PREFIX, extension, bytes)?.let { f ->
        LocalAvatarImage("file://${f.absolutePath}", f.name.removePrefix(LOCAL_AVATAR_PREFIX).substringBefore('.').toLong(), f.length())
    }
}

actual suspend fun listLocalAvatarImages(): List<LocalAvatarImage> {
    val backedUp = readBackedUp(LOCAL_AVATAR_DIR).keys
    return listLocalMediaFiles(LOCAL_AVATAR_DIR, LOCAL_AVATAR_PREFIX)
        .map { (f, created) -> LocalAvatarImage("file://${f.absolutePath}", created, f.length(), f.name in backedUp) }
}

actual suspend fun deleteLocalAvatarImage(uri: String): Boolean = deleteLocalMediaFile(LOCAL_AVATAR_DIR, uri)

// The active photo's file name is kept in a small marker file next to the photos (not matched by the photo prefix).
private fun activeAvatarMarker(): java.io.File = java.io.File(localMediaDir(LOCAL_AVATAR_DIR), "active.txt")

actual suspend fun setActiveLocalAvatarImage(uri: String) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            localMediaDir(LOCAL_AVATAR_DIR).mkdirs()
            activeAvatarMarker().writeText(java.io.File(uri.removePrefix("file://")).name)
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "setActiveLocalAvatarImage failed: ${e.message}", e)
        }
    }
}

actual suspend fun getActiveLocalAvatarImageUri(): String? {
    val photos = listLocalAvatarImages()
    val activeName = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        activeAvatarMarker().takeIf { it.isFile }?.readText()?.trim()
    }
    return (photos.firstOrNull { it.uri.endsWith("/$activeName") } ?: photos.firstOrNull())?.uri
}

// Files already uploaded to the server, one per line as "<file name>" or "<file name><TAB><server id>",
// in a marker file next to the media (like active.txt, not matched by the media prefix).
private const val BACKED_UP_MARKER = "backed_up.txt"

/** Uploaded file name -> server id (null when the upload predates recorded ids). */
private suspend fun readBackedUp(dirName: String): Map<String, Long?> =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            java.io.File(localMediaDir(dirName), BACKED_UP_MARKER).takeIf { it.isFile }
                ?.readLines()?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?.associate { line -> line.substringBefore('\t') to line.substringAfter('\t', "").toLongOrNull() }
                .orEmpty()
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "readBackedUp($dirName) failed: ${e.message}", e)
            emptyMap()
        }
    }

private suspend fun writeBackedUp(dirName: String, entries: Map<String, Long?>) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            localMediaDir(dirName).mkdirs()
            java.io.File(localMediaDir(dirName), BACKED_UP_MARKER).writeText(
                entries.entries.joinToString("\n") { (name, id) -> if (id != null) "$name\t$id" else name }
            )
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "writeBackedUp($dirName) failed: ${e.message}", e)
        }
    }
}

/** The media folder (voice or avatar) holding [uri], or null when the file isn't one of ours. */
private fun ownMediaFile(uri: String): Pair<java.io.File, String>? {
    val file = java.io.File(uri.removePrefix("file://"))
    val parent = file.parentFile?.canonicalPath ?: return null
    val dirName = listOf(LOCAL_VOICE_DIR, LOCAL_AVATAR_DIR).firstOrNull { localMediaDir(it).canonicalPath == parent }
        ?: return null
    return file to dirName
}

actual suspend fun readLocalMediaBytes(uri: String): ByteArray? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val (file, _) = ownMediaFile(uri) ?: return@withContext null
        try {
            file.takeIf { it.isFile }?.readBytes()
        } catch (e: Exception) {
            TamixaLog.w("PlatformAndroid", "readLocalMediaBytes failed: ${e.message}", e)
            null
        }
    }

actual suspend fun readLocalAvatarForUpload(uri: String): Pair<ByteArray, String>? {
    val bytes = readLocalMediaBytes(uri) ?: return null
    return when (uri.substringAfterLast('.').lowercase()) {
        "jpg", "jpeg" -> bytes to "image/jpeg"
        "png" -> bytes to "image/png"
        else -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext null
                val out = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                bitmap.recycle()
                out.toByteArray() to "image/jpeg"
            } catch (e: Exception) {
                TamixaLog.w("PlatformAndroid", "Converting local photo to JPEG failed: ${e.message}", e)
                null
            }
        }
    }
}

actual suspend fun takeDeletedBackedUpVoiceRecordings(): List<Pair<String, Long?>> {
    val backedUp = readBackedUp(LOCAL_VOICE_DIR)
    if (backedUp.isEmpty()) return emptyList()
    val existing = listLocalMediaFiles(LOCAL_VOICE_DIR, LOCAL_VOICE_PREFIX).map { it.first.name }.toSet()
    val missing = backedUp.filterKeys { it !in existing }
    if (missing.isNotEmpty()) writeBackedUp(LOCAL_VOICE_DIR, backedUp - missing.keys)
    return missing.map { (name, id) -> name to id }
}

actual suspend fun markLocalMediaBackedUp(uri: String, remoteId: Long?) {
    val (file, dirName) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ownMediaFile(uri) } ?: return
    // The server keeps a single avatar, so for photos only the latest upload counts as backed up.
    val entries = if (dirName == LOCAL_AVATAR_DIR) mapOf(file.name to remoteId) else readBackedUp(dirName) + (file.name to remoteId)
    writeBackedUp(dirName, entries)
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
