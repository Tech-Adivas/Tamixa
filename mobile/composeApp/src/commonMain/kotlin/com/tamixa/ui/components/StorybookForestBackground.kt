package com.tamixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val ForestTop = Color(0xFF2D4A3E)
private val ForestBottom = Color(0xFF1A3329)

/**
 * Soft forest-style gradient background for storytelling screens.
 * No image asset; works on all platforms.
 */
@Composable
fun StorybookForestBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ForestTop, ForestBottom),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}
