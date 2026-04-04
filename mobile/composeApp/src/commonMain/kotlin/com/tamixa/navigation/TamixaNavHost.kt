package com.tamixa.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import com.tamixa.platform.rememberAvatarVideoController
import com.tamixa.platform.rememberLocalFileController
import com.tamixa.platform.rememberStreamingController
import com.tamixa.platform.rememberAudioPickerLauncher
import com.tamixa.platform.rememberImagePickerLauncher
import com.tamixa.platform.synthesizeStoryToFile
import com.tamixa.network.ApiConfig
import com.tamixa.network.AuthApi
import com.tamixa.network.StoryApi
import com.tamixa.ui.data.SampleData
import com.tamixa.ui.state.UiState
import com.tamixa.domain.CurrentUser
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.navigation.Screen
import com.tamixa.application.port.PreferencesPort
import com.tamixa.util.NarrationTextUtils
import com.tamixa.util.TamixaConstants
import com.tamixa.util.TamixaLog
import com.tamixa.ui.screen.*
import com.tamixa.ui.strings.Strings
import com.tamixa.analytics.AppAnalytics
import com.tamixa.ui.viewmodel.EducationViewModel
import com.tamixa.ui.viewmodel.AuthViewModel
import com.tamixa.ui.viewmodel.SettingsViewModel
import com.tamixa.ui.viewmodel.StoryViewModel
import com.tamixa.ui.viewmodel.SubscriptionViewModel
import com.tamixa.ui.viewmodel.AvatarViewModel
import com.tamixa.ui.viewmodel.VoiceViewModel
import com.tamixa.ui.AppMessageNotifier
import com.tamixa.ui.components.platformIsReduceMotionEnabled
import androidx.compose.ui.Alignment

private val onboardingRoutes = setOf(
    Screen.OnboardingHook.route,
    Screen.OnboardingDemo.route,
    Screen.OnboardingVoiceInvitation.route,
    Screen.OnboardingAvatarInvitation.route,
)

private fun isOnboardingRoute(route: String?): Boolean = route != null && onboardingRoutes.contains(route)

