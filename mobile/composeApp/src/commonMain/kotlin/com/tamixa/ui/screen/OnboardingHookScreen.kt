package com.tamixa.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_hook_hero
import com.tamixa.composeapp.generated.resources.onboarding_hook_hero_animated
import com.tamixa.ui.components.AppScreenBackground
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

/** Persona-tinted halos so each step feels distinct on the same starfield. */
enum class OnboardingHeroHaloKind {
    HookWarmth,
    LanternGlow,
    VoiceRipple,
    AvatarShimmer
}

/** Learn-lane ribbon — consistent edu-story positioning across onboarding steps. */
@Composable
internal fun OnboardingEduStoryBadge(modifier: Modifier = Modifier) {
    val label = Strings.onboardingEduStoryBadgeLabel()
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(50),
        color = TamixaColors.deepTeal.copy(alpha = 0.44f),
        border = BorderStroke(1.dp, TamixaColors.goldAccent.copy(alpha = 0.52f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📚",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.35.sp
                ),
                color = Color(0xFFFFF8F0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
        totalSteps = 5,
        modifier = modifier,
        onSwipeToNext = onSwipeToNext,
        onSwipeToPrevious = onSwipeToPrevious,
        footer = {
            val spec = LocalOnboardingLayoutSpec.current
            TamixaPrimaryButton(
                onClick = onStartStoryMagic,
                text = Strings.startStoryMagic(),
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
        
        // Large hero visual - the main focus
        OnboardingHeroSpotlight(
            kind = OnboardingHeroHaloKind.HookWarmth,
            haloHeight = 340.dp,
            modifier = Modifier.onboardingEntrance(delayMs = 70)
        ) {
            HookVisualPreviewCard()
        }
        
        Spacer(Modifier.height(spec.gapLg))
        
        // Simple headline only - no subline
        OnboardingEditorialTextCard(
            modifier = Modifier.onboardingEntrance(delayMs = 130)
        ) {
            OnboardingHeadline(
                text = Strings.onboardingHookHeadline(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Soft radial halo — color story shifts per [OnboardingHeroHaloKind]. */
@Composable
internal fun OnboardingHeroSpotlight(
    kind: OnboardingHeroHaloKind,
    modifier: Modifier = Modifier,
    haloHeight: Dp = 260.dp,
    content: @Composable () -> Unit
) {
    val spec = LocalOnboardingLayoutSpec.current
    val haloH = spec.scaledHaloHeight(haloHeight)
    val density = LocalDensity.current
    val (inner, outer) = when (kind) {
        OnboardingHeroHaloKind.HookWarmth ->
            TamixaColors.goldAccent to TamixaColors.deepTeal
        OnboardingHeroHaloKind.LanternGlow ->
            TamixaColors.goldAccent to TamixaColors.mintGreen
        OnboardingHeroHaloKind.VoiceRipple ->
            TamixaColors.deepTeal to TamixaColors.goldAccent
        OnboardingHeroHaloKind.AvatarShimmer ->
            TamixaColors.softPurple to TamixaColors.deepTeal
    }
    val centerX = with(density) { 198.dp.toPx() }
    val centerY = with(density) {
        when (kind) {
            OnboardingHeroHaloKind.HookWarmth -> 102.dp.toPx()
            OnboardingHeroHaloKind.LanternGlow -> 118.dp.toPx()
            OnboardingHeroHaloKind.VoiceRipple -> 96.dp.toPx()
            OnboardingHeroHaloKind.AvatarShimmer -> 108.dp.toPx()
        }
    }
    val radius = with(density) {
        when (kind) {
            OnboardingHeroHaloKind.LanternGlow -> 258.dp.toPx()
            OnboardingHeroHaloKind.VoiceRipple -> 248.dp.toPx()
            OnboardingHeroHaloKind.AvatarShimmer -> 252.dp.toPx()
            OnboardingHeroHaloKind.HookWarmth -> 238.dp.toPx()
        }
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(haloH)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            inner.copy(
                                alpha = when (kind) {
                                    OnboardingHeroHaloKind.VoiceRipple -> 0.22f
                                    OnboardingHeroHaloKind.HookWarmth -> 0.27f
                                    else -> 0.2f
                                }
                            ),
                            outer.copy(
                                alpha = when (kind) {
                                    OnboardingHeroHaloKind.HookWarmth -> 0.19f
                                    else -> 0.1f
                                }
                            ),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
internal fun OnboardingValueStripTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val spec = LocalOnboardingLayoutSpec.current
    val gemPad = spec.gapSm
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📖",
            style = MaterialTheme.typography.titleMedium,
            color = TamixaColors.deepTeal.copy(alpha = 0.72f),
            modifier = Modifier.padding(end = gemPad)
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = spec.valueStripLetterSpacing.sp
            ),
            color = Color(0xFFE8F4F2).copy(alpha = 0.94f)
        )
        Text(
            text = "📖",
            style = MaterialTheme.typography.titleMedium,
            color = TamixaColors.deepTeal.copy(alpha = 0.72f),
            modifier = Modifier.padding(start = gemPad)
        )
    }
}

/**
 * Headline + subline — edu-story “open book” frame: teal-forward rim, parchment panel, spine accent.
 */
@Composable
internal fun OnboardingEditorialTextCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = LocalOnboardingLayoutSpec.current
    val outerR = spec.scaledCorner(26.dp)
    val innerR = (outerR - 2.dp).coerceAtLeast(12.dp)
    val outer = RoundedCornerShape(outerR)
    val inner = RoundedCornerShape(innerR)
    val spineShape = RoundedCornerShape(topStart = innerR, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = innerR)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 11.dp,
                shape = outer,
                ambientColor = OnboardingCardDefaults.cardShadowAmbient,
                spotColor = TamixaColors.deepTeal.copy(alpha = 0.22f)
            )
            .clip(outer)
            .background(TamixaGradients.onboardingEduStoryFrameBrush())
            .padding(2.dp)
            .clip(inner)
            .background(Color(0xFFFFF4EC).copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TamixaColors.deepTeal.copy(alpha = 0.92f),
                                TamixaColors.goldAccent.copy(alpha = 0.42f)
                            )
                        ),
                        shape = spineShape
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spec.editorialHPadding, vertical = spec.editorialVPadding),
                verticalArrangement = Arrangement.spacedBy(spec.editorialSectionGap)
            ) {
                content()
            }
        }
    }
}

