package com.tamixa.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaDialogDefaults
import kotlinx.coroutines.launch

/**
 * Asks for voice cloning consent before the first recording or upload (the backend refuses voice uploads without it).
 * Returns a wrapper: `gate { openRecorder() }` runs the action right away once [hasConsent], otherwise shows the
 * consent dialog and runs it after [onGiveConsent] succeeds.
 */
@Composable
fun rememberVoiceConsentGate(
    hasConsent: Boolean,
    onGiveConsent: suspend () -> Boolean
): (() -> Unit) -> Unit {
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) pendingAction = null },
            shape = TamixaDialogDefaults.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(Strings.voiceConsentTitle(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(Strings.voiceConsentMessage(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                if (onGiveConsent()) {
                                    pendingAction = null
                                    action()
                                }
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                ) {
                    Text(Strings.voiceConsentAgree())
                }
            },
            dismissButton = {
                TextButton(enabled = !isSaving, onClick = { pendingAction = null }) {
                    Text(Strings.cancel())
                }
            }
        )
    }

    return { action -> if (hasConsent) action() else pendingAction = action }
}
