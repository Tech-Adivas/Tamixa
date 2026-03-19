package com.tamixa.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Listening/empty-state illustration. Uses mascot for consistency across screens.
 */
@Composable
fun TamixaChildrenListeningIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    TamixaMascot(modifier = modifier.size(size), size = size)
}
