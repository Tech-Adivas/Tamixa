package com.tamixa.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.network.ChildVocabularyDto
import com.tamixa.network.VocabularyProgressDto
import com.tamixa.network.VocabularyWordDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.*

private val masteryColors = @Composable { level: Int ->
    when (level) {
        3 -> TamixaColors.goldAccent
        2 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        1 -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.outline
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    progress: VocabularyProgressDto?,
    learnedWords: List<ChildVocabularyDto>,
    suggestions: List<VocabularyWordDto>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onMarkLearned: (wordId: Long) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.vocabularyTitle(),
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
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(TamixaDesignTokens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing),
                    contentPadding = PaddingValues(bottom = TamixaDesignTokens.screenPaddingBottomWithNav)
                ) {
                    // Progress card
                    progress?.let { p ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                                colors = TamixaCardColors.surface(),
                                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                            ) {
                                Column(Modifier.padding(TamixaDesignTokens.cardContentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(Strings.vocabularyProgress(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TamixaContentColors.cardPrimary())
                                    LinearProgressIndicator(
                                        progress = { p.progressPercentage / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = TamixaColors.goldAccent,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(Strings.vocabularyWordsLearned(p.wordsLearned), style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary())
                                        Text(Strings.vocabularyWordsMastered(p.wordsMastered), style = MaterialTheme.typography.bodySmall, color = TamixaColors.goldAccent)
                                    }
                                }
                            }
                        }
                    }

                    // Learned words
                    if (learnedWords.isNotEmpty()) {
                        item {
                            Text(Strings.vocabularyMyWords(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TamixaContentColors.cardSecondary())
                        }
                        items(learnedWords) { cv ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusMedium),
                                colors = TamixaCardColors.surface(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(TamixaDesignTokens.cardContentPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(cv.word, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TamixaContentColors.cardPrimary())
                                        Text(cv.definition, style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary(), maxLines = 2)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = masteryColors(cv.masteryLevel).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = Strings.vocabularyMasteryLabel(cv.masteryLevel),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = masteryColors(cv.masteryLevel)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Suggestions
                    if (suggestions.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(Strings.vocabularySuggestedWords(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TamixaContentColors.cardSecondary())
                        }
                        items(suggestions) { word ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusMedium),
                                colors = TamixaCardColors.surface(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(TamixaDesignTokens.cardContentPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(word.word, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TamixaContentColors.cardPrimary())
                                        Text(word.definition, style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary(), maxLines = 2)
                                    }
                                    TextButton(onClick = { onMarkLearned(word.id) }) {
                                        Text(Strings.vocabularyLearnAction(), color = TamixaColors.goldAccent, style = MaterialTheme.typography.labelMedium)
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
