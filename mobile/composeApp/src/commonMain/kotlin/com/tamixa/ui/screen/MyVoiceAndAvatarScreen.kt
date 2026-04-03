package com.tamixa.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.*
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens
import org.jetbrains.compose.resources.painterResource

/**
 * Hub screen for "My voice & Avatar" tab: single entry to voice cloning and avatar upload.
 * Links cloned voice and avatar; after recording voice the flow continues to clone, then optionally upload image (if credits).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVoiceAndAvatarScreen(
    onNavigateToVoice: () -> Unit,
    onNavigateToAvatar: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToShortContent: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onBack: () -> Unit,
    isPremiumForVoice: Boolean = false,
    isPremiumForAvatar: Boolean = false,
    onNavigateToSubscription: () -> Unit = {},
    hasMyVoice: Boolean = false,
    onBackToStory: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.tabMyVoiceAndAvatar(),
                onBack = onBackToStory ?: onBack,
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
    ) { paddingValues ->
        val colorScheme = MaterialTheme.colorScheme
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AppScreenBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(TamixaDesignTokens.screenPadding)
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.voiceScreenHeadline(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = TamixaColors.cream
                )
                Text(
                    text = Strings.voiceScreenSubline(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamixaColors.cream.copy(alpha = 0.9f)
                )
                Card(
                    onClick = if (isPremiumForVoice) onNavigateToVoice else onNavigateToSubscription,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                            vertical = TamixaDesignTokens.smallSpacing,
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = Strings.voiceCtaCloneYourVoice(),
                            tint = TamixaColors.goldAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                        Text(
                            Strings.voiceCtaCloneYourVoice(),
                            style = MaterialTheme.typography.titleLarge,
                            color = TamixaContentColors.cardPrimary()
                        )
                        Text(
                            if (isPremiumForVoice) Strings.tapToStartRecording() else Strings.uploadVoicePremiumPrompt(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary()
                        )
                    }
                }
                Card(
                    onClick = if (isPremiumForAvatar && hasMyVoice) onNavigateToAvatar
                    else if (!isPremiumForAvatar) onNavigateToSubscription
                    else ({ }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = TamixaDesignTokens.contentPaddingHorizontal,
                            vertical = TamixaDesignTokens.smallSpacing,
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📖",
                                fontSize = 48.sp
                            )
                        }
                        Spacer(Modifier.height(TamixaDesignTokens.smallSpacing))
                        Text(
                            Strings.avatarScreenHeadline(),
                            style = MaterialTheme.typography.titleLarge,
                            color = TamixaContentColors.cardPrimary()
                        )
                        Text(
                            when {
                                !isPremiumForAvatar -> Strings.avatarUploadVoiceAndImageSuperPremium()
                                !hasMyVoice -> Strings.avatarCreateVoiceFirst()
                                else -> Strings.avatarCtaAddPhoto()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary()
                        )
                    }
                }
            }
        }
    }
}
