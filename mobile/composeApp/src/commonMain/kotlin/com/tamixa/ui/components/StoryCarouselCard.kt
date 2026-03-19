package com.tamixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaGradients

/**
 * Poster-style card for horizontal carousels.
 * Cover-dominant with gradient overlay, title overlay at bottom.
 */
@Composable
fun StoryCarouselCard(
    story: Story,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    apiBaseUrl: String? = null,
    width: Dp = 160.dp,
    height: Dp = 232.dp,
    /** Progress 0f-1f for Continue Listening; null = no progress bar */
    progress: Float? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val carouselShape = RoundedCornerShape(TamixaDesignTokens.carouselCardRadius)
    Card(
        onClick = { if (story.status == StoryStatus.READY) onClick() },
        modifier = modifier
            .width(width)
            .height(height)
            .then(
                Modifier.shadow(
                    elevation = 6.dp,
                    shape = carouselShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
            ),
        shape = carouselShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        val heightPx = with(LocalDensity.current) { height.toPx() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            StoryCoverImage(
                story = story,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(carouselShape),
                contentScale = ContentScale.Crop,
                apiBaseUrl = apiBaseUrl
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(
                        Brush.verticalGradient(
                            colors = TamixaGradients.imageOverlay,
                            startY = 0f,
                            endY = heightPx
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = story.title?.takeIf { it.isNotBlank() } ?: story.theme,
                    style = MaterialTheme.typography.titleSmall.copy(
                        letterSpacing = (-0.1).sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (progress != null && progress in 0f..1f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = TamixaColors.goldAccent
                    )
                }
            }
        }
    }
}
