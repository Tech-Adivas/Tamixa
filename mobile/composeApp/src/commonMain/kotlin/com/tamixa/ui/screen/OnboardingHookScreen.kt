package com.tamixa.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_hook_hero
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaSkipButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.OnboardingCardDefaults
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun OnboardingHookScreen(
    onStartStoryMagic: () -> Unit,
    onSkip: () -> Unit,
    onSwipeToNext: (() -> Unit)? = null,
    onSwipeToPrevious: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OnboardingShell(
        step = 1,
        totalSteps = 4,
        modifier = modifier,
        onSwipeToNext = onSwipeToNext,
        onSwipeToPrevious = onSwipeToPrevious
    ) {
        Text(
            text = Strings.onboardingHookHeadline(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
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
            text = Strings.onboardingHookSubline(),
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
            HookVisualPreviewCard()
        }
        Spacer(Modifier.height(16.dp))
        OnboardingDailyQuote(step = 1, modifier = Modifier.onboardingEntrance(delayMs = 220))
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.weight(1f))
        TamixaPrimaryButton(
            onClick = onStartStoryMagic,
            text = Strings.startStoryMagic(),
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
private fun HookVisualPreviewCard() {
    val transition = rememberInfiniteTransition(label = "hookCardPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hookCardPulseValue"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = OnboardingCardDefaults.cardShadowElevation,
                shape = OnboardingCardDefaults.cardShape,
                ambientColor = OnboardingCardDefaults.cardShadowAmbient,
                spotColor = OnboardingCardDefaults.cardShadowSpot
            )
            .graphicsLayer(scaleX = pulse, scaleY = pulse),
        shape = OnboardingCardDefaults.cardShape,
        color = OnboardingCardColors.cardBackground,
        border = BorderStroke(1.dp, OnboardingCardColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                OnboardingImageBanner(
                    image = Res.drawable.onboarding_hook_hero,
                    contentDescription = "Story preview",
                    height = 176.dp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HookThemePill(text = Strings.themeAdventure(), accent = OnboardingCardColors.pillWarmOrange)
                HookThemePill(text = Strings.themeFriendship(), accent = OnboardingCardColors.pillSoftPurple)
            }
        }
    }
}

@Composable
private fun HookThemePill(text: String, accent: androidx.compose.ui.graphics.Color = OnboardingCardColors.pillBackground) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )
        }
    }
}

private val OnboardingEntranceDuration = 520
private val OnboardingEntranceEasing = FastOutSlowInEasing

fun Modifier.onboardingEntrance(
    delayMs: Int = 0,
    fromYOffset: Float = 16f,
    fromScale: Float = 0.96f
): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(OnboardingEntranceDuration, easing = OnboardingEntranceEasing),
        label = "onboardingEntranceAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else fromYOffset,
        animationSpec = tween(OnboardingEntranceDuration, easing = OnboardingEntranceEasing),
        label = "onboardingEntranceTranslateY"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else fromScale,
        animationSpec = tween(OnboardingEntranceDuration, easing = OnboardingEntranceEasing),
        label = "onboardingEntranceScale"
    )
    this.graphicsLayer {
        this.alpha = alpha
        translationY = translateY
        scaleX = scale
        scaleY = scale
    }
}

private const val SWIPE_THRESHOLD_DP = 80f

internal fun Modifier.onboardingSwipeNavigation(
    onSwipeToNext: (() -> Unit)?,
    onSwipeToPrevious: (() -> Unit)?
): Modifier = composed {
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }
    this.pointerInput(Unit) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onHorizontalDrag = { _, amount -> totalDrag += amount },
            onDragEnd = {
                when {
                    totalDrag > thresholdPx -> onSwipeToPrevious?.invoke()
                    totalDrag < -thresholdPx -> onSwipeToNext?.invoke()
                }
            }
        )
    }
}

@Composable
fun OnboardingShell(
    step: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    onSwipeToNext: (() -> Unit)? = null,
    onSwipeToPrevious: (() -> Unit)? = null,
    background: @Composable BoxScope.() -> Unit = {
        AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
    },
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onboardingSwipeNavigation(onSwipeToNext, onSwipeToPrevious)
    ) {
        background()
        Column(modifier = Modifier.fillMaxSize()) {
            // Premium top gradient strip — refined for HD clarity
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                com.tamixa.ui.theme.TamixaColors.goldAccent.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = TamixaDesignTokens.screenPadding)
                    .padding(top = TamixaDesignTokens.screenPadding, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                OnboardingProgressHeader(
                    step = step,
                    totalSteps = totalSteps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onboardingEntrance(delayMs = 40)
                )
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Enterprise-grade image banner for onboarding cards.
 * Features: cinematic gradient overlay, refined corners, subtle rim, premium depth.
 */
@Composable
fun OnboardingImageBanner(
    image: DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    cornerRadius: Dp = 16.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(shape)
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        // Cinematic bottom gradient for depth and readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.35f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        // Subtle inner rim for premium frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.06f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

/** Step-specific accent for quote cards: Thought=gold, Proverb=teal, Tongue twister=purple, Quote=mint */
private fun quoteStepAccent(step: Int): Color {
    val idx = when (step) {
        1 -> 0
        2 -> 1
        3 -> 2
        4 -> 3
        else -> (step - 1) % 4
    }
    return when (idx) {
        0 -> TamixaColors.goldAccent
        1 -> TamixaColors.deepTeal
        2 -> TamixaColors.softPurple
        else -> OnboardingCardColors.pillMint
    }
}

/** 1st screen: Thought for the day. 2nd: Proverb. 3rd: Tongue twister. 4th: Quote. */
@Composable
fun OnboardingDailyQuote(
    step: Int,
    modifier: Modifier = Modifier
) {
    val (label, content) = when (step) {
        1 -> Strings.thoughtForTheDay() to Strings.onboardingQuoteThought()
        2 -> Strings.shortContentTypeLabel("PROVERB") to Strings.onboardingQuoteProverb()
        3 -> Strings.shortContentTypeLabel("TONGUE_TWISTER") to Strings.onboardingQuoteTongueTwister()
        4 -> Strings.shortContentTypeLabel("QUOTE") to Strings.onboardingQuoteQuote()
        else -> {
            val idx = (step - 1) % 4
            when (idx) {
                0 -> Strings.thoughtForTheDay() to Strings.onboardingQuoteThought()
                1 -> Strings.shortContentTypeLabel("PROVERB") to Strings.onboardingQuoteProverb()
                2 -> Strings.shortContentTypeLabel("TONGUE_TWISTER") to Strings.onboardingQuoteTongueTwister()
                else -> Strings.shortContentTypeLabel("QUOTE") to Strings.onboardingQuoteQuote()
            }
        }
    }
    val accent = quoteStepAccent(step)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(14.dp),
        color = OnboardingCardColors.cardBackground,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = 0.9f))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        letterSpacing = 0.15.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun OnboardingProgressHeader(
    step: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .semantics { contentDescription = "Step $step of $totalSteps" }
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            if (index > 0) {
                Spacer(Modifier.width(8.dp))
            }
            val isActive = (index + 1) == step
            val isPast = (index + 1) < step
            val size = if (isActive) 8.dp else 6.dp
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> TamixaColors.goldAccent
                            isPast -> TamixaColors.goldAccent.copy(alpha = 0.8f)
                            else -> OnboardingCardColors.progressTrack
                        }
                    )
            )
        }
    }
}
