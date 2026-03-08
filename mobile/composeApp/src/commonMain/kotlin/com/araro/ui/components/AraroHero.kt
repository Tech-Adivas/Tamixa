package com.araro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design spec: Child sleeping under moonlight illustration for Welcome screen.
 * Calm, emotionally safe, premium bedtime aesthetic.
 */
@Composable
fun AraroHeroIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    Box(
        modifier = modifier.size(size * 1.6f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌙😴",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = (size.value * 0.52f).sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** HD-quality friendly emoji for empty states and hero sections. */
@Composable
fun AraroEmojiDisplay(
    emoji: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 64.sp
) {
    Text(
        text = emoji,
        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize),
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}
