package com.tamixa.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_voice_hero
import com.tamixa.composeapp.generated.resources.onboarding_voice_hero_animated
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaSkipButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.OnboardingCardDefaults
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Optional onboarding step: introduce family voice cloning.
 * Per MVP blueprint – Record / Skip. Recording requires auth, so this is a value prop + Skip.
 */
@Composable
fun OnboardingVoiceInvitationScreen(
    onRecordVoice: () -> Unit,
    onSkip: () -> Unit,
    onSwipeToNext: (() -> Unit)? = null,
    onSwipeToPrevious: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OnboardingShell(
        step = 3,
        totalSteps = 4,
        modifier = modifier,
        onSwipeToNext = onSwipeToNext,
        onSwipeToPrevious = onSwipeToPrevious
    ) {
        Text(
            text = Strings.onboardingVoiceHeadline(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = OnboardingCardColors.onboardingHeadline,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onboardingEntrance(delayMs = 80)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = Strings.onboardingVoiceSubline(),
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 26.sp,
                letterSpacing = 0.2.sp
            ),
            color = OnboardingCardColors.onboardingSubline,
            textAlign = TextAlign.Center,
            maxLines = 3,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onboardingEntrance(delayMs = 140)
        )
        Spacer(Modifier.height(28.dp))
        Box(modifier = Modifier.onboardingEntrance(delayMs = 200)) {
            VoiceInvitationPreviewCard()
        }
        Spacer(Modifier.height(16.dp))
        OnboardingDailyQuote(step = 3, modifier = Modifier.onboardingEntrance(delayMs = 220))
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.weight(1f))
        TamixaPrimaryButton(
            onClick = onRecordVoice,
            text = Strings.continueLabel(),
            modifier = Modifier
                .fillMaxWidth()
                .onboardingEntrance(delayMs = 280)
        )
        Spacer(Modifier.height(12.dp))
        TamixaSkipButton(
            onClick = onSkip,
            text = Strings.skip(),
            modifier = Modifier
                .fillMaxWidth()
                .onboardingEntrance(delayMs = 320)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VoiceInvitationPreviewCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
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
            modifier = Modifier.padding(TamixaDesignTokens.cardSpacing),
            verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                OnboardingImageBanner(
                    image = Res.drawable.onboarding_voice_hero,
                    animatedGif = Res.drawable.onboarding_voice_hero_animated,
                    contentDescription = "Voice recording",
                    height = 176.dp
                )
            }
            Text(
                text = Strings.onboardingVoiceCardLabel(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                color = OnboardingCardColors.onCardText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
