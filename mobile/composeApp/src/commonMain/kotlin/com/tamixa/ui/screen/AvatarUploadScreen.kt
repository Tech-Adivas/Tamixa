package com.tamixa.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.ui.components.StoryCard
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.*
import com.tamixa.ui.components.TamixaEmojiDisplay
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarUploadScreen(
    uploadState: UiState<*>,
    avatarUrl: String?,
    avatarStories: List<Story>,
    onUpload: (ByteArray, String) -> Unit,
    onPickImage: () -> Unit,
    onDelete: () -> Unit,
    onLoadAvatar: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onBack: () -> Unit,
    apiBaseUrl: String? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToAvatar: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMyVoiceAndAvatarHub: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToShortContent: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    LaunchedEffect(Unit) { onLoadAvatar() }
    LaunchedEffect(uploadState) {
        if (uploadState is UiState.Success) onLoadAvatar()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenBackground(showClouds = false)
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                com.tamixa.ui.components.TamixaScreenTopBar(
                    title = Strings.tabMyVoiceAndAvatar(),
                    onBack = onBack,
                    useTransparentBackground = true
                )
            },
            bottomBar = {
                TamixaBottomBar(
                    selectedTab = TamixaTab.MyVoiceAndAvatar,
                    onHome = onNavigateToHome,
                    onLibrary = onNavigateToLibrary,
                    onFunAndLearn = onNavigateToShortContent,
                    onProfile = onNavigateToProfile
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = TamixaDesignTokens.screenPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav)
            ) {
                Text(
                    text = Strings.avatarScreenHeadline(),
                    style = MaterialTheme.typography.titleLarge,
                    color = TamixaColors.cream
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Strings.avatarScreenSubline(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamixaColors.lavenderGlow.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (avatarUrl == null) onPickImage else ({}),
                    shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                if (avatarUrl != null) {
                    val resolvedUrl = com.tamixa.network.ApiConfig.resolveCoverUrl(apiBaseUrl ?: "", avatarUrl) ?: avatarUrl
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = resolvedUrl,
                            contentDescription = Strings.yourAvatar(),
                            modifier = Modifier.size(160.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = Strings.removeAvatar(), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.removeAvatar())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = Strings.avatarCtaAddPhoto(),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(Strings.avatarCtaAddPhoto(), style = MaterialTheme.typography.titleMedium, color = TamixaContentColors.cardPrimary())
                            Text(Strings.avatarTellsStoriesHint(), style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary())
                        }
                    }
                }
            }
            when (uploadState) {
                is UiState.Idle -> {}
                is UiState.Loading -> {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is UiState.Success -> {
                    val successData = (uploadState as UiState.Success<String>).data
                    if (successData.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            colors = TamixaCardColors.primaryContainer(),
                            shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                        ) {
                            Text(
                                Strings.avatarUploadSuccess(),
                                color = TamixaContentColors.onPrimaryContainer(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = TamixaCardColors.errorContainer(),
                        shape = RoundedCornerShape(TamixaDesignTokens.inputRadius)
                    ) {
                        Text(
                            (uploadState as UiState.Error).message,
                            color = TamixaContentColors.onErrorContainer(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = Strings.avatarSectionStoriesWithAvatar(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TamixaColors.cream
                )
                Spacer(Modifier.height(12.dp))
                if (avatarStories.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            Strings.avatarSectionNoStoriesYet(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary(),
                            modifier = Modifier.padding(20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(avatarStories.take(10), key = { it.id }) { story ->
                            Box(modifier = Modifier.width(140.dp)) {
                                StoryCard(
                                    story = story,
                                    onClick = { if (story.status == StoryStatus.READY) onStoryClick(story) },
                                    apiBaseUrl = apiBaseUrl
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📖",
                                fontSize = 52.sp
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            Strings.avatarTellsStoriesDescription(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
