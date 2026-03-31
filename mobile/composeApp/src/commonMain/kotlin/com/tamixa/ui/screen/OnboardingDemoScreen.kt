package com.tamixa.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.tamixa.ui.theme.TamixaDesignTokens
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
        onSwipeToPrevious = onSwipeToPrevious
    ) {
        Text(
            text = Strings.onboardingDemoHeadline(),
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
        Spacer(Modifier.height(28.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = OnboardingCardDefaults.cardShadowElevation,
                    shape = OnboardingCardDefaults.cardShape,
                    ambientColor = OnboardingCardDefaults.cardShadowAmbient,
                    spotColor = OnboardingCardDefaults.cardShadowSpot
                )
                .onboardingEntrance(delayMs = 200),
            shape = OnboardingCardDefaults.cardShape,
            color = OnboardingCardColors.cardBackground,
            border = BorderStroke(1.dp, OnboardingCardColors.cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(TamixaDesignTokens.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
            ) {
                when {
                    ttsUri == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
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
                                .height(176.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            OnboardingImageBanner(
                                image = Res.drawable.onboarding_demo_hero,
                                animatedGif = Res.drawable.onboarding_demo_hero_animated,
                                contentDescription = "Story demo",
                                height = 176.dp,
                                cornerRadius = 16.dp
                            )
                        }
                        Text(
                            text = DEMO_SUBTITLE,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnboardingCardColors.onCardText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = { controller.playPause() }) {
                                Text(
                                    text = if (controller.isPlaying) Strings.pause() else Strings.play(),
                                    color = TamixaColors.goldAccent
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(horizontal = 4.dp),
                            color = TamixaColors.goldAccent,
                            trackColor = TamixaColors.lavenderGlow.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OnboardingDailyQuote(step = 2, modifier = Modifier.onboardingEntrance(delayMs = 240))
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.weight(1f))
        TamixaPrimaryButton(
            onClick = onContinue,
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

