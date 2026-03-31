package com.tamixa.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
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
import coil3.compose.SubcomposeAsyncImage
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_hook_hero
import com.tamixa.composeapp.generated.resources.onboarding_hook_hero_animated
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaMascot
import com.tamixa.ui.components.platformIsReduceMotionEnabled
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaSkipButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.OnboardingCardDefaults
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.rememberResourceEnvironment

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
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                lineHeight = 34.sp
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
                    image = Res.drawable.onboarding_hook_hero,
                    animatedGif = Res.drawable.onboarding_hook_hero_animated,
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

private val OnboardingSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

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
        animationSpec = OnboardingSpring,
        label = "onboardingEntranceAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else fromYOffset,
        animationSpec = OnboardingSpring,
        label = "onboardingEntranceTranslateY"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else fromScale,
        animationSpec = OnboardingSpring,
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
        AppScreenBackground(
            showStars = true,
            showClouds = true,
            animateStars = true,
            ambientPresence = true
        )
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
            // Top atmosphere — soft brand wash (matches modern splash language, no flat strip)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(TamixaGradients.onboardingTopAtmosphereBrush())
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
 * Hero strip for onboarding preview cards.
 * Uses an optional bundled animated GIF ([animatedGif]) via Coil when motion is allowed; otherwise
 * falls back to [image]. Keeps overlays light so art stays visible.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnboardingImageBanner(
    image: DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    cornerRadius: Dp = 16.dp,
    animatedGif: DrawableResource? = null
) {
    val shape = RoundedCornerShape(cornerRadius)
    val allowAnimated = !platformIsReduceMotionEnabled() && animatedGif != null
    val resourceEnv = rememberResourceEnvironment()
    var gifBytes by remember(animatedGif, allowAnimated) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(animatedGif, resourceEnv, allowAnimated) {
        gifBytes = if (!allowAnimated) {
            null
        } else {
            animatedGif?.let { gif ->
                runCatching { getDrawableResourceBytes(resourceEnv, gif) }.getOrNull()
            }
        }
    }
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
        if (gifBytes != null) {
            SubcomposeAsyncImage(
                model = gifBytes,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                loading = {
                    Image(
                        painter = painterResource(image),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                },
                error = {
                    Image(
                        painter = painterResource(image),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                }
            )
        } else {
            Image(
                painter = painterResource(image),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        }
        // Light bottom wash only — avoids crushing hero detail or overlaid UI (e.g. home preview)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.06f),
                            Color.Black.copy(alpha = 0.18f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.04f)
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
    val outerShape = RoundedCornerShape(26.dp)
    val innerShape = RoundedCornerShape(22.dp)
    val paperColor = Color(0xFFFFFBF7)
    val quoteInk = Color(0xFF1A1628)
    val density = LocalDensity.current
    val frameGradientEnd = with(density) { Offset(220.dp.toPx(), 200.dp.toPx()) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = OnboardingCardDefaults.cardShadowElevation,
                shape = outerShape,
                ambientColor = OnboardingCardDefaults.cardShadowAmbient,
                spotColor = OnboardingCardDefaults.cardShadowSpot
            )
            .clip(outerShape)
            .background(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to accent.copy(alpha = 0.92f),
                        0.45f to TamixaColors.goldAccent.copy(alpha = 0.78f),
                        1f to accent.copy(alpha = 0.65f)
                    ),
                    start = Offset.Zero,
                    end = frameGradientEnd
                ),
                shape = outerShape
            )
            .padding(3.dp)
            .clip(innerShape)
            .background(paperColor, innerShape)
    ) {
        Text(
            text = "\u201C",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 44.sp
            ),
            color = accent.copy(alpha = 0.07f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 10.dp, top = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TamixaMascot(size = 52.dp, emoji = "🧸")
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Strings.appName(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 0.1.sp
                        ),
                        color = quoteInk
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = accent.copy(alpha = 0.95f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = accent.copy(alpha = 0.18f)
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    letterSpacing = 0.2.sp
                ),
                color = quoteInk
            )
        }
    }
}

@Composable
fun OnboardingProgressHeader(
    step: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val inactiveFill = Color.White.copy(alpha = 0.14f)
    val inactiveRing = OnboardingCardColors.onboardingSubline.copy(alpha = 0.42f)
    val completedFill = TamixaColors.goldAccent.copy(alpha = 0.85f)
    val currentFill = TamixaColors.deepTeal.copy(alpha = 0.92f)
    val currentRing = TamixaColors.goldAccent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Step $step of $totalSteps" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val stepIndex = index + 1
            val isPast = stepIndex < step
            val isCurrent = stepIndex == step
            if (index > 0) {
                val segmentComplete = step > index
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    repeat(4) { d ->
                        if (d > 0) Spacer(Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .size(2.5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (segmentComplete) {
                                        TamixaColors.goldAccent.copy(alpha = 0.5f)
                                    } else {
                                        Color.White.copy(alpha = 0.16f)
                                    }
                                )
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }
            val dotSize = when {
                isCurrent -> 9.dp
                isPast -> 8.dp
                else -> 7.dp
            }
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 13.dp else dotSize)
                    .then(
                        if (isCurrent) Modifier.border(2.dp, currentRing, CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                isPast -> completedFill
                                isCurrent -> currentFill
                                else -> inactiveFill
                            }
                        )
                        .then(
                            if (!isPast && !isCurrent) {
                                Modifier.border(1.dp, inactiveRing, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}
