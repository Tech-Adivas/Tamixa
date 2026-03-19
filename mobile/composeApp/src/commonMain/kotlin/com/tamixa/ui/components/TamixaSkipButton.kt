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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Skip button per Tamixa design system: white background, warm orange border.
 * Use for optional onboarding steps (voice, avatar, etc.).
 */
@Composable
fun TamixaSkipButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, TamixaColors.goldAccent, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = TamixaColors.goldAccent
        )
    }
}
