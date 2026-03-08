package com.araro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.araro.ui.theme.AraroDesignTokens
/**
 * Araro filter: minimal segmented control with underline for selected.
 * Distinct from OHO-style horizontal scroll chips.
 */
@Composable
fun AraroStoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AraroDesignTokens.inputRadius))
            .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEach { category ->
            val selected = category == selectedCategory
            AraroFilterChip(
                label = category,
                selected = selected,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

@Composable
private fun AraroFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) com.araro.ui.theme.AraroColors.goldAccent
        else colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
        animationSpec = tween(200),
        label = "filterChipColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onPrimary
        else colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "filterChipContentColor"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(AraroDesignTokens.inputRadius),
        color = containerColor,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
