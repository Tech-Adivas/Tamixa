package com.tamixa.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tamixa.ui.components.TamixaLanguageLogo
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.strings.Strings

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
    var selectedCode by remember { mutableStateOf<String?>(com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE) }
    val effectiveCode = selectedCode ?: com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE
    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenBackground(showClouds = false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TamixaDesignTokens.screenPadding)
        ) {
            Spacer(Modifier.height(8.dp))
            // Header strip: title + tagline
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = Strings.chooseLanguage(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TamixaColors.cream
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Strings.storiesInPreferredLanguage(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TamixaColors.lavenderGlow.copy(alpha = 0.95f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Strings.appTagline(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TamixaColors.goldAccent.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
            // Logo in soft container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(
                            elevation = TamixaDesignTokens.cardElevationHover,
                            shape = RoundedCornerShape(80.dp),
                            ambientColor = TamixaColors.purple.copy(alpha = 0.2f),
                            spotColor = TamixaColors.purple.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(80.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    TamixaLanguageLogo(
                        languageCode = effectiveCode,
                        modifier = Modifier.size(140.dp),
                        size = 140.dp
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
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
            TamixaPrimaryButton(
                onClick = { selectedCode?.let { onLanguageSelected(it) } },
                text = Strings.confirmLanguage(),
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCode != null
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LanguageRow(
    option: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = tween(120), label = "scale"
    )
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                elevation = if (isSelected) TamixaDesignTokens.cardElevationHover else TamixaDesignTokens.cardElevation,
                shape = shape,
                ambientColor = if (isSelected) TamixaColors.purple.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f),
                spotColor = if (isSelected) TamixaColors.purple.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f)
            )
            .scale(scale)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Select ${option.label}" },
        shape = shape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        },
        border = if (isSelected) {
            BorderStroke(2.dp, TamixaColors.purple.copy(alpha = 0.7f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) TamixaColors.purple.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.flag,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.size(16.dp))
            Text(
                text = option.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = Strings.selected(),
                    tint = TamixaColors.goldAccent,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = Strings.select(),
                    tint = TamixaColors.lavenderGlow.copy(alpha = 0.75f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionScreenPreview() {
    com.tamixa.ui.theme.TamixaTheme {
        LanguageSelectionScreen(onLanguageSelected = {})
    }
}
