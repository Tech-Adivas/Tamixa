package com.tamixa.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_demo_hero
import com.tamixa.composeapp.generated.resources.onboarding_demo_hero_animated
import com.tamixa.platform.rememberLocalFileController
import com.tamixa.platform.synthesizeStoryToFile
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaSkipButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.OnboardingCardDefaults
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.util.TamixaConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DEMO_TEXT = "Once upon a time, in a quiet village, there lived a curious fox. " +
    "The fox loved to explore the forest and make new friends. " +
    "One day, he discovered a magical lantern that could light up the darkest night."

private val DEMO_SUBTITLE = "Once upon a time, in a quiet village, there lived a curious fox…"

@Composable
fun OnboardingDemoScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onSwipeToNext: (() -> Unit)? = null,
    onSwipeToPrevious: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var ttsUri by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        ttsUri = withContext(Dispatchers.Default) {
            synthesizeStoryToFile(DEMO_TEXT, TamixaConstants.DEFAULT_LANGUAGE)
        }
    }

    val controller = rememberLocalFileController(
        fileUri = ttsUri,
        storyTitle = "Demo",
        storyTheme = "Adventure",
        scope = scope,
        onProgressChanged = { progress = it }
    )

    OnboardingShell(
        step = 2,
        totalSteps = 4,
        modifier = modifier,
        onSwipeToNext = onSwipeToNext,
        onSwipeToPrevious = onSwipeToPrevious,
        footer = {
            val spec = LocalOnboardingLayoutSpec.current
            TamixaPrimaryButton(
                onClick = onContinue,
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
            kind = OnboardingHeroHaloKind.LanternGlow,
            haloHeight = 278.dp,
            modifier = Modifier.onboardingEntrance(delayMs = 55)
        ) {
            OnboardingDemoStoryCard(
                ttsUri = ttsUri,
                progress = progress,
                isPlaying = controller.isPlaying,
                onPlayPause = { controller.playPause() }
            )
        }
        Spacer(Modifier.height(spec.gapLg))
        OnboardingEditorialTextCard(
            modifier = Modifier.onboardingEntrance(delayMs = 115)
        ) {
            OnboardingHeadline(
                text = Strings.onboardingDemoHeadline(),
                modifier = Modifier.fillMaxWidth()
            )
            OnboardingSubline(
                text = Strings.onboardingDemoSubline(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(spec.gapMd))
        OnboardingValueStripTitle(
            text = Strings.onboardingStripTitle(2),
            modifier = Modifier.onboardingEntrance(delayMs = 150)
        )
        Spacer(Modifier.height(spec.gapSm))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spec.benefitRowSpacing)
        ) {
            OnboardingBenefitRow(
                emoji = "🦊",
                text = Strings.onboardingDemoBenefitHear(),
                modifier = Modifier.onboardingEntrance(delayMs = 185)
            )
            OnboardingBenefitRow(
                emoji = "⏯️",
                text = Strings.onboardingDemoBenefitControl(),
                modifier = Modifier.onboardingEntrance(delayMs = 235)
            )
            OnboardingBenefitRow(
                emoji = "👂",
                text = Strings.onboardingDemoBenefitYoung(),
                modifier = Modifier.onboardingEntrance(delayMs = 285)
            )
        }
    }
}

@Composable
private fun OnboardingDemoStoryCard(
    ttsUri: String?,
    progress: Float,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = LocalOnboardingLayoutSpec.current
    val loadH = spec.scaledImageHeight(148.dp)
    val imgH = spec.scaledImageHeight(188.dp)
    val imgCorner = spec.scaledCorner(22.dp)
    Surface(
        modifier = modifier
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
            when {
                ttsUri == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(loadH),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = TamixaColors.goldAccent,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = Strings.preparingDemo(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnboardingCardColors.onCardText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imgH)
                            .clip(RoundedCornerShape(imgCorner))
                    ) {
                        OnboardingImageBanner(
                            image = Res.drawable.onboarding_demo_hero,
                            animatedGif = Res.drawable.onboarding_demo_hero_animated,
                            contentDescription = "Story demo",
                            height = imgH,
                            cornerRadius = imgCorner
                        )
                    }
                    Text(
                        text = DEMO_SUBTITLE,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnboardingCardColors.onCardText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(spec.demoPlayButtonDp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.14f),
                                    spotColor = TamixaColors.goldAccent.copy(alpha = 0.28f)
                                )
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            TamixaColors.goldAccent,
                                            TamixaColors.goldAccent.copy(alpha = 0.82f)
                                        )
                                    )
                                )
                                .clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) Strings.pause() else Strings.play(),
                                tint = Color.White,
                                modifier = Modifier.size(spec.demoPlayIconDp)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .padding(horizontal = 2.dp),
                        color = TamixaColors.goldAccent,
                        trackColor = TamixaColors.lavenderGlow.copy(alpha = 0.28f)
                    )
                }
            }
        }
    }
}
