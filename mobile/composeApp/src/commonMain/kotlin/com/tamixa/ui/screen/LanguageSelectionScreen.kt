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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaLanguageLogo
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

data class LanguageOption(
    val code: String,
    /** Primary label in the language itself (or English for English). */
    val labelNative: String,
    val flag: String,
    /** Latin / English disambiguator under native script (helps parents scan the grid). */
    val labelLatin: String
)

private val LANGUAGES = listOf(
    LanguageOption("en", "English", "🇬🇧", "English"),
    LanguageOption("ta", "தமிழ்", "🏳️", "Tamil"),
    LanguageOption("hi", "हिन्दी", "🇮🇳", "Hindi"),
    LanguageOption("te", "తెలుగు", "🏳️", "Telugu"),
    LanguageOption("kn", "ಕನ್ನಡ", "🏳️", "Kannada"),
    LanguageOption("ml", "മലയാളം", "🏳️", "Malayalam")
)

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: (String) -> Unit
) {
    var selectedCode by remember { mutableStateOf<String?>(com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE) }
    val effectiveCode = selectedCode ?: com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE
    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = TamixaDesignTokens.screenPadding)
        ) {
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
            OnboardingEduStoryBadge(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing + 4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                color = Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, TamixaColors.deepTeal.copy(alpha = 0.32f)),
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
                        color = OnboardingCardColors.onboardingHeadline,
                    )
                    Spacer(Modifier.height(TamixaDesignTokens.smallSpacing - 4.dp))
                    Text(
                        text = Strings.languageSelectionSubtitle(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = OnboardingCardColors.onboardingSubline,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = Strings.appTagline(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.4.sp,
                        ),
                        color = TamixaColors.eduStoryMint.copy(alpha = 0.92f),
                    )
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing + 2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val haloShape = RoundedCornerShape(100.dp)
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .shadow(
                                elevation = TamixaDesignTokens.cardElevation,
                                shape = haloShape,
                                ambientColor = Color.Black.copy(alpha = 0.2f),
                                spotColor = TamixaColors.deepTeal.copy(alpha = 0.22f),
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
                            modifier = Modifier.size(92.dp),
                            size = 92.dp
                        )
                    }
                    Text(
                        text = "Tamixa",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = TamixaColors.cream,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing),
            ) {
                items(LANGUAGES, key = { it.code }) { option ->
                    LanguageGridTile(
                        option = option,
                        isSelected = selectedCode == option.code,
                        onClick = { selectedCode = option.code }
                    )
                }
            }
            Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, TamixaColors.deepTeal.copy(alpha = 0.22f)),
            ) {
                Text(
                    text = Strings.storiesInPreferredLanguage(),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = TamixaColors.cream.copy(alpha = 0.72f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center,
                )
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
private fun LanguageGridTile(
    option: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = tween(120), label = "langTileScale"
    )
    val shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                elevation = if (isSelected) TamixaDesignTokens.carouselCardShadowElevation else TamixaDesignTokens.listCardShadowElevation,
                shape = shape,
                ambientColor = TamixaDesignTokens.listCardShadowAmbient,
                spotColor = if (isSelected) {
                    TamixaColors.deepTeal.copy(alpha = 0.24f)
                } else {
                    TamixaColors.deepTeal.copy(alpha = 0.07f)
                },
            )
            .scale(scale)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Select ${option.labelLatin}" },
        shape = shape,
        color = if (isSelected) {
            TamixaColors.deepTeal.copy(alpha = 0.2f)
        } else {
            Color.White.copy(alpha = 0.06f)
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TamixaColors.eduStoryMint.copy(alpha = 0.75f)
            else TamixaColors.deepTeal.copy(alpha = 0.28f)
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = option.flag,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = option.labelNative,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = (-0.1).sp,
                        lineHeight = 20.sp,
                    ),
                    color = if (isSelected) TamixaColors.cream else TamixaColors.cream.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (option.labelNative != option.labelLatin) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = option.labelLatin,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp,
                        ),
                        color = TamixaColors.eduStoryMint.copy(alpha = if (isSelected) 0.95f else 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = Strings.selected(),
                    tint = TamixaColors.eduStoryMint,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
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
