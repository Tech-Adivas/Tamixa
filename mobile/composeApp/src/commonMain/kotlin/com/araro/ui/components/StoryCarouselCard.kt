package com.araro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.araro.domain.Story
import com.araro.domain.StoryStatus
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.theme.AraroGradients

/**
 * Poster-style card for horizontal carousels.
 * Cover-dominant with gradient overlay, title overlay at bottom.
 */
@Composable
fun StoryCarouselCard(
    story: Story,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    apiBaseUrl: String? = null
) {
    val overlayEndY = with(LocalDensity.current) { 210.dp.toPx() }
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = { if (story.status == StoryStatus.READY) onClick() },
        modifier = modifier
            .width(140.dp)
            .height(210.dp),
        shape = RoundedCornerShape(AraroDesignTokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = AraroDesignTokens.cardElevation),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            StoryCoverImage(
                story = story,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(AraroDesignTokens.cardRadius)),
                contentScale = ContentScale.Crop,
                apiBaseUrl = apiBaseUrl
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = AraroGradients.imageOverlay,
                            startY = 0f,
                            endY = overlayEndY
                        )
                    )
            )
            Text(
                text = story.theme,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            )
        }
    }
}
