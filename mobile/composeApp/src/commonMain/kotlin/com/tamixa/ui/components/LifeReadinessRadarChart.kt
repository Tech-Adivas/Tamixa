package com.tamixa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamixa.domain.LIFE_READINESS_AXIS_ORDER
import com.tamixa.domain.LifeReadinessSnapshot
import com.tamixa.domain.valueForAxis
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Lightweight pentagon radar (Compose). Geometry mirrors the web SVG chart in
 * [web/src/components/LifeReadinessRadarChart.tsx].
 */
@Composable
fun LifeReadinessRadarChart(
    snapshot: LifeReadinessSnapshot,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
) {
    val n = LIFE_READINESS_AXIS_ORDER.size
    val values = remember(snapshot) {
        LIFE_READINESS_AXIS_ORDER.map { valueForAxis(snapshot, it.id) / 100f }.map { it.coerceIn(0f, 1f) }
    }
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val maxR = min(w, h) * 0.36f
        val baseAngle = -Math.PI / 2.0

        fun point(angle: Double, r: Float): Offset {
            return Offset(
                cx + (r * cos(angle)).toFloat(),
                cy + (r * sin(angle)).toFloat(),
            )
        }

        val rings = listOf(0.25f, 0.5f, 0.75f, 1f)
        for (t in rings) {
            val path = Path().apply {
                for (i in 0 until n) {
                    val ang = baseAngle + i * 2.0 * Math.PI / n
                    val p = point(ang, maxR * t)
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.14f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        for (i in 0 until n) {
            val ang = baseAngle + i * 2.0 * Math.PI / n
            val end = point(ang, maxR)
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(cx, cy),
                end = end,
                strokeWidth = 1.dp.toPx(),
            )
        }

        val dataPath = Path().apply {
            for (i in 0 until n) {
                val ang = baseAngle + i * 2.0 * Math.PI / n
                val r = maxR * values[i]
                val p = point(ang, r)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(
            path = dataPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF128C7E).copy(alpha = 0.45f),
                    Color(0xFF34B7F1).copy(alpha = 0.35f),
                ),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
            style = Fill,
        )
        drawPath(
            path = dataPath,
            color = Color(0xFF80CBC4).copy(alpha = 0.95f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
