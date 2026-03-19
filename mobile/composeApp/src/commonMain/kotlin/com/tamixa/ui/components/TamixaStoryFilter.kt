package com.tamixa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.tamixa.ui.theme.TamixaDesignTokens
/**
 * Tamixa filter: minimal segmented control with underline for selected.
 * Distinct from OHO-style horizontal scroll chips.
 */
@Composable
fun TamixaStoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TamixaDesignTokens.inputRadius))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .horizontalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { category ->
            val selected = category == selectedCategory
            TamixaFilterChip(
                label = category,
                selected = selected,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

/** Storybook Dusk theme: category pills use consistent palette (terracotta, teal, mint) */
private fun categoryAccentColor(label: String): androidx.compose.ui.graphics.Color = when (label.lowercase()) {
    "animals" -> com.tamixa.ui.theme.TamixaColors.goldAccent
    "friendship" -> com.tamixa.ui.theme.TamixaColors.terracotta
    "adventure" -> com.tamixa.ui.theme.TamixaColors.deepTeal
    "village life" -> com.tamixa.ui.theme.TamixaColors.mintGreen
    "moral stories" -> com.tamixa.ui.theme.TamixaColors.mintGreen
    "funny stories" -> com.tamixa.ui.theme.TamixaColors.goldAccent
    "family stories" -> com.tamixa.ui.theme.TamixaColors.terracotta
    "fantasy" -> com.tamixa.ui.theme.TamixaColors.deepTeal
    "nature" -> com.tamixa.ui.theme.TamixaColors.mintGreen
    "bravery" -> com.tamixa.ui.theme.TamixaColors.goldAccent
    "all" -> com.tamixa.ui.theme.TamixaColors.goldAccent
    else -> com.tamixa.ui.theme.TamixaColors.goldAccent
}

@Composable
private fun TamixaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = categoryAccentColor(label)
    val themeBg = com.tamixa.ui.theme.TamixaColors.goldAccent.copy(alpha = 0.08f)
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.28f)
        else themeBg,
        animationSpec = tween(200),
        label = "filterChipColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "filterChipContentColor"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
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
