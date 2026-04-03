package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tamixa.domain.GenerateStoryRequest
import com.tamixa.domain.GenerationTopicResponse
import com.tamixa.network.LimitReachedException
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults

private val STORY_THEMES = listOf(
    "Adventure", "Fantasy", "Animals", "Nature", "Space", "Bedtime",
    "History & heroes", "Festivals", "Science & discovery",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryGenerationScreen(
    generateState: UiState<*>,
    generationTopics: List<GenerationTopicResponse> = emptyList(),
    storiesUsed: Int = 0,
    storiesLimit: Int? = null,
    onGenerate: (GenerateStoryRequest) -> Unit,
    onBack: () -> Unit,
    onStoryGenerated: () -> Unit,
    showSuccessModal: Boolean = false,
    onDismissSuccess: () -> Unit = {},
    onUpgradeRequired: () -> Unit = {},
    onClearGenerateError: () -> Unit = {},
) {
    var theme by remember { mutableStateOf("Adventure") }
    /** When non-null, server uses catalog topic; [theme] chips become optional override text. */
    var selectedTopicId by remember { mutableStateOf<String?>(null) }
    var topicThemeOverride by remember { mutableStateOf("") }
    var listenerName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf(com.tamixa.util.TamixaConstants.DEFAULT_CHILD_AGE.toString()) }
    var parentCustomPrompt by remember { mutableStateOf("") }
    var bedtimeMode by remember { mutableStateOf(false) }
    /** null = no learning-focus hint sent to API. */
    var learningFocusSelection by remember { mutableStateOf<String?>(null) }
    val language = com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE
    val scrollState = rememberScrollState()
    val effectiveListenerName = listenerName.trim().ifBlank { Strings.listener() }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenBackground(showStars = true, showClouds = true, animateStars = false)
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                com.tamixa.ui.components.TamixaScreenTopBar(
                    title = Strings.sendStory(),
                    onBack = onBack,
                    useTransparentBackground = true
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = TamixaDesignTokens.screenPadding)
                    .padding(bottom = 32.dp)
            ) {
                val colorScheme = MaterialTheme.colorScheme
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Strings.sendStoryInTextFormat(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = TamixaColors.cream
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Strings.sendStoryHint(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamixaColors.cream.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (storiesLimit != null) {
                    Surface(
                        shape = RoundedCornerShape(TamixaDesignTokens.inputRadius),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            Strings.freePlanLimit(storiesUsed, storiesLimit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Surface(
                    shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                        Text(
                            text = Strings.yourStoryText(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = parentCustomPrompt,
                            onValueChange = { parentCustomPrompt = it.take(5000) },
                            placeholder = { Text(Strings.storyTextPlaceholder()) },
                            singleLine = false,
                            minLines = 6,
                            maxLines = 16,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = Strings.listenerNameOptional(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = listenerName,
                            onValueChange = { listenerName = it },
                            placeholder = { Text(Strings.listener()) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = Strings.ageForStory(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            placeholder = { Text(com.tamixa.util.TamixaConstants.DEFAULT_CHILD_AGE.toString()) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (generationTopics.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = Strings.curatedTopicsLabel(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTopicId == null,
                                    onClick = {
                                        selectedTopicId = null
                                        topicThemeOverride = ""
                                    },
                                    label = { Text(Strings.curatedTopicCustom(), style = MaterialTheme.typography.labelMedium) }
                                )
                                generationTopics.forEach { topic ->
                                    val label = topic.descriptionEn?.takeIf { it.isNotBlank() } ?: topic.theme
                                    FilterChip(
                                        selected = selectedTopicId == topic.id,
                                        onClick = {
                                            selectedTopicId = topic.id
                                            topic.suggestedLearningFocus?.takeIf { it.isNotBlank() }?.let {
                                                learningFocusSelection = it
                                            }
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                    )
                                }
                            }
                            if (selectedTopicId != null) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = Strings.themeOverrideHint(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = topicThemeOverride,
                                    onValueChange = { topicThemeOverride = it.take(100) },
                                    placeholder = { Text(Strings.storyTextPlaceholder()) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = Strings.selectStoryTheme(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            STORY_THEMES.forEach { t ->
                                FilterChip(
                                    selected = theme == t,
                                    onClick = {
                                        theme = t
                                        selectedTopicId = null
                                        topicThemeOverride = ""
                                    },
                                    enabled = selectedTopicId == null,
                                    label = { Text(t, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        FilterChip(
                            selected = bedtimeMode,
                            onClick = { bedtimeMode = !bedtimeMode },
                            label = { Text(Strings.bedtimeStory(), style = MaterialTheme.typography.labelMedium) }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = Strings.learningFocusOptional(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = learningFocusSelection == null,
                                onClick = { learningFocusSelection = null },
                                label = { Text(Strings.learningFocusNone(), style = MaterialTheme.typography.labelMedium) }
                            )
                            FilterChip(
                                selected = learningFocusSelection == "public_speaking",
                                onClick = { learningFocusSelection = "public_speaking" },
                                label = { Text(Strings.learningFocusPublicSpeaking(), style = MaterialTheme.typography.labelMedium) }
                            )
                            FilterChip(
                                selected = learningFocusSelection == "money_literacy",
                                onClick = { learningFocusSelection = "money_literacy" },
                                label = { Text(Strings.learningFocusMoneyLiteracy(), style = MaterialTheme.typography.labelMedium) }
                            )
                            FilterChip(
                                selected = learningFocusSelection == "research_skills",
                                onClick = { learningFocusSelection = "research_skills" },
                                label = { Text(Strings.learningFocusResearchSkills(), style = MaterialTheme.typography.labelMedium) }
                            )
                            FilterChip(
                                selected = learningFocusSelection == "empathy",
                                onClick = { learningFocusSelection = "empathy" },
                                label = { Text(Strings.learningFocusEmpathy(), style = MaterialTheme.typography.labelMedium) }
                            )
                            FilterChip(
                                selected = learningFocusSelection == "problem_solving",
                                onClick = { learningFocusSelection = "problem_solving" },
                                label = { Text(Strings.learningFocusProblemSolving(), style = MaterialTheme.typography.labelMedium) }
                            )
                            FilterChip(
                                selected = learningFocusSelection == "vocabulary",
                                onClick = { learningFocusSelection = "vocabulary" },
                                label = { Text(Strings.learningFocusVocabulary(), style = MaterialTheme.typography.labelMedium) }
                            )
                            FilterChip(
                                selected = learningFocusSelection == "curiosity",
                                onClick = { learningFocusSelection = "curiosity" },
                                label = { Text(Strings.learningFocusCuriosity(), style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        when (generateState) {
                            is UiState.Loading -> {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .semantics { contentDescription = Strings.loading() },
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(Strings.generatingStory(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                            }
                            is UiState.Error -> {
                                val err = generateState as UiState.Error
                                val isLimitReached = err.throwable is LimitReachedException
                                if (isLimitReached) {
                                    AlertDialog(
                                        onDismissRequest = onClearGenerateError,
                                        shape = TamixaDialogDefaults.shape,
                                        title = { Text(Strings.upgrade()) },
                                        text = {
                                            Column {
                                                Text(Strings.storyLimitReachedUpgradeMessage())
                                                Spacer(Modifier.height(8.dp))
                                                Text(Strings.upgradeToCreateMore(), style = MaterialTheme.typography.bodyMedium)
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    onClearGenerateError()
                                                    onUpgradeRequired()
                                                }
                                            ) { Text(Strings.upgrade()) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = onClearGenerateError) { Text(Strings.cancel()) }
                                        }
                                    )
                                } else {
                                    Text(
                                        err.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            else -> {}
                        }
                        TamixaPrimaryButton(
                            onClick = {
                                val ageInt = age.toIntOrNull()?.coerceIn(1, 99)
                                    ?: com.tamixa.util.TamixaConstants.DEFAULT_CHILD_AGE
                                val topicId = selectedTopicId?.trim()?.takeIf { it.isNotBlank() }
                                val overrideTheme = topicThemeOverride.trim().takeIf { it.isNotBlank() }
                                onGenerate(
                                    if (topicId != null) {
                                        GenerateStoryRequest(
                                            age = ageInt,
                                            language = language,
                                            theme = overrideTheme,
                                            generationTopicId = topicId,
                                            childName = effectiveListenerName,
                                            childId = null,
                                            emotionMode = if (bedtimeMode) "CALM" else null,
                                            parentCustomPrompt = parentCustomPrompt.trim().takeIf { it.isNotBlank() },
                                            learningFocus = learningFocusSelection,
                                        )
                                    } else {
                                        GenerateStoryRequest(
                                            age = ageInt,
                                            language = language,
                                            theme = theme.lowercase(),
                                            generationTopicId = null,
                                            childName = effectiveListenerName,
                                            childId = null,
                                            emotionMode = if (bedtimeMode) "CALM" else null,
                                            parentCustomPrompt = parentCustomPrompt.trim().takeIf { it.isNotBlank() },
                                            learningFocus = learningFocusSelection,
                                        )
                                    }
                                )
                            },
                            text = Strings.sendStory(),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true,
                            loading = generateState is UiState.Loading
                        )
                    }
                }
            }
        }

        if (showSuccessModal) {
            SuccessModal(
                onExploreStories = onDismissSuccess
            )
        }
    }
}

private val SuccessModalOverlay = Color(0xCC1E1F3F)

@Composable
fun SuccessModal(
    onExploreStories: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SuccessModalOverlay
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⭐",
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = TamixaColors.goldAccent.copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 20f
                    )
                ),
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Text(
                text = Strings.success(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = Strings.notPremiumMember(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onExploreStories,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(Strings.exploreStories(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
