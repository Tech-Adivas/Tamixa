package com.tamixa.android.component

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tamixa.android.player.rememberMediaPlayerController
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaDialogDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.TimeSource

private const val TAG = "FamilyVoiceRecord"

/**
 * Shortest recording that can be saved. The recording becomes the parent's cloned voice, and instant voice
 * cloning sounds more like the speaker the more speech it gets: 30 s samples came out generic, 1–2 minutes of
 * clear read-aloud speech is what ElevenLabs recommends. The reading script takes about 60–75 s at a bedtime pace.
 */
private const val MIN_RECORDING_SECONDS = 60

/** Recording stops by itself here: past 2–3 minutes an instant clone barely improves and the upload only grows. */
private const val MAX_RECORDING_SECONDS = 180

private fun hasRecordAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Tips and a story to read aloud → tap to record → stop (or auto-stop at [MAX_RECORDING_SECONDS]) → preview
 * (play/pause) → Save / Record again / Cancel. Only a recording of at least [MIN_RECORDING_SECONDS] can be saved.
 * The recording stays in the cache dir until Save (bytes handed to [onRecordingComplete]) or discard.
 */
@Composable
fun FamilyVoiceRecordDialog(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    var micPermissionGranted by remember { mutableStateOf(hasRecordAudioPermission(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    // Android stops showing the system prompt after repeated denials; only app settings can re-enable it then.
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }
    var startFailed by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micPermissionGranted = granted
        permissionDenied = !granted
        permissionPermanentlyDenied = !granted && context.findActivity()?.let { activity ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        } == true
        if (!granted) Log.i(TAG, "RECORD_AUDIO denied (permanently=$permissionPermanentlyDenied)")
    }

    fun requestMicPermission() {
        if (hasRecordAudioPermission(context)) {
            micPermissionGranted = true
            permissionDenied = false
            permissionPermanentlyDenied = false
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun openAppSettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not open app settings", e)
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    /** Length of the finished recording, from the recording timer. */
    var recordedSeconds by remember { mutableIntStateOf(0) }
    var isPreviewing by remember { mutableStateOf(false) }
    var previewProgress by remember { mutableFloatStateOf(0f) }
    var isSaving by remember { mutableStateOf(false) }
    var stoppedAtLimit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!micPermissionGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            // Read a monotonic clock rather than counting delay(1000) ticks: each tick runs late under load, so a
            // counted timer fell behind the real recording (a "180 s" capture was 209 s) and the limit fired late.
            val startMark = TimeSource.Monotonic.markNow()
            while (true) {
                delay(250)
                elapsedSeconds = startMark.elapsedNow().inWholeSeconds.toInt()
            }
        }
    }

    val recordedFile = recordFile?.takeIf { !isRecording }
    // Mounted only while previewing; MediaPlayer starts on load and is released when the URI goes back to null.
    val previewController = rememberMediaPlayerController(
        fileUri = if (isPreviewing && recordedFile != null) "file://${recordedFile.absolutePath}" else null,
        storyTitle = Strings.previewRecording(),
        storyTheme = "",
        scope = scope,
        onProgressChanged = { p ->
            previewProgress = p
            if (p >= 1f) isPreviewing = false
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.apply {
                    if (isRecording) {
                        try {
                            stop()
                        } catch (e: Exception) {
                            Log.d(TAG, "MediaRecorder stop() during cleanup (recorder may already be stopped): ${e.message}")
                        }
                    }
                    release()
                }
                mediaRecorder = null
                recordFile?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Cleanup error", e)
            }
        }
    }

    fun discardRecording() {
        isPreviewing = false
        previewProgress = 0f
        recordFile?.delete()
        recordFile = null
    }

    fun startRecording() {
        if (!micPermissionGranted) {
            requestMicPermission()
            return
        }
        try {
            startFailed = false
            discardRecording()
            val cacheDir = context.cacheDir
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "family_voice_$timestamp.m4a")
            recordFile = file

            // MediaRecorder(Context) is API 31+; minSdk is 26.
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // Recording quality: without these MediaRecorder defaults to 8 kHz (telephone quality), which
                // sounds muffled on preview and is poor voice-clone input. 48 kHz mono AAC at 128 kbps keeps
                // speech clear (~1 MB per minute). 48 kHz is the native mic rate on most phones and the emulator,
                // so no resampling happens during capture.
                setAudioSamplingRate(48_000)
                setAudioChannels(1)
                setAudioEncodingBitRate(128_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            stoppedAtLimit = false
            isRecording = true
        } catch (e: Exception) {
            Log.e(TAG, "Start recording failed", e)
            mediaRecorder?.release()
            mediaRecorder = null
            discardRecording()
            // Stay open so the user sees the error and can retry from the mic button.
            startFailed = true
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            recordedSeconds = elapsedSeconds

            val file = recordFile ?: return
            if (!file.exists() || file.length() == 0L) {
                Log.w(TAG, "Recording produced an empty file; discarding")
                discardRecording()
            }
        } catch (e: Exception) {
            // stop() throws when almost nothing was captured (e.g. immediate stop) — return to idle so the user can retry.
            Log.e(TAG, "Stop recording failed", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            discardRecording()
        }
    }

    LaunchedEffect(elapsedSeconds) {
        if (isRecording && elapsedSeconds >= MAX_RECORDING_SECONDS) {
            Log.i(TAG, "Recording reached the ${MAX_RECORDING_SECONDS}s limit; stopping")
            stopRecording()
            stoppedAtLimit = true
        }
    }

    fun togglePreview() {
        if (!isPreviewing) {
            previewProgress = 0f
            isPreviewing = true
        } else {
            previewController.playPause()
        }
    }

    fun saveRecording() {
        val file = recordFile ?: return
        isPreviewing = false
        isSaving = true
        scope.launch {
            val bytes = try {
                // Even out the level (too loud → just under full scale, too quiet → boosted);
                // falls back to the original recording if normalization isn't needed or fails.
                withContext(Dispatchers.IO) {
                    RecordingLevelNormalizer.normalize(file, context.cacheDir) ?: file.readBytes()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reading recording failed", e)
                null
            }
            withContext(Dispatchers.IO) { file.delete() }
            recordFile = null
            isSaving = false
            if (bytes != null && bytes.isNotEmpty()) {
                onRecordingComplete(bytes)
            }
            onDismiss()
        }
    }

    fun cancel() {
        discardRecording()
        onDismiss()
    }

    val hasRecording = recordedFile != null
    val recordingTooShort = hasRecording && recordedSeconds < MIN_RECORDING_SECONDS
    val showPermissionDenied = permissionDenied && !micPermissionGranted

    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { cancel() },
            shape = TamixaDialogDefaults.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    Strings.recordYourVoiceNow(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        Strings.micPermissionRequired(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (permissionPermanentlyDenied) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Strings.micPermissionSettingsHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        // After enabling the mic in Settings, Retry re-checks without reopening the dialog.
                        TextButton(onClick = { requestMicPermission() }) {
                            Text(Strings.retry())
                        }
                    }
                }
            },
            confirmButton = {
                if (permissionPermanentlyDenied) {
                    TextButton(onClick = { openAppSettings() }) { Text(Strings.openSettings()) }
                } else {
                    TextButton(onClick = { requestMicPermission() }) { Text(Strings.retry()) }
                }
            },
            dismissButton = {
                TextButton(onClick = { cancel() }) { Text(Strings.close()) }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) cancel() },
        shape = TamixaDialogDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                if (hasRecording) Strings.previewRecording() else Strings.recordYourVoiceNow(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasRecording && stoppedAtLimit) {
                    Text(
                        Strings.recordingStoppedAtLimit(MAX_RECORDING_SECONDS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (recordingTooShort) {
                    Text(
                        Strings.recordingTooShort(MIN_RECORDING_SECONDS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (hasRecording) {
                    val playing = isPreviewing && previewController.isPlaying
                    Surface(
                        onClick = { if (!isSaving) togglePreview() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) Strings.pause() else Strings.play(),
                            modifier = Modifier
                                .padding(24.dp)
                                .size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { if (isPreviewing) previewProgress else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                    } else {
                        TextButton(onClick = { startRecording() }) {
                            Text(Strings.recordAgain())
                        }
                    }
                } else {
                    Text(
                        when {
                            !isRecording -> Strings.tapToStartRecording()
                            elapsedSeconds < MIN_RECORDING_SECONDS -> Strings.recordingKeepGoing(elapsedSeconds, MIN_RECORDING_SECONDS)
                            else -> Strings.recordingLongEnough(elapsedSeconds)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRecording) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            Strings.tapToStopRecording(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (startFailed && !isRecording) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Strings.recordingStartFailed(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        onClick = { if (isRecording) stopRecording() else startRecording() },
                        shape = CircleShape,
                        color = if (isRecording) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) Strings.tapToStopRecording() else Strings.tapToStartRecording(),
                            modifier = Modifier
                                .padding(24.dp)
                                .size(48.dp),
                            tint = if (isRecording) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (!isRecording) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            Strings.voiceCloneTips(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Read aloud: a story in the app language gives the clone the voice it will narrate in.
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            Strings.voiceCloneReadingScript(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (hasRecording) {
                TextButton(onClick = { saveRecording() }, enabled = !isSaving && !recordingTooShort) {
                    Text(Strings.save())
                }
            } else {
                TextButton(onClick = { cancel() }) {
                    Text(Strings.cancel())
                }
            }
        },
        dismissButton = if (hasRecording) {
            {
                TextButton(onClick = { cancel() }, enabled = !isSaving) {
                    Text(Strings.cancel())
                }
            }
        } else null
    )
}
