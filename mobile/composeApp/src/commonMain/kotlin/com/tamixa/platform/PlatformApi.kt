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

/** Writes audio bytes to a temp/cache file for local playback. Returns a file:// URI or null on failure. */
expect suspend fun writeTempAudioFile(bytes: ByteArray, fileName: String): String?

/** Deletes a file written by [writeTempAudioFile] (same [fileName]); a missing file is not an error. */
expect suspend fun deleteTempAudioFile(fileName: String)

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

/**
 * Dev-only local voice recording test: lets non-premium users record and preview on-device without uploading.
 * True only for Android dev debug builds; always false on qa/prod/release and iOS.
 */
expect fun isLocalVoiceRecordingTestEnabled(): Boolean

/** A voice recording saved on the device by the local recording test. */
data class LocalVoiceRecording(
    /** file:// URI, usable with rememberLocalFileController. */
    val uri: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
    /** True once this recording has been uploaded to the server (S3). */
    val backedUp: Boolean = false,
    /**
     * Id the server returned for the upload; null if not uploaded yet or uploaded before ids were recorded.
     * For reference only: uploads before the voice-flow fix saved the cloning job id here, not the voice id,
     * so deletes find the server copy by file name instead (see VoiceBackupSync).
     */
    val remoteId: Long? = null
)

/**
 * Local voice recording: saves [bytes] as a new on-device recording in app-private storage
 * (survives restarts). Earlier recordings are kept. Returns the saved recording or null. Saving does not upload;
 * LocalMediaSyncRepository backs recordings up to the server afterwards.
 */
expect suspend fun saveLocalVoiceRecording(bytes: ByteArray): LocalVoiceRecording?

/** All saved local voice recordings, newest first. */
expect suspend fun listLocalVoiceRecordings(): List<LocalVoiceRecording>

/** Deletes one saved local voice recording by its [uri]. Returns true if it was removed. */
expect suspend fun deleteLocalVoiceRecording(uri: String): Boolean

/** A photo saved on the device by the local avatar test (picked from the gallery; the active one is backed up). */
data class LocalAvatarImage(
    /** file:// URI, loadable by Coil. */
    val uri: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
    /** True when this photo is the one currently stored on the server (S3) as the profile avatar. */
    val backedUp: Boolean = false
)

/** Saves a picked photo as a new on-device image in app-private storage. Earlier photos are kept. */
expect suspend fun saveLocalAvatarImage(bytes: ByteArray, contentType: String?): LocalAvatarImage?

/** All saved local avatar photos, newest first. */
expect suspend fun listLocalAvatarImages(): List<LocalAvatarImage>

/** Deletes one saved local avatar photo by its [uri]. Returns true if it was removed. */
expect suspend fun deleteLocalAvatarImage(uri: String): Boolean

/** Marks a saved local photo as the active profile photo (shown on the Me screen). Persists across restarts. */
expect suspend fun setActiveLocalAvatarImage(uri: String)

/**
 * URI of the active local profile photo: the one chosen with [setActiveLocalAvatarImage] if it still exists,
 * otherwise the newest saved photo, or null when none are saved.
 */
expect suspend fun getActiveLocalAvatarImageUri(): String?

/** Bytes of a saved local recording or photo (by its [uri]), for uploading; null if missing or not ours. */
expect suspend fun readLocalMediaBytes(uri: String): ByteArray?

/**
 * A saved local photo ready for the avatar upload, as (bytes, contentType). The server accepts JPEG or PNG only,
 * so other formats (e.g. WebP from the gallery) are converted to JPEG. Null if missing, not ours, or unreadable.
 */
expect suspend fun readLocalAvatarForUpload(uri: String): Pair<ByteArray, String>?

/**
 * Records that the saved local recording or photo at [uri] was uploaded to the server, with the server's
 * [remoteId] when known (voice profile id, used to delete it from the server later).
 * Recordings: each one is marked on its own. Photos: the server keeps one avatar, so only the latest is marked.
 */
expect suspend fun markLocalMediaBackedUp(uri: String, remoteId: Long? = null)

/**
 * Recordings that were uploaded but whose file is no longer on the device (deleted by an app version that
 * only removed the local copy), as (file name, server id or null). They are removed from the backed-up list,
 * so each is returned once; the caller deletes their cloud copies.
 */
expect suspend fun takeDeletedBackedUpVoiceRecordings(): List<Pair<String, Long?>>

/** Avatar video surface. Player type is platform-specific (ExoPlayer on Android, AVPlayer on iOS). */
@Composable
expect fun AvatarVideoSurface(
    player: Any?,
    modifier: Modifier
)

/** Plays a short reveal chime synced with splash logo animation. Call when logo lands (~850ms). */
expect fun playSplashRevealSound()

/**
 * Speak plain text with on-device TTS (e.g. crisis navigator checklists). Stops any prior [speakPlainText] utterance.
 * [languageCode] is app language (ta, hi, en, …). No-op if text is blank.
 */
expect fun speakPlainText(text: String, languageCode: String)

/** Stop speech started by [speakPlainText]. */
expect fun stopPlainTextSpeech()
