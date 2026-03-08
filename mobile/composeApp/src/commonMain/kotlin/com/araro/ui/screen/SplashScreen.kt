package com.araro.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.theme.AraroColors
import kotlinx.coroutines.delay

private val LogoRingColor = Color(0xFFFFFFFF)
private val LogoDotColor = Color(0xFFE53935)

/** Letter pop delay (ms) – slower sequence (right to left). */
private const val LETTER_DELAY_MS = 450L

/** Hold after full logo before navigating (ms). */
private const val HOLD_BEFORE_NAV_MS = 2200L

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    isLoggedIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showA1 by remember { mutableStateOf(false) }
    var showR1 by remember { mutableStateOf(false) }
    var showA2 by remember { mutableStateOf(false) }
    var showR2 by remember { mutableStateOf(false) }
    var showO by remember { mutableStateOf(false) }

    LaunchedEffect(Unit, isLoggedIn) {
        delay(LETTER_DELAY_MS)
        showA1 = true
        delay(LETTER_DELAY_MS)
        showR1 = true
        delay(LETTER_DELAY_MS)
        showA2 = true
        delay(LETTER_DELAY_MS)
        showR2 = true
        delay(LETTER_DELAY_MS)
        showO = true
        delay(HOLD_BEFORE_NAV_MS)
        if (!isLoggedIn) onNavigateToLogin()
    }

    val floatOffset by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(modifier = modifier.fillMaxSize()) {
        StarryNightBackground(modifier = Modifier.fillMaxSize(), showStars = true, showClouds = false)

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = floatOffset },
            verticalAlignment = Alignment.CenterVertically
        ) {
            LetterPop(text = "A", visible = showA1)
            LetterPop(text = "R", visible = showR1)
            LetterPop(text = "A", visible = showA2)
            LetterPop(text = "R", visible = showR2)
            Spacer(Modifier.width(12.dp))
            RippleO(visible = showO)
        }
    }
}

@Composable
private fun LetterPop(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(100))
    ) {
        Text(
            text = text,
            modifier = modifier,
            fontSize = 56.sp,
            fontWeight = FontWeight.SemiBold,
            color = AraroColors.cream,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun RippleO(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(150, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(100))
    ) {
        val ringSize = 72.dp
        val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(625, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        val pulseAlpha by rememberInfiniteTransition(label = "alpha").animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(625, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            modifier = modifier
                .size(ringSize)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val strokeWidth = ringSize.toPx() * 0.08f
                val radius = (ringSize.toPx() - strokeWidth) / 2f
                drawCircle(
                    color = LogoRingColor.copy(alpha = pulseAlpha),
                    radius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
            Box(
                modifier = Modifier
                    .size(ringSize * 0.31f)
                    .background(LogoDotColor, CircleShape)
            )
        }
    }
}
