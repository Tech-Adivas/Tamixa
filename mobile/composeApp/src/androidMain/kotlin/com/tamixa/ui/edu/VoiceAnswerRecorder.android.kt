package com.tamixa.ui.edu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File

private const val TAG = "VoiceAnswerRecorder"

@Composable
actual fun rememberVoiceAnswerRecorder(): VoiceAnswerRecorder? {
    val context = LocalContext.current
    val recorder = remember(context) { AndroidVoiceAnswerRecorder(context.applicationContext) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        recorder.onPermissionResult(granted)
    }
    recorder.launchPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    return recorder
}

private class AndroidVoiceAnswerRecorder(private val context: Context) : VoiceAnswerRecorder {
    var launchPermission: (() -> Unit)? = null
    private var pendingPermission: ((Boolean) -> Unit)? = null
    private var mediaRecorder: MediaRecorder? = null
    private var file: File? = null

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        if (hasPermission()) return onResult(true)
        pendingPermission = onResult
        launchPermission?.invoke() ?: onPermissionResult(false)
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) Log.i(TAG, "RECORD_AUDIO denied for voice answer")
        pendingPermission?.invoke(granted)
        pendingPermission = null
    }

    override fun start(): Boolean {
        cancel()
        if (!hasPermission()) return false
        return try {
            val out = File.createTempFile("voice_answer_", ".m4a", context.cacheDir)
            // MediaRecorder(Context) is API 31+; minSdk is 26.
            @Suppress("DEPRECATION")
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            rec.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioSamplingRate(16_000)
            rec.setAudioEncodingBitRate(32_000)
            rec.setAudioChannels(1)
            rec.setOutputFile(out.absolutePath)
            rec.prepare()
            rec.start()
            file = out
            mediaRecorder = rec
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start voice answer recording: ${e.message}")
            cancel()
            false
        }
    }

    override fun level(): Float =
        runCatching { (mediaRecorder?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0f).coerceIn(0f, 1f)

    override fun stop(): ByteArray? {
        val rec = mediaRecorder ?: return null
        val out = file
        mediaRecorder = null
        file = null
        return try {
            rec.stop()
            out?.readBytes()?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            // stop() throws when nothing was captured (stopped right after start).
            Log.w(TAG, "Voice answer recording stop failed: ${e.message}")
            null
        } finally {
            runCatching { rec.release() }
            out?.delete()
        }
    }

    override fun cancel() {
        val rec = mediaRecorder
        mediaRecorder = null
        if (rec != null) {
            runCatching { rec.stop() }
            runCatching { rec.release() }
        }
        file?.delete()
        file = null
    }
}
