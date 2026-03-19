package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.network.AchievementDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<AchievementDto>?,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.achievements(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showClouds = false)
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = com.tamixa.ui.theme.TamixaColors.goldAccent)
                    }
                }
                loadError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(TamixaDesignTokens.screenPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = loadError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = onRetry) {
                            Text(Strings.retry(), color = com.tamixa.ui.theme.TamixaColors.goldAccent)
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(TamixaDesignTokens.screenPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                        verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
                    ) {
                        Text(
                            text = Strings.achievementsSubtitle(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary()
                        )
                        Spacer(Modifier.height(8.dp))
                        (achievements ?: emptyList()).forEach { achievement ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                                colors = TamixaCardColors.surface(),
                                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(TamixaDesignTokens.cardContentPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = if (achievement.earned) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (achievement.earned) com.tamixa.ui.theme.TamixaColors.goldAccent else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = achievement.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TamixaContentColors.cardPrimary()
                                        )
                                        Text(
                                            text = achievement.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TamixaContentColors.cardSecondary()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
