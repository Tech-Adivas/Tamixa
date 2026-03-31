package com.tamixa.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.ui.theme.TamixaColors

/**
 * In-app “story host” — soft 3D-style orb with a friendly emoji (default teddy 🧸) so we do not rely
 * on a raster mascot asset. Pass a different [emoji] if you want a variant narrator.
 * Used in onboarding quotes, dashboard, and listening illustrations.
 */
@Composable
fun TamixaMascot(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    emoji: String = "🧸"
) {
    val shadowElev = (size.value / 12f).dp.coerceIn(3.dp, 10.dp)
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = shadowElev,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.09f)
            )
            .clip(CircleShape)
            .drawBehind {
                val r = this.size.minDimension / 2f
                val highlight = Offset(center.x - r * 0.28f, center.y - r * 0.35f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            TamixaColors.goldAccent.copy(alpha = 0.95f),
                            Color(0xFFC4956A),
                            TamixaColors.deepTeal.copy(alpha = 0.58f)
                        ),
                        center = highlight,
                        radius = r * 1.38f
                    ),
                    radius = r
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size.value * 0.46f).sp,
            lineHeight = (size.value * 0.46f).sp
        )
    }
}
