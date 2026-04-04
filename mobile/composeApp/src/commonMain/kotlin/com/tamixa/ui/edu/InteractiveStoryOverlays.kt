package com.tamixa.ui.edu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.ui.strings.Strings

private fun normalizeOverlayStyle(raw: String?): String =
    raw?.trim()?.uppercase().orEmpty()

/** WhatsApp-chat-style choice bubbles for scam / safety pilots; [overlayStyle] from interactive graph JSON. */
@Composable
fun InteractiveChoiceOverlay(
    choices: List<InteractiveChoice>,
    onChoice: (InteractiveChoice) -> Unit,
    modifier: Modifier = Modifier,
    overlayStyle: String? = null,
) {
    val style = normalizeOverlayStyle(overlayStyle).ifBlank { "WHATSAPP_CHAT" }
    val scrimAlpha = if (style == "RESEARCH_CARDS" || style == "CARDS") 0.62f else 0.55f
    val bubbleColor =
        if (style == "RESEARCH_CARDS" || style == "CARDS") Color(0xFFE8EEF5) else Color(0xFFDCF8C6)
    val bubbleText = if (style == "RESEARCH_CARDS" || style == "CARDS") Color(0xFF0D1B2A) else Color(0xFF111111)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = Strings.interactiveStoryChoose(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            choices.forEach { c ->
                Surface(
                    shape = RoundedCornerShape(if (style == "RESEARCH_CARDS" || style == "CARDS") 12.dp else 18.dp),
                    color = bubbleColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChoice(c) },
                ) {
                    Text(
                        text = c.label,
                        modifier = Modifier.padding(14.dp),
                        color = bubbleText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun MissionCardOverlay(
    missionText: String?,
    resourceUrl: String?,
    onDismiss: () -> Unit,
    onOpenResource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E2A32),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = Strings.familyMissionTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = Strings.missionCardChallengeHint(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll),
                ) {
                    missionText?.trim()?.takeIf { it.isNotBlank() }?.let { t ->
                        Text(
                            text = t,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                resourceUrl?.trim()?.takeIf { it.isNotBlank() }?.let { url ->
                    Button(
                        onClick = { onOpenResource(url) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(Strings.openLinkedResource())
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(Strings.missionCardDone())
                }
            }
        }
    }
}
