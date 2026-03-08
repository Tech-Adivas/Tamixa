package com.araro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.araro.domain.Child
import com.araro.ui.components.AraroEmojiDisplay
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.state.UiState
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildListScreen(
    childrenState: UiState<List<Child>>,
    onAddChild: () -> Unit,
    onRetry: () -> Unit,
    onChildClick: (Child) -> Unit,
    onBack: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = com.araro.ui.theme.AraroColors.nightSkyBg,
        topBar = {
            com.araro.ui.components.AraroScreenTopBar(
                title = Strings.children(),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onAddChild) {
                        Icon(Icons.Default.Add, contentDescription = Strings.addChild())
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddChild,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(Strings.addChild()) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StarryNightBackground(showClouds = false)
            when (val state = childrenState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onRetry, shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)) {
                            Text(Strings.retry())
                        }
                    }
                }
                is UiState.Success -> {
                    val list = state.data
                    if (list.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                AraroEmojiDisplay(emoji = "👶🌈")
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    Strings.noChildrenYet(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = com.araro.ui.theme.AraroColors.cream
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    Strings.addFirstChildPrompt(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.araro.ui.theme.AraroColors.cream.copy(alpha = 0.9f)
                                )
                                Spacer(Modifier.height(24.dp))
                                FilledTonalButton(
                                    onClick = onAddChild,
                                    shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(Strings.addChild())
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(list, key = { it.id }) { child ->
                                ChildListItem(
                                    child = child,
                                    onClick = { onChildClick(child) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildListItem(child: Child, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    child.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Age: ${child.age}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
