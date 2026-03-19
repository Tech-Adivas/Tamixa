package com.tamixa.android.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tamixa.ui.data.SampleData
import com.tamixa.ui.state.UiState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tamixa.ui.navigation.Screen
import com.tamixa.ui.screen.*
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.viewmodel.AuthViewModel
import com.tamixa.ui.viewmodel.SettingsViewModel
import com.tamixa.ui.viewmodel.StoryViewModel
import com.tamixa.ui.viewmodel.SubscriptionViewModel
import com.tamixa.ui.viewmodel.AvatarViewModel
import com.tamixa.ui.viewmodel.VoiceViewModel
import com.tamixa.application.port.OnboardingReminderPort
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults
import com.tamixa.ui.AppMessageNotifier

/** Parses tamixa://story/{id} or /s/{id} redirects to story ID, or null if invalid. */
private fun parseStoryIdFromDeepLink(uri: String?): Long? {
    if (uri.isNullOrBlank()) return null
    return when {
        uri.contains("tamixa://story/") -> uri.substringAfter("tamixa://story/").substringBefore("?").trim().toLongOrNull()
        uri.contains("/s/") -> uri.substringAfterLast("/s/").substringBefore("?").trim().toLongOrNull()
        else -> null
    }
}

@Composable
fun TamixaNavHost(
    authViewModel: AuthViewModel,
    storyViewModel: StoryViewModel,
    voiceViewModel: VoiceViewModel,
    avatarViewModel: AvatarViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    settingsViewModel: SettingsViewModel,
    shortContentViewModel: com.tamixa.ui.viewmodel.ShortContentViewModel,
    onSensitiveScreen: ((Boolean) -> Unit)? = null,
    initialDeepLinkUri: String? = null
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var deepLinkHandled by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        val sensitive = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route
        onSensitiveScreen?.invoke(sensitive)
    }
    LaunchedEffect(initialDeepLinkUri, currentRoute, deepLinkHandled) {
        if (deepLinkHandled || initialDeepLinkUri.isNullOrBlank()) return@LaunchedEffect
        if (currentRoute == Screen.Splash.route || currentRoute == Screen.Login.route) return@LaunchedEffect
        val storyId = parseStoryIdFromDeepLink(initialDeepLinkUri) ?: return@LaunchedEffect
        deepLinkHandled = true
        navController.navigate(Screen.AudioPlayer.withId(storyId)) { launchSingleTop = true }
    }
    val loginState by authViewModel.loginState.collectAsState()
    val otpSentToPhone by authViewModel.otpSentToPhone.collectAsState()
    val passwordlessCodeSentToEmail by authViewModel.passwordlessCodeSentToEmail.collectAsState()
    val registerState by authViewModel.registerState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val generateState by storyViewModel.generateState.collectAsState()
    val uploadState by voiceViewModel.uploadState.collectAsState()
    val profilesState by voiceViewModel.profiles.collectAsState()
    val avatarUploadState by avatarViewModel.uploadState.collectAsState()
    val avatarUrl by avatarViewModel.avatarUrl.collectAsState()
    val subscription by subscriptionViewModel.subscription.collectAsState()
    val usage by subscriptionViewModel.usage.collectAsState()
    val subscriptionLoading by subscriptionViewModel.loading.collectAsState()
    val subscriptionCanceling by subscriptionViewModel.canceling.collectAsState()
    val appliedReferral by subscriptionViewModel.appliedReferral.collectAsState()
    val referralError by subscriptionViewModel.referralError.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()

    val postLoginDestination = when {
        !settingsState.settingsLoaded -> null
        !settingsState.hasCompletedLanguageSelection -> Screen.LanguageSelection.route
        else -> Screen.Dashboard.route
    }

    val startDestination = when {
        !settingsState.settingsLoaded -> Screen.Splash.route
        authViewModel.isLoggedIn() && postLoginDestination != null -> postLoginDestination
        else -> Screen.Splash.route
    }

    val appMessageNotifier: AppMessageNotifier = koinInject()
    val showErrorDialog by appMessageNotifier.showErrorDialog.collectAsState()
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { appMessageNotifier.dismissErrorDialog() },
            shape = TamixaDialogDefaults.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            text = { Text(Strings.somethingWentWrong(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = { appMessageNotifier.dismissErrorDialog() },
                    shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                ) { Text(Strings.close()) }
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            LaunchedEffect(settingsState.settingsLoaded, authViewModel.isLoggedIn()) {
                if (authViewModel.isLoggedIn() && settingsState.settingsLoaded && postLoginDestination != null) {
                    navController.navigate(postLoginDestination) { popUpTo(Screen.Splash.route) { inclusive = true } }
                }
            }
            SplashScreen(
                isLoggedIn = authViewModel.isLoggedIn(),
                hasCompletedOnboarding = settingsState.hasCompletedOnboarding,
                onNavigateToHook = {
                    navController.navigate(Screen.OnboardingHook.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LaunchedEffect(loginState, settingsState.settingsLoaded, currentRoute) {
                if (currentRoute == Screen.Login.route && authViewModel.isLoggedIn() && loginState is UiState.Success<*> && settingsState.settingsLoaded && postLoginDestination != null) {
                    navController.navigate(postLoginDestination) { popUpTo(Screen.Login.route) { inclusive = true } }
                }
            }
            LoginScreen(
                loginState = loginState,
                otpSentToPhone = otpSentToPhone,
                passwordlessCodeSentToEmail = passwordlessCodeSentToEmail,
                onRequestPasswordlessCode = { authViewModel.requestPasswordlessCode(it) },
                onVerifyPasswordlessCode = { email, code, terms, privacy, parentalAttestation -> authViewModel.verifyPasswordlessCode(email, code, terms, privacy, parentalAttestation) },
                onClearPasswordlessState = { authViewModel.clearPasswordlessState() },
                onSendOtp = { authViewModel.sendOtp(it) },
                onVerifyOtp = { phone, code -> authViewModel.loginWithOtp(phone, code) },
                onClearOtpState = { authViewModel.clearOtpState() }
            )
        }
        composable(Screen.OnboardingHook.route) {
            OnboardingHookScreen(
                onStartStoryMagic = { navController.navigate(Screen.OnboardingDemo.route) },
                onSkip = { navController.navigate(Screen.OnboardingDemo.route) }
            )
        }
        composable(Screen.OnboardingDemo.route) {
            OnboardingDemoScreen(
                onContinue = { navController.navigate(Screen.OnboardingVoiceInvitation.route) }
            )
        }
        composable(Screen.OnboardingVoiceInvitation.route) {
            OnboardingVoiceInvitationScreen(
                onRecordVoice = { navController.navigate(Screen.OnboardingAvatarInvitation.route) },
                onSkip = { navController.navigate(Screen.OnboardingAvatarInvitation.route) }
            )
        }
        composable(Screen.OnboardingAvatarInvitation.route) {
            val scope = rememberCoroutineScope()
            OnboardingAvatarInvitationScreen(
                onUploadPhoto = {
                    scope.launch {
                        withContext(Dispatchers.Default) { settingsViewModel.completeOnboarding() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OnboardingHook.route) { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    scope.launch {
                        withContext(Dispatchers.Default) { settingsViewModel.completeOnboarding() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OnboardingHook.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.OnboardingBedtimeReminder.route) {
            val scope = rememberCoroutineScope()
            val reminderPort: OnboardingReminderPort = koinInject()
            OnboardingBedtimeReminderScreen(
                onContinue = { enabled, hour, minute ->
                    scope.launch {
                        settingsViewModel.setBedtimeReminder(enabled, hour, minute)
                        if (enabled) {
                            reminderPort.scheduleBedtimeReminder(hour, minute)
                        } else {
                            reminderPort.cancelBedtimeReminder()
                        }
                        withContext(Dispatchers.Default) {
                            settingsViewModel.completeOnboarding()
                        }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OnboardingHook.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            LaunchedEffect(registerState, settingsState.settingsLoaded, currentRoute) {
                if (currentRoute == Screen.Register.route && authViewModel.isLoggedIn() && registerState is UiState.Success<*> && settingsState.settingsLoaded && postLoginDestination != null) {
                    navController.navigate(postLoginDestination) { popUpTo(Screen.Register.route) { inclusive = true } }
                }
            }
            RegisterScreen(
                registerState = registerState,
                onRegister = { email, password, terms, privacy, parentalAttestation -> authViewModel.register(email, password, terms, privacy, parentalAttestation) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.LanguageSelection.route) {
            LaunchedEffect(settingsState.settingsLoaded, settingsState.hasCompletedLanguageSelection) {
                if (settingsState.settingsLoaded && settingsState.hasCompletedLanguageSelection) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                    }
                }
            }
            val scope = rememberCoroutineScope()
            LanguageSelectionScreen(
                onLanguageSelected = { code ->
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            settingsViewModel.persistLanguageSelection(code)
                        }
                        Strings.setLanguage(code)
                        navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.LanguageSelection.route) { inclusive = true } }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            var selectedCategory by remember { mutableStateOf(SampleData.categories.first()) }
            val curated by storyViewModel.libraryStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLang) { storyViewModel.loadLibraryStories(prefLang) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            LaunchedEffect(Screen.Dashboard.route) {
                subscriptionViewModel.loadSubscription()
                settingsViewModel.loadListeningStreak()
            }
            val stories = storyViewModel.allStories().ifEmpty { SampleData.sampleStories() }
            DashboardScreen(
                greeting = Strings.goodEvening(),
                childName = null,
                stories = stories,
                selectedCategory = selectedCategory,
                categories = SampleData.categories,
                onCategorySelect = { selectedCategory = it },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onNewStory = { navController.navigate(Screen.StoryGeneration.route) },
                onNavigateToMyVoiceAndAvatar = { navController.navigate(Screen.MyVoiceAndAvatar.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onRefresh = {
                    storyViewModel.loadLibraryStories(prefLang)
                    storyViewModel.loadMyStories()
                    subscriptionViewModel.loadSubscription()
                    settingsViewModel.loadListeningStreak()
                },
                isRefreshing = false,
                usageStoriesUsed = usage?.storiesUsed ?: 0,
                usageStoriesLimit = usage?.storiesLimit,
                listeningStreakDays = settingsState.listeningStreakDays
            )
        }
        composable(Screen.Library.route) {
            val prefLangLib = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLangLib) { storyViewModel.loadLibraryStories(prefLangLib) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            LibraryScreen(
                cachedStories = storyViewModel.allStories(),
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.Profile.route) {
            LaunchedEffect(Unit) { authViewModel.loadCurrentUser() }
            ProfileScreen(
                userState = currentUser,
                onRetryLoadUser = { authViewModel.loadCurrentUser() },
                onUpdateProfile = { nickname, displayName -> authViewModel.updateProfile(nickname, displayName) },
                onNavigateToMyVoiceAndAvatar = { navController.navigate(Screen.MyVoiceAndAvatar.route) },
                onNavigateToVoiceUpload = { navController.navigate(Screen.VoiceUpload.route) },
                onNavigateToAvatarUpload = { navController.navigate(Screen.AvatarUpload.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToListeningHistory = { navController.navigate(Screen.ListeningHistory.route) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToSendStory = { navController.navigate(Screen.StoryGeneration.route) },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.StorySelection.route) {
            val curated by storyViewModel.libraryStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLang) { storyViewModel.loadLibraryStories(prefLang) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            StorySelectionScreen(
                cachedStories = storyViewModel.allStories(),
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) }
            )
        }
        composable(Screen.StoryGeneration.route) {
            val generateState by storyViewModel.generateState.collectAsState()
            LaunchedEffect(generateState) {
                if (generateState is UiState.Success<*>) storyViewModel.loadMyStories()
            }
            LaunchedEffect(Unit) { subscriptionViewModel.loadSubscription() }
            StoryGenerationScreen(
                generateState = generateState,
                storiesUsed = usage?.storiesUsed ?: 0,
                storiesLimit = usage?.storiesLimit,
                onGenerate = { storyViewModel.generateStory(it) },
                onBack = { navController.popBackStack() },
                onStoryGenerated = { navController.navigate(Screen.Dashboard.route) }
            )
        }
        composable(
            "audio/{storyId}",
            arguments = listOf(navArgument("storyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getLong("storyId") ?: 0L
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            val curated by storyViewModel.libraryStories.collectAsState()
            val allStories = storyViewModel.allStories()
            var story by remember(storyId) { mutableStateOf<com.tamixa.domain.Story?>(null) }
            var progress by remember { mutableStateOf(0f) }
            var showMoralDialog by remember { mutableStateOf(false) }
            LaunchedEffect(storyId, allStories, curated, prefLang) {
                story = allStories.find { it.id == storyId }
                    ?: SampleData.sampleStories().find { it.id == storyId }
                    ?: if (storyId > 0) storyViewModel.fetchStoryById(storyId, prefLang) else null
            }
            val tracker = storyViewModel.playbackTracker()
            LaunchedEffect(story?.id) { tracker.reset() }
            val analytics = remember(tracker) {
                com.tamixa.ui.screen.StoryAnalyticsCallbacks(
                    onStoryStarted = { s, pos -> tracker.onStoryStarted(s, pos) },
                    onProgress = { s, p, pos ->
                        progress = p
                        tracker.onProgress(s, p, pos)
                    },
                    onCompleted = { s, pos -> tracker.onCompleted(s, pos) },
                    onStoppedEarly = { s, pos -> tracker.onStoppedEarly(s, pos) }
                )
            }
            story?.let { currentStory ->
                if (showMoralDialog) {
                    AlertDialog(
                        onDismissRequest = { showMoralDialog = false },
                        shape = TamixaDialogDefaults.shape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text(Strings.moralOfStory(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
                        text = {
                            Column {
                                Text(Strings.moralDialogIntro(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(12.dp))
                                Text(currentStory.moral ?: Strings.noMoralAvailable(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                Text(Strings.talkAboutIt(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("• ${Strings.discussionPromptWhatLearned()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text("• ${Strings.discussionPromptFavoritePart()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showMoralDialog = false }, shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)) { Text(Strings.continueWith()) }
                        }
                    )
                }
            }
            AudioPlayerScreen(
                story = story,
                isPlaying = false,
                onPlayPause = { /* ExoPlayer in app */ },
                onBack = {
                    story?.let { s ->
                        if (progress < 0.99f) {
                            val pos = (progress * s.readingTimeMinutes * 60).toInt()
                            tracker.onStoppedEarly(s, pos)
                        }
                    }
                    navController.popBackStack()
                },
                progress = progress,
                onSleepTimer = { },
                onDownload = { },
                onShare = { },
                analytics = analytics,
                onMoralClick = { showMoralDialog = true },
                upNextStories = (allStories + curated)
                    .filter { it.status == com.tamixa.domain.StoryStatus.READY && it.id != storyId }
                    .distinctBy { it.id }
                    .take(12),
                onUpNextStoryClick = { s ->
                    val src = if (s.parentId == 0L) com.tamixa.util.TamixaConstants.STORY_SOURCE_LIBRARY else com.tamixa.util.TamixaConstants.STORY_SOURCE_GENERATED
                    navController.navigate(Screen.AudioPlayer.withId(s.id, src)) { launchSingleTop = true }
                }
            )
        }
        composable(Screen.MyVoiceAndAvatar.route) {
            MyVoiceAndAvatarScreen(
                onNavigateToVoice = { navController.navigate(Screen.VoiceUpload.route) { popUpTo(Screen.MyVoiceAndAvatar.route) { inclusive = true } } },
                onNavigateToAvatar = { navController.navigate(Screen.AvatarUpload.route) { popUpTo(Screen.MyVoiceAndAvatar.route) { inclusive = true } } },
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onBack = { navController.popBackStack() },
                isPremiumForVoice = subscription?.isActive == true,
                isPremiumForAvatar = subscription?.isActive == true,
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) }
            )
        }
        composable(Screen.VoiceUpload.route) {
            val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                uri?.let { u ->
                    context.contentResolver.openInputStream(u)?.use { stream ->
                        val bytes = stream.readBytes()
                        val name = u.lastPathSegment?.substringAfterLast('/') ?: "voice_audio"
                        voiceViewModel.uploadVoice(bytes, name)
                    }
                }
            }
            val myStories by storyViewModel.myStories.collectAsState()
            VoiceUploadScreen(
                uploadState = uploadState,
                profilesState = profilesState,
                voiceStories = myStories,
                onUpload = { bytes, name -> voiceViewModel.uploadVoice(bytes, name) },
                onPickAudio = { audioPickerLauncher.launch("audio/*") },
                onLoadProfiles = { voiceViewModel.loadProfiles() },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onBack = { navController.popBackStack() },
                isPremium = subscription?.isActive == true,
                onUpgradeClick = { navController.navigate(Screen.Subscription.route) },
                apiBaseUrl = null,
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToAvatar = { navController.navigate(Screen.AvatarUpload.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToVoice = { },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToMyVoiceAndAvatarHub = { navController.navigate(Screen.MyVoiceAndAvatar.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.AvatarUpload.route) {
            val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                uri?.let { u ->
                    context.contentResolver.openInputStream(u)?.use { stream ->
                        val bytes = stream.readBytes()
                        val contentType = context.contentResolver.getType(u) ?: "image/jpeg"
                        avatarViewModel.uploadAvatar(bytes, contentType)
                    }
                }
            }
            val avatarStories by storyViewModel.myStories.collectAsState()
            AvatarUploadScreen(
                uploadState = avatarUploadState,
                avatarUrl = avatarUrl,
                avatarStories = avatarStories,
                onUpload = { bytes, contentType -> avatarViewModel.uploadAvatar(bytes, contentType) },
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onDelete = { avatarViewModel.deleteAvatar() },
                onLoadAvatar = { avatarViewModel.loadAvatar() },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onBack = { navController.popBackStack() },
                apiBaseUrl = null,
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToAvatar = { },
                onNavigateToVoice = { navController.navigate(Screen.VoiceUpload.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToMyVoiceAndAvatarHub = { navController.navigate(Screen.MyVoiceAndAvatar.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.ListeningHistory.route) {
            val prefLangLh = settingsState.languageCode.ifEmpty { "ta" }
            ListeningHistoryScreen(
                storyViewModel = storyViewModel,
                languageCode = prefLangLh,
                onBack = { navController.popBackStack() },
                onStoryClick = { storyId, source -> navController.navigate(Screen.AudioPlayer.withId(storyId, source)) }
            )
        }
        composable(Screen.Achievements.route) {
            val achievementApi: com.tamixa.network.AchievementApi = koinInject()
            val scope = rememberCoroutineScope()
            var achievements by remember { mutableStateOf<List<com.tamixa.network.AchievementDto>?>(null) }
            var achievementsLoading by remember { mutableStateOf(true) }
            var achievementsError by remember { mutableStateOf<String?>(null) }
            fun loadAchievements() {
                scope.launch {
                    achievementsLoading = true
                    achievementsError = null
                    try {
                        achievements = achievementApi.getMe()
                    } catch (e: Throwable) {
                        achievementsError = e.message ?: "Failed to load"
                    }
                    achievementsLoading = false
                }
            }
            LaunchedEffect(Unit) { loadAchievements() }
            AchievementsScreen(
                achievements = achievements,
                loading = achievementsLoading,
                loadError = achievementsError,
                onRetry = { loadAchievements() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Favorites.route) {
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLang) { storyViewModel.loadFavorites(prefLang) }
            FavoritesScreen(
                storyViewModel = storyViewModel,
                onBack = { navController.popBackStack() },
                onStoryClick = { story ->
                    val source = if (story.parentId == 0L) com.tamixa.util.TamixaConstants.STORY_SOURCE_LIBRARY else com.tamixa.util.TamixaConstants.STORY_SOURCE_GENERATED
                    navController.navigate(Screen.AudioPlayer.withId(story.id, source))
                },
                languageCode = prefLang,
                apiBaseUrl = null,
                onFavoriteRemoved = { _, _ -> }
            )
        }
        composable(Screen.ShortContent.route) {
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            ShortContentScreen(
                shortContentViewModel = shortContentViewModel,
                onBack = { navController.popBackStack() },
                languageCode = prefLang,
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToShortContent = { },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.Subscription.route) {
            LaunchedEffect(Unit) { subscriptionViewModel.loadSubscription() }
            SubscriptionScreen(
                subscription = subscription,
                usage = usage,
                loading = subscriptionLoading,
                canceling = subscriptionCanceling,
                appliedReferral = appliedReferral,
                referralError = referralError,
                onLoadSubscription = { subscriptionViewModel.loadSubscription() },
                onCancelSubscription = {
                    subscriptionViewModel.cancelSubscription {
                        // Optional: show snackbar or navigate
                    }
                },
                onApplyReferralCode = { subscriptionViewModel.applyReferralCode(it) },
                onClearReferralCode = { subscriptionViewModel.clearReferralCode() },
                onSubscribe = { referralCode ->
                    subscriptionViewModel.createCheckoutAndOpen(
                        referralCode,
                        onUrl = { url ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        onFallback = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(com.tamixa.network.ApiConfig.SUBSCRIPTION_WEB_URL))
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            val settingsScope = rememberCoroutineScope()
            LaunchedEffect(Unit) { settingsViewModel.loadSettings() }
            SettingsScreen(
                languageCode = settingsState.languageCode.ifEmpty { com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE },
                useSystemTheme = settingsState.useSystemTheme,
                darkMode = settingsState.darkMode,
                preferredVoiceProfile = settingsState.preferredVoiceProfile.ifEmpty { com.tamixa.util.TamixaConstants.VOICE_PROFILE_DEFAULT },
                consentRecords = settingsState.consentRecords,
                exportJobs = settingsState.exportJobs,
                listeningProgress = settingsState.listeningProgress,
                settingsLoading = settingsState.settingsLoading,
                exporting = settingsState.exporting,
                onLanguageChange = { code ->
                    settingsViewModel.setLanguage(code)
                    Strings.setLanguage(code)
                },
                onUseSystemThemeChange = { settingsViewModel.setUseSystemTheme(it) },
                onPreferredVoiceChange = { settingsViewModel.setPreferredVoiceProfile(it) },
                onDarkModeChange = { settingsViewModel.setDarkMode(it) },
                onLoadSettings = { settingsViewModel.loadSettings() },
                onRequestDataExport = { settingsViewModel.requestDataExport() },
                onOpenUrl = { url ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    settingsScope.launch {
                        authViewModel.deleteAccount()
                            .onSuccess {
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                            .onFailure { appMessageNotifier.showError() }
                    }
                },
                onBack = { navController.popBackStack() },
                onNavigateToVoiceUpload = { navController.navigate(Screen.VoiceUpload.route) },
                onNavigateToAvatarUpload = { navController.navigate(Screen.AvatarUpload.route) },
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                isPremiumForAvatar = subscription?.isActive == true,
                isPremiumForVoice = subscription?.isActive == true,
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToAvatar = { navController.navigate(Screen.AvatarUpload.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToVoice = { navController.navigate(Screen.VoiceUpload.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToSettings = { },
                onNavigateToMyVoiceAndAvatar = { navController.navigate(Screen.MyVoiceAndAvatar.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
    }
}
