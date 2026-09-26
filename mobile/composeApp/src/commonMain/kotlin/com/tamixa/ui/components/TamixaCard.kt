package com.tamixa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Tamixa-styled card component with consistent elevation, radius, and content color.
 * Provides variants for different use cases (surface, primary, secondary).
 */
@Composable
fun TamixaCard(
    modifier: Modifier = Modifier,
    colors: CardColors = TamixaCardDefaults.surfaceColors(),
    elevation: Dp = TamixaDesignTokens.cardElevation,
    radius: Dp = TamixaDesignTokens.cardRadius,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(radius),
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border
        ) {
            Column(
                modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(radius),
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border
        ) {
            Column(
                modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)
            ) {
                content()
            }
        }
    }
}

/**
 * Default color schemes for TamixaCard variants.
 */
object TamixaCardDefaults {
    
    /**
     * Default surface card colors (most common use case).
     * Uses surface container color with onSurface text.
     */
    @Composable
    fun surfaceColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    
    /**
     * Primary container card colors (for emphasized content).
     * Uses primary container with onPrimaryContainer text.
     */
    @Composable
    fun primaryContainerColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    
    /**
     * Secondary container card colors (for supporting content).
     * Uses secondary container with onSecondaryContainer text.
     */
    @Composable
    fun secondaryContainerColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
    
    /**
     * Surface variant card colors (for subtle differentiation).
     * Uses surface variant with onSurfaceVariant text.
     */
    @Composable
    fun surfaceVariantColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
