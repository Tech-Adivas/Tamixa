package com.araro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Araro logo: "ARAR" + ripple O (brand SVG).
 * Language param kept for API compatibility; logo is always the official mark.
 */
@Composable
fun AraroLanguageLogo(
    languageCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    alignment: Alignment = Alignment.Center
) {
    Box(modifier = modifier, contentAlignment = alignment) {
        AraroLogo(size = size, alignment = alignment)
    }
}