@Composable
fun TamixaNavHost(
    authViewModel: AuthViewModel,
    storyViewModel: StoryViewModel,
    voiceViewModel: VoiceViewModel,
    avatarViewModel: AvatarViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    settingsViewModel: SettingsViewModel,
    shortContentViewModel: com.tamixa.ui.viewmodel.ShortContentViewModel,
    educationViewModel: EducationViewModel,
    onSensitiveScreen: ((Boolean) -> Unit)? = null
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        val sensitive = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route
        onSensitiveScreen?.invoke(sensitive)
    }
    val loginState by authViewModel.loginState.collectAsState()
    val otpSentToPhone by authViewModel.otpSentToPhone.collectAsState()
    val otpDevCode by authViewModel.otpDevCode.collectAsState()
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
    val subscriptionLoadError by subscriptionViewModel.loadError.collectAsState()
    val subscriptionCanceling by subscriptionViewModel.canceling.collectAsState()
    val appliedReferral by subscriptionViewModel.appliedReferral.collectAsState()
    val referralError by subscriptionViewModel.referralError.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()

    val effectiveLanguage = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
    LaunchedEffect(effectiveLanguage) {
        Strings.setLanguage(effectiveLanguage)
    }

    // Load current user when logged in so nickname/displayName is available across the app
    LaunchedEffect(authViewModel.isLoggedIn()) {
        if (authViewModel.isLoggedIn()) {
            authViewModel.loadCurrentUser()
        }
    }

    val currentUserState by authViewModel.currentUser.collectAsState()
    LaunchedEffect(currentUserState) {
        val u = when (val s = currentUserState) {
            is UiState.Success<*> -> s.data as? com.tamixa.domain.CurrentUser
            else -> null
        } ?: return@LaunchedEffect
        settingsViewModel.applyServerStoryArtOptIn(u.storyArtPersonalizationOptIn)
    }

    // After login/register: home once device language is chosen (same gate as pre-login first run).
    val postAuthHomeDestination = when {
        !settingsState.settingsLoaded -> null
        !settingsState.hasCompletedLanguageSelection -> null
        else -> Screen.Dashboard.route
    }

    /** Splash is always the graph root so branding matches before language, onboarding, or login. */
    val startDestination = Screen.Splash.route

    val snackbarHostState = remember { SnackbarHostState() }
    val appMessageNotifier: AppMessageNotifier = koinInject()
    val sessionExpired by authViewModel.sessionExpired.collectAsState()
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            appMessageNotifier.clear()
            authViewModel.logout()
            authViewModel.clearSessionExpired()
            navController.navigate(Screen.Login.route) {
                val root =
                    navController.graph.startDestinationRoute ?: Screen.Splash.route
                popUpTo(root) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
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
        startDestination = startDestination,
        enterTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (isOnboardingRoute(from) && isOnboardingRoute(to)) {
                slideInHorizontally(
                    animationSpec = tween(340),
                    initialOffsetX = { fullWidth -> fullWidth }
                ) + fadeIn(animationSpec = tween(240))
            } else {
                fadeIn(animationSpec = tween(140))
            }
        },
        exitTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (isOnboardingRoute(from) && isOnboardingRoute(to)) {
                slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { fullWidth -> -fullWidth }
                ) + fadeOut(animationSpec = tween(210))
            } else {
                fadeOut(animationSpec = tween(120))
            }
        },
        popEnterTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (isOnboardingRoute(from) && isOnboardingRoute(to)) {
                slideInHorizontally(
                    animationSpec = tween(320),
                    initialOffsetX = { fullWidth -> -fullWidth }
                ) + fadeIn(animationSpec = tween(230))
            } else {
                fadeIn(animationSpec = tween(140))
            }
        },
        popExitTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (isOnboardingRoute(from) && isOnboardingRoute(to)) {
                slideOutHorizontally(
                    animationSpec = tween(280),
                    targetOffsetX = { fullWidth -> fullWidth }
                ) + fadeOut(animationSpec = tween(200))
            } else {
                fadeOut(animationSpec = tween(120))
            }
        }
    ) {
        composable(Screen.Splash.route) {
            LaunchedEffect(
                settingsState.settingsLoaded,
                settingsState.hasCompletedLanguageSelection,
                authViewModel.isLoggedIn()
            ) {
                if (!settingsState.settingsLoaded) return@LaunchedEffect
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
        composable(Screen.OnboardingHook.route) {
            val scope = rememberCoroutineScope()
            OnboardingHookScreen(
                onStartStoryMagic = { navController.navigate(Screen.OnboardingDemo.route) },
                onSkip = {
                    scope.launch {
                        withContext(Dispatchers.Default) { settingsViewModel.completeOnboarding() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OnboardingHook.route) { inclusive = true }
                        }
                    }
                },
                onSwipeToNext = { navController.navigate(Screen.OnboardingDemo.route) }
            )
        }
        composable(Screen.OnboardingDemo.route) {
            OnboardingDemoScreen(
                onContinue = { navController.navigate(Screen.OnboardingVoiceInvitation.route) },
                onSkip = { navController.navigate(Screen.OnboardingVoiceInvitation.route) },
                onSwipeToNext = { navController.navigate(Screen.OnboardingVoiceInvitation.route) },
                onSwipeToPrevious = { navController.popBackStack() }
            )
        }
        composable(Screen.OnboardingVoiceInvitation.route) {
            val scope = rememberCoroutineScope()
            OnboardingVoiceInvitationScreen(
                onRecordVoice = { navController.navigate(Screen.OnboardingAvatarInvitation.route) },
                onSkip = {
                    scope.launch {
                        withContext(Dispatchers.Default) { settingsViewModel.completeOnboarding() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OnboardingHook.route) { inclusive = true }
                        }
                    }
                },
                onSwipeToNext = { navController.navigate(Screen.OnboardingAvatarInvitation.route) },
                onSwipeToPrevious = { navController.popBackStack() }
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
                },
                onSwipeToNext = null,
                onSwipeToPrevious = { navController.popBackStack() }
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
                onRequestPasswordlessCode = { authViewModel.requestPasswordlessCode(it) },
                onVerifyPasswordlessCode = { email, code, terms, privacy, parentalAttestation -> authViewModel.verifyPasswordlessCode(email, code, terms, privacy, parentalAttestation) },
                onClearPasswordlessState = { authViewModel.clearPasswordlessState() },
                onSendOtp = { authViewModel.sendOtp(it) },
                onVerifyOtp = { phone, code -> authViewModel.loginWithOtp(phone, code) },
                onClearOtpState = { authViewModel.clearOtpState() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
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
            LaunchedEffect(settingsState.settingsLoaded, settingsState.hasCompletedLanguageSelection) {
                if (settingsState.settingsLoaded && settingsState.hasCompletedLanguageSelection) {
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.LanguageSelection.route) { inclusive = true } }
                }
            }
        }
        composable(Screen.Dashboard.route) {
            var selectedCategory by remember { mutableStateOf(SampleData.categories.first()) }
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val appAnalytics: AppAnalytics = koinInject()
            LaunchedEffect(Unit) { appAnalytics.trackScreenView("dashboard") }
            val curated by storyViewModel.libraryStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val recentPlaybackWithStories by storyViewModel.recentPlaybackWithStories.collectAsState()
            val recommendedWithStories by storyViewModel.recommendedWithStories.collectAsState()
            val libraryLoading by storyViewModel.libraryLoading.collectAsState()
            val dashboardRefreshing by storyViewModel.dashboardRefreshing.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(prefLang) {
                storyViewModel.loadLibraryStories(prefLang)
                storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                storyViewModel.loadFavorites(prefLang)
                storyViewModel.loadRecommended(null, prefLang, TamixaConstants.RECOMMENDED_LIMIT)
            }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            val favorites by storyViewModel.favorites.collectAsState()
            val favoriteIds = favorites.map { it.id }.toSet()
            val vmStories = storyViewModel.allStories()
            val stories = if (vmStories.isEmpty()) SampleData.sampleStories() else vmStories
            val recentPlayback = recentPlaybackWithStories.map { row ->
                com.tamixa.ui.screen.RecentPlaybackItem(
                    story = row.story,
                    positionSeconds = row.dto.positionSeconds,
                    storySource = row.dto.storySource,
                    progressFraction = row.progressFraction
                )
            }
            val recommended = recommendedWithStories.map { (dto, story) ->
                com.tamixa.ui.screen.RecommendedItem(
                    story = story,
                    title = dto.title,
                    theme = dto.theme,
                    reason = dto.reason,
                    storyId = dto.storyId,
                    storySource = dto.storySource
                )
            }
            LaunchedEffect(Screen.Dashboard.route) {
                authViewModel.loadCurrentUser()
                subscriptionViewModel.loadSubscription()
                settingsViewModel.loadListeningStreak()
            }
            val timeBasedGreeting = remember {
                val hour = with(kotlinx.datetime.TimeZone.currentSystemDefault()) {
                    kotlinx.datetime.Clock.System.now().toLocalDateTime().hour
                }
                when { hour < 12 -> Strings.goodMorning(); hour < 17 -> Strings.goodAfternoon(); else -> Strings.goodEvening() }
            }
            val dashboardChildName = when (val u = currentUser) {
                is UiState.Success -> (u.data as? CurrentUser)?.displayNameOrFallback("")
                else -> null
            }?.takeIf { it.isNotBlank() }
            val spotlightPreviewRows = remember(curated) { curated.take(8) }
            val spotlightSectionLoading = libraryLoading && curated.isEmpty()
            DashboardScreen(
                greeting = timeBasedGreeting,
                childName = dashboardChildName,
                stories = stories,
                selectedCategory = selectedCategory,
                categories = SampleData.categories,
                onCategorySelect = { selectedCategory = it },
                onStoryClick = { story ->
                    val source = if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED
                    navController.navigate(Screen.AudioPlayer.withId(story.id, source))
                },
                onNewStory = { navController.navigate(Screen.StoryGeneration.route) },
                onRefresh = {
                    storyViewModel.refreshDashboard(prefLang)
                    settingsViewModel.loadListeningStreak()
                },
                isRefreshing = dashboardRefreshing,
                onNavigateToMyVoiceAndAvatar = { navController.navigate(Screen.MyVoiceAndAvatar.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.withHub()) },
                onNavigateToLibraryFunCorner = { navController.navigate(Screen.Library.withHub(Screen.Library.HUB_FUN)) },
                onNavigateToLibraryLearnSafety = { navController.navigate(Screen.Library.withHub(Screen.Library.HUB_LEARN_SAFETY)) },
                onNavigateToLibrarySimulator = { navController.navigate(Screen.Library.withHub(Screen.Library.HUB_SIMULATOR)) },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                recentPlayback = recentPlayback,
                recommended = recommended,
                apiBaseUrl = apiBaseUrl,
                languageCode = prefLang,
                favoriteStoryIds = favoriteIds,
                onRecommendedStoryClick = { id, source -> navController.navigate(Screen.AudioPlayer.withId(id, source)) },
                onFavoriteToggle = { story, add ->
                    if (add) {
                        storyViewModel.addFavorite(story.id, if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED)
                        appAnalytics.trackFavoriteAdd(story.id, if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED)
                    } else {
                        storyViewModel.removeFavorite(story.id)
                    }
                },
                usageStoriesUsed = usage?.storiesUsed ?: 0,
                usageStoriesLimit = usage?.storiesLimit,
                listeningStreakDays = settingsState.listeningStreakDays,
                spotlightPreview = spotlightPreviewRows,
                spotlightSectionLoading = spotlightSectionLoading,
                onSpotlightStoryClick = { s ->
                    navController.navigate(Screen.AudioPlayer.withId(s.id, TamixaConstants.STORY_SOURCE_LIBRARY))
                }
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
            val hubArg = libEntry.arguments?.getString("hub") ?: Screen.Library.HUB_BROWSE
            val initialLibraryHub = when (hubArg) {
                Screen.Library.HUB_FUN -> com.tamixa.ui.screen.LibraryHubTab.FunCorner
                Screen.Library.HUB_LEARN, Screen.Library.HUB_LEARN_SAFETY -> com.tamixa.ui.screen.LibraryHubTab.LearnSafety
                Screen.Library.HUB_SIMULATOR -> com.tamixa.ui.screen.LibraryHubTab.Simulator
                else -> com.tamixa.ui.screen.LibraryHubTab.Browse
            }
            val prefLangLib = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
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
                onStoryClick = { story ->
                    val source = if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED
                    navController.navigate(Screen.AudioPlayer.withId(story.id, source))
                },
                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onNavigateToLibrary = { },
                onNavigateToShortContent = { navController.navigate(Screen.ShortContent.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                apiBaseUrl = apiBaseUrl,
                onHubTabChange = { tab ->
                    appAnalyticsLib.trackLibraryHub(tab.toAnalyticsHubKey())
                },
            )
        }
        composable(Screen.Profile.route) {
            val storyApi: StoryApi = koinInject()
            val authApi: AuthApi = koinInject()
            val preferencesPort: PreferencesPort = koinInject()
            val profilePrefsScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                authViewModel.loadCurrentUser()
                storyViewModel.loadMyStories()
            }
            val myStories by storyViewModel.myStories.collectAsState()
            val lifeSkillRefreshVersion by storyViewModel.lifeSkillCountersRefreshVersion.collectAsState()
            val educationChildId = remember(myStories) { storyViewModel.firstEducationChildId() }
            var profileChildren by remember { mutableStateOf<List<com.tamixa.network.ProfileChildJson>>(emptyList()) }
            LaunchedEffect(myStories) {
                profileChildren = authApi.getProfile()?.children.orEmpty()
            }
            val lifeSkillChildOptions = remember(profileChildren, educationChildId) {
                when {
                    profileChildren.isNotEmpty() -> profileChildren.map { it.id to it.name }
                    educationChildId != null && educationChildId > 0L ->
                        listOf(educationChildId to com.tamixa.ui.strings.Strings.lifeSkillPracticeUnnamedChild())
                    else -> emptyList()
                }
            }
            var selectedLifeSkillChildId by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(lifeSkillChildOptions) {
                if (lifeSkillChildOptions.isEmpty()) {
                    selectedLifeSkillChildId = null
                    return@LaunchedEffect
                }
                val currentValid =
                    selectedLifeSkillChildId?.let { id -> lifeSkillChildOptions.any { it.first == id } } == true
                if (currentValid) return@LaunchedEffect
                val preferred = preferencesPort.getLifeSkillPreferredChildId()
                val preferredValid =
                    preferred != null && lifeSkillChildOptions.any { it.first == preferred }
                selectedLifeSkillChildId =
                    if (preferredValid) preferred else lifeSkillChildOptions.first().first
            }
            var lifeSkillCounters by remember { mutableStateOf<com.tamixa.network.LifeSkillCountersResponseDto?>(null) }
            var lifeSkillCountersLoading by remember { mutableStateOf(false) }
            LaunchedEffect(selectedLifeSkillChildId, lifeSkillRefreshVersion) {
                lifeSkillCounters = null
                val cid = selectedLifeSkillChildId ?: return@LaunchedEffect
                if (cid <= 0L) return@LaunchedEffect
                lifeSkillCountersLoading = true
                try {
                    lifeSkillCounters = storyApi.getLifeSkillCounters(cid)
                } finally {
                    lifeSkillCountersLoading = false
                }
            }
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
                educationChildId = educationChildId,
                lifeSkillCounters = lifeSkillCounters,
                lifeSkillCountersLoading = lifeSkillCountersLoading,
                lifeSkillChildOptions = lifeSkillChildOptions,
                selectedLifeSkillChildId = selectedLifeSkillChildId,
                onLifeSkillChildChange = { id ->
                    selectedLifeSkillChildId = id
                    profilePrefsScope.launch {
                        preferencesPort.setLifeSkillPreferredChildId(id)
                    }
                },
                onNavigateToReadingLevel = {
                    educationChildId?.let { cid ->
                        navController.navigate(Screen.ReadingLevel.withId(cid))
                    }
                },
                onNavigateToReadingStreak = {
                    educationChildId?.let { cid ->
                        navController.navigate(Screen.Streak.withId(cid))
                    }
                },
                onNavigateToVocabulary = {
                    educationChildId?.let { cid ->
                        navController.navigate(Screen.Vocabulary.withId(cid))
                    }
                },
                onNavigateToClassroom = {
                    educationChildId?.let { cid ->
                        navController.navigate(Screen.Classroom.withId(cid))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        /** Deep-link / legacy alias: same destination as [Screen.StorySelection]. */
        composable(Screen.Categories.route) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.StorySelection.route) {
                    popUpTo(Screen.Categories.route) { inclusive = true }
                }
            }
            Box(Modifier.fillMaxSize())
        }
        composable(Screen.Search.route) {
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val appAnalytics: AppAnalytics = koinInject()
            val prefLang = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(prefLang) { storyViewModel.loadFavorites(prefLang) }
            val searchResults by storyViewModel.searchResults.collectAsState()
            val searchLoading by storyViewModel.searchLoading.collectAsState()
            val searchError by storyViewModel.searchError.collectAsState()
            val favorites by storyViewModel.favorites.collectAsState()
            val favoriteIds = favorites.map { it.id }.toSet()
            var searchQuery by remember { mutableStateOf("") }
            LaunchedEffect(Unit) { appAnalytics.trackScreenView("search") }
            LaunchedEffect(searchQuery) {
                if (searchQuery.length >= 2) storyViewModel.searchStories(searchQuery, prefLang)
                else storyViewModel.searchStories("", prefLang)
            }
            SearchScreen(
                searchResults = searchResults,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearch = {
                    storyViewModel.searchStories(it, prefLang)
                    if (it.isNotBlank()) appAnalytics.trackSearch(it.length)
                },
                onBack = { navController.popBackStack() },
                onStoryClick = { storyId, storySource ->
                    navController.navigate(Screen.AudioPlayer.withId(storyId, storySource))
                },
                apiBaseUrl = apiBaseUrl,
                favoriteStoryIds = favoriteIds,
                onFavoriteToggle = { storyId, storySource, add ->
                    if (add) {
                        storyViewModel.addFavorite(storyId, storySource)
                        appAnalytics.trackFavoriteAdd(storyId, storySource)
                    } else {
                        storyViewModel.removeFavorite(storyId)
                    }
                },
                searchLoading = searchLoading,
                searchError = searchError,
                onRetry = { storyViewModel.searchStories(searchQuery, prefLang) }
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
        composable(Screen.ListeningHistory.route) {
            val prefLang = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
            ListeningHistoryScreen(
                storyViewModel = storyViewModel,
                languageCode = prefLang,
                onBack = { navController.popBackStack() },
                onStoryClick = { storyId, source -> navController.navigate(Screen.AudioPlayer.withId(storyId, source)) }
            )
        }
        composable(Screen.Favorites.route) {
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val appAnalytics: AppAnalytics = koinInject()
            val prefLang = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(Unit) { appAnalytics.trackScreenView("favorites") }
            FavoritesScreen(
                storyViewModel = storyViewModel,
                onBack = { navController.popBackStack() },
                onStoryClick = { story ->
                    val source = if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED
                    navController.navigate(Screen.AudioPlayer.withId(story.id, source))
                },
                languageCode = prefLang,
                apiBaseUrl = apiBaseUrl,
                onFavoriteRemoved = { id, source -> appAnalytics.trackFavoriteRemove(id, source) }
            )
        }
        composable(Screen.ShortContent.route) {
            val prefLang = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
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
        composable(Screen.StorySelection.route) {
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val curated by storyViewModel.libraryStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(prefLang) { storyViewModel.loadLibraryStories(prefLang) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            StorySelectionScreen(
                cachedStories = storyViewModel.allStories(),
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id, if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED)) },
                onBack = { navController.popBackStack() },
                listLayout = StorySelectionListLayout.LibraryPosterGrid,
                apiBaseUrl = apiBaseUrl,
            )
        }
        composable(Screen.StoryGeneration.route) {
            val generateState by storyViewModel.generateState.collectAsState()
            val usage by subscriptionViewModel.usage.collectAsState()
            var showSuccessModal by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                storyViewModel.clearGenerateState()
                subscriptionViewModel.loadSubscription()
                storyViewModel.loadGenerationTopics()
            }
            LaunchedEffect(generateState) {
                when (generateState) {
                    is UiState.Success<*> -> {
                        storyViewModel.loadMyStories()
                        subscriptionViewModel.loadSubscription()
                        showSuccessModal = true
                    }
                    else -> {}
                }
            }
            val generationTopics by storyViewModel.generationTopics.collectAsState()
            StoryGenerationScreen(
                generateState = generateState,
                generationTopics = generationTopics,
                storiesUsed = usage?.storiesUsed ?: 0,
                storiesLimit = usage?.storiesLimit,
                onGenerate = { storyViewModel.generateStory(it) },
                onBack = { navController.popBackStack() },
                onStoryGenerated = { },
                showSuccessModal = showSuccessModal,
                onDismissSuccess = {
                    showSuccessModal = false
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.StoryGeneration.route) { inclusive = true } }
                },
                onUpgradeRequired = { navController.navigate(Screen.Subscription.route) },
                onClearGenerateError = { storyViewModel.clearGenerateState() },
            )
        }
        composable(
            "audio/{storyId}?storySource={storySource}",
            arguments = listOf(
                navArgument("storyId") { type = NavType.LongType },
                navArgument("storySource") { defaultValue = TamixaConstants.STORY_SOURCE_GENERATED }
            )
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getLong("storyId") ?: 0L
            val storySource = backStackEntry.arguments?.getString("storySource") ?: TamixaConstants.STORY_SOURCE_GENERATED
            val scope = rememberCoroutineScope()
            val storyApi: StoryApi = koinInject()
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val prefLang = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE }
            val curated by storyViewModel.libraryStories.collectAsState()
            val favorites by storyViewModel.favorites.collectAsState()
            val allStories = storyViewModel.allStories()
            var story by remember(storyId) { mutableStateOf<com.tamixa.domain.Story?>(null) }
            var streamUrl by remember(storyId) { mutableStateOf<String?>(null) }
            var streamAvatarUrl by remember(storyId) { mutableStateOf<String?>(null) }
            var streamAvatarVideoUrl by remember(storyId) { mutableStateOf<String?>(null) }
            var streamWordTimings by remember(storyId) { mutableStateOf<List<com.tamixa.network.WordTiming>?>(null) }
            var streamNarrativeScenes by remember(storyId) { mutableStateOf<List<com.tamixa.network.NarrativeSceneVisual>?>(null) }
            var streamHostStoryClipUrl by remember(storyId) { mutableStateOf<String?>(null) }
            var streamDurationSeconds by remember(storyId) { mutableStateOf<Int?>(null) }
            var streamUrlFailed by remember(storyId) { mutableStateOf(false) }
            var streamUrlLoading by remember(storyId) { mutableStateOf(false) }
            var streamUrlError by remember(storyId) { mutableStateOf<String?>(null) }
            var streamLoadRetryTrigger by remember(storyId) { mutableStateOf(0) }
            var avatarVideoSurfaceFailed by remember(storyId) { mutableStateOf(false) }
            var synthesizedUri by remember(storyId) { mutableStateOf<String?>(null) }
            var conversationalTtsUri by remember(storyId) { mutableStateOf<String?>(null) }
            var selectedVoice by remember(storyId) { mutableStateOf(TamixaConstants.VOICE_PROFILE_DEFAULT) }
            var selectedPlaybackMode by remember(storyId) { mutableStateOf("default") }
            var streamAvatarStatus by remember(storyId) { mutableStateOf<String?>(null) }
            var streamVoiceFallback by remember(storyId) { mutableStateOf(false) }
            var availableVoices by remember(storyId) { mutableStateOf<List<com.tamixa.network.VoiceOptionDto>>(emptyList()) }
            var showUpgradeDialog by remember { mutableStateOf(false) }
            val appMessageNotifier: AppMessageNotifier = koinInject()
            var showSleepTimerDialog by remember { mutableStateOf(false) }
            val appAnalytics: AppAnalytics = koinInject()
            val interactiveStoryGraph = remember(story?.id, story?.interactiveGraph) {
                com.tamixa.ui.edu.parseInteractiveStoryGraph(story?.interactiveGraph)
            }
            var currentSegmentId by remember(story?.id) { mutableStateOf<String?>(null) }
            var interactiveSegmentEpoch by remember(story?.id) { mutableIntStateOf(0) }
            var choiceOverlayVisible by remember(story?.id) { mutableStateOf(false) }
            var missionOverlayVisible by remember(story?.id) { mutableStateOf(false) }
            LaunchedEffect(story?.id, interactiveStoryGraph?.startSegmentId) {
                currentSegmentId = interactiveStoryGraph?.startSegmentId
                interactiveSegmentEpoch = 0
                choiceOverlayVisible = false
                missionOverlayVisible = false
            }
            val effectiveInteractiveMode =
                storySource == TamixaConstants.STORY_SOURCE_LIBRARY && interactiveStoryGraph != null
            val effectiveSegmentId = currentSegmentId ?: interactiveStoryGraph?.startSegmentId
            val currentInteractiveSegment =
                interactiveStoryGraph?.segments?.get(effectiveSegmentId.orEmpty())
            LaunchedEffect(storyId, allStories, curated, prefLang, storySource) {
                story = if (storyId > 0) {
                    storyViewModel.fetchStoryById(storyId, prefLang, storySource, forceRefresh = true)
                } else {
                    null
                } ?: allStories.find { it.id == storyId }
                    ?: SampleData.sampleStories().find { it.id == storyId }
            }
            var familyVoiceRefreshTrigger by remember(storyId) { mutableStateOf(0) }
            val voiceRefreshTrigger by voiceViewModel.refreshVoicesTrigger.collectAsState(0)
            LaunchedEffect(prefLang, storyId) {
                storyViewModel.loadFavorites(prefLang)
            }
            LaunchedEffect(story?.id, familyVoiceRefreshTrigger, voiceRefreshTrigger, storySource) {
                val id = story?.id ?: return@LaunchedEffect
                availableVoices = storyApi.getAvailableVoices(id, prefLang)
                if (availableVoices.isEmpty()) availableVoices = listOf(
                    com.tamixa.network.VoiceOptionDto(TamixaConstants.VOICE_PROFILE_DEFAULT, false),
                    com.tamixa.network.VoiceOptionDto("calm", true)
                )
                val voices = availableVoices
                val hasCloned = voices.any { it.voiceProfile.startsWith("cloned:", ignoreCase = true) }
                val deduped = if (hasCloned) voices.filter { !it.voiceProfile.equals("family", ignoreCase = true) } else voices
                val (voicePref, modePref) = storyApi.getVoicePreference(id, storySource)
                selectedVoice = when {
                    voicePref.equals(TamixaConstants.VOICE_PROFILE_DEFAULT, ignoreCase = true) -> TamixaConstants.VOICE_PROFILE_DEFAULT
                    deduped.any { it.voiceProfile.equals(voicePref, ignoreCase = true) } -> voicePref
                    else -> TamixaConstants.VOICE_PROFILE_DEFAULT
                }
                val voiceIsCloned = selectedVoice.startsWith("cloned:", ignoreCase = true) || selectedVoice.equals("family", ignoreCase = true)
                val sanitizedMode = when {
                    modePref == "avatar" && !voiceIsCloned -> "default"
                    modePref in setOf("default", "my_voice", "avatar") -> modePref
                    else -> "default"
                }
                selectedPlaybackMode = sanitizedMode
                if (sanitizedMode != modePref) {
                    scope.launch { storyApi.setVoicePreference(id, storySource, selectedVoice, sanitizedMode) }
                }
            }
            LaunchedEffect(streamVoiceFallback) {
                if (streamVoiceFallback) {
                    appMessageNotifier.show(Strings.voiceFallbackMessage(), tag = "voiceFallback")
                    streamVoiceFallback = false
                }
            }
            LaunchedEffect(
                story?.id,
                story?.audioFileUrl,
                story?.content,
                apiBaseUrl,
                prefLang,
                selectedVoice,
                selectedPlaybackMode,
                streamLoadRetryTrigger,
                interactiveSegmentEpoch,
                effectiveSegmentId,
                effectiveInteractiveMode,
                interactiveStoryGraph,
                currentSegmentId,
            ) {
                val s = story ?: return@LaunchedEffect
                streamUrlFailed = false
                streamUrlError = null
                if (effectiveInteractiveMode && interactiveStoryGraph != null) {
                    val segId = currentSegmentId ?: interactiveStoryGraph.startSegmentId
                    val seg = interactiveStoryGraph.segments[segId]
                    if (seg != null) {
                        val rawUrl = seg.audioUrl.trim()
                        if (rawUrl.isNotBlank()) {
                            streamUrlLoading = true
                            try {
                                streamUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                                    rawUrl
                                } else {
                                    ApiConfig.resolveAudioUrl(apiBaseUrl, rawUrl) ?: rawUrl
                                }
                                streamAvatarUrl = null
                                streamAvatarVideoUrl = null
                                streamAvatarStatus = null
                                streamWordTimings = null
                                streamDurationSeconds = null
                                streamNarrativeScenes = null
                                streamHostStoryClipUrl = null
                            } catch (e: Exception) {
                                com.tamixa.util.TamixaLog.w("TamixaNavHost", "interactive segment audio failed", e)
                                streamUrlError = e.message ?: "Unable to prepare audio"
                            } finally {
                                streamUrlLoading = false
                            }
                            return@LaunchedEffect
                        }
                    }
                }
                val isPlaceholder = s.audioFileUrl.isNullOrBlank() || s.audioFileUrl.startsWith(TamixaConstants.PLACEHOLDER_AUDIO_PREFIX)
                val voiceParam = if (selectedVoice == TamixaConstants.VOICE_PROFILE_DEFAULT) null else selectedVoice
                if (isPlaceholder && !s.content.isNullOrBlank()) {
                    streamUrlLoading = false
                    streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, s.audioFileUrl)
                    streamAvatarUrl = null
                    streamAvatarVideoUrl = null
                    streamAvatarStatus = null
                    streamWordTimings = null
                    streamNarrativeScenes = null
                    streamHostStoryClipUrl = null
                } else {
                    streamUrlLoading = true
                    try {
                        when (val result = storyApi.getStreamUrl(s.id, prefLang, voiceParam, storySource, selectedPlaybackMode)) {
                            is StoryApi.StreamUrlResult.Url -> {
                                streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, result.url) ?: result.url
                                streamAvatarUrl = result.avatarUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                                streamAvatarVideoUrl = result.avatarVideoUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                                streamAvatarStatus = result.avatarStatus
                                streamVoiceFallback = result.voiceFallback
                                streamWordTimings = result.wordTimings
                                streamDurationSeconds = result.durationSeconds
                                streamNarrativeScenes = result.narrativeScenes
                                streamHostStoryClipUrl = result.hostStoryClipUrl
                            }
                            is StoryApi.StreamUrlResult.UpgradeRequired -> {
                                showUpgradeDialog = true
                                streamUrl = null
                                streamAvatarUrl = null
                                streamAvatarVideoUrl = null
                                streamAvatarStatus = null
                                streamWordTimings = null
                                streamDurationSeconds = null
                                streamNarrativeScenes = null
                                streamHostStoryClipUrl = null
                            }
                            is StoryApi.StreamUrlResult.NotFound -> {
                                if (voiceParam != null) {
                                    val fallback = storyApi.getStreamUrl(s.id, prefLang, null, storySource, selectedPlaybackMode)
                                    if (fallback is StoryApi.StreamUrlResult.Url) {
                                        streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, fallback.url) ?: fallback.url
                                        streamAvatarUrl = fallback.avatarUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                                        streamAvatarVideoUrl = fallback.avatarVideoUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                                        streamAvatarStatus = fallback.avatarStatus
                                        streamVoiceFallback = fallback.voiceFallback
                                        streamWordTimings = fallback.wordTimings
                                        streamDurationSeconds = fallback.durationSeconds
                                        streamNarrativeScenes = fallback.narrativeScenes
                                        streamHostStoryClipUrl = fallback.hostStoryClipUrl
                                    } else {
                                        streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, s.audioFileUrl)
                                        streamAvatarUrl = null
                                        streamAvatarVideoUrl = null
                                        streamAvatarStatus = null
                                        streamDurationSeconds = null
                                        streamNarrativeScenes = null
                                        streamHostStoryClipUrl = null
                                    }
                                } else {
                                    streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, s.audioFileUrl)
                                    streamAvatarUrl = null
                                    streamAvatarVideoUrl = null
                                    streamAvatarStatus = null
                                    streamDurationSeconds = null
                                    streamNarrativeScenes = null
                                    streamHostStoryClipUrl = null
                                }
                            }
                        }
                    } catch (e: Exception) {
                        TamixaLog.w("TamixaNavHost", "getStreamUrl failed", e)
                        streamUrlError = e.message ?: "Unable to prepare audio"
                    } finally {
                        streamUrlLoading = false
                    }
                }
            }
            val useTts =
                story?.content?.trim()?.let { NarrationTextUtils.stripRemainingMarkers(it).isNotBlank() } == true &&
                    !effectiveInteractiveMode
            LaunchedEffect(story?.id, story?.content, prefLang, useTts) {
                if (useTts) {
                    val s = story ?: return@LaunchedEffect
                    val content =
                        NarrationTextUtils.stripRemainingMarkers(s.content?.trim() ?: "")
                    if (content.isBlank()) {
                        synthesizedUri = null
                        conversationalTtsUri = null
                        return@LaunchedEffect
                    }
                    synthesizedUri = withContext(Dispatchers.Default) {
                        com.tamixa.platform.synthesizeStoryToFile(content, prefLang)
                    }
                    val script = storyApi.getNarrationScript(s.id, prefLang)
                    if (!script.isNullOrBlank()) {
                        val scriptClean = NarrationTextUtils.stripRemainingMarkers(script.trim())
                        conversationalTtsUri = if (scriptClean.isNotBlank()) {
                            withContext(Dispatchers.Default) {
                                com.tamixa.platform.synthesizeStoryToFile(scriptClean, prefLang)
                            }
                        } else {
                            null
                        }
                    } else {
                        conversationalTtsUri = null
                    }
                } else {
                    synthesizedUri = null
                    conversationalTtsUri = null
                }
            }
            val currentStreamUrl = streamUrl
            val hasAvatarVideo = !streamAvatarVideoUrl.isNullOrBlank()
            val reduceMotion = platformIsReduceMotionEnabled()
            val hostStoryClipResolved = streamHostStoryClipUrl?.let { raw ->
                ApiConfig.resolveCoverUrl(apiBaseUrl, raw) ?: raw
            }
            val hostClipVideoUrlForPlayer =
                if (!hasAvatarVideo && !reduceMotion && !hostStoryClipResolved.isNullOrBlank()) hostStoryClipResolved else null
            val hasRealAudio = !currentStreamUrl.isNullOrBlank() && !currentStreamUrl.startsWith(TamixaConstants.PLACEHOLDER_AUDIO_PREFIX)
            val ttsFallback = conversationalTtsUri ?: synthesizedUri
            val playbackUrl = when {
                hasAvatarVideo -> streamAvatarVideoUrl
                streamUrlFailed -> ttsFallback
                hasRealAudio -> ApiConfig.resolveAudioUrl(apiBaseUrl, currentStreamUrl) ?: currentStreamUrl
                else -> ttsFallback
            }
            val isTtsFile = playbackUrl?.startsWith("file://") == true && !hasAvatarVideo
            val avatarVideoResult = if (hasAvatarVideo && playbackUrl != null) {
                rememberAvatarVideoController(
                    videoUrl = playbackUrl,
                    storyTitle = story?.title ?: story?.theme ?: "Story",
                    storyTheme = story?.theme ?: "",
                    scope = scope,
                    onProgressChanged = { },
                    onPlaybackError = { avatarVideoSurfaceFailed = true },
                    muteVideoAudio = false,
                    repeatVideo = false
                )
            } else null
            val hostClipVideoResult = rememberAvatarVideoController(
                videoUrl = hostClipVideoUrlForPlayer,
                storyTitle = story?.title ?: story?.theme ?: "Story",
                storyTheme = story?.theme ?: "",
                scope = scope,
                onProgressChanged = { },
                onPlaybackError = null,
                muteVideoAudio = true,
                repeatVideo = true
            )
            val controller = when {
                avatarVideoResult != null -> avatarVideoResult.controller
                isTtsFile -> rememberLocalFileController(
                    fileUri = playbackUrl,
                    storyTitle = story?.title ?: story?.theme ?: "Story",
                    storyTheme = story?.theme ?: "",
                    scope = scope,
                    onProgressChanged = { }
                )
                else -> rememberStreamingController(
                    streamUrl = playbackUrl,
                    storyTitle = story?.title ?: story?.theme ?: "Story",
                    storyTheme = story?.theme ?: "",
                    scope = scope,
                    onProgressChanged = { },
                    onPlaybackError = if (useTts) { { streamUrlFailed = true } } else null
                )
            }
            val tracker = storyViewModel.playbackTracker()
            LaunchedEffect(story?.id) { tracker.reset() }
            val audioPlayerPageLoading =
                (story == null && storyId > 0) ||
                    (useTts && synthesizedUri == null && story != null) ||
                    streamUrlLoading
            LaunchedEffect(playbackUrl, controller.isReady, audioPlayerPageLoading, streamUrlError) {
                if (audioPlayerPageLoading || streamUrlError != null) return@LaunchedEffect
                if (playbackUrl != null && controller.isReady && !controller.isPlaying) {
                    delay(48)
                    if (audioPlayerPageLoading || streamUrlError != null) return@LaunchedEffect
                    if (controller.isReady && !controller.isPlaying) {
                        controller.playPause()
                    }
                }
            }
            val hostClipController = hostClipVideoResult.controller
            // Host clip follows main narration only (no separate auto-start); avoids racing main's LaunchedEffect above.
            LaunchedEffect(controller.isPlaying, hostClipVideoUrlForPlayer, hostClipController.isReady) {
                if (hostClipVideoUrlForPlayer == null || !hostClipController.isReady) return@LaunchedEffect
                val mainPlaying = controller.isPlaying
                val clipPlaying = hostClipController.isPlaying
                if (mainPlaying && !clipPlaying) hostClipController.playPause()
                else if (!mainPlaying && clipPlaying) hostClipController.playPause()
            }
            LaunchedEffect(hostClipVideoUrlForPlayer, story?.id) {
                val sid = story?.id ?: return@LaunchedEffect
                if (hostClipVideoUrlForPlayer == null) return@LaunchedEffect
                val src =
                    if (story?.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED
                appAnalytics.trackHostStoryClipImpression(sid, src)
            }
            val analytics = remember(tracker) {
                com.tamixa.ui.screen.StoryAnalyticsCallbacks(
                    onStoryStarted = { s, pos -> tracker.onStoryStarted(s, pos) },
                    onProgress = { s, p, pos -> tracker.onProgress(s, p, pos) },
                    onCompleted = { s, pos -> tracker.onCompleted(s, pos) },
                    onStoppedEarly = { s, pos -> tracker.onStoppedEarly(s, pos) }
                )
            }
            val hasMissionContent =
                !story?.postStoryMission.isNullOrBlank() || !story?.postStoryResourceUrl.isNullOrBlank()
            LaunchedEffect(
                effectiveInteractiveMode,
                interactiveStoryGraph,
                currentSegmentId,
                interactiveSegmentEpoch,
                story?.id,
                hasMissionContent,
            ) {
                if (!effectiveInteractiveMode || interactiveStoryGraph == null) return@LaunchedEffect
                while (true) {
                    delay(400)
                    val segId = currentSegmentId ?: interactiveStoryGraph.startSegmentId
                    val seg = interactiveStoryGraph.segments[segId] ?: continue
                    if (seg.choices.isNotEmpty()) {
                        if (controller.progress >= 0.97f && !choiceOverlayVisible) {
                            choiceOverlayVisible = true
                            if (controller.isPlaying) controller.playPause()
                        }
                    } else if (controller.progress >= 0.98f && hasMissionContent && !missionOverlayVisible) {
                        missionOverlayVisible = true
                        if (controller.isPlaying) controller.playPause()
                    }
                }
            }
            val playbackSubtitle = remember(story?.id, story?.theme, story?.category, story?.title, storySource) {
                story?.let { com.tamixa.ui.listenerPlaybackSubtitle(it, storySource) }
            }
            if (showUpgradeDialog) {
                AlertDialog(
                    onDismissRequest = { showUpgradeDialog = false },
                    shape = TamixaDialogDefaults.shape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            Strings.premiumVoiceTitle(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Text(
                            Strings.premiumVoiceMessage(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUpgradeDialog = false
                                navController.navigate(Screen.Subscription.route)
                            },
                            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                        ) { Text(Strings.upgrade()) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showUpgradeDialog = false
                            selectedVoice = TamixaConstants.VOICE_PROFILE_DEFAULT
                        }) { Text(Strings.cancel()) }
                    }
                )
            }
            com.tamixa.platform.PlatformBackHandler {
                scope.launch {
                    val s = story
                    if (s != null) {
                        val totalSec = (streamDurationSeconds?.takeIf { it > 0 }
                            ?: (s.readingTimeMinutes * 60).toInt()).coerceAtLeast(1)
                        val pos = (controller.progress * totalSec).toInt().coerceAtLeast(0)
                        if (controller.progress < 0.99f) {
                            tracker.onStoppedEarly(s, pos)
                        }
                        delay(400)
                    }
                    storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                    navController.popBackStack()
                }
            }
            if (showSleepTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    shape = TamixaDialogDefaults.shape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            Strings.sleepTimer(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column {
                            Text(
                                Strings.sleepTimerHint(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            TamixaConstants.SLEEP_TIMER_PRESETS.forEach { min ->
                                TextButton(onClick = {
                                    controller.setSleepTimer(min)
                                    showSleepTimerDialog = false
                                }) { Text(Strings.minutesShort(min)) }
                            }
                            TextButton(onClick = {
                                controller.setSleepTimer(0)
                                showSleepTimerDialog = false
                            }) { Text(Strings.cancelTimer()) }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSleepTimerDialog = false }) { Text(Strings.cancel()) }
                    }
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
            AudioPlayerScreen(
                story = story,
                playbackSubtitle = playbackSubtitle,
                showInteractivePracticeChip = effectiveInteractiveMode,
                playbackUsesDeviceTts = isTtsFile,
                wordTimings = streamWordTimings,
                isPlayerReady = controller.isReady,
                isPlaying = controller.isPlaying,
                onPlayPause = controller::playPause,
                onRewind = controller::rewind,
                onFastForward = controller::fastForward,
                apiBaseUrl = apiBaseUrl,
                storytellingAvatarUrl = if (avatarVideoResult == null || avatarVideoSurfaceFailed) streamAvatarUrl else null,
                storytellingAvatarVideoContent = if (!avatarVideoSurfaceFailed && avatarVideoResult != null) {
                    avatarVideoResult.let { r ->
                        { com.tamixa.platform.AvatarVideoSurface(player = r.player, modifier = Modifier.fillMaxSize()) }
                    }
                } else null,
                hostStoryClipVideoContent = if (hostClipVideoUrlForPlayer != null && hostClipVideoResult.player != null) {
                    {
                        com.tamixa.platform.AvatarVideoSurface(
                            player = hostClipVideoResult.player,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        )
                    }
                } else null,
                onNavigateToVoiceUpload = {
                    voiceViewModel.setReturnToStory(storyId, storySource)
                    navController.navigate(Screen.VoiceUpload.route) { launchSingleTop = true }
                },
                onNavigateToMyVoiceAndAvatar = {
                    voiceViewModel.setReturnToStory(storyId, storySource)
                    avatarViewModel.setReturnToStory(storyId, storySource)
                    navController.navigate(Screen.MyVoiceAndAvatar.route) { launchSingleTop = true }
                },
                hasAvatarAvailable = hasAvatarVideo,
                avatarStatus = streamAvatarStatus,
                playbackMode = selectedPlaybackMode,
                isPremiumForVoice = subscription?.isActive == true,
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                onBack = {
                    scope.launch {
                        val s = story
                        if (s != null) {
                            val totalSec = (streamDurationSeconds?.takeIf { it > 0 }
                                ?: (s.readingTimeMinutes * 60).toInt()).coerceAtLeast(1)
                            val pos = (controller.progress * totalSec).toInt().coerceAtLeast(0)
                            if (controller.progress < 0.99f) {
                                tracker.onStoppedEarly(s, pos)
                            }
                            delay(400)
                        }
                        storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                        navController.popBackStack()
                    }
                },
                progress = controller.progress,
                onSleepTimer = { showSleepTimerDialog = true },
                onDownload = if (hasAvatarVideo) {
                    { controller.download(streamAvatarVideoUrl!!, "story_${story?.id ?: 0}_avatar", "video/mp4") }
                } else if (hasRealAudio) {
                    { controller.download(currentStreamUrl!!, "story_${story?.id ?: 0}", "audio/mpeg") }
                } else null,
                onShare = { controller.share() },
                bottomNavSelectedTab = if (storySource == TamixaConstants.STORY_SOURCE_LIBRARY) TamixaTab.Library else TamixaTab.Home,
                onBottomNavHome = {
                    scope.launch {
                        story?.let { s ->
                            val totalSec = (streamDurationSeconds?.takeIf { it > 0 }
                                ?: (s.readingTimeMinutes * 60).toInt()).coerceAtLeast(1)
                            val pos = (controller.progress * totalSec).toInt().coerceAtLeast(0)
                            if (controller.progress < 0.99f) tracker.onStoppedEarly(s, pos)
                        }
                        storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                        navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } }
                    }
                },
                onBottomNavLibrary = {
                    scope.launch {
                        story?.let { s ->
                            val totalSec = (streamDurationSeconds?.takeIf { it > 0 }
                                ?: (s.readingTimeMinutes * 60).toInt()).coerceAtLeast(1)
                            val pos = (controller.progress * totalSec).toInt().coerceAtLeast(0)
                            if (controller.progress < 0.99f) tracker.onStoppedEarly(s, pos)
                        }
                        storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                        navController.navigate(Screen.Library.withHub()) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                },
                onBottomNavFunAndLearn = {
                    scope.launch {
                        story?.let { s ->
                            val totalSec = (streamDurationSeconds?.takeIf { it > 0 }
                                ?: (s.readingTimeMinutes * 60).toInt()).coerceAtLeast(1)
                            val pos = (controller.progress * totalSec).toInt().coerceAtLeast(0)
                            if (controller.progress < 0.99f) tracker.onStoppedEarly(s, pos)
                        }
                        storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                        navController.navigate(Screen.ShortContent.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                },
                onBottomNavProfile = {
                    scope.launch {
                        story?.let { s ->
                            val totalSec = (streamDurationSeconds?.takeIf { it > 0 }
                                ?: (s.readingTimeMinutes * 60).toInt()).coerceAtLeast(1)
                            val pos = (controller.progress * totalSec).toInt().coerceAtLeast(0)
                            if (controller.progress < 0.99f) tracker.onStoppedEarly(s, pos)
                        }
                        storyViewModel.loadRecentPlayback(TamixaConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                },
                onRemix = { id, instruction ->
                    storyViewModel.remixStory(id, instruction) { newStory ->
                        navController.popBackStack()
                        navController.navigate(Screen.AudioPlayer.withId(newStory.id, TamixaConstants.STORY_SOURCE_GENERATED))
                    }
                },
                analytics = analytics,
                isLoading = (story == null && storyId > 0) || (useTts && synthesizedUri == null && story != null) || streamUrlLoading,
                loadingMessage = if (streamUrlLoading) Strings.preparingAudio() else null,
                loadError = streamUrlError,
                onRetryLoad = if (streamUrlError != null) { { streamUrlError = null; streamLoadRetryTrigger++ } } else null,
                selectedVoice = selectedVoice,
                onVoiceAndModeChange = { newVoice, newMode ->
                    selectedVoice = newVoice
                    selectedPlaybackMode = newMode
                    story?.id?.let { sid ->
                        scope.launch { storyApi.setVoicePreference(sid, storySource, newVoice, newMode) }
                    }
                },
                availableVoices = availableVoices,
                durationSeconds = streamDurationSeconds,
                narrativeScenes = streamNarrativeScenes,
                storyArtPersonalizationOptIn = settingsState.storyArtPersonalizationOptIn,
                isFavorite = story?.let { s -> favorites.any { it.id == s.id } } == true,
                onFavoriteToggle = story?.let { s ->
                    {
                        val src = if (s.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED
                        val fav = favorites.any { it.id == s.id }
                        if (fav) {
                            storyViewModel.removeFavorite(s.id) {
                                appAnalytics.trackFavoriteRemove(s.id, src)
                            }
                        } else {
                            storyViewModel.addFavorite(s.id, src) {
                                appAnalytics.trackFavoriteAdd(s.id, src)
                            }
                        }
                    }
                },
                onAddToList = story?.let { s ->
                    {
                        val src = if (s.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED
                        if (favorites.any { it.id == s.id }) {
                            appMessageNotifier.show(Strings.playerAlreadyInListSnackbar())
                        } else {
                            storyViewModel.addFavorite(s.id, src) {
                                appMessageNotifier.show(Strings.playerAddedToListSnackbar())
                                appAnalytics.trackFavoriteAdd(s.id, src)
                            }
                        }
                    }
                },
                onVolumeClick = null,
                onOpenQuiz = story?.childId?.takeIf { it > 0L }?.let { cid ->
                    {
                        navController.navigate(Screen.Quiz.withIds(storyId, cid))
                    }
                }
            )
            val segmentChoices = currentInteractiveSegment?.choices
            LaunchedEffect(choiceOverlayVisible, segmentChoices, interactiveStoryGraph, apiBaseUrl) {
                if (!choiceOverlayVisible || segmentChoices.isNullOrEmpty() || interactiveStoryGraph == null) return@LaunchedEffect
                val resolved = segmentChoices.mapNotNull { ch ->
                    val seg = interactiveStoryGraph.segments[ch.nextSegmentId] ?: return@mapNotNull null
                    val raw = seg.audioUrl.trim()
                    when {
                        raw.isBlank() -> null
                        raw.startsWith("http://") || raw.startsWith("https://") -> raw
                        else -> ApiConfig.resolveAudioUrl(apiBaseUrl, raw) ?: raw
                    }
                }
                if (resolved.isNotEmpty()) storyApi.prefetchInteractiveSegmentAudio(resolved)
            }
            if (choiceOverlayVisible && !segmentChoices.isNullOrEmpty()) {
                com.tamixa.ui.edu.InteractiveChoiceOverlay(
                    choices = segmentChoices,
                    overlayStyle = interactiveStoryGraph?.overlayStyle,
                    onChoice = { ch ->
                        choiceOverlayVisible = false
                        val segKey = effectiveSegmentId.orEmpty()
                        story?.let { st ->
                            appAnalytics.trackInteractiveBranch(st.id, storySource, prefLang)
                        }
                        scope.launch {
                            val cid = storyViewModel.firstEducationChildId()
                            val st = story
                            if (cid != null && st != null) {
                                val ok = storyApi.recordLifeSkillChoice(
                                    libraryStoryId = st.id,
                                    childId = cid,
                                    segmentId = segKey,
                                    choiceId = ch.id,
                                    skillDeltas = ch.skillDeltas,
                                )
                                if (ok) storyViewModel.bumpLifeSkillCountersRefresh()
                            }
                        }
                        currentSegmentId = ch.nextSegmentId
                        interactiveSegmentEpoch++
                    },
                )
            }
            if (missionOverlayVisible && hasMissionContent) {
                com.tamixa.ui.edu.MissionCardOverlay(
                    missionText = story?.postStoryMission,
                    resourceUrl = story?.postStoryResourceUrl,
                    onDismiss = { missionOverlayVisible = false },
                    onOpenResource = { com.tamixa.platform.openUrl(it) },
                )
            }
            }
        }
        composable(Screen.MyVoiceAndAvatar.route) {
            LaunchedEffect(Unit) { voiceViewModel.loadProfiles() }
            val hasMyVoice = (profilesState as? UiState.Success)?.data?.isNotEmpty() == true
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
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                hasMyVoice = hasMyVoice,
                onBackToStory = if (voiceViewModel.hasReturnToStory()) { { navController.popBackStack() } } else null
            )
        }
        composable(Screen.VoiceUpload.route) {
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val myStories by storyViewModel.myStories.collectAsState()
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            LaunchedEffect(uploadState) {
                if (uploadState is UiState.Success && (uploadState as UiState.Success).data.id > 0) {
                    voiceViewModel.getAndClearReturnToStory()?.let { _ ->
                        voiceViewModel.notifyVoiceUploadCompleted()
                        navController.popBackStack()
                    }
                }
            }
            val launchAudioPicker = rememberAudioPickerLauncher { bytes, name ->
                bytes?.let { voiceViewModel.uploadVoice(it, name ?: "voice_audio") }
            }
            VoiceUploadScreen(
                uploadState = uploadState,
                profilesState = profilesState,
                voiceStories = myStories,
                onUpload = { bytes, name -> voiceViewModel.uploadVoice(bytes, name) },
                onPickAudio = launchAudioPicker,
                onLoadProfiles = { voiceViewModel.loadProfiles() },
                onStoryClick = { story ->
                    navController.navigate(Screen.AudioPlayer.withId(story.id, TamixaConstants.STORY_SOURCE_GENERATED))
                },
                onBack = { navController.popBackStack() },
                isPremium = subscription?.isActive == true,
                onUpgradeClick = { navController.navigate(Screen.Subscription.route) },
                apiBaseUrl = apiBaseUrl,
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
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val myStories by storyViewModel.myStories.collectAsState()
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            LaunchedEffect(avatarUploadState) {
                if (avatarUploadState is UiState.Success && (avatarUploadState as UiState.Success).data.isNotBlank()) {
                    avatarViewModel.getAndClearReturnToStory()?.let { _ ->
                        navController.popBackStack()
                    }
                }
            }
            val launchImagePicker = rememberImagePickerLauncher { bytes, contentType ->
                bytes?.let { avatarViewModel.uploadAvatar(it, contentType ?: "image/jpeg") }
            }
            AvatarUploadScreen(
                uploadState = avatarUploadState,
                avatarUrl = avatarUrl,
                avatarStories = myStories,
                onUpload = { bytes, contentType -> avatarViewModel.uploadAvatar(bytes, contentType) },
                onPickImage = launchImagePicker,
                onDelete = { avatarViewModel.deleteAvatar() },
                onLoadAvatar = { avatarViewModel.loadAvatar() },
                onStoryClick = { story ->
                    navController.navigate(Screen.AudioPlayer.withId(story.id, TamixaConstants.STORY_SOURCE_GENERATED))
                },
                onBack = { navController.popBackStack() },
                apiBaseUrl = apiBaseUrl,
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
        composable(Screen.Subscription.route) {
            LaunchedEffect(Unit) { subscriptionViewModel.loadSubscription() }
            SubscriptionScreen(
                subscription = subscription,
                usage = usage,
                loading = subscriptionLoading,
                loadError = subscriptionLoadError,
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
                        onUrl = { url -> com.tamixa.platform.openUrl(url) },
                        onFallback = { com.tamixa.platform.openUrl(com.tamixa.platform.getSubscriptionWebUrl()) }
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            val settingsScope = rememberCoroutineScope()
            LaunchedEffect(Unit) { settingsViewModel.loadSettings() }
            SettingsScreen(
                languageCode = settingsState.languageCode.ifEmpty { TamixaConstants.DEFAULT_LANGUAGE },
                useSystemTheme = settingsState.useSystemTheme,
                darkMode = settingsState.darkMode,
                preferredVoiceProfile = settingsState.preferredVoiceProfile.ifEmpty { TamixaConstants.VOICE_PROFILE_DEFAULT },
                consentRecords = settingsState.consentRecords,
                exportJobs = settingsState.exportJobs,
                listeningProgress = settingsState.listeningProgress,
                settingsLoading = settingsState.settingsLoading,
                settingsLoadError = settingsState.settingsLoadError,
                exporting = settingsState.exporting,
                onPreferredVoiceChange = { settingsViewModel.setPreferredVoiceProfile(it) },
                storyArtPersonalizationOptIn = settingsState.storyArtPersonalizationOptIn,
                onStoryArtPersonalizationOptInChange = { settingsViewModel.setStoryArtPersonalizationOptIn(it) },
                apiBaseUrlOverride = settingsState.apiBaseUrlOverride,
                subscriptionWebUrlOverride = settingsState.subscriptionWebUrlOverride,
                serverEnvironmentMessage = settingsState.serverEnvironmentMessage,
                serverEnvironmentError = settingsState.serverEnvironmentError,
                onApiBaseUrlOverrideChange = { settingsViewModel.setApiBaseUrlOverrideDraft(it) },
                onSubscriptionWebUrlOverrideChange = { settingsViewModel.setSubscriptionWebUrlOverrideDraft(it) },
                onSaveServerEnvironment = { settingsViewModel.saveServerEnvironment() },
                onClearServerEnvironment = { settingsViewModel.clearServerEnvironment() },
                onDismissServerEnvironmentMessage = { settingsViewModel.dismissServerEnvironmentMessage() },
                onLanguageChange = { code ->
                    settingsViewModel.setLanguage(code)
                    Strings.setLanguage(code)
                },
                onUseSystemThemeChange = { settingsViewModel.setUseSystemTheme(it) },
                onDarkModeChange = { settingsViewModel.setDarkMode(it) },
                onLoadSettings = { settingsViewModel.loadSettings() },
                onRequestDataExport = { settingsViewModel.requestDataExport() },
                onOpenUrl = { url -> com.tamixa.platform.openUrl(url) },
                onLogout = {
                    appMessageNotifier.clear()
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        val root =
                            navController.graph.startDestinationRoute ?: Screen.Splash.route
                        popUpTo(root) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    settingsScope.launch {
                        authViewModel.deleteAccount()
                            .onSuccess {
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    val root =
                                        navController.graph.startDestinationRoute
                                            ?: Screen.Splash.route
                                    popUpTo(root) { inclusive = true }
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
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        // ── Education routes ──────────────────────────────────────────────────
        composable(
            Screen.ReadingLevel.route,
            arguments = listOf(navArgument("childId") { type = NavType.LongType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getLong("childId") ?: 0L
            val readingLevel by educationViewModel.readingLevel.collectAsState()
            val readingLevelLoading by educationViewModel.readingLevelLoading.collectAsState()
            val readingLevelError by educationViewModel.readingLevelError.collectAsState()
            LaunchedEffect(childId) {
                if (childId > 0L) educationViewModel.loadReadingLevel(childId)
            }
            ReadingLevelScreen(
                readingLevel = readingLevel,
                loading = readingLevelLoading,
                error = readingLevelError,
                onRetry = {
                    if (childId > 0L) educationViewModel.loadReadingLevel(childId)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Streak.route,
            arguments = listOf(navArgument("childId") { type = NavType.LongType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getLong("childId") ?: 0L
            val streak by educationViewModel.streak.collectAsState()
            val loading by educationViewModel.streakLoading.collectAsState()
            val error by educationViewModel.streakError.collectAsState()
            LaunchedEffect(childId) { educationViewModel.loadStreak(childId) }
            StreakScreen(
                streak = streak,
                loading = loading,
                error = error,
                onRetry = { educationViewModel.loadStreak(childId) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Vocabulary.route,
            arguments = listOf(navArgument("childId") { type = NavType.LongType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getLong("childId") ?: 0L
            val progress by educationViewModel.vocabProgress.collectAsState()
            val learnedWords by educationViewModel.learnedWords.collectAsState()
            val suggestions by educationViewModel.suggestions.collectAsState()
            val loading by educationViewModel.vocabLoading.collectAsState()
            val error by educationViewModel.vocabError.collectAsState()
            LaunchedEffect(childId) { educationViewModel.loadVocabulary(childId) }
            VocabularyScreen(
                progress = progress,
                learnedWords = learnedWords,
                suggestions = suggestions,
                loading = loading,
                error = error,
                onRetry = { educationViewModel.loadVocabulary(childId) },
                onMarkLearned = { wordId -> educationViewModel.markWordLearned(childId, wordId) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Classroom.route,
            arguments = listOf(navArgument("childId") { type = NavType.LongType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getLong("childId") ?: 0L
            val classrooms by educationViewModel.classrooms.collectAsState()
            val loading by educationViewModel.classroomsLoading.collectAsState()
            val error by educationViewModel.classroomsError.collectAsState()
            val joinLoading by educationViewModel.joinLoading.collectAsState()
            val joinError by educationViewModel.joinError.collectAsState()
            val joinResult by educationViewModel.joinResult.collectAsState()
            LaunchedEffect(childId) { educationViewModel.loadClassrooms(childId) }
            ClassroomScreen(
                classrooms = classrooms,
                loading = loading,
                error = error,
                joinLoading = joinLoading,
                joinError = joinError,
                joinSuccess = joinResult,
                onRetry = { educationViewModel.loadClassrooms(childId) },
                onJoin = { code -> educationViewModel.joinClassroom(code, childId) },
                onClearJoin = { educationViewModel.clearJoinResult() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Quiz.route,
            arguments = listOf(
                navArgument("storyId") { type = NavType.LongType },
                navArgument("childId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getLong("storyId") ?: 0L
            val childId = backStackEntry.arguments?.getLong("childId") ?: 0L
            val quiz by educationViewModel.quiz.collectAsState()
            val result by educationViewModel.quizResult.collectAsState()
            val loading by educationViewModel.quizLoading.collectAsState()
            val error by educationViewModel.quizError.collectAsState()
            LaunchedEffect(storyId, childId) {
                educationViewModel.loadQuiz(storyId)
            }
            QuizScreen(
                quiz = quiz,
                result = result,
                loading = loading,
                error = error,
                quizChildId = childId,
                onSubmit = { answers ->
                    val q = quiz
                    if (q != null && childId != 0L) {
                        educationViewModel.submitQuiz(q.id, childId, answers)
                    }
                },
                onDone = {
                    educationViewModel.clearQuiz()
                    navController.popBackStack()
                },
                onBack = {
                    educationViewModel.clearQuiz()
                    navController.popBackStack()
                }
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
