package com.tamixa.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Loading overlay with magical particles for story transitions and UI effects.
 * Use during story loading, transitions, or any magical moment.
 *
 * @param message Optional message shown below the spinner (e.g. "Preparing audio...").
 * @param variant DEFAULT or TAMIL_NADU (South Indian aesthetic).
 */
@Composable
fun MagicLoadingOverlay(
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    message: String? = null,
    variant: MagicParticlesVariant = MagicParticlesVariant.DEFAULT
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TamixaMagicParticles(
            modifier = Modifier.fillMaxSize(),
            animate = true,
            density = 0.8f,
            variant = variant
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = progressColor,
                strokeWidth = 3.dp
            )
            if (message != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
