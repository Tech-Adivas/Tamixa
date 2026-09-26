package com.tamixa.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

/**
 * Primary button with gradient background (terracotta → coral).
 * Use for main CTAs and important actions.
 */
@Composable
fun TamixaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = TamixaColors.terracotta,
            contentColor = Color.White,
            disabledContainerColor = TamixaColors.terracotta.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = TamixaDesignTokens.buttonShadowElevation,
            pressedElevation = TamixaDesignTokens.buttonShadowElevation / 2
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                icon()
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Secondary button with solid deep teal background.
 * Use for secondary actions and supporting CTAs.
 */
@Composable
fun TamixaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = TamixaColors.deepTeal,
            contentColor = Color.White,
            disabledContainerColor = TamixaColors.deepTeal.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            icon()
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Outline button with transparent background and teal border.
 * Use for tertiary actions and less prominent CTAs.
 */
@Composable
fun TamixaOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.buttonRadius),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TamixaColors.deepTeal,
            disabledContentColor = TamixaColors.deepTeal.copy(alpha = 0.38f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (enabled) TamixaColors.deepTeal else TamixaColors.deepTeal.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            icon()
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Ghost button with no background or border.
 * Use for minimal actions and inline links.
 */
@Composable
fun TamixaGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.buttonRadius),
        colors = ButtonDefaults.textButtonColors(
            contentColor = TamixaColors.deepTeal,
            disabledContentColor = TamixaColors.deepTeal.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            icon()
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
