package com.tamixa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.ui.theme.TamixaColors

/** Purple ring + gold inner dot from brand spec. */
private val LogoRingColor = Color(0xFFFFFFFF)
private val LogoDotColor = Color(0xFFE53935)

/**
 * Tamixa logo: brand text + ripple O (purple ring, gold dot).
 * Used on login, settings, and language selection.
 */
@Composable
fun TamixaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    textColor: Color = TamixaColors.cream,
    alignment: Alignment = Alignment.Center
) {
    val isCompact = size < 56.dp
    val textSize = if (isCompact) 18.sp else (size.value * 0.4f).sp
    val ringSize = if (isCompact) size * 0.5f else size * 0.4f

    Box(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tamixa",
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.width(if (isCompact) 4.dp else 8.dp))
            // Ripple O: outer ring + inner dot
            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(ringSize)) {
                    val strokeWidth = ringSize.toPx() * 0.09f
                    val radius = (ringSize.toPx() - strokeWidth) / 2f
                    drawCircle(
                        color = LogoRingColor,
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(ringSize * 0.32f)
                        .background(LogoDotColor, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}
