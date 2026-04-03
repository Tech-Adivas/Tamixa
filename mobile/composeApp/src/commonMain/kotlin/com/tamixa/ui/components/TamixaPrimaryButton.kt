package com.tamixa.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

/**
 * Primary CTA: warm gradient ([TamixaGradients.primaryButtonBrush]).
 * Idle pulse + press shrink only when system motion is allowed; otherwise a **fixed** deeper shadow so the
 * button still feels premium (no user settings changes required — works with “Remove animations” / reduce motion).
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
    val pressed by interactionSource.collectIsPressedAsState()
    val allowMotion = !platformIsReduceMotionEnabled()
    val motionPulseOk = enabled && !loading && allowMotion
    val breath = rememberPrimaryBreathScale(motionPulseOk)
    val shadowPulse = rememberPrimaryShadowPulse(motionPulseOk)
    val idleScale = if (motionPulseOk) breath else 1f
    val pressScale = if (pressed && enabled && !loading) 0.98f else 1f
    val combinedScale = idleScale * pressScale
    val shadowMul = when {
        !enabled || loading -> 1f
        !allowMotion -> 1.16f
        else -> shadowPulse
    }
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) { role = Role.Button }
            .graphicsLayer {
                scaleX = combinedScale
                scaleY = combinedScale
            }
            .height(56.dp)
            .shadow(
                elevation = TamixaDesignTokens.buttonShadowElevation * shadowMul,
                shape = shape,
                ambientColor = Color(0xFF2C2520).copy(alpha = 0.22f * shadowMul),
                spotColor = com.tamixa.ui.theme.TamixaColors.goldAccent.copy(alpha = 0.18f * shadowMul)
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

@Composable
private fun rememberPrimaryBreathScale(active: Boolean): Float {
    if (!active) return 1f
    val infinite = rememberInfiniteTransition(label = "primaryCtaBreath")
    val value by infinite.animateFloat(
        initialValue = 0.972f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    return value
}

@Composable
private fun rememberPrimaryShadowPulse(active: Boolean): Float {
    if (!active) return 1f
    val infinite = rememberInfiniteTransition(label = "primaryCtaShadow")
    val value by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadowPulse"
    )
    return value
}
