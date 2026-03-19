package com.tamixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

/**
 * Primary CTA button with Indigo → Purple gradient (Tamixa design system).
 * Uses clickable Box instead of Material3 Button to avoid focus indicator when
 * text fields are focused on login, OTP, and confirm language screens.
 */
@Composable
fun TamixaPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) { role = Role.Button }
            .height(56.dp)
            .shadow(
                elevation = TamixaDesignTokens.buttonShadowElevation,
                shape = shape,
                ambientColor = com.tamixa.ui.theme.TamixaColors.goldAccent.copy(alpha = 0.35f),
                spotColor = com.tamixa.ui.theme.TamixaColors.goldAccent.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(
                brush = TamixaGradients.primaryButtonBrush(),
                shape = shape
            )
            .then(
                if (enabled && !loading) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = true,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled && !loading) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                }
            )
        }
    }
}
