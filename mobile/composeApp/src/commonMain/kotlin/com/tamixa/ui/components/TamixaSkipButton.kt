package com.tamixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Secondary / skip CTA: theme surface, terracotta border, soft elevation (M3 outlined style).
 */
@Composable
fun TamixaSkipButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 2.dp,
                shape = shape,
                ambientColor = TamixaDesignTokens.listCardShadowAmbient,
                spotColor = TamixaDesignTokens.listCardShadowSpot
            )
            .clip(shape)
            .background(scheme.surfaceContainerLow)
            .border(1.5.dp, TamixaColors.goldAccent.copy(alpha = 0.9f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = TamixaColors.goldAccent
        )
    }
}
