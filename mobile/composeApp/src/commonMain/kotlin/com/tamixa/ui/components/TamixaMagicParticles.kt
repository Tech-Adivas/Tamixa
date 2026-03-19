package com.tamixa.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.ic_magic_particles
import com.tamixa.composeapp.generated.resources.ic_magic_particles_tamil
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Variant for magic particles: default (programmatic) or Tamil Nadu style (image overlay). */
enum class MagicParticlesVariant {
    /** Default sparkles, stars, dust. */
    DEFAULT,
    /** Tamil Nadu aesthetic: saffron, diya glow, South Indian palette. */
    TAMIL_NADU
}

private val SparkleGold = Color(0xFFFFE082)
private val SparkleLavender = Color(0xFFD4CFE8)
private val SparkleMint = Color(0xFFB5EAD7)
private val SparkleWarm = Color(0xFFFFDAB9)

// Tamil Nadu palette: saffron, turmeric, diya glow
private val TamilSaffron = Color(0xFFFF9933)
private val TamilTurmeric = Color(0xFFE4A11B)
private val TamilDiyaGlow = Color(0xFFFFE082)
private val TamilJasmine = Color(0xFFF8F8FF)

/**
 * Magical glowing particles for storytelling app.
 * Use for: story transitions, UI effects, loading animation.
 *
 * @param variant DEFAULT = programmatic particles; TAMIL_NADU = image overlay for South Indian audience.
 */
@Composable
fun TamixaMagicParticles(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    density: Float = 1f,
    variant: MagicParticlesVariant = MagicParticlesVariant.DEFAULT
) {
    when (variant) {
        MagicParticlesVariant.DEFAULT -> TamixaMagicParticlesProgrammatic(
            modifier = modifier,
            animate = animate,
            density = density
        )
        MagicParticlesVariant.TAMIL_NADU -> TamixaMagicParticlesImage(
            modifier = modifier,
            variant = variant
        )
    }
}

@Composable
internal fun TamixaMagicParticlesProgrammatic(
    modifier: Modifier,
    animate: Boolean,
    density: Float
) {
    val particles = remember { generateMagicParticles((40 * density).toInt()) }
    val infiniteTransition = rememberInfiniteTransition(label = "magicParticles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            particles.forEachIndexed { index, p ->
                val cx = p.x * w
                val cy = p.y * h
                val driftX = if (animate) sin(phase * 2 * PI.toFloat() + p.phaseOffset) * p.drift * w * 0.03f else 0f
                val driftY = if (animate) cos(phase * 2 * PI.toFloat() * 0.7f + p.phaseOffset * 1.3f) * p.drift * h * 0.02f else 0f
                val center = Offset(cx + driftX, cy + driftY)
                val twinkle = if (animate) {
                    0.4f + 0.6f * (0.5f + 0.5f * sin(twinklePhase * 2 * PI.toFloat() + index * 0.7f))
                } else 1f
                val alpha = p.baseAlpha * twinkle
                when (p.type) {
                    ParticleType.SPARKLE -> drawSparkle(center, p.size, p.color, alpha)
                    ParticleType.STAR -> drawGlowingStar(center, p.size, p.color, alpha)
                    ParticleType.DUST -> drawMagicDust(center, p.size, p.color, alpha)
                }
            }
        }
    }
}

@Composable
internal fun TamixaMagicParticlesImage(
    modifier: Modifier,
    variant: MagicParticlesVariant
) {
    val drawableRes = when (variant) {
        MagicParticlesVariant.DEFAULT -> Res.drawable.ic_magic_particles
        MagicParticlesVariant.TAMIL_NADU -> Res.drawable.ic_magic_particles_tamil
    }
    Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

private data class MagicParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val type: ParticleType,
    val color: Color,
    val baseAlpha: Float,
    val phaseOffset: Float,
    val drift: Float
)

private enum class ParticleType { SPARKLE, STAR, DUST }

private fun generateMagicParticles(count: Int): List<MagicParticle> {
    val r = Random(0x5C4D) // Stable seed
    val colors = listOf(SparkleGold, SparkleLavender, SparkleMint, SparkleWarm)
    return (0 until count).map {
        MagicParticle(
            x = r.nextFloat(),
            y = r.nextFloat(),
            size = r.nextFloat() * 0.015f + 0.005f,
            type = when (r.nextInt(3)) {
                0 -> ParticleType.SPARKLE
                1 -> ParticleType.STAR
                else -> ParticleType.DUST
            },
            color = colors[r.nextInt(colors.size)],
            baseAlpha = r.nextFloat() * 0.4f + 0.3f,
            phaseOffset = r.nextFloat() * 2 * PI.toFloat(),
            drift = r.nextFloat() * 0.5f + 0.5f
        )
    }
}

private fun DrawScope.drawSparkle(center: Offset, sizeRatio: Float, color: Color, alpha: Float) {
    val baseRadius = minOf(size.width, size.height) * sizeRatio
    val colorWithAlpha = color.copy(alpha = alpha)
    drawCircle(color = colorWithAlpha.copy(alpha = alpha * 0.25f), radius = baseRadius * 2.5f, center = center)
    drawCircle(color = colorWithAlpha, radius = baseRadius, center = center)
}

private fun DrawScope.drawGlowingStar(center: Offset, sizeRatio: Float, color: Color, alpha: Float) {
    val r = minOf(size.width, size.height) * sizeRatio
    val colorWithAlpha = color.copy(alpha = alpha)
    drawCircle(color = colorWithAlpha.copy(alpha = alpha * 0.2f), radius = r * 3f, center = center)
    drawCircle(color = colorWithAlpha, radius = r * 1.2f, center = center)
    drawCircle(color = colorWithAlpha.copy(alpha = alpha * 0.7f), radius = r * 0.5f, center = center)
}

private fun DrawScope.drawMagicDust(center: Offset, sizeRatio: Float, color: Color, alpha: Float) {
    val r = minOf(size.width, size.height) * sizeRatio * 0.6f
    drawCircle(color = color.copy(alpha = alpha * 0.6f), radius = r, center = center)
}
