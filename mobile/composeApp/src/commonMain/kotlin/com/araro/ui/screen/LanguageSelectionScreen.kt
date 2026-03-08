package com.araro.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.araro.ui.components.AraroLanguageLogo
import com.araro.ui.components.AraroPrimaryButton
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.strings.Strings

data class LanguageOption(val code: String, val label: String, val flag: String)

private val LANGUAGES = listOf(
    LanguageOption("en", "English", "🇬🇧"),
    LanguageOption("ta", "தமிழ்", "🏳️"),
    LanguageOption("hi", "हिन्दी", "🇮🇳"),
    LanguageOption("te", "తెలుగు", "🏳️"),
    LanguageOption("kn", "ಕನ್ನಡ", "🏳️"),
    LanguageOption("ml", "മലയാളം", "🏳️")
)

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: (String) -> Unit
) {
    var selectedCode by remember { mutableStateOf<String?>(com.araro.util.AraroConstants.DEFAULT_LANGUAGE) }
    val effectiveCode = selectedCode ?: com.araro.util.AraroConstants.DEFAULT_LANGUAGE
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AraroDesignTokens.screenPadding)
            .background(colorScheme.background)
    ) {
        Spacer(Modifier.height(16.dp))
        AraroLanguageLogo(
            languageCode = effectiveCode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            size = 100.dp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = Strings.chooseLanguage(),
            style = MaterialTheme.typography.headlineMedium,
            color = colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = Strings.storiesInPreferredLanguage(),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
            Spacer(Modifier.height(20.dp))
            LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(LANGUAGES, key = { it.code }) { option ->
                LanguageRow(
                    option = option,
                    isSelected = selectedCode == option.code,
                    onClick = { selectedCode = option.code }
                )
            }
        }
            Spacer(Modifier.height(20.dp))
            AraroPrimaryButton(
                onClick = { selectedCode?.let { onLanguageSelected(it) } },
                text = Strings.confirmLanguage(),
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCode != null
            )
            Spacer(Modifier.height(24.dp))
        }
    }

@Composable
private fun LanguageRow(
    option: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = tween(100), label = "scale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(AraroDesignTokens.inputRadius))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Select ${option.label}" },
        shape = RoundedCornerShape(AraroDesignTokens.inputRadius),
        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f)
        else colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.flag,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = option.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionScreenPreview() {
    com.araro.ui.theme.AraroTheme {
        LanguageSelectionScreen(onLanguageSelected = {})
    }
}
