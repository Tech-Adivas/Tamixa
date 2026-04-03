package com.tamixa.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.network.ShortContentResponseDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.viewmodel.ShortContentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortContentScreen(
    shortContentViewModel: ShortContentViewModel,
    onBack: () -> Unit,
    languageCode: String = com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE,
    onNavigateToHome: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToShortContent: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val items by shortContentViewModel.items.collectAsState()
    val types by shortContentViewModel.types.collectAsState()
    val loading by shortContentViewModel.loading.collectAsState()
    val error by shortContentViewModel.error.collectAsState()

    var selectedType by remember { mutableStateOf("RIDDLE") }
    val revealedIds = remember { mutableStateMapOf<Long, Boolean>() }

    LaunchedEffect(Unit) {
        shortContentViewModel.loadTypes()
    }
    LaunchedEffect(types) {
        if (types.isNotEmpty() && selectedType !in types) {
            selectedType = types.first()
        }
    }
    LaunchedEffect(selectedType, languageCode) {
        shortContentViewModel.loadList(selectedType, languageCode)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.funAndLearn(),
                onBack = onBack,
                useTransparentBackground = true
            )
        },
        bottomBar = {
            TamixaBottomBar(
                selectedTab = TamixaTab.FunAndLearn,
                onHome = onNavigateToHome,
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        val colorScheme = MaterialTheme.colorScheme
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(
                showStars = true,
                showClouds = true,
                animateStars = true,
                ambientPresence = true
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = TamixaDesignTokens.screenPadding,
                        top = TamixaDesignTokens.screenPadding,
                        end = TamixaDesignTokens.screenPadding,
                        bottom = 0.dp
                    )
            ) {
                if (types.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { type ->
                            val label = Strings.shortContentTypeLabel(type)
                            val isSelected = type == selectedType
                            Card(
                                onClick = { selectedType = type },
                                modifier = Modifier.padding(vertical = 4.dp),
                                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                                colors = if (isSelected) {
                                    CardDefaults.cardColors(
                                        containerColor = TamixaColors.goldAccent,
                                        contentColor = colorScheme.onSecondary
                                    )
                                } else {
                                    TamixaCardColors.surface()
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                when {
                    loading && items.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TamixaColors.goldAccent)
                        }
                    }
                    error != null && items.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TamixaColors.cream.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { shortContentViewModel.loadList(selectedType, languageCode) }) {
                                Text(Strings.retry(), color = TamixaColors.goldAccent)
                            }
                        }
                    }
                    items.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = TamixaDesignTokens.screenPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = Strings.noShortContentYet(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                ),
                                color = TamixaColors.cream,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = Strings.noShortContentHint(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TamixaColors.cream.copy(alpha = 0.88f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        @OptIn(ExperimentalMaterial3Api::class)
                        PullToRefreshBox(
                            isRefreshing = loading,
                            onRefresh = { shortContentViewModel.loadList(selectedType, languageCode) }
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items) { item ->
                                    ShortContentItemCard(
                                        item = item,
                                        isRevealed = revealedIds[item.id] == true,
                                        onTap = {
                                            if (item.answer != null) {
                                                revealedIds[item.id] = !(revealedIds[item.id] ?: false)
                                            }
                                        }
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

@Composable
private fun ShortContentItemCard(
    item: ShortContentResponseDto,
    isRevealed: Boolean,
    onTap: () -> Unit
) {
    val hasAnswer = item.answer != null && item.answer.isNotBlank()
    Card(
        onClick = { if (hasAnswer) onTap() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
        colors = TamixaCardColors.surface(),
        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TamixaDesignTokens.cardContentPadding)
        ) {
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyLarge,
                color = TamixaContentColors.cardPrimary()
            )
            if (hasAnswer) {
                if (isRevealed) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${Strings.answer()}: ${item.answer}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TamixaColors.goldAccent
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = Strings.tapToRevealAnswer(),
                        style = MaterialTheme.typography.labelMedium,
                        color = TamixaContentColors.cardSecondary()
                    )
                }
            }
        }
    }
}
