package com.tamixa.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.network.ReadingStreakDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(
    streak: ReadingStreakDto?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.readingStreakTitle(),
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
                streak != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(TamixaDesignTokens.screenPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                    verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
                ) {
                    // Current streak hero card
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
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = Strings.readingStreakTitle(),
                                tint = if (streak.isActive) TamixaColors.goldAccent else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "${streak.currentStreak}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = TamixaContentColors.cardPrimary()
                            )
                            Text(
                                text = Strings.streakLengthSubtitle(streak.currentStreak),
                                style = MaterialTheme.typography.titleMedium,
                                color = TamixaContentColors.cardSecondary()
                            )
                            if (streak.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = TamixaColors.goldAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = Strings.streakStatusActive(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TamixaColors.goldAccent
                                    )
                                }
                            }
                        }
                    }

                    // Longest streak
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(TamixaDesignTokens.cardContentPadding),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Strings.streakBestLabel(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TamixaContentColors.cardSecondary()
                            )
                            Text(
                                text = Strings.streakBestDays(streak.longestStreak),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TamixaContentColors.cardPrimary()
                            )
                        }
                    }

                    streak.lastReadAt?.let { lastRead ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                            colors = TamixaCardColors.surface(),
                            elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(TamixaDesignTokens.cardContentPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.streakLastReadLabel(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TamixaContentColors.cardSecondary()
                                )
                                Text(
                                    text = lastRead.take(10), // date portion only
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TamixaContentColors.cardPrimary()
                                )
                            }
                        }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        Strings.streakNoDataYet(),
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
