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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.tamixa_logo_full
import com.tamixa.ui.components.StarryNightBackground
import com.tamixa.ui.strings.Strings
import com.tamixa.platform.playSplashRevealSound
import com.tamixa.ui.theme.TamixaDesignTokens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin

private val TaglineWhite = Color(0xFFFFFFFF).copy(alpha = 0.88f)

private const val SPLASH_DURATION_MS = 3200L
private const val HERO_DELAY_MS = 250L
private const val SPARKLE_START_MS = 850L
private const val TAGLINE_DELAY_MS = 1750L

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

    // Play reveal chime when logo lands (~850ms — ring expands, sparkles pop)
    LaunchedEffect(elapsed) {
        if (elapsed >= SPARKLE_START_MS && !soundPlayed) {
            soundPlayed = true
            playSplashRevealSound()
        }
    }

    LaunchedEffect(Unit, isLoggedIn, hasCompletedOnboarding, skipRequested) {
        fun navigateAfterSplash() {
            when {
                isLoggedIn -> { /* LaunchedEffect in NavHost handles logged-in redirect */ }
                !hasCompletedOnboarding -> onNavigateToHook()
                else -> onNavigateToLogin()
            }
        }
        if (skipRequested) {
            navigateAfterSplash()
            return@LaunchedEffect
        }
        var total: Long = 0
        while (total < SPLASH_DURATION_MS) {
            delay(50)
            total += 50
            elapsed = total
        }
        navigateAfterSplash()
    }

    // Hero: soft bounce + slight rotation on enter, then idle float
    val heroProgress = ((elapsed - HERO_DELAY_MS) / 650f).coerceIn(0f, 1f)
    val heroScale = when {
        heroProgress < 0.5f -> 0.5f + (heroProgress / 0.5f) * 0.58f
        heroProgress < 0.65f -> 1.08f - (heroProgress - 0.5f) / 0.15f * 0.1f
        heroProgress < 0.8f -> 0.98f + (heroProgress - 0.65f) / 0.15f * 0.04f
        else -> 1.02f
    }
    val heroAlpha = (heroProgress * 2.5f).coerceIn(0f, 1f)
    val heroOffsetY = when {
        heroProgress < 0.5f -> 28f * (1f - heroProgress / 0.5f)
        heroProgress < 0.65f -> -8f + (heroProgress - 0.5f) / 0.15f * 8f
        else -> -2f * (1f - (heroProgress - 0.65f) / 0.35f).coerceIn(0f, 1f)
    }
    val heroRotationDeg = when {
        heroProgress < 0.45f -> -8f + (heroProgress / 0.45f) * 10f
        heroProgress < 0.65f -> 2f - (heroProgress - 0.45f) / 0.2f * 2.5f
        else -> 0f
    }
    val heroRotationRad = heroRotationDeg * (PI / 180).toFloat()

    // Idle float + gentle rotation (starts after hero lands)
    val idleProgress = ((elapsed - 900L) / 2400f).coerceIn(0f, 1f)
    val idleFloat = if (elapsed >= 900L) sin(idleProgress * 2 * PI.toFloat()).toFloat() * 6f else 0f
    val idleRotation = if (elapsed >= 900L) sin(idleProgress * 2 * PI.toFloat()).toFloat() * 0.025f else 0f

    // Sparkles: pop in with scale overshoot + rotation, then twinkle (8 total)
    val sparkleDelays = listOf(0L, 80L, 160L, 240L, 320L, 100L, 180L, 260L)
    fun sparkleProgress(index: Int): Float {
        val start = SPARKLE_START_MS + sparkleDelays[index]
        if (elapsed < start) return 0f
        return ((elapsed - start) / 550f).coerceIn(0f, 1f)
    }
    fun sparkleScale(index: Int): Float {
        val p = sparkleProgress(index)
        if (p == 0f) return 0f
        val overshoot = 1.35f
        return when {
            p < 0.45f -> p / 0.45f * overshoot
            p < 0.7f -> overshoot - (p - 0.45f) / 0.25f * (overshoot - 1f)
            else -> 1f
        }
    }
    fun sparkleRotation(index: Int): Float = sparkleProgress(index) * 2 * PI.toFloat()
    fun sparkleAlpha(index: Int): Float {
        val p = sparkleProgress(index)
        if (p == 0f) return 0f
        val base = (p * 2f).coerceIn(0f, 1f)
        val start = SPARKLE_START_MS + 400L
        if (elapsed < start) return base
        val phase = (elapsed - start) / 2000f
        return base * (0.7f + 0.15f * (1f + sin(phase * 2 * PI.toFloat())))
    }

    // Tagline: word-by-word stagger (Listen, ·, Learn, ·, Shine)
    val taglineItemDelays = listOf(0L, 120L, 240L, 360L, 480L)  // 5 items
    fun taglineItemProgress(itemIndex: Int): Float {
        val start = TAGLINE_DELAY_MS + taglineItemDelays[itemIndex]
        if (elapsed < start) return 0f
        return ((elapsed - start) / 380f).coerceIn(0f, 1f)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val logoSize: Dp = (minOf(maxWidth, maxHeight) * 0.38f).coerceAtLeast(240.dp).coerceAtMost(320.dp)
        StarryNightBackground(showStars = true, showClouds = true, animateStars = false)

        // Floating particles (behind hero)
        val particlePhase = elapsed / 1200f * 2 * PI.toFloat()
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
            val driftX = sin(particlePhase + phaseOff) * 8f + sin(particlePhase * 0.7f + index) * 6f
            val driftY = sin(particlePhase * 0.8f + phaseOff * 1.2f) * 10f
            val alphaP = (0.35f + 0.2f * sin(particlePhase * 0.5f + index)).coerceIn(0.2f, 0.6f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (xFrac * 360f + driftX).dp,
                        y = (yFrac * 640f + driftY).dp
                    )
                    .size((4 + (index % 3)).dp)
                    .graphicsLayer { alpha = alphaP }
                    .background(
                        color = Color.White.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }

        // Expanding ring when hero lands
        if (elapsed in 850L..2200L) {
            val ringProgress = ((elapsed - 850L) / 1200f).coerceIn(0f, 1f)
            val ringScale = 0.6f + ringProgress * 1.8f
            val ringAlpha = (1f - ringProgress) * 0.5f
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .background(
                        color = Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    drawCircle(
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f - strokeWidth / 2f,
                        color = Color.White.copy(alpha = 0.4f),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        // Hero: logo (title in asset) — bounce + rotation on enter, idle float + pulse
        if (elapsed >= HERO_DELAY_MS) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = heroScale
                        scaleY = heroScale
                        translationY = heroOffsetY + idleFloat
                        rotationZ = heroRotationRad + idleRotation
                        alpha = heroAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                val pulse = if (elapsed > 1200L) 1f + 0.025f * sin(((elapsed - 1200L) / 800f) * 2 * PI.toFloat()) else 1f
                Image(
                    painter = painterResource(Res.drawable.tamixa_logo_full),
                    contentDescription = "Tamixa",
                    modifier = Modifier
                        .size(logoSize)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse },
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Sparkles: each pops with scale overshoot + rotation (8 around logo)
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
                        .size(10.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                            alpha = sparkleAlphaVal
                        }
                        .background(
                            color = Color.White.copy(alpha = 0.95f),
                            shape = CircleShape
                        )
                )
            }
        }

        // Tagline: five items (Listen · Learn · Shine) with staggered slide-up + fade
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-78).dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Strings.appTagline().split(" · ").flatMapIndexed { i, w -> if (i > 0) listOf(" · ", w) else listOf(w) }.forEachIndexed { index, word ->
                    val progress = taglineItemProgress(index)
                    val offsetY = 10f * (1f - progress)
                    Text(
                        text = word,
                        modifier = Modifier.graphicsLayer {
                            translationY = offsetY
                            alpha = progress
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = if (word.startsWith(" ")) 0.sp else 2.sp
                        ),
                        color = TaglineWhite
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
                    style = MaterialTheme.typography.labelLarge,
                    color = TaglineWhite
                )
            }
        }
    }
}

