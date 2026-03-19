package com.tamixa.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_home_hero
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.OnboardingCardDefaults
import com.tamixa.ui.theme.TamixaColors

@Composable
fun OnboardingHomePreviewScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingShell(step = 4, totalSteps = 4, modifier = modifier) {
        Text(
            text = Strings.yourHomeScreen(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = OnboardingCardColors.onboardingHeadline,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onboardingEntrance(delayMs = 120)
        )
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.onboardingEntrance(delayMs = 200)) {
            HomePreviewCard()
        }
        Spacer(Modifier.height(16.dp))
        OnboardingDailyQuote(step = 4, modifier = Modifier.onboardingEntrance(delayMs = 220))
        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.weight(1f))
        TamixaPrimaryButton(
            onClick = onContinue,
            text = Strings.continueLabel(),
            modifier = Modifier
                .fillMaxWidth()
                .onboardingEntrance(delayMs = 280)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomePreviewCard() {
    val transition = rememberInfiniteTransition(label = "homePreviewPulse")
    val cardPulse by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homePreviewCardPulse"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = cardPulse, scaleY = cardPulse)
            .shadow(
                elevation = OnboardingCardDefaults.cardShadowElevation,
                shape = OnboardingCardDefaults.cardShape,
                ambientColor = OnboardingCardDefaults.cardShadowAmbient,
                spotColor = OnboardingCardDefaults.cardShadowSpot
            ),
        shape = OnboardingCardDefaults.cardShape,
        color = OnboardingCardColors.cardBackground,
        border = BorderStroke(1.dp, OnboardingCardColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                OnboardingImageBanner(
                    image = Res.drawable.onboarding_home_hero,
                    contentDescription = "Home story preview",
                    height = 140.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hello, Achappan",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnboardingCardColors.onCardText
                    )
                }
            }
            PreviewRow(Strings.continueListening(), TamixaColors.goldAccent)
            PreviewRow(Strings.recommendedForYou(), TamixaColors.deepTeal)
            PreviewRow(Strings.popular(), TamixaColors.goldAccent)
            PreviewRow(Strings.categories(), TamixaColors.mintGreen)
        }
    }
}

@Composable
private fun PreviewRow(label: String, accent: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnboardingCardColors.pillBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = OnboardingCardColors.onCardText
            )
        }
    }
}
