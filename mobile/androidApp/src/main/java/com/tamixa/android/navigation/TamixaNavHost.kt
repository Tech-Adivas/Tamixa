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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tamixa.ui.data.SampleData
import com.tamixa.ui.isInteractivePracticeLibraryStory
import com.tamixa.ui.isLearnOrDigitalSafetyStory
import com.tamixa.util.AuthValidation
import com.tamixa.util.TamixaConstants
import com.tamixa.ui.state.UiState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.navigation.Screen
import com.tamixa.ui.screen.*
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.viewmodel.AuthViewModel
import com.tamixa.ui.viewmodel.SettingsViewModel
import com.tamixa.ui.viewmodel.StoryViewModel
import com.tamixa.ui.viewmodel.SubscriptionViewModel
import com.tamixa.ui.viewmodel.AvatarViewModel
import com.tamixa.ui.viewmodel.VoiceViewModel
import com.tamixa.analytics.AppAnalytics
import com.tamixa.application.port.PreferencesPort
import com.tamixa.network.AuthApi
import com.tamixa.network.StoryApi
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults
import com.tamixa.ui.AppMessageNotifier
import com.tamixa.analytics.AppAnalytics

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
    LaunchedEffect(initialDeepLinkUri) {
        val token = AuthValidation.extractPasswordlessLoginTokenFromUri(initialDeepLinkUri)
        if (!token.isNullOrBlank() && !authViewModel.isLoggedIn()) {
            authViewModel.setPendingPasswordlessMagicLinkToken(token)
        }
    }
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
    val otpDevCode by authViewModel.otpDevCode.collectAsState()
    val passwordlessCodeSentToEmail by authViewModel.passwordlessCodeSentToEmail.collectAsState()
    val pendingPasswordlessMagicLinkToken by authViewModel.pendingPasswordlessMagicLinkToken.collectAsState()
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

    val postAuthHomeDestination = when {
        !settingsState.settingsLoaded -> null
        !settingsState.hasCompletedLanguageSelection -> null
        else -> Screen.Dashboard.route
    }

    val startDestination = Screen.Splash.route

    val appMessageNotifier: AppMessageNotifier = koinInject()
    val snackbarHostState = remember { SnackbarHostState() }
    val appMessage by appMessageNotifier.message.collectAsState()
    LaunchedEffect(appMessage) {
        appMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.text)
            appMessageNotifier.clear()
        }
    }
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

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            LaunchedEffect(
                settingsState.settingsLoaded,
                settingsState.hasCompletedLanguageSelection,
                authViewModel.isLoggedIn()
            ) {
                if (!settingsState.settingsLoaded) return@LaunchedEffect
                // Let the splash animation play for a moment before navigating
                kotlinx.coroutines.delay(3000L)
                if (!settingsState.hasCompletedLanguageSelection) {
                    navController.navigate(Screen.LanguageSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
                if (authViewModel.isLoggedIn()) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            SplashScreen(
                isLoggedIn = authViewModel.isLoggedIn(),
                hasCompletedOnboarding = settingsState.hasCompletedOnboarding,
                needsLanguageSelection = settingsState.settingsLoaded && !settingsState.hasCompletedLanguageSelection,
                onNavigateToLanguage = {
                    navController.navigate(Screen.LanguageSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
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
            LaunchedEffect(loginState, settingsState.settingsLoaded, settingsState.hasCompletedLanguageSelection, currentRoute) {
                if (currentRoute == Screen.Login.route &&
                    authViewModel.isLoggedIn() &&
                    loginState is UiState.Success<*> &&
                    postAuthHomeDestination != null
                ) {
                    navController.navigate(postAuthHomeDestination) { popUpTo(Screen.Login.route) { inclusive = true } }
                }
            }
            LoginScreen(
                loginState = loginState,
                otpSentToPhone = otpSentToPhone,
                otpDevCode = otpDevCode,
                passwordlessCodeSentToEmail = passwordlessCodeSentToEmail,
                passwordlessMagicLinkToken = pendingPasswordlessMagicLinkToken,
                onRequestPasswordlessCode = { authViewModel.requestPasswordlessCode(it) },
                onVerifyPasswordlessCode = { email, code, terms, privacy, parentalAttestation -> authViewModel.verifyPasswordlessCode(email, code, terms, privacy, parentalAttestation) },
                onVerifyPasswordlessMagicLink = { token, terms, privacy, parental ->
                    authViewModel.verifyPasswordlessMagicLink(token, terms, privacy, parental)
                },
                onDismissPasswordlessMagicLink = { authViewModel.clearPendingPasswordlessMagicLinkToken() },
                onClearPasswordlessState = { authViewModel.clearPasswordlessState() },
                onSendOtp = { authViewModel.sendOtp(it) },
                onVerifyOtp = { phone, code -> authViewModel.loginWithOtp(phone, code) },
                onClearOtpState = { authViewModel.clearOtpState() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.OnboardingHook.route) {
            OnboardingHookScreen(
                onStartStoryMagic = { navController.navigate(Screen.OnboardingDemo.route) },
                onSkip = { navController.navigate(Screen.OnboardingDemo.route) }
            )
        }
        composable(Screen.OnboardingDemo.route) {
            println("🔵 Navigation: OnboardingDemo composable")
            OnboardingDemoScreen(
                onContinue = { 
                    println("🔵 Navigation: OnboardingDemo -> OnboardingInteractivePreview")
                    navController.navigate(Screen.OnboardingInteractivePreview.route) 
                },
                onSkip = { 
                    println("🔵 Navigation: OnboardingDemo SKIP -> OnboardingInteractivePreview")
                    navController.navigate(Screen.OnboardingInteractivePreview.route) 
                }
            )
        }
        composable(Screen.OnboardingInteractivePreview.route) {
            println("🟢 Navigation: OnboardingInteractivePreview composable")
            OnboardingInteractivePreviewScreen(
                onContinue = { 
                    println("🟢 Navigation: OnboardingInteractivePreview -> OnboardingVoiceInvitation")
                    navController.navigate(Screen.OnboardingVoiceInvitation.route) 
                },
                onSkip = { 
                    println("🟢 Navigation: OnboardingInteractivePreview SKIP -> OnboardingVoiceInvitation")
                    navController.navigate(Screen.OnboardingVoiceInvitation.route) 
                }
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
        composable(Screen.Register.route) {
            LaunchedEffect(registerState, settingsState.settingsLoaded, settingsState.hasCompletedLanguageSelection, currentRoute) {
                if (currentRoute == Screen.Register.route &&
                    authViewModel.isLoggedIn() &&
                    registerState is UiState.Success<*> &&
                    postAuthHomeDestination != null
                ) {
                    navController.navigate(postAuthHomeDestination) { popUpTo(Screen.Register.route) { inclusive = true } }
                }
            }
            RegisterScreen(
                registerState = registerState,
                onRegister = { email, password, terms, privacy, parentalAttestation -> authViewModel.register(email, password, terms, privacy, parentalAttestation) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.LanguageSelection.route) {
            val scope = rememberCoroutineScope()
            LanguageSelectionScreen(
                onLanguageSelected = { code ->
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            settingsViewModel.persistLanguageSelection(code)
                        }
                        Strings.setLanguage(code)
                        val nextRoute = when {
                            authViewModel.isLoggedIn() -> Screen.Dashboard.route
                            !settingsState.hasCompletedOnboarding -> Screen.OnboardingHook.route
                            else -> Screen.Login.route
                        }
                        navController.navigate(nextRoute) {
                            popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            var selectedCategory by remember { mutableStateOf(SampleData.categories.first()) }
            val curated by storyViewModel.libraryStories.collectAsState()
            val libraryLoading by storyViewModel.libraryLoading.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLang) {
                storyViewModel.loadLibraryStories(prefLang)
            }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            LaunchedEffect(Screen.Dashboard.route) {
                subscriptionViewModel.loadSubscription()
                settingsViewModel.loadListeningStreak()
            }
            val stories = storyViewModel.allStories().ifEmpty { SampleData.sampleStories() }
            val spotlightPreviewRows = remember(curated) {
                val practice =
                    curated.filter { isInteractivePracticeLibraryStory(it) }
                        .sortedByDescending { it.createdAt }
                val learnOnly =
                    curated.filter { isLearnOrDigitalSafetyStory(it) && !isInteractivePracticeLibraryStory(it) }
                        .sortedByDescending { it.createdAt }
                val rest =
                    curated.filter { !isLearnOrDigitalSafetyStory(it) }
                        .sortedByDescending { it.createdAt }
                (practice + learnOnly + rest).distinctBy { it.id }.take(8)
            }
            val spotlightSectionLoading = libraryLoading && curated.isEmpty()
            DashboardScreen(
                greeting = Strings.goodEvening(),
                childName = null,
                stories = stories,
                selectedCategory = selectedCategory,
                categories = SampleData.categories,
                onCategorySelect = { selectedCategory = it },
                onStoryClick = { story ->
                    val source =
                        if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED
                    navController.navigate(Screen.AudioPlayer.withId(story.id, source))
                },
                onNewStory = { navController.navigate(Screen.StoryGeneration.route) },
                onNavigateToMyVoiceAndAvatar = { navController.navigate(Screen.MyVoiceAndAvatar.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
                onNavigateToLibraryFunCorner = { navController.navigate(Screen.Library.withHub(Screen.Library.HUB_FUN)) },
                onNavigateToLibraryLearnSafety = { navController.navigate(Screen.Library.withHub(Screen.Library.HUB_LEARN_SAFETY)) },
                onNavigateToLibrarySimulator = { navController.navigate(Screen.Library.withHub(Screen.Library.HUB_SIMULATOR)) },
                onNavigateToLifeReadiness = { navController.navigate(Screen.LifeReadiness.route) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                spotlightPreview = spotlightPreviewRows,
                spotlightSectionLoading = spotlightSectionLoading,
                onSpotlightStoryClick = { s ->
                    navController.navigate(Screen.AudioPlayer.withId(s.id, TamixaConstants.STORY_SOURCE_LIBRARY))
                },
                onRefresh = {
                    storyViewModel.loadLibraryStories(prefLang)
                    storyViewModel.loadMyStories()
                    subscriptionViewModel.loadSubscription()
                    settingsViewModel.loadListeningStreak()
                },
                isRefreshing = false,
                usageStoriesUsed = usage?.storiesUsed ?: 0,
                usageStoriesLimit = usage?.storiesLimit,
                listeningStreakDays = settingsState.listeningStreakDays,
            )
        }
        composable(
            route = Screen.Library.route,
            arguments = listOf(
                navArgument("hub") {
                    type = NavType.StringType
                    defaultValue = Screen.Library.HUB_BROWSE
                }
            )
        ) { libEntry ->
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val appAnalyticsLib: AppAnalytics = koinInject()
            val storyApiLib: StoryApi = koinInject()
            val buildEnv: String = koinInject(named("tamixaBuildEnvironment"))
            val libraryPrepareScope = rememberCoroutineScope()
            var devDsgPrepareBusy by remember { mutableStateOf(false) }
            val showDevDsgPrepare = buildEnv == "dev"
            val hubArg = libEntry.arguments?.getString("hub") ?: Screen.Library.HUB_BROWSE
            val initialLibraryHub = when (hubArg) {
                Screen.Library.HUB_FUN -> com.tamixa.ui.screen.LibraryHubTab.FunCorner
                Screen.Library.HUB_LEARN, Screen.Library.HUB_LEARN_SAFETY -> com.tamixa.ui.screen.LibraryHubTab.LearnSafety
                Screen.Library.HUB_SIMULATOR -> com.tamixa.ui.screen.LibraryHubTab.Simulator
                else -> com.tamixa.ui.screen.LibraryHubTab.Browse
            }
            val prefLangLib = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLangLib) { storyViewModel.loadLibraryScreen(prefLangLib) }
            val libraryLoading by storyViewModel.libraryLoading.collectAsState()
            val libraryError by storyViewModel.libraryError.collectAsState()
            LibraryScreen(
                initialHubTab = initialLibraryHub,
                cachedStories = storyViewModel.allStories(),
                loading = libraryLoading,
                loadError = libraryError,
                onRetry = { storyViewModel.loadLibraryScreen(prefLangLib) },
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                apiBaseUrl = apiBaseUrl,
                onHubTabChange = { tab -> appAnalyticsLib.trackLibraryHub(tab.toAnalyticsHubKey()) },
                showDevDigitalSurvivalPrepare = showDevDsgPrepare,
                devDigitalSurvivalPrepareBusy = devDsgPrepareBusy,
                onPrepareDevDigitalSurvivalSeed =
                    if (showDevDsgPrepare) {
                        {
                            libraryPrepareScope.launch {
                                devDsgPrepareBusy = true
                                val r = storyApiLib.prepareDigitalSurvivalDevE2eSeed()
                                devDsgPrepareBusy = false
                                if (r.success) {
                                    appMessageNotifier.show(r.message, tag = "dev_dsg_seed")
                                    storyViewModel.loadLibraryScreen(prefLangLib)
                                } else {
                                    appMessageNotifier.show("DSG seed failed: ${r.message}", tag = "dev_dsg_seed")
                                }
                            }
                        }
                    } else {
                        null
                    },
                onNavigateToCrisisHelp = {
                    navController.navigate(Screen.CrisisHelp.route(Screen.CrisisHelp.FROM_LIB_SIM))
                },
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
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
                onNavigateToLifeReadiness = { navController.navigate(Screen.LifeReadiness.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LifeReadiness.route) {
            val storyApiLr: StoryApi = koinInject()
            val authApiLr: AuthApi = koinInject()
            val preferencesPortLr: PreferencesPort = koinInject()
            val profilePrefsScopeLr = rememberCoroutineScope()
            val appAnalyticsLr: AppAnalytics = koinInject()
            LaunchedEffect(Unit) {
                authViewModel.loadCurrentUser()
                storyViewModel.loadMyStories()
                appAnalyticsLr.trackScreenView("life_readiness")
            }
            val myStoriesLr by storyViewModel.myStories.collectAsState()
            val lifeSkillRefreshVersionLr by storyViewModel.lifeSkillCountersRefreshVersion.collectAsState()
            val educationChildIdLr = remember(myStoriesLr) { storyViewModel.firstEducationChildId() }
            var profileChildrenLr by remember { mutableStateOf<List<com.tamixa.network.ProfileChildJson>>(emptyList()) }
            LaunchedEffect(myStoriesLr) {
                profileChildrenLr = authApiLr.getProfile()?.children.orEmpty()
            }
            val lifeSkillChildOptionsLr = remember(profileChildrenLr, educationChildIdLr) {
                when {
                    profileChildrenLr.isNotEmpty() -> profileChildrenLr.map { it.id to it.name }
                    educationChildIdLr != null && educationChildIdLr > 0L ->
                        listOf(educationChildIdLr to Strings.lifeSkillPracticeUnnamedChild())
                    else -> emptyList()
                }
            }
            var selectedLifeSkillChildIdLr by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(lifeSkillChildOptionsLr) {
                if (lifeSkillChildOptionsLr.isEmpty()) {
                    selectedLifeSkillChildIdLr = null
                    return@LaunchedEffect
                }
                val currentValid =
                    selectedLifeSkillChildIdLr?.let { id -> lifeSkillChildOptionsLr.any { it.first == id } } == true
                if (currentValid) return@LaunchedEffect
                val preferred = preferencesPortLr.getLifeSkillPreferredChildId()
                val preferredValid =
                    preferred != null && lifeSkillChildOptionsLr.any { it.first == preferred }
                selectedLifeSkillChildIdLr =
                    if (preferredValid) preferred else lifeSkillChildOptionsLr.first().first
            }
            var lifeSkillCountersLr by remember { mutableStateOf<com.tamixa.network.LifeSkillCountersResponseDto?>(null) }
            var lifeSkillCountersLoadingLr by remember { mutableStateOf(false) }
            LaunchedEffect(selectedLifeSkillChildIdLr, lifeSkillRefreshVersionLr) {
                lifeSkillCountersLr = null
                val cid = selectedLifeSkillChildIdLr ?: return@LaunchedEffect
                if (cid <= 0L) return@LaunchedEffect
                lifeSkillCountersLoadingLr = true
                try {
                    lifeSkillCountersLr = storyApiLr.getLifeSkillCounters(cid)
                } finally {
                    lifeSkillCountersLoadingLr = false
                }
            }
            LifeReadinessScreen(
                counters = lifeSkillCountersLr,
                countersLoading = lifeSkillCountersLoadingLr,
                lifeSkillChildOptions = lifeSkillChildOptionsLr,
                selectedLifeSkillChildId = selectedLifeSkillChildIdLr,
                onLifeSkillChildChange = { id ->
                    selectedLifeSkillChildIdLr = id
                    profilePrefsScopeLr.launch {
                        preferencesPortLr.setLifeSkillPreferredChildId(id)
                    }
                },
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLibraryHub = { hub ->
                    navController.navigate(Screen.Library.withHub(hub)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Screen.StorySelection.route) {
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val curated by storyViewModel.libraryStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            LaunchedEffect(prefLang) { storyViewModel.loadLibraryStories(prefLang) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            StorySelectionScreen(
                cachedStories = storyViewModel.allStories(),
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                listLayout = StorySelectionListLayout.LibraryPosterGrid,
                apiBaseUrl = apiBaseUrl,
            )
        }
        composable(Screen.StoryGeneration.route) {
            val generateState by storyViewModel.generateState.collectAsState()
            val generationTopics by storyViewModel.generationTopics.collectAsState()
            LaunchedEffect(generateState) {
                if (generateState is UiState.Success<*>) storyViewModel.loadMyStories()
            }
            LaunchedEffect(Unit) {
                storyViewModel.clearGenerateState()
                subscriptionViewModel.loadSubscription()
                storyViewModel.loadGenerationTopics()
            }
            StoryGenerationScreen(
                generateState = generateState,
                generationTopics = generationTopics,
                storiesUsed = usage?.storiesUsed ?: 0,
                storiesLimit = usage?.storiesLimit,
                onGenerate = { storyViewModel.generateStory(it) },
                onBack = { navController.popBackStack() },
                onStoryGenerated = { navController.navigate(Screen.Dashboard.route) },
                onClearGenerateError = { storyViewModel.clearGenerateState() },
            )
        }
        composable(
            "audio/{storyId}",
            arguments = listOf(navArgument("storyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getLong("storyId") ?: 0L
            val scope = rememberCoroutineScope()
            val prefLang = settingsState.languageCode.ifEmpty { "ta" }
            val curated by storyViewModel.libraryStories.collectAsState()
            val favorites by storyViewModel.favorites.collectAsState()
            val allStories = storyViewModel.allStories()
            val appAnalytics: AppAnalytics = koinInject()
            var story by remember(storyId) { mutableStateOf<com.tamixa.domain.Story?>(null) }
            var progress by remember { mutableStateOf(0f) }
            LaunchedEffect(prefLang) { storyViewModel.loadFavorites(prefLang) }
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
            val playbackSubtitle = remember(story?.id, story?.theme, story?.category, story?.title) {
                story?.let { s ->
                    val src =
                        if (s.parentId == 0L) com.tamixa.util.TamixaConstants.STORY_SOURCE_LIBRARY
                        else com.tamixa.util.TamixaConstants.STORY_SOURCE_GENERATED
                    com.tamixa.ui.listenerPlaybackSubtitle(s, src)
                }
            }
            val showCrisisSos =
                story?.parentId == 0L && !story?.interactiveGraph.isNullOrBlank()
            Box(Modifier.fillMaxSize()) {
            AudioPlayerScreen(
                story = story,
                playbackSubtitle = playbackSubtitle,
                isPlaying = false,
                onPlayPause = { /* ExoPlayer in app */ },
                onBack = {
                    scope.launch {
                        story?.let { s ->
                            if (progress < 0.99f) {
                                val totalSec = (s.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
                                val pos = (progress * totalSec).toInt().coerceAtLeast(0)
                                tracker.onStoppedEarly(s, pos)
                            }
                            delay(400)
                        }
                        storyViewModel.loadRecentPlayback(
                            com.tamixa.util.TamixaConstants.RECENT_PLAYBACK_LIMIT,
                            prefLang
                        )
                        navController.popBackStack()
                    }
                },
                progress = progress,
                onSleepTimer = { },
                onDownload = { },
                onShare = { },
                analytics = analytics,
                bottomNavSelectedTab = TamixaTab.Home,
                onBottomNavHome = {
                    scope.launch {
                        story?.let { s ->
                            if (progress < 0.99f) {
                                val totalSec = (s.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
                                val pos = (progress * totalSec).toInt().coerceAtLeast(0)
                                tracker.onStoppedEarly(s, pos)
                            }
                        }
                        storyViewModel.loadRecentPlayback(
                            com.tamixa.util.TamixaConstants.RECENT_PLAYBACK_LIMIT,
                            prefLang
                        )
                        navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } }
                    }
                },
                onBottomNavLibrary = {
                    scope.launch {
                        story?.let { s ->
                            if (progress < 0.99f) {
                                val totalSec = (s.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
                                val pos = (progress * totalSec).toInt().coerceAtLeast(0)
                                tracker.onStoppedEarly(s, pos)
                            }
                        }
                        storyViewModel.loadRecentPlayback(
                            com.tamixa.util.TamixaConstants.RECENT_PLAYBACK_LIMIT,
                            prefLang
                        )
                        navController.navigate(Screen.Library.withHub()) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                },
                onBottomNavFunAndLearn = {
                    scope.launch {
                        story?.let { s ->
                            if (progress < 0.99f) {
                                val totalSec = (s.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
                                val pos = (progress * totalSec).toInt().coerceAtLeast(0)
                                tracker.onStoppedEarly(s, pos)
                            }
                        }
                        storyViewModel.loadRecentPlayback(
                            com.tamixa.util.TamixaConstants.RECENT_PLAYBACK_LIMIT,
                            prefLang
                        )
                        navController.navigate(Screen.ShortContent.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                },
                onBottomNavProfile = {
                    scope.launch {
                        story?.let { s ->
                            if (progress < 0.99f) {
                                val totalSec = (s.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
                                val pos = (progress * totalSec).toInt().coerceAtLeast(0)
                                tracker.onStoppedEarly(s, pos)
                            }
                        }
                        storyViewModel.loadRecentPlayback(
                            com.tamixa.util.TamixaConstants.RECENT_PLAYBACK_LIMIT,
                            prefLang
                        )
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                },
                isFavorite = story?.let { s -> favorites.any { it.id == s.id } } == true,
                onFavoriteToggle = story?.let { s ->
                    {
                        val src = if (s.parentId == 0L) com.tamixa.util.TamixaConstants.STORY_SOURCE_LIBRARY else com.tamixa.util.TamixaConstants.STORY_SOURCE_GENERATED
                        val fav = favorites.any { it.id == s.id }
                        if (fav) {
                            storyViewModel.removeFavorite(s.id) { appAnalytics.trackFavoriteRemove(s.id, src) }
                        } else {
                            storyViewModel.addFavorite(s.id, src) { appAnalytics.trackFavoriteAdd(s.id, src) }
                        }
                    }
                },
                onAddToList = story?.let { s ->
                    {
                        val src = if (s.parentId == 0L) com.tamixa.util.TamixaConstants.STORY_SOURCE_LIBRARY else com.tamixa.util.TamixaConstants.STORY_SOURCE_GENERATED
                        if (favorites.any { it.id == s.id }) {
                            appMessageNotifier.show(Strings.playerAlreadyInListSnackbar())
                        } else {
                            storyViewModel.addFavorite(s.id, src) {
                                appMessageNotifier.show(Strings.playerAddedToListSnackbar())
                                appAnalytics.trackFavoriteAdd(s.id, src)
                            }
                        }
                    }
                }
            )
            if (showCrisisSos) {
                Surface(
                    onClick = {
                        story?.let { s ->
                            val src =
                                if (s.parentId == 0L) com.tamixa.util.TamixaConstants.STORY_SOURCE_LIBRARY
                                else com.tamixa.util.TamixaConstants.STORY_SOURCE_GENERATED
                            appAnalytics.trackCrisisHelpSosTap(s.id, src, prefLang)
                        }
                        navController.navigate(Screen.CrisisNavigator.route)
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 12.dp),
                ) {
                    Text(
                        text = Strings.crisisHelpSosChip(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            }
        }
        composable(Screen.MyVoiceAndAvatar.route) {
            MyVoiceAndAvatarScreen(
                onNavigateToVoice = { navController.navigate(Screen.VoiceUpload.route) { popUpTo(Screen.MyVoiceAndAvatar.route) { inclusive = true } } },
                onNavigateToAvatar = { navController.navigate(Screen.AvatarUpload.route) { popUpTo(Screen.MyVoiceAndAvatar.route) { inclusive = true } } },
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
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
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
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
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
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
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
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
                        popUpTo(navController.graph.startDestinationRoute ?: Screen.Splash.route) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    settingsScope.launch {
                        authViewModel.deleteAccount()
                            .onSuccess {
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(navController.graph.startDestinationRoute ?: Screen.Splash.route) { inclusive = true }
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
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToCrisisHelp = {
                    navController.navigate(Screen.CrisisHelp.route(Screen.CrisisHelp.FROM_SETTINGS))
                },
            )
        }
        composable(
            route = Screen.CrisisHelp.route,
            arguments = listOf(
                navArgument(Screen.CrisisHelp.FROM_ARG) {
                    type = NavType.StringType
                    defaultValue = Screen.CrisisHelp.FROM_UNKNOWN
                },
            ),
        ) { crisisEntry ->
            val fromRaw = crisisEntry.arguments?.getString(Screen.CrisisHelp.FROM_ARG)
            val from = when (fromRaw) {
                Screen.CrisisHelp.FROM_LIB_SIM,
                Screen.CrisisHelp.FROM_SETTINGS,
                Screen.CrisisHelp.FROM_PLAYER,
                -> fromRaw
                else -> Screen.CrisisHelp.FROM_UNKNOWN
            }
            val appAnalyticsCrisis: AppAnalytics = koinInject()
            LaunchedEffect(from) { appAnalyticsCrisis.trackCrisisHelpOpen(from) }
            val preferencesPortCrisis: PreferencesPort = koinInject()
            var vaultInit by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) { vaultInit = preferencesPortCrisis.getCrisisSafetyVaultText() }
            when (val v = vaultInit) {
                null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                else -> CrisisHelpScreen(
                    initialVaultText = v,
                    onSaveVault = { text -> preferencesPortCrisis.setCrisisSafetyVaultText(text) },
                    onOpenUrl = { url ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateToCrisisNavigator = {
                        navController.navigate(Screen.CrisisNavigator.route)
                    },
                )
            }
        }
        composable(Screen.CrisisNavigator.route) {
            val appAnalyticsNavigator: AppAnalytics = koinInject()
            CrisisNavigatorScreen(
                onBack = { navController.popBackStack() },
                onOpenFullDirectory = {
                    navController.navigate(Screen.CrisisHelp.route(Screen.CrisisHelp.FROM_PLAYER))
                },
                onOpenUrl = { url ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                onTrackScreenView = { appAnalyticsNavigator.trackScreenView("crisis_navigator") },
            )
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
