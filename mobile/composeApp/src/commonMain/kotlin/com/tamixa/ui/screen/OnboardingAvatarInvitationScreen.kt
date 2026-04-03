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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_avatar_hero
import com.tamixa.composeapp.generated.resources.onboarding_avatar_hero_animated
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaSkipButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.OnboardingCardDefaults

/**
 * Optional onboarding step: introduce talking avatar.
 * Per MVP blueprint – Upload Photo / Skip. Upload requires auth, so this is a value prop + Skip.
 */
@Composable
fun OnboardingAvatarInvitationScreen(
    onUploadPhoto: () -> Unit,
    onSkip: () -> Unit,
    onSwipeToNext: (() -> Unit)? = null,
    onSwipeToPrevious: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OnboardingShell(
        step = 4,
        totalSteps = 4,
        modifier = modifier,
        onSwipeToNext = onSwipeToNext,
        onSwipeToPrevious = onSwipeToPrevious,
        footer = {
            val spec = LocalOnboardingLayoutSpec.current
            TamixaPrimaryButton(
                onClick = onUploadPhoto,
                text = Strings.continueLabel(),
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
        OnboardingHeroSpotlight(
            kind = OnboardingHeroHaloKind.AvatarShimmer,
            haloHeight = 266.dp,
            modifier = Modifier.onboardingEntrance(delayMs = 55)
        ) {
            AvatarInvitationPreviewCard()
        }
        Spacer(Modifier.height(spec.gapLg))
        OnboardingEditorialTextCard(
            modifier = Modifier.onboardingEntrance(delayMs = 115)
        ) {
            OnboardingHeadline(
                text = Strings.onboardingAvatarHeadline(),
                modifier = Modifier.fillMaxWidth()
            )
            OnboardingSubline(
                text = Strings.onboardingAvatarSubline(),
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 26.sp
            )
        }
        Spacer(Modifier.height(spec.gapMd))
        OnboardingValueStripTitle(
            text = Strings.onboardingStripTitle(4),
            modifier = Modifier.onboardingEntrance(delayMs = 150)
        )
        Spacer(Modifier.height(spec.gapSm))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spec.benefitRowSpacing)
        ) {
            OnboardingBenefitRow(
                emoji = "🖼️",
                text = Strings.onboardingAvatarBenefitFace(),
                modifier = Modifier.onboardingEntrance(delayMs = 185)
            )
            OnboardingBenefitRow(
                emoji = "💛",
                text = Strings.onboardingAvatarBenefitTrust(),
                modifier = Modifier.onboardingEntrance(delayMs = 235)
            )
            OnboardingBenefitRow(
                emoji = "🚪",
                text = Strings.onboardingAvatarBenefitOptional(),
                modifier = Modifier.onboardingEntrance(delayMs = 285)
            )
        }
    }
}

@Composable
private fun AvatarInvitationPreviewCard() {
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
                spotColor = OnboardingCardDefaults.cardShadowSpot
            ),
        shape = OnboardingCardDefaults.cardShape,
        color = OnboardingCardColors.cardBackground,
        border = BorderStroke(1.dp, OnboardingCardColors.cardBorder)
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
                    image = Res.drawable.onboarding_avatar_hero,
                    animatedGif = Res.drawable.onboarding_avatar_hero_animated,
                    contentDescription = "Avatar",
                    height = imgH,
                    cornerRadius = imgCorner
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OnboardingCardColors.pillBackground,
                border = BorderStroke(1.dp, OnboardingCardColors.cardBorder.copy(alpha = 0.3f))
            ) {
                Text(
                    text = Strings.onboardingAvatarCardLabel(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    ),
                    color = OnboardingCardColors.onCardText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
