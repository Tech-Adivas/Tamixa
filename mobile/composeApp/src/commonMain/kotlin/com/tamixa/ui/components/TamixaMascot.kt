package com.tamixa.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.ic_tamixa_mascot
import org.jetbrains.compose.resources.painterResource

/**
 * Tamixa app mascot: golden star holding a glowing storybook.
 * Used as app guide character in onboarding, empty states, and helper UI.
 */
@Composable
fun TamixaMascot(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Image(
        painter = painterResource(Res.drawable.ic_tamixa_mascot),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
