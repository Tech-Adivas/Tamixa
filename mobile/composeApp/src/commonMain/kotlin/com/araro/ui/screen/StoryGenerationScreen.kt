package com.araro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.araro.domain.GenerateStoryRequest
import com.araro.ui.state.UiState
import com.araro.ui.strings.Strings
import com.araro.ui.components.AraroPrimaryButton
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignColors
import com.araro.ui.theme.AraroDesignTokens

private val STORY_THEMES = listOf("Adventure", "Fantasy", "Animals", "Nature", "Space", "Bedtime")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryGenerationScreen(
    generateState: UiState<*>,
    children: List<com.araro.domain.Child>,
    onGenerate: (GenerateStoryRequest) -> Unit,
    onBack: () -> Unit,
    onStoryGenerated: () -> Unit,
    showSuccessModal: Boolean = false,
    onDismissSuccess: () -> Unit = {}
) {
    var theme by remember { mutableStateOf("Adventure") }
    var childName by remember { mutableStateOf(children.firstOrNull()?.name ?: "") }
    var selectedPricing by remember { mutableStateOf(0) }
    var childExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    val age = (children.find { it.name == childName }?.age ?: com.araro.util.AraroConstants.DEFAULT_CHILD_AGE).toString()
    val childId = children.find { it.name == childName }?.id
    val language = com.araro.util.AraroConstants.DEFAULT_LANGUAGE
    val scrollState = rememberScrollState()

    LaunchedEffect(children) {
        if (childName.isEmpty() && children.isNotEmpty()) childName = children.first().name
    }


    Box(modifier = Modifier.fillMaxSize()) {
        StarryNightBackground()
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                com.araro.ui.components.AraroScreenTopBar(
                    title = Strings.generateNewStory(),
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
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { childExpanded = true }
                ) {
                    OutlinedTextField(
                        value = childName.ifBlank { Strings.selectChild() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.childFirstName()) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AraroColors.inputSurface,
                            unfocusedContainerColor = AraroColors.inputSurface,
                            focusedTextColor = AraroColors.onInputSurface,
                            unfocusedTextColor = AraroColors.onInputSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(
                        expanded = childExpanded,
                        onDismissRequest = { childExpanded = false }
                    ) {
                        children.forEach { child ->
                            DropdownMenuItem(
                                text = { Text(child.name) },
                                onClick = {
                                    childName = child.name
                                    childExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeExpanded = true }
                ) {
                    OutlinedTextField(
                        value = theme,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.selectStoryTheme()) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AraroColors.inputSurface,
                            unfocusedContainerColor = AraroColors.inputSurface,
                            focusedTextColor = AraroColors.onInputSurface,
                            unfocusedTextColor = AraroColors.onInputSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        STORY_THEMES.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    theme = t
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text(
                    text = Strings.chooseYourPlan(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                PricingOptionCard(
                    title = "\$299",
                    strikethrough = "\$349",
                    subtitle = "${Strings.oneTime()} (583 coins)",
                    selected = selectedPricing == 0,
                    onClick = { selectedPricing = 0 }
                )
                Spacer(Modifier.height(12.dp))
                PricingOptionCard(
                    title = "\$199",
                    strikethrough = "\$11.39",
                    subtitle = "Special bounty (2209 points)",
                    selected = selectedPricing == 1,
                    onClick = { selectedPricing = 1 }
                )
                Spacer(Modifier.height(24.dp))

                when (generateState) {
                    is UiState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(Strings.loading(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                    }
                    is UiState.Error -> {
                        Text(
                            (generateState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    else -> {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AraroPrimaryButton(
                        onClick = {
                            onGenerate(
                                GenerateStoryRequest(
                                    age = age.toIntOrNull() ?: 5,
                                    language = language,
                                    theme = theme.lowercase(),
                                    childName = childName.ifBlank { Strings.child() },
                                    childId = childId
                                )
                            )
                        },
                        text = Strings.proceed(),
                        modifier = Modifier.weight(1f),
                        enabled = childName.isNotBlank(),
                        loading = generateState is UiState.Loading
                    )
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)
                    ) {
                        Text(Strings.option())
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

@Composable
private fun PricingOptionCard(
    title: String,
    strikethrough: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(AraroDesignTokens.inputRadius))
                else Modifier
            ),
        shape = RoundedCornerShape(AraroDesignTokens.inputRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (strikethrough.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "($strikethrough)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                        color = AraroColors.goldAccent.copy(alpha = 0.6f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 24f
                    )
                ),
                modifier = Modifier.padding(bottom = 16.dp)
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
                    .height(56.dp),
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AraroDesignColors.softBlue,
                    contentColor = Color.White
                )
            ) {
                Text(Strings.exploreStories(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
