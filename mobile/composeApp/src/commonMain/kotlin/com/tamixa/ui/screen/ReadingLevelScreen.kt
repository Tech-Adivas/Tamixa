package com.tamixa.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.network.ReadingLevelDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingLevelScreen(
    readingLevel: ReadingLevelDto?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.readingLevelTitle(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TamixaColors.goldAccent)
                }
                error != null -> Column(
                    Modifier.fillMaxSize().padding(TamixaDesignTokens.screenPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TamixaColors.cream.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onRetry) { Text(Strings.retry(), color = TamixaColors.goldAccent) }
                }
                readingLevel != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(TamixaDesignTokens.screenPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                    verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
                ) {
                    // Current level hero card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📚",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = Strings.readingLevelNumber(readingLevel.level),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = TamixaColors.goldAccent
                            )
                            Text(
                                text = Strings.readingLevelOutOf(10),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TamixaContentColors.cardSecondary()
                            )
                            LinearProgressIndicator(
                                progress = { readingLevel.level / 10f },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = TamixaColors.goldAccent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(TamixaDesignTokens.cardContentPadding),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Strings.readingLevelLastUpdated(), style = MaterialTheme.typography.bodyMedium, color = TamixaContentColors.cardSecondary())
                            Text(readingLevel.updatedAt.take(10), style = MaterialTheme.typography.bodyMedium, color = TamixaContentColors.cardPrimary())
                        }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        Strings.readingLevelNoDataYet(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TamixaColors.cream.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(TamixaDesignTokens.screenPadding),
                    )
                }
            }
        }
    }
}
