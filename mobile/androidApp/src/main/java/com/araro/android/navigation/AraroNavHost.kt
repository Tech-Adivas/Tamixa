package com.araro.android.navigation

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
import com.araro.ui.data.SampleData
import com.araro.ui.state.UiState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.araro.ui.navigation.Screen
import com.araro.ui.screen.*
import com.araro.ui.strings.Strings
import com.araro.ui.viewmodel.AuthViewModel
import com.araro.ui.viewmodel.ChildViewModel
import com.araro.ui.viewmodel.SettingsViewModel
import com.araro.ui.viewmodel.StoryViewModel
import com.araro.ui.viewmodel.SubscriptionViewModel
import com.araro.ui.viewmodel.AvatarViewModel
import com.araro.ui.viewmodel.VoiceViewModel

@Composable
fun AraroNavHost(
    authViewModel: AuthViewModel,
    childViewModel: ChildViewModel,
    storyViewModel: StoryViewModel,
    voiceViewModel: VoiceViewModel,
    avatarViewModel: AvatarViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    settingsViewModel: SettingsViewModel,
    onSensitiveScreen: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        val sensitive = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route
        onSensitiveScreen?.invoke(sensitive)
    }
    val loginState by authViewModel.loginState.collectAsState()
    val otpSentToPhone by authViewModel.otpSentToPhone.collectAsState()
    val passwordlessCodeSentToEmail by authViewModel.passwordlessCodeSentToEmail.collectAsState()
    val registerState by authViewModel.registerState.collectAsState()
    val childrenState by childViewModel.children.collectAsState()
    val createState by childViewModel.createState.collectAsState()
    val generateState by storyViewModel.generateState.collectAsState()
    val uploadState by voiceViewModel.uploadState.collectAsState()
    val profilesState by voiceViewModel.profiles.collectAsState()
    val avatarUploadState by avatarViewModel.uploadState.collectAsState()
    val avatarUrl by avatarViewModel.avatarUrl.collectAsState()
    val subscription by subscriptionViewModel.subscription.collectAsState()
    val usage by subscriptionViewModel.usage.collectAsState()
    val subscriptionLoading by subscriptionViewModel.loading.collectAsState()
    val subscriptionCanceling by subscriptionViewModel.canceling.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()

    val postLoginDestination = when {
        !settingsState.settingsLoaded -> null
        !settingsState.hasCompletedLanguageSelection -> Screen.LanguageSelection.route
        else -> Screen.Dashboard.route
    }

    NavHost(
        navController = navController,
        startDestination = when {
            !authViewModel.isLoggedIn() -> Screen.Splash.route
            postLoginDestination == null -> Screen.Splash.route
            else -> postLoginDestination
        }
    ) {
        composable(Screen.Splash.route) {
            LaunchedEffect(settingsState.settingsLoaded, authViewModel.isLoggedIn()) {
                if (authViewModel.isLoggedIn() && settingsState.settingsLoaded && postLoginDestination != null) {
                    navController.navigate(postLoginDestination) { popUpTo(Screen.Splash.route) { inclusive = true } }
                }
            }
            SplashScreen(
                onNavigateToLogin = {
                    if (!authViewModel.isLoggedIn()) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                },
                isLoggedIn = authViewModel.isLoggedIn()
            )
        }
        composable(Screen.Login.route) {
            LaunchedEffect(loginState, settingsState.settingsLoaded, currentRoute) {
                if (currentRoute == Screen.Login.route && loginState is UiState.Success<*> && settingsState.settingsLoaded && postLoginDestination != null) {
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
        composable(Screen.Register.route) {
            LaunchedEffect(registerState, settingsState.settingsLoaded, currentRoute) {
                if (currentRoute == Screen.Register.route && registerState is UiState.Success<*> && settingsState.settingsLoaded && postLoginDestination != null) {
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
            val curated by storyViewModel.curatedStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLang) { storyViewModel.loadCuratedStories(prefLang) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            val stories = storyViewModel.allStories().ifEmpty { SampleData.sampleStories() }
            val firstChildName = (childrenState as? UiState.Success)?.data?.firstOrNull()?.name
            DashboardScreen(
                greeting = Strings.goodEvening(),
                childName = firstChildName,
                stories = stories,
                selectedCategory = selectedCategory,
                categories = SampleData.categories,
                onCategorySelect = { selectedCategory = it },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onNewStory = { navController.navigate(Screen.StoryGeneration.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onRefresh = {
                    childViewModel.loadChildren()
                    storyViewModel.loadCuratedStories(prefLang)
                    storyViewModel.loadMyStories()
                },
                isRefreshing = childrenState is UiState.Loading
            )
        }
        composable(Screen.ChildList.route) {
            LaunchedEffect(Unit) { childViewModel.loadChildren() }
            ChildListScreen(
                childrenState = childrenState,
                onAddChild = { navController.navigate(Screen.ChildCreate.route) },
                onRetry = { childViewModel.loadChildren() },
                onChildClick = { navController.navigate(Screen.Dashboard.route) }
            )
        }
        composable(Screen.ChildCreate.route) {
            LaunchedEffect(createState) {
                if (createState is UiState.Success<*>) navController.popBackStack()
            }
            ChildCreateScreen(
                createState = createState,
                onCreate = { req -> childViewModel.createChild(req) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.StorySelection.route) {
            val curated by storyViewModel.curatedStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLang) { storyViewModel.loadCuratedStories(prefLang) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            StorySelectionScreen(
                cachedStories = storyViewModel.allStories(),
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) }
            )
        }
        composable(Screen.StoryGeneration.route) {
            val children by childViewModel.children.collectAsState()
            val generateState by storyViewModel.generateState.collectAsState()
            LaunchedEffect(generateState) {
                if (generateState is UiState.Success<*>) storyViewModel.loadMyStories()
            }
            val list = (children as? UiState.Success)?.data ?: emptyList()
            StoryGenerationScreen(
                generateState = generateState,
                children = list,
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
            val curated by storyViewModel.curatedStories.collectAsState()
            val allStories = storyViewModel.allStories()
            var story by remember(storyId) { mutableStateOf<com.araro.domain.Story?>(null) }
            var progress by remember { mutableStateOf(0f) }
            LaunchedEffect(storyId, allStories, curated, prefLang) {
                story = allStories.find { it.id == storyId }
                    ?: SampleData.sampleStories().find { it.id == storyId }
                    ?: if (storyId > 0) storyViewModel.fetchStoryById(storyId, prefLang) else null
            }
            val tracker = storyViewModel.playbackTracker()
            LaunchedEffect(story?.id) { tracker.reset() }
            val analytics = remember(tracker) {
                com.araro.ui.screen.StoryAnalyticsCallbacks(
                    onStoryStarted = { s, pos -> tracker.onStoryStarted(s, pos) },
                    onProgress = { s, p, pos ->
                        progress = p
                        tracker.onProgress(s, p, pos)
                    },
                    onCompleted = { s, pos -> tracker.onCompleted(s, pos) },
                    onStoppedEarly = { s, pos -> tracker.onStoppedEarly(s, pos) }
                )
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
                analytics = analytics
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
            VoiceUploadScreen(
                uploadState = uploadState,
                profilesState = profilesState,
                onUpload = { bytes, name -> voiceViewModel.uploadVoice(bytes, name) },
                onPickAudio = { audioPickerLauncher.launch("audio/*") },
                onLoadProfiles = { voiceViewModel.loadProfiles() },
                onBack = { navController.popBackStack() }
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
            AvatarUploadScreen(
                uploadState = avatarUploadState,
                avatarUrl = avatarUrl,
                onUpload = { bytes, contentType -> avatarViewModel.uploadAvatar(bytes, contentType) },
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onDelete = { avatarViewModel.deleteAvatar() },
                onLoadAvatar = { avatarViewModel.loadAvatar() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Subscription.route) {
            LaunchedEffect(Unit) { subscriptionViewModel.loadSubscription() }
            SubscriptionScreen(
                subscription = subscription,
                usage = usage,
                loading = subscriptionLoading,
                canceling = subscriptionCanceling,
                onLoadSubscription = { subscriptionViewModel.loadSubscription() },
                onCancelSubscription = {
                    subscriptionViewModel.cancelSubscription {
                        // Optional: show snackbar or navigate
                    }
                },
                onManageSubscription = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(com.araro.network.ApiConfig.SUBSCRIPTION_WEB_URL))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            LaunchedEffect(Unit) { settingsViewModel.loadSettings() }
            SettingsScreen(
                languageCode = settingsState.languageCode.ifEmpty { com.araro.util.AraroConstants.DEFAULT_LANGUAGE },
                useSystemTheme = settingsState.useSystemTheme,
                darkMode = settingsState.darkMode,
                preferredVoiceProfile = settingsState.preferredVoiceProfile.ifEmpty { com.araro.util.AraroConstants.VOICE_PROFILE_DEFAULT },
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
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
                onNavigateToVoiceUpload = { navController.navigate(Screen.VoiceUpload.route) },
                onNavigateToAvatarUpload = { navController.navigate(Screen.AvatarUpload.route) },
                isPremiumForAvatar = subscription?.isActive == true
            )
        }
    }
}