/** Value prop row — Learn-lane list: teal spine + soft glass (edu-story scan pattern). */
@Composable
internal fun OnboardingBenefitRow(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val spec = LocalOnboardingLayoutSpec.current
    val rowR = spec.scaledCorner(TamixaDesignTokens.cardRadius)
    val spineShape = RoundedCornerShape(topStart = rowR, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = rowR)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(rowR),
        color = Color.White.copy(alpha = 0.085f),
        border = BorderStroke(1.dp, TamixaColors.deepTeal.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TamixaColors.deepTeal.copy(alpha = 0.88f),
                                TamixaColors.mintGreen.copy(alpha = 0.38f)
                            )
                        ),
                        shape = spineShape
                    )
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = (spec.cardContentPadding - 2.dp).coerceAtLeast(10.dp),
                        vertical = spec.benefitRowVerticalPadding
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = text
                    },
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(spec.gapMd)
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium.copy(lineHeight = spec.benefitEmojiLineHeight),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = spec.sublineLineHeight,
                        letterSpacing = 0.12.sp
                    ),
                    color = Color(0xFFF5FAF9).copy(alpha = 0.97f),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HookVisualPreviewCard() {
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
                    image = Res.drawable.onboarding_hook_hero,
                    animatedGif = Res.drawable.onboarding_hook_hero_animated,
                    contentDescription = "Story preview",
                    height = imgH,
                    cornerRadius = imgCorner
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OnboardingLanePill(
                    text = Strings.onboardingHookPillLearnSafety(),
                    accent = OnboardingCardColors.pillLearnLane
                )
                OnboardingLanePill(text = Strings.themeAdventure(), accent = OnboardingCardColors.pillWarmOrange)
                OnboardingLanePill(text = Strings.themeFriendship(), accent = OnboardingCardColors.pillSoftPurple)
            }
        }
    }
}

/** Theme / lane chip — reused on Hook and Demo onboarding cards. */
@Composable
internal fun OnboardingLanePill(
    text: String,
    accent: androidx.compose.ui.graphics.Color = OnboardingCardColors.pillBackground
) {
    Surface(
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadius),
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

/** Shared onboarding title — pair with [Modifier.onboardingEntrance] per step. */
@Composable
fun OnboardingHeadline(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int? = null
) {
    val spec = LocalOnboardingLayoutSpec.current
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            lineHeight = spec.headlineLineHeight
        ),
        color = OnboardingCardColors.onboardingHeadline,
        textAlign = textAlign,
        maxLines = maxLines ?: spec.headlineMaxLines,
        modifier = modifier
    )
}

/** Shared onboarding supporting copy under the title. */
@Composable
fun OnboardingSubline(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int? = null,
    lineHeight: TextUnit? = null
) {
    val spec = LocalOnboardingLayoutSpec.current
    val lh = lineHeight ?: spec.sublineLineHeight
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = lh,
            letterSpacing = 0.15.sp
        ),
        color = OnboardingCardColors.onboardingSubline,
        textAlign = textAlign,
        maxLines = maxLines ?: spec.sublineMaxLines,
        modifier = modifier
    )
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
    footer: @Composable ColumnScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onboardingSwipeNavigation(onSwipeToNext, onSwipeToPrevious)
    ) {
        background()
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val spec = remember(maxHeight, maxWidth) {
                onboardingLayoutSpecForScreen(maxHeight, maxWidth)
            }
            CompositionLocalProvider(LocalOnboardingLayoutSpec provides spec) {
                val bodyScroll = rememberScrollState()
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spec.topAtmosphereHeight)
                            .align(Alignment.TopCenter)
                            .background(TamixaGradients.onboardingTopAtmosphereBrush())
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = TamixaDesignTokens.screenPadding)
                                .padding(
                                    top = spec.progressPaddingTop,
                                    bottom = spec.progressPaddingBottom
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OnboardingProgressHeader(
                                step = step,
                                totalSteps = totalSteps,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onboardingEntrance(delayMs = 40)
                            )
                            Spacer(Modifier.height(8.dp))
                            OnboardingEduStoryBadge(
                                modifier = Modifier.onboardingEntrance(delayMs = 52)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(bodyScroll)
                                    .padding(horizontal = TamixaDesignTokens.screenPadding)
                                    .padding(
                                        bottom = spec.bodyColumnBottomPadding + 12.dp
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                body()
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = TamixaDesignTokens.screenPadding)
                                .padding(bottom = spec.footerBottomPadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            footer()
                        }
                    }
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "${Strings.onboardingProgressShort(step, totalSteps)}. Step $step of $totalSteps"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
    Row(
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
                            ) {}
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
                    ) {}
                }
            }
    }
    Text(
        text = Strings.onboardingProgressShort(step, totalSteps),
        style = MaterialTheme.typography.labelMedium,
        color = OnboardingCardColors.onboardingSubline.copy(alpha = 0.78f),
        modifier = Modifier.padding(top = 8.dp)
    )
    }
}
