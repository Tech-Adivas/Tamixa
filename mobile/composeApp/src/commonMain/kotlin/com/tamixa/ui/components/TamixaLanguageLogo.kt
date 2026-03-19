package com.tamixa.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamixa.ui.components.TamixaFullLogo

/**
 * Tamixa logo (logo 5) for dashboard, language selection, settings.
 */
@Composable
fun TamixaLanguageLogo(
    languageCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    alignment: Alignment = Alignment.Center
) {
    Box(modifier = modifier, contentAlignment = alignment) {
        TamixaFullLogo(size = size)
    }
}
