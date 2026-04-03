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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.ui.components.TamixaLanguageLogo
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients
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
        AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TamixaGradients.splashStorybookVignetteBrush())
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(TamixaDesignTokens.screenPadding)
        ) {
            // Glass header — clean editorial panel (Vocal-style chrome on starfield)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                color = Color.White.copy(alpha = 0.07f),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.2f),
                ),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                        vertical = TamixaDesignTokens.smallSpacing + 2.dp,
                    )
                ) {
                    Text(
                        text = Strings.chooseLanguage(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.35).sp,
                            lineHeight = 30.sp,
                        ),
                        color = TamixaColors.cream,
                    )
                    Spacer(Modifier.height(TamixaDesignTokens.smallSpacing - 4.dp))
                    Text(
                        text = Strings.storiesInPreferredLanguage(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = TamixaColors.cream.copy(alpha = 0.82f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Strings.appTagline(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.6.sp,
                        ),
                        color = TamixaColors.goldAccent.copy(alpha = 0.88f),
                    )
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing + 4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val haloShape = RoundedCornerShape(100.dp)
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .shadow(
                            elevation = TamixaDesignTokens.cardElevation,
                            shape = haloShape,
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = TamixaColors.goldAccent.copy(alpha = 0.12f),
                        )
                        .clip(haloShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.11f),
                                    Color.White.copy(alpha = 0.03f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    TamixaLanguageLogo(
                        languageCode = effectiveCode,
                        modifier = Modifier.size(124.dp),
                        size = 124.dp
                    )
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
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
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
            TamixaPrimaryButton(
                onClick = { selectedCode?.let { onLanguageSelected(it) } },
                text = Strings.confirmLanguage(),
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCode != null
            )
            Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
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
    val shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                elevation = if (isSelected) TamixaDesignTokens.cardElevationHover else TamixaDesignTokens.listCardShadowElevation,
                shape = shape,
                ambientColor = TamixaDesignTokens.listCardShadowAmbient,
                spotColor = if (isSelected) {
                    TamixaColors.goldAccent.copy(alpha = 0.18f)
                } else {
                    TamixaDesignTokens.listCardShadowSpot
                },
            )
            .scale(scale)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Select ${option.label}" },
        shape = shape,
        color = if (isSelected) {
            TamixaColors.goldAccent.copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = 0.06f)
        },
        border = if (isSelected) {
            BorderStroke(1.5.dp, TamixaColors.goldAccent.copy(alpha = 0.55f))
        } else {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        },
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                    vertical = 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(TamixaDesignTokens.inputRadius))
                    .background(
                        if (isSelected) {
                            TamixaColors.deepTeal.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.flag,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.size(TamixaDesignTokens.smallSpacing))
            Text(
                text = option.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = (-0.15).sp,
                ),
                color = if (isSelected) {
                    TamixaColors.cream
                } else {
                    TamixaColors.cream.copy(alpha = 0.78f)
                },
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = Strings.selected(),
                    tint = TamixaColors.goldAccent,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = Strings.select(),
                    tint = TamixaColors.cream.copy(alpha = 0.42f),
                    modifier = Modifier.size(22.dp)
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
