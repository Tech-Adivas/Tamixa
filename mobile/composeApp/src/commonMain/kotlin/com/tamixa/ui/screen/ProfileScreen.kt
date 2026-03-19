package com.tamixa.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.state.UiState
import com.tamixa.ui.state.dataOrNull
import com.tamixa.domain.CurrentUser

/**
 * Unified Profile hub per MVP blueprint: My Voices, My Avatars, Favorite Stories,
 * Listening History, Premium Subscription, Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userState: UiState<CurrentUser>,
    onRetryLoadUser: () -> Unit,
    onUpdateProfile: (nickname: String?, displayName: String?) -> Unit,
    onNavigateToMyVoiceAndAvatar: () -> Unit,
    onNavigateToVoiceUpload: () -> Unit,
    onNavigateToAvatarUpload: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToListeningHistory: () -> Unit,
    onNavigateToShortContent: () -> Unit,
    onNavigateToSendStory: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onBack: () -> Unit
) {
    val user = userState.dataOrNull()
    val displayName = user?.displayNameOrFallback(Strings.profile()) ?: Strings.profile()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.profile(),
                onBack = onBack,
                useTransparentBackground = true
            )
        },
        bottomBar = {
            TamixaBottomBar(
                selectedTab = TamixaTab.Profile,
                onHome = onNavigateToHome,
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = { } // No-op when already on Profile
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = TamixaDesignTokens.screenPadding,
                        top = TamixaDesignTokens.screenPadding,
                        end = TamixaDesignTokens.screenPadding,
                        bottom = 0.dp
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val colorScheme = MaterialTheme.colorScheme
                Spacer(Modifier.height(8.dp))
                when (userState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TamixaColors.cream)
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = userState.message.ifBlank { Strings.somethingWentWrong() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaColors.cream.copy(alpha = 0.9f)
                        )
                        TextButton(onClick = onRetryLoadUser) {
                            Text(
                                text = Strings.retry(),
                                color = TamixaColors.goldAccent
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TamixaColors.cream
                        )
                    }
                }
                Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
                user?.let { ProfileDetailsCard(user = it, onUpdateProfile = onUpdateProfile) }
                if (user != null) Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
                ProfileMenuItem(
                    icon = Icons.Filled.Mic,
                    label = Strings.tabMyVoiceAndAvatar(),
                    onClick = onNavigateToMyVoiceAndAvatar
                )
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = Strings.sendStory(),
                    onClick = onNavigateToSendStory
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Favorite,
                    label = Strings.favorites(),
                    onClick = onNavigateToFavorites
                )
                ProfileMenuItem(
                    icon = Icons.Filled.History,
                    label = Strings.listeningHistory(),
                    onClick = onNavigateToListeningHistory
                )
                ProfileMenuItem(
                    icon = Icons.Filled.School,
                    label = Strings.funAndLearn(),
                    onClick = onNavigateToShortContent
                )
                ProfileMenuItem(
                    icon = Icons.Filled.EmojiEvents,
                    label = Strings.achievements(),
                    onClick = onNavigateToAchievements
                )
                ProfileMenuItem(
                    icon = Icons.Filled.WorkspacePremium,
                    label = Strings.premiumSubscription(),
                    onClick = onNavigateToSubscription
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Settings,
                    label = Strings.settings(),
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun ProfileDetailsCard(
    user: CurrentUser,
    onUpdateProfile: (nickname: String?, displayName: String?) -> Unit
) {
    val hasProfileData = user.nickname?.trim()?.isNotBlank() == true ||
        user.displayName?.trim()?.isNotBlank() == true
    var isEditing by remember { mutableStateOf(!hasProfileData) }
    var nickname by remember(user.nickname, isEditing) { mutableStateOf(user.nickname ?: "") }
    var displayName by remember(user.displayName, isEditing) { mutableStateOf(user.displayName ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
        colors = TamixaCardColors.surface(),
        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TamixaDesignTokens.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TamixaColors.goldAccent
                    )
                    Text(
                        text = Strings.personalDetails(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TamixaContentColors.cardPrimary()
                    )
                }
                if (isEditing) {
                    TextButton(onClick = { isEditing = false }) {
                        Text(text = Strings.cancel(), color = TamixaColors.goldAccent)
                    }
                } else {
                    TextButton(onClick = { isEditing = true }) {
                        Text(text = Strings.edit(), color = TamixaColors.goldAccent)
                    }
                }
            }
            if (isEditing) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(Strings.nickname()) },
                    placeholder = { Text(Strings.nicknameHint()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TamixaColors.goldAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = TamixaColors.goldAccent,
                        cursorColor = TamixaColors.goldAccent,
                        focusedTextColor = TamixaContentColors.cardPrimary(),
                        unfocusedTextColor = TamixaContentColors.cardPrimary()
                    )
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(Strings.name()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TamixaColors.goldAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = TamixaColors.goldAccent,
                        cursorColor = TamixaColors.goldAccent,
                        focusedTextColor = TamixaContentColors.cardPrimary(),
                        unfocusedTextColor = TamixaContentColors.cardPrimary()
                    )
                )
                TamixaPrimaryButton(
                    onClick = {
                        onUpdateProfile(
                            nickname.trim().takeIf { it.isNotBlank() },
                            displayName.trim().takeIf { it.isNotBlank() }
                        )
                        isEditing = false
                    },
                    text = Strings.save(),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ProfileDetailRow(label = Strings.nickname(), value = user.nickname?.trim()?.takeIf { it.isNotBlank() })
                ProfileDetailRow(label = Strings.name(), value = user.displayName?.trim()?.takeIf { it.isNotBlank() })
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TamixaContentColors.cardPrimary().copy(alpha = 0.8f)
        )
        Text(
            text = value ?: Strings.notSet(),
            style = MaterialTheme.typography.bodyMedium,
            color = TamixaContentColors.cardPrimary()
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = TamixaColors.goldAccent.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = TamixaColors.goldAccent
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = TamixaContentColors.cardPrimary(),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = TamixaContentColors.cardPrimary().copy(alpha = 0.6f)
            )
        }
    }
}
