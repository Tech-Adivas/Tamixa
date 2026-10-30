package com.tamixa.ios.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.tamixa.ios.audio.IosAudioRecorder
import com.tamixa.platform.rememberLocalFileController
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaDialogDefaults
import com.tamixa.util.TamixaLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.time.TimeSource

private const val TAG = "FamilyVoiceRecordIos"

/** Same rules as Android (see FamilyVoiceRecordDialog.kt): 60 s minimum for a good clone, auto-stop at 3 minutes. */
private const val MIN_RECORDING_SECONDS = 60
private const val MAX_RECORDING_SECONDS = 180

/**
 * Parent voice recording on iOS, matching Android: tips and a story to read aloud → tap to record → stop (or
 * auto-stop at [MAX_RECORDING_SECONDS]) → preview → Save / Record again / Cancel. Only a recording of at least
 * [MIN_RECORDING_SECONDS] can be saved. 48 kHz mono AAC at 128 kbps (AVAudioRecorder); the file stays in the temp
 * directory until Save (bytes handed to [onRecordingComplete]) or discard. Android also evens out the level before
 * upload; on iOS the server's background-noise removal is relied on instead.
 *
 * NOT YET BUILT ON A MAC (written on Windows, 2026-10-17): compile and test on an iPhone before release.
 */
@Composable
fun FamilyVoiceRecordDialogIos(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
) {
    val recorder = remember { IosAudioRecorder(sampleRate = 48_000.0, bitRate = 128_000, filePrefix = "family_voice") }
    var micPermissionGranted by remember { mutableStateOf(recorder.hasPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }
    var startFailed by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordPath by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var recordedSeconds by remember { mutableIntStateOf(0) }
    var isPreviewing by remember { mutableStateOf(false) }
    var previewStarted by remember { mutableStateOf(false) }
    var previewProgress by remember { mutableFloatStateOf(0f) }
    var isSaving by remember { mutableStateOf(false) }
    var stoppedAtLimit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun requestMicPermission() {
        recorder.requestPermission { granted ->
            micPermissionGranted = granted
            permissionDenied = !granted
        }
    }

    fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }

    LaunchedEffect(Unit) {
        if (!micPermissionGranted) requestMicPermission()
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            // Monotonic clock, not counted delay(1000) ticks (those drift late under load; see Android dialog).
            val startMark = TimeSource.Monotonic.markNow()
            while (true) {
                delay(250)
                elapsedSeconds = startMark.elapsedNow().inWholeSeconds.toInt()
            }
        }
    }

    fun discardRecording() {
        isPreviewing = false
        previewProgress = 0f
        recordPath?.let { IosAudioRecorder.deleteFile(it) }
        recordPath = null
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            recordPath?.let { IosAudioRecorder.deleteFile(it) }
        }
    }

    fun startRecording() {
        if (!micPermissionGranted) {
            requestMicPermission()
            return
        }
        startFailed = false
        discardRecording()
        if (recorder.start()) {
            stoppedAtLimit = false
            isRecording = true
        } else {
            // Stay open so the user sees the error and can retry from the mic button.
            startFailed = true
        }
    }

    fun stopRecording() {
        isRecording = false
        recordedSeconds = elapsedSeconds
        recordPath = recorder.stopToFile()
        if (recordPath == null) TamixaLog.w(TAG, "Recording produced no audio; discarded")
    }

    LaunchedEffect(elapsedSeconds) {
        if (isRecording && elapsedSeconds >= MAX_RECORDING_SECONDS) {
            TamixaLog.i(TAG, "Recording reached the ${MAX_RECORDING_SECONDS}s limit; stopping")
            stopRecording()
            stoppedAtLimit = true
        }
    }

    val recordedFile = recordPath?.takeIf { !isRecording }
    // Mounted only while previewing; the AVPlayer is released when the URI goes back to null.
    val previewController = rememberLocalFileController(
        fileUri = if (isPreviewing && recordedFile != null) "file://$recordedFile" else null,
        storyTitle = Strings.previewRecording(),
        storyTheme = "",
        scope = scope,
        onProgressChanged = { p ->
            previewProgress = p
            if (p >= 1f) isPreviewing = false
        }
    )
    // The iOS file player does not start by itself: start it once when it is ready.
    LaunchedEffect(isPreviewing, previewController.isReady) {
        if (isPreviewing && previewController.isReady && !previewStarted) {
            previewStarted = true
            previewController.playPause()
        }
        if (!isPreviewing) previewStarted = false
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
        val filePath = recordPath ?: return
        isPreviewing = false
        isSaving = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { IosAudioRecorder.readFile(filePath) }
            IosAudioRecorder.deleteFile(filePath)
            recordPath = null
            isSaving = false
            if (bytes != null && bytes.isNotEmpty()) onRecordingComplete(bytes)
            else TamixaLog.w(TAG, "Reading recording failed")
            onDismiss()
        }
    }

    fun cancel() {
        recorder.cancel()
        discardRecording()
        onDismiss()
    }

    val hasRecording = recordedFile != null
    val recordingTooShort = hasRecording && recordedSeconds < MIN_RECORDING_SECONDS

    if (permissionDenied && !micPermissionGranted) {
        AlertDialog(
            onDismissRequest = { cancel() },
            shape = TamixaDialogDefaults.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(Strings.recordYourVoiceNow(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(Strings.micPermissionRequired(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    // iOS asks only once; after a denial the microphone can only be allowed in the Settings app.
                    Text(Strings.micPermissionSettingsHint(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { requestMicPermission() }) { Text(Strings.retry()) }
                }
            },
            confirmButton = { TextButton(onClick = { openAppSettings() }) { Text(Strings.openSettings()) } },
            dismissButton = { TextButton(onClick = { cancel() }) { Text(Strings.close()) } }
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
                    Text(Strings.recordingStoppedAtLimit(MAX_RECORDING_SECONDS), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                }
                if (recordingTooShort) {
                    Text(Strings.recordingTooShort(MIN_RECORDING_SECONDS), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
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
                            modifier = Modifier.padding(24.dp).size(48.dp),
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
                        TextButton(onClick = { startRecording() }) { Text(Strings.recordAgain()) }
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
                        Text(Strings.tapToStopRecording(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (startFailed && !isRecording) {
                        Spacer(Modifier.height(8.dp))
                        Text(Strings.recordingStartFailed(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        onClick = { if (isRecording) stopRecording() else startRecording() },
                        shape = CircleShape,
                        color = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) Strings.tapToStopRecording() else Strings.tapToStartRecording(),
                            modifier = Modifier.padding(24.dp).size(48.dp),
                            tint = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (!isRecording) {
                        Spacer(Modifier.height(16.dp))
                        Text(Strings.voiceCloneTips(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                TextButton(onClick = { saveRecording() }, enabled = !isSaving && !recordingTooShort) { Text(Strings.save()) }
            } else {
                TextButton(onClick = { cancel() }) { Text(Strings.cancel()) }
            }
        },
        dismissButton = if (hasRecording) {
            { TextButton(onClick = { cancel() }, enabled = !isSaving) { Text(Strings.cancel()) } }
        } else null
    )
}
