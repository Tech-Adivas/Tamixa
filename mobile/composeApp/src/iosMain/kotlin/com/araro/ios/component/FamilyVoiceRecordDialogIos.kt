package com.araro.ios.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.araro.ui.theme.AraroDialogDefaults

/**
 * Placeholder for family voice recording on iOS.
 * Full AVAudioRecorder implementation requires additional cinterop setup.
 */
@Composable
fun FamilyVoiceRecordDialogIos(
    onDismiss: () -> Unit,
    onRecordingComplete: (ByteArray) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AraroDialogDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                com.araro.ui.strings.Strings.recordYourVoiceNow(),
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
                    com.araro.ui.strings.Strings.voiceRecordingComingSoonIos(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(com.araro.ui.strings.Strings.cancel())
            }
        }
    )
}
