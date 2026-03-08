package com.araro.ui.screen

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.araro.ui.components.AraroEmojiDisplay
import com.araro.ui.state.UiState
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarUploadScreen(
    uploadState: UiState<*>,
    avatarUrl: String?,
    onUpload: (ByteArray, String) -> Unit,
    onPickImage: () -> Unit,
    onDelete: () -> Unit,
    onLoadAvatar: () -> Unit,
    onBack: () -> Unit,
    apiBaseUrl: String? = null
) {
    LaunchedEffect(Unit) { onLoadAvatar() }
    LaunchedEffect(uploadState) {
        if (uploadState is UiState.Success) onLoadAvatar()
    }

    Scaffold(
        topBar = {
            com.araro.ui.components.AraroScreenTopBar(
                title = Strings.avatarUpload(),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (avatarUrl != null) Modifier
                        else Modifier
                    ),
                onClick = if (avatarUrl == null) onPickImage else ({}),
                shape = RoundedCornerShape(AraroDesignTokens.dialogRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (avatarUrl != null) {
                    val resolvedUrl = com.araro.network.ApiConfig.resolveCoverUrl(apiBaseUrl ?: "", avatarUrl) ?: avatarUrl
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
                            Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.removeAvatar())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(Strings.tapToUploadAvatar(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(Strings.avatarTellsStoriesHint(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            when (uploadState) {
                is UiState.Loading -> {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is UiState.Success -> {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                    ) {
                        Text(
                            Strings.avatarUploadSuccess(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is UiState.Error -> {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(AraroDesignTokens.inputRadius)
                    ) {
                        Text(
                            (uploadState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AraroEmojiDisplay(emoji = "📖", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(Strings.avatarTellsStoriesDescription(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
