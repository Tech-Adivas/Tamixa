package com.tamixa.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.tamixa_logo_full
import com.tamixa.ui.components.StarryNightBackground
import com.tamixa.ui.strings.Strings
import com.tamixa.platform.playSplashRevealSound
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin

private const val SPLASH_DURATION_MS = 4200L
private const val TICK_MS = 16L
private const val HERO_DELAY_MS = 200L
private const val SPARKLE_START_MS = 780L
private const val TAGLINE_DELAY_MS = 1680L

@Composable
fun SplashScreen(
    isLoggedIn: Boolean = false,
    hasCompletedOnboarding: Boolean = false,
    onNavigateToHook: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var elapsed by remember { mutableStateOf(0L) }
    var skipRequested by remember { mutableStateOf(false) }
    var soundPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(elapsed) {
        if (elapsed >= SPARKLE_START_MS && !soundPlayed) {
            soundPlayed = true
            playSplashRevealSound()
        }
    }

    LaunchedEffect(Unit, isLoggedIn, hasCompletedOnboarding, skipRequested) {
        fun navigateAfterSplash() {
            when {
                isLoggedIn -> { }
                !hasCompletedOnboarding -> onNavigateToHook()
                else -> onNavigateToLogin()
            }
        }
        if (skipRequested) {
            navigateAfterSplash()
            return@LaunchedEffect
        }
        var total = 0L
        while (total < SPLASH_DURATION_MS) {
            delay(TICK_MS)
            total += TICK_MS
            elapsed = total
        }
        navigateAfterSplash()
    }

    // Smoother elastic-style landing (overshoot then settle)
    val heroProgress = ((elapsed - HERO_DELAY_MS) / 820f).coerceIn(0f, 1f)
    val heroScale = when {
        heroProgress <= 0f -> 0.32f
        heroProgress < 0.62f -> {
            val t = heroProgress / 0.62f
            val smooth = t * t * (3f - 2f * t)
            0.32f + smooth * (1.1f - 0.32f)
        }
        heroProgress < 0.82f -> {
            val t = (heroProgress - 0.62f) / 0.2f
            1.1f - t * 0.14f
        }
        else -> {
            val t = (heroProgress - 0.82f) / 0.18f
            0.96f + t * 0.04f
        }
    }
    val heroAlpha = (heroProgress * 2.2f).coerceIn(0f, 1f)
    val heroOffsetY = when {
        heroProgress < 0.55f -> 36f * (1f - heroProgress / 0.55f)
        heroProgress < 0.75f -> -10f + (heroProgress - 0.55f) / 0.2f * 10f
        else -> 0f
    }
    val heroRotationDeg = when {
        heroProgress < 0.5f -> -5f + (heroProgress / 0.5f) * 5f
        heroProgress < 0.75f -> (heroProgress - 0.5f) / 0.25f * -2f
        else -> -2f + (heroProgress - 0.75f) / 0.25f * 2f
    }
    val heroRotationRad = heroRotationDeg * (PI / 180).toFloat()

    val idleProgress = ((elapsed - 950L) / 2600f).coerceIn(0f, 1f)
    val idleFloat = if (elapsed >= 950L) sin(idleProgress * 2 * PI.toFloat()) * 5f else 0f
    val idleRotation = if (elapsed >= 950L) sin(idleProgress * 2 * PI.toFloat()) * 0.018f else 0f

    val sparkleDelays = listOf(0L, 70L, 140L, 210L, 280L, 90L, 170L, 250L)
    fun sparkleProgress(index: Int): Float {
        val start = SPARKLE_START_MS + sparkleDelays[index]
        if (elapsed < start) return 0f
        return ((elapsed - start) / 480f).coerceIn(0f, 1f)
    }
    fun sparkleScale(index: Int): Float {
        val p = sparkleProgress(index)
        if (p == 0f) return 0f
        val overshoot = 1.42f
        return when {
            p < 0.42f -> p / 0.42f * overshoot
            p < 0.68f -> overshoot - (p - 0.42f) / 0.26f * (overshoot - 1f)
            else -> 1f
        }
    }
    fun sparkleRotation(index: Int): Float = sparkleProgress(index) * 2 * PI.toFloat()
    fun sparkleAlpha(index: Int): Float {
        val p = sparkleProgress(index)
        if (p == 0f) return 0f
        val base = (p * 2f).coerceIn(0f, 1f)
        val start = SPARKLE_START_MS + 380L
        if (elapsed < start) return base
        val phase = (elapsed - start) / 1900f
        return base * (0.72f + 0.18f * (1f + sin(phase * 2 * PI.toFloat())))
    }

    val taglineItemDelays = listOf(0L, 100L, 220L, 340L, 460L)
    fun taglineItemProgress(itemIndex: Int): Float {
        val start = TAGLINE_DELAY_MS + taglineItemDelays[itemIndex]
        if (elapsed < start) return 0f
        val raw = ((elapsed - start) / 320f).coerceIn(0f, 1f)
        return raw * raw * (3f - 2f * raw)
    }

    val logoPulse = if (elapsed > 1100L) {
        1f + 0.035f * sin(((elapsed - 1100L) / 700f) * 2 * PI.toFloat())
    } else {
        1f
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val logoSize: Dp = (minOf(maxWidth, maxHeight) * 0.36f).coerceAtLeast(220.dp).coerceAtMost(300.dp)
        val glowSize = (logoSize.value * 1.75f).dp.coerceAtMost(minOf(maxWidth, maxHeight) * 0.72f)

        StarryNightBackground(
            showStars = true,
            showClouds = true,
            animateStars = true,
            ambientPresence = true
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(TamixaGradients.splashStorybookVignetteBrush())
        )

        // Concentric shockwaves — modern, no logo “plate”
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.46f
            if (elapsed > 520L) {
                repeat(3) { i ->
                    val delay = i * 160L
                    val pr = ((elapsed - 520L - delay).coerceAtLeast(0L) / 980f).coerceIn(0f, 1f)
                    val radius = size.minDimension * 0.12f + pr * size.minDimension * 0.42f
                    val alpha = (1f - pr) * 0.32f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = TamixaColors.goldAccent.copy(alpha = alpha * 0.55f),
                        radius = radius * 0.92f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        val particlePhase = elapsed / 1100f * 2 * PI.toFloat()
        listOf(
            Triple(0.12f, 0.18f, 0f),
            Triple(0.22f, 0.85f, -1.5f),
            Triple(0.72f, 0.28f, -3f),
            Triple(0.82f, 0.78f, -0.5f),
            Triple(0.08f, 0.48f, -2f),
            Triple(0.92f, 0.55f, -4f),
            Triple(0.28f, 0.72f, -1f),
            Triple(0.55f, 0.12f, -2.5f)
        ).forEachIndexed { index, (xFrac, yFrac, phaseOff) ->
            val driftX = sin(particlePhase + phaseOff) * 10f + sin(particlePhase * 0.65f + index) * 7f
            val driftY = sin(particlePhase * 0.82f + phaseOff * 1.1f) * 12f
            val alphaP = (0.38f + 0.22f * sin(particlePhase * 0.48f + index)).coerceIn(0.22f, 0.65f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (xFrac * 360f + driftX).dp,
                        y = (yFrac * 640f + driftY).dp
                    )
                    .size((5 + (index % 3)).dp)
                    .graphicsLayer { alpha = alphaP }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                TamixaColors.goldAccent.copy(alpha = 0.5f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        if (elapsed >= HERO_DELAY_MS) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = heroScale * logoPulse
                        scaleY = heroScale * logoPulse
                        translationY = heroOffsetY + idleFloat
                        rotationZ = heroRotationRad + idleRotation
                        alpha = heroAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(glowSize)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.maxDimension / 2f * 0.92f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TamixaColors.goldAccent.copy(alpha = 0.5f),
                                TamixaColors.deepTeal.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = c,
                            radius = r
                        ),
                        radius = r,
                        center = c
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = c,
                            radius = r * 0.45f
                        ),
                        radius = r * 0.45f,
                        center = c
                    )
                }
                Image(
                    painter = painterResource(Res.drawable.tamixa_logo_full),
                    contentDescription = "Tamixa",
                    modifier = Modifier.size(logoSize),
                    contentScale = ContentScale.Fit
                )
            }
        }

        listOf(
            Pair(0.5f - 0.22f, 0.5f - 0.28f),
            Pair(0.5f - 0.22f, 0.5f + 0.22f),
            Pair(0.5f + 0.22f, 0.5f - 0.24f),
            Pair(0.5f + 0.22f, 0.5f + 0.22f),
            Pair(0.5f - 0.30f, 0.5f - 0.05f),
            Pair(0.5f + 0.28f, 0.5f + 0.18f),
            Pair(0.5f + 0.20f, 0.5f - 0.32f),
            Pair(0.5f - 0.26f, 0.5f + 0.28f)
        ).forEachIndexed { index, (yFrac, xFrac) ->
            val sparkleAlphaVal = sparkleAlpha(index)
            val scale = sparkleScale(index)
            val rotation = sparkleRotation(index)
            if (sparkleAlphaVal > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (xFrac * 360f).dp,
                            y = (yFrac * 640f).dp
                        )
                        .size(12.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                            alpha = sparkleAlphaVal
                        }
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    TamixaColors.goldAccent.copy(alpha = 0.85f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-84).dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Strings.appTagline().split(" · ").flatMapIndexed { i, w ->
                    if (i > 0) listOf(" · ", w) else listOf(w)
                }.forEachIndexed { index, word ->
                    val progress = taglineItemProgress(index)
                    val offsetY = 18f * (1f - progress)
                    val wordScale = 0.88f + 0.12f * progress
                    Text(
                        text = word,
                        modifier = Modifier.graphicsLayer {
                            translationY = offsetY
                            alpha = progress
                            scaleX = wordScale
                            scaleY = wordScale
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = if (word.startsWith(" ")) 0.sp else 1.4.sp,
                            lineHeight = 28.sp
                        ),
                        color = Color.White.copy(alpha = 0.96f)
                    )
                }
            }
        }

        if (!isLoggedIn) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(TamixaDesignTokens.screenPadding)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable { skipRequested = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.skip(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}
