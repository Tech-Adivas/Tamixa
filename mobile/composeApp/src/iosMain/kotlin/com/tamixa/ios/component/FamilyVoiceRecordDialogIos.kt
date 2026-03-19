package com.tamixa.ios.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaDialogDefaults

/**
 * Family voice recording on iOS.
 * UI matches Android (record prompt + mic button). Actual recording requires
 * AVAudioRecorder via Swift bridge or custom cinterop (see docs/IOS_IMPROVEMENTS_ANALYSIS.md).
 */
@Composable
fun FamilyVoiceRecordDialogIos(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = TamixaDialogDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                com.tamixa.ui.strings.Strings.recordYourVoiceNow(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    com.tamixa.ui.strings.Strings.voiceRecordingComingSoonIos(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Surface(
                    onClick = { /* Recording coming soon */ },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(com.tamixa.ui.strings.Strings.cancel())
            }
        }
    )
}
