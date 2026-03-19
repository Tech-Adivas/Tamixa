package com.tamixa.android.component

import android.media.MediaRecorder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaDialogDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "FamilyVoiceRecord"

@Composable
fun FamilyVoiceRecordDialog(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions["android.permission.RECORD_AUDIO"] == true
        if (!granted) onDismiss()
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
    }

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

    fun startRecording() {
        try {
            val cacheDir = context.cacheDir
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "family_voice_$timestamp.m4a")
            recordFile = file

            val recorder = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            Log.e(TAG, "Start recording failed", e)
            onDismiss()
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

            val file = recordFile ?: return
            if (file.exists() && file.length() > 0) {
                scope.launch {
                    recordedBytes = withContext(Dispatchers.IO) { file.readBytes() }
                    file.delete()
                    recordFile = null
                }
            } else {
                file.delete()
                recordFile = null
                onDismiss()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop recording failed", e)
            mediaRecorder = null
            recordFile?.delete()
            recordFile = null
            onDismiss()
        }
    }

    fun useThisVoice() {
        recordedBytes?.let { bytes ->
            onRecordingComplete(bytes)
            onDismiss()
        }
    }

    fun cancelRecording() {
        recordedBytes = null
        recordFile?.delete()
        recordFile = null
        onDismiss()
    }

    val bytes = recordedBytes
    val showConfirmStep = bytes != null

    AlertDialog(
        onDismissRequest = { if (showConfirmStep) cancelRecording() else onDismiss() },
        shape = TamixaDialogDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                when {
                    showConfirmStep -> com.tamixa.ui.strings.Strings.useThisRecording()
                    else -> com.tamixa.ui.strings.Strings.recordYourVoiceNow()
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    showConfirmStep -> {
                        Text(
                            com.tamixa.ui.strings.Strings.useThisVoiceForStory(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            if (isRecording) com.tamixa.ui.strings.Strings.tapToStopRecording()
                            else com.tamixa.ui.strings.Strings.tapToStartRecording(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Surface(
                            onClick = { if (isRecording) stopRecording() else startRecording() },
                            shape = CircleShape,
                            color = if (isRecording) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(24.dp)
                                    .size(48.dp),
                                tint = if (isRecording) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showConfirmStep) {
                TextButton(onClick = { useThisVoice() }) {
                    Text(com.tamixa.ui.strings.Strings.useThisVoice())
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(com.tamixa.ui.strings.Strings.cancel())
                }
            }
        },
        dismissButton = if (showConfirmStep) {
            {
                TextButton(onClick = { cancelRecording() }) {
                    Text(com.tamixa.ui.strings.Strings.cancel())
                }
            }
        } else null
    )
}
