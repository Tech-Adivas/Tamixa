package com.tamixa.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.TamixaColors

data class ThemeOption(val id: String, val label: () -> String)

private val ONBOARDING_THEMES = listOf(
    ThemeOption("Animals") { Strings.themeAnimals() },
    ThemeOption("Adventure") { Strings.themeAdventure() },
    ThemeOption("Friendship") { Strings.themeFriendship() },
    ThemeOption("Village Life") { Strings.themeVillageLife() },
    ThemeOption("Funny") { Strings.themeFunny() }
)

/** Figma wireframe category colors: warm orange, sky blue, soft purple, mint green */
private fun themeAccentColor(themeId: String) = when (themeId) {
    "Animals" -> TamixaColors.goldAccent
    "Adventure" -> TamixaColors.deepTeal
    "Friendship" -> TamixaColors.goldAccent
    "Village Life" -> TamixaColors.mintGreen
    else -> TamixaColors.goldAccent
}

private fun themeEmoji(themeId: String) = when (themeId) {
    "Animals" -> "\uD83D\uDC3B"
    "Adventure" -> "\uD83D\uDDFA\uFE0F"
    "Friendship" -> "\uD83E\uDD1D"
    "Village Life" -> "\uD83C\uDFE1"
    else -> "\uD83D\uDE04"
}

@Composable
fun OnboardingInterestsScreen(
    onContinue: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    OnboardingShell(step = 5, totalSteps = 6, modifier = modifier) {
        Text(
            text = Strings.whatStoriesDoYouLike(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = OnboardingCardColors.onboardingHeadline,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onboardingEntrance(delayMs = 120)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = Strings.select2to3Themes(),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                letterSpacing = 0.2.sp
            ),
            color = OnboardingCardColors.onboardingSubline,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onboardingEntrance(delayMs = 180)
        )
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.onboardingEntrance(delayMs = 220)) {
            InterestsHeroCard(selectedCount = selected.size)
        }
        Spacer(Modifier.height(16.dp))
        OnboardingDailyQuote(step = 5, modifier = Modifier.onboardingEntrance(delayMs = 240))
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(ONBOARDING_THEMES, key = { it.id }) { option ->
                val isSelected = selected.contains(option.id)
                val accent = themeAccentColor(option.id)
                Surface(
                    modifier = Modifier
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            selected = if (isSelected) {
                                selected - option.id
                            } else if (selected.size < 3) {
                                selected + option.id
                            } else {
                                selected
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        accent.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    },
                    border = if (isSelected) {
                        BorderStroke(2.dp, accent.copy(alpha = 0.8f))
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = themeEmoji(option.id),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = option.label(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        TamixaPrimaryButton(
            onClick = { onContinue(selected.toList()) },
            text = Strings.continueLabel(),
            modifier = Modifier
                .fillMaxWidth()
                .onboardingEntrance(delayMs = 300),
            enabled = selected.size in 2..3
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InterestsHeroCard(selectedCount: Int) {
    val transition = rememberInfiniteTransition(label = "interestsHeroPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "interestsHeroPulseValue"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .graphicsLayer(scaleX = pulse, scaleY = pulse),
        shape = RoundedCornerShape(20.dp),
        color = OnboardingCardColors.cardBackground,
        border = BorderStroke(1.dp, OnboardingCardColors.cardBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = TamixaColors.goldAccent.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, TamixaColors.goldAccent.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "${selectedCount}/3",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = OnboardingCardColors.onCardText,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OnboardingHintPill(Strings.themeAnimals())
                OnboardingHintPill(Strings.themeAdventure())
                OnboardingHintPill(Strings.themeFriendship())
            }
        }
    }
}

@Composable
private fun OnboardingHintPill(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnboardingCardColors.pillBackground,
        border = BorderStroke(1.dp, OnboardingCardColors.cardBorder.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ),
            color = OnboardingCardColors.onCardText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
