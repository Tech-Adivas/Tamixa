package com.tamixa.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.tamixa.network.LifeSkillCountersResponseDto

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
    /** From a generated story linked to a child; enables reading level, streak, vocabulary, classroom. */
    educationChildId: Long? = null,
    /** Soft pillars from interactive Edu episodes; [lifeSkillChildOptions] from GET /profile + story-linked child. */
    lifeSkillCounters: LifeSkillCountersResponseDto? = null,
    lifeSkillCountersLoading: Boolean = false,
    lifeSkillChildOptions: List<Pair<Long, String>> = emptyList(),
    selectedLifeSkillChildId: Long? = null,
    onLifeSkillChildChange: (Long) -> Unit = {},
    onNavigateToReadingLevel: () -> Unit = {},
    onNavigateToReadingStreak: () -> Unit = {},
    onNavigateToVocabulary: () -> Unit = {},
    onNavigateToClassroom: () -> Unit = {},
    onNavigateToLifeReadiness: () -> Unit = {},
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
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.sectionSpacing),
            ) {
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
                        // User info section with premium styling
                        user?.let { 
                            ProfileHeaderCard(
                                user = it, 
                                onUpdateProfile = onUpdateProfile
                            ) 
                        }
                    }
                }
                
                // Learning Progress Section
                if (educationChildId != null && educationChildId > 0L) {
                    ProfileSectionCard(
                        title = Strings.profileLearningProgressSection(),
                        icon = Icons.AutoMirrored.Filled.MenuBook
                    ) {
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = Strings.readingLevelTitle(),
                            onClick = onNavigateToReadingLevel,
                            compact = true
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                            color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.LocalFireDepartment,
                            label = Strings.readingStreakTitle(),
                            onClick = onNavigateToReadingStreak,
                            compact = true
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                            color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.Abc,
                            label = Strings.vocabularyTitle(),
                            onClick = onNavigateToVocabulary,
                            compact = true
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                            color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.Groups,
                            label = Strings.myClassrooms(),
                            onClick = onNavigateToClassroom,
                            compact = true
                        )
                    }
                }
                
                // Life Skills Practice Section
                if (lifeSkillChildOptions.isNotEmpty() &&
                    selectedLifeSkillChildId != null &&
                    selectedLifeSkillChildId > 0L &&
                    (lifeSkillCountersLoading || lifeSkillCounters != null)
                ) {
                    LifeSkillPracticeCard(
                        loading = lifeSkillCountersLoading,
                        counters = lifeSkillCounters,
                        childOptions = lifeSkillChildOptions,
                        selectedChildId = selectedLifeSkillChildId,
                        onChildSelected = onLifeSkillChildChange,
                    )
                }
                
                // Quick Actions Section
                ProfileSectionCard(
                    title = Strings.quickActions(),
                    icon = Icons.Filled.AutoAwesome
                ) {
                    ProfileMenuItem(
                        icon = Icons.Filled.Mic,
                        label = Strings.tabMyVoiceAndAvatar(),
                        onClick = onNavigateToMyVoiceAndAvatar,
                        compact = true
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                        color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Filled.Favorite,
                        label = Strings.favorites(),
                        onClick = onNavigateToFavorites,
                        compact = true
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                        color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Filled.History,
                        label = Strings.listeningHistory(),
                        onClick = onNavigateToListeningHistory,
                        compact = true
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                        color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Filled.EmojiEvents,
                        label = Strings.achievements(),
                        onClick = onNavigateToAchievements,
                        compact = true
                    )
                }
                
                // Settings & Subscription Section
                ProfileSectionCard(
                    title = Strings.settingsAndMore(),
                    icon = Icons.Filled.Settings
                ) {
                    ProfileMenuItem(
                        icon = Icons.Filled.WorkspacePremium,
                        label = Strings.premiumSubscription(),
                        onClick = onNavigateToSubscription,
                        compact = true
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                        color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Filled.AutoAwesome,
                        label = Strings.lifeReadinessMenuItem(),
                        onClick = onNavigateToLifeReadiness,
                        compact = true
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = TamixaDesignTokens.contentPaddingHorizontal),
                        color = TamixaContentColors.cardPrimary().copy(alpha = 0.1f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Filled.Settings,
                        label = Strings.settings(),
                        onClick = onNavigateToSettings,
                        compact = true
                    )
                }
                
                Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TamixaColors.deepTeal.copy(alpha = 0.38f),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
            ),
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.68f),
            contentColor = TamixaContentColors.cardPrimary()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                        vertical = TamixaDesignTokens.smallSpacing
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = TamixaColors.deepTeal.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TamixaColors.deepTeal
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TamixaContentColors.cardPrimary()
                )
            }
            
            // Section content
            content()
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    user: CurrentUser,
    onUpdateProfile: (nickname: String?, displayName: String?) -> Unit
) {
    val hasProfileData = user.nickname?.trim()?.isNotBlank() == true ||
        user.displayName?.trim()?.isNotBlank() == true
    var isEditing by remember { mutableStateOf(!hasProfileData) }
    var nickname by remember(user.nickname, isEditing) { mutableStateOf(user.nickname ?: "") }
    var displayName by remember(user.displayName, isEditing) { mutableStateOf(user.displayName ?: "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TamixaColors.deepTeal.copy(alpha = 0.38f),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
            ),
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.68f),
            contentColor = TamixaContentColors.cardPrimary()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TamixaDesignTokens.contentPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
        ) {
            Spacer(Modifier.height(4.dp))
            
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = TamixaColors.deepTeal.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .border(
                        width = 3.dp,
                        color = TamixaColors.deepTeal.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TamixaColors.deepTeal
                )
            }
            
            if (isEditing) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(Strings.name()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TamixaColors.deepTeal,
                        unfocusedBorderColor = TamixaColors.deepTeal.copy(alpha = 0.5f),
                        focusedLabelColor = TamixaColors.deepTeal,
                        cursorColor = TamixaColors.deepTeal,
                        focusedTextColor = TamixaContentColors.cardPrimary(),
                        unfocusedTextColor = TamixaContentColors.cardPrimary()
                    )
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(Strings.nickname()) },
                    placeholder = { Text(Strings.nicknameHint()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TamixaColors.deepTeal,
                        unfocusedBorderColor = TamixaColors.deepTeal.copy(alpha = 0.5f),
                        focusedLabelColor = TamixaColors.deepTeal,
                        cursorColor = TamixaColors.deepTeal,
                        focusedTextColor = TamixaContentColors.cardPrimary(),
                        unfocusedTextColor = TamixaContentColors.cardPrimary()
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = Strings.cancel(), color = TamixaColors.deepTeal)
                    }
                    TamixaPrimaryButton(
                        onClick = {
                            onUpdateProfile(
                                nickname.trim().takeIf { it.isNotBlank() },
                                displayName.trim().takeIf { it.isNotBlank() }
                            )
                            isEditing = false
                        },
                        text = Strings.save(),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = user.displayNameOrFallback(Strings.profile()),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TamixaContentColors.cardPrimary()
                )
                if (user.nickname?.trim()?.isNotBlank() == true) {
                    Text(
                        text = "@${user.nickname}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TamixaContentColors.cardSecondary()
                    )
                }
                TextButton(onClick = { isEditing = true }) {
                    Text(text = Strings.edit(), color = TamixaColors.deepTeal)
                }
            }
            
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LifeSkillPracticeCard(
    loading: Boolean,
    counters: LifeSkillCountersResponseDto?,
    childOptions: List<Pair<Long, String>>,
    selectedChildId: Long,
    onChildSelected: (Long) -> Unit,
) {
    var childMenuExpanded by remember { mutableStateOf(false) }
    val selectedLabel = childOptions.find { it.first == selectedChildId }?.second
        ?: Strings.lifeSkillPracticeUnnamedChild()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TamixaColors.deepTeal.copy(alpha = 0.38f),
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
            ),
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.68f),
            contentColor = TamixaContentColors.cardPrimary()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                    vertical = TamixaDesignTokens.contentPaddingHorizontal,
                ),
            verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing),
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = TamixaColors.deepTeal.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TamixaColors.deepTeal
                    )
                }
                Text(
                    text = Strings.lifeSkillPracticeTitle(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TamixaContentColors.cardPrimary(),
                )
            }
            
            if (childOptions.size > 1) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { childMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${Strings.lifeSkillPracticeChooseChild()}: $selectedLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardPrimary(),
                        )
                    }
                    DropdownMenu(
                        expanded = childMenuExpanded,
                        onDismissRequest = { childMenuExpanded = false },
                    ) {
                        childOptions.forEach { (cid, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onChildSelected(cid)
                                    childMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            
            Text(
                text = Strings.lifeSkillPracticeDisclaimer(),
                style = MaterialTheme.typography.bodySmall,
                color = TamixaContentColors.cardPrimary().copy(alpha = 0.75f),
            )
            
            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = TamixaColors.deepTeal,
                    )
                }
            } else if (counters != null) {
                Spacer(Modifier.height(4.dp))
                LifeSkillPillarRow(Strings.lifeSkillPillarWisdom(), counters.wisdom)
                LifeSkillPillarRow(Strings.lifeSkillPillarSocial(), counters.social)
                LifeSkillPillarRow(Strings.lifeSkillPillarMoney(), counters.money)
                LifeSkillPillarRow(Strings.lifeSkillPillarBalance(), counters.balance)
            }
        }
    }
}

@Composable
private fun LifeSkillPillarRow(label: String, value: Int) {
    val frac = (value / 40f).coerceIn(0f, 1f)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = TamixaContentColors.cardPrimary(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TamixaColors.deepTeal,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TamixaColors.deepTeal.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(TamixaColors.deepTeal.copy(alpha = 0.88f)),
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    if (compact) {
        // Compact mode for items inside section cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                    vertical = TamixaDesignTokens.smallSpacing,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = TamixaColors.deepTeal
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TamixaContentColors.cardPrimary(),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TamixaContentColors.cardPrimary().copy(alpha = 0.5f)
            )
        }
    } else {
        // Standalone card mode (not currently used but kept for flexibility)
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = TamixaColors.deepTeal.copy(alpha = 0.38f),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
                ),
            shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.68f),
                contentColor = TamixaContentColors.cardPrimary()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                        vertical = 14.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = TamixaColors.deepTeal.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                        tint = TamixaColors.deepTeal
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
}
