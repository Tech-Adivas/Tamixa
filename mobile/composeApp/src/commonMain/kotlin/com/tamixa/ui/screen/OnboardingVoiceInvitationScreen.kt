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
        step = 4,
        totalSteps = 5,
        modifier = modifier,
        onSwipeToNext = onSwipeToNext,
        onSwipeToPrevious = onSwipeToPrevious,
        footer = {
            val spec = LocalOnboardingLayoutSpec.current
            TamixaPrimaryButton(
                onClick = onRecordVoice,
                text = Strings.onboardingAddMyVoice(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onboardingEntrance(delayMs = 280)
            )
            Spacer(Modifier.height(spec.footerButtonGap))
            TamixaSkipButton(
                onClick = onSkip,
                text = Strings.skip(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onboardingEntrance(delayMs = 320)
            )
        }
    ) {
        val spec = LocalOnboardingLayoutSpec.current
        
        // Large hero visual
        OnboardingHeroSpotlight(
            kind = OnboardingHeroHaloKind.VoiceRipple,
            haloHeight = 320.dp,
            modifier = Modifier.onboardingEntrance(delayMs = 55)
        ) {
            VoiceInvitationPreviewCard()
        }
        
        Spacer(Modifier.height(spec.gapLg))
        
        // Simple headline only
        OnboardingEditorialTextCard(
            modifier = Modifier.onboardingEntrance(delayMs = 115)
        ) {
            OnboardingHeadline(
                text = Strings.onboardingVoiceHeadline(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VoiceInvitationPreviewCard() {
    val spec = LocalOnboardingLayoutSpec.current
    val imgH = spec.scaledImageHeight(200.dp)
    val imgCorner = spec.scaledCorner(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = OnboardingCardDefaults.cardShadowElevation,
                shape = OnboardingCardDefaults.cardShape,
                ambientColor = OnboardingCardDefaults.cardShadowAmbient,
                spotColor = TamixaColors.deepTeal.copy(alpha = 0.2f)
            ),
        shape = OnboardingCardDefaults.cardShape,
        color = OnboardingCardColors.cardBackground,
        border = BorderStroke(1.dp, TamixaColors.deepTeal.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(spec.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(spec.cardInnerSpacing)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imgH)
                    .clip(RoundedCornerShape(imgCorner))
            ) {
                OnboardingImageBanner(
                    image = Res.drawable.onboarding_voice_hero,
                    animatedGif = Res.drawable.onboarding_voice_hero_animated,
                    contentDescription = "Voice recording",
                    height = imgH,
                    cornerRadius = imgCorner
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
