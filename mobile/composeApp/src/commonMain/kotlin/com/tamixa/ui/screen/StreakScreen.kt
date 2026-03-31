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
import androidx.compose.ui.unit.dp
import com.tamixa.network.ReadingStreakDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaScreenTopBar
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
                title = "Reading Streak",
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showClouds = false)
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TamixaColors.goldAccent)
                }
                error != null -> Column(
                    Modifier.fillMaxSize().padding(TamixaDesignTokens.screenPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onRetry) { Text("Retry", color = TamixaColors.goldAccent) }
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
                                contentDescription = null,
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
                                text = if (streak.currentStreak == 1) "day streak" else "day streak",
                                style = MaterialTheme.typography.titleMedium,
                                color = TamixaContentColors.cardSecondary()
                            )
                            if (streak.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = TamixaColors.goldAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "🔥 Active",
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
                                text = "Best streak",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TamixaContentColors.cardSecondary()
                            )
                            Text(
                                text = "${streak.longestStreak} days",
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
                                    text = "Last read",
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
                    Text("No streak data yet. Start reading!", color = TamixaContentColors.cardSecondary())
                }
            }
        }
    }
}
