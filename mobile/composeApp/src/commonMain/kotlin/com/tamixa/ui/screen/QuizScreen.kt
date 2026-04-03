package com.tamixa.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamixa.network.QuizDto
import com.tamixa.network.QuizResultDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quiz: QuizDto?,
    result: QuizResultDto?,
    loading: Boolean,
    error: String?,
    /** From navigation; must be > 0 to submit answers to the API. */
    quizChildId: Long = 0L,
    onSubmit: (answers: Map<String, String>) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val answers = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.storyQuiz(),
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
                    TextButton(onClick = onBack) { Text(Strings.back(), color = TamixaColors.goldAccent) }
                }
                quiz != null && quizChildId <= 0L -> Column(
                    Modifier.fillMaxSize().padding(TamixaDesignTokens.screenPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        Strings.storyQuizNeedsChildProfile(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TamixaContentColors.cardSecondary()
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) { Text(Strings.close(), color = TamixaColors.goldAccent) }
                }
                result != null -> QuizResultView(result = result, onDone = onDone)
                quiz != null -> QuizQuestionsView(
                    quiz = quiz,
                    answers = answers,
                    onSubmit = { onSubmit(answers.toMap()) }
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        Strings.storyQuizNoQuizAvailable(),
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

@Composable
private fun QuizQuestionsView(
    quiz: QuizDto,
    answers: MutableMap<String, String>,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TamixaDesignTokens.screenPadding)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing)
    ) {
        Text(
            text = Strings.storyQuizQuestionCount(quiz.questions.size),
            style = MaterialTheme.typography.bodySmall,
            color = TamixaContentColors.cardSecondary()
        )

        quiz.questions.forEachIndexed { index, question ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                colors = TamixaCardColors.surface(),
                elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
            ) {
                Column(
                    modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${question.text}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TamixaContentColors.cardPrimary()
                    )
                    if (question.options != null) {
                        question.options.forEach { option ->
                            val selected = answers[question.id] == option
                            OutlinedButton(
                                onClick = { answers[question.id] = option },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) TamixaColors.goldAccent.copy(alpha = 0.15f)
                                    else androidx.compose.ui.graphics.Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) TamixaColors.goldAccent else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text(
                                    text = option,
                                    color = if (selected) TamixaColors.goldAccent else TamixaContentColors.cardPrimary()
                                )
                            }
                        }
                    } else {
                        // Free text answer
                        OutlinedTextField(
                            value = answers[question.id] ?: "",
                            onValueChange = { answers[question.id] = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(Strings.storyQuizYourAnswerPlaceholder()) },
                            singleLine = true
                        )
                    }
                }
            }
        }

        val allAnswered = quiz.questions.all { answers.containsKey(it.id) }
        Button(
            onClick = onSubmit,
            enabled = allAnswered,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
        ) {
            Text(Strings.storyQuizSubmit())
        }
    }
}

@Composable
private fun QuizResultView(result: QuizResultDto, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TamixaDesignTokens.screenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val emoji = when {
            result.percentage >= 90 -> "🏆"
            result.percentage >= 70 -> "⭐"
            result.percentage >= 50 -> "👍"
            else -> "📚"
        }
        Text(emoji, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${result.percentage}%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = TamixaColors.goldAccent
        )
        Text(
            text = Strings.storyQuizScoreLine(result.score, result.maxScore),
            style = MaterialTheme.typography.titleMedium,
            color = TamixaContentColors.cardSecondary()
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
        ) {
            Text(Strings.storyQuizDone())
        }
    }
}
