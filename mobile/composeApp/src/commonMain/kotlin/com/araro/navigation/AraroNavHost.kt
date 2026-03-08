package com.araro.navigation

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
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.theme.AraroDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import com.araro.player.StoryPlaybackController
import com.araro.platform.rememberAvatarVideoController
import com.araro.platform.rememberLocalFileController
import com.araro.platform.rememberStreamingController
import com.araro.platform.rememberAudioPickerLauncher
import com.araro.platform.rememberImagePickerLauncher
import com.araro.platform.synthesizeStoryToFile
import com.araro.network.ApiConfig
import com.araro.network.StoryApi
import com.araro.ui.data.SampleData
import com.araro.ui.state.UiState
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.araro.ui.navigation.Screen
import com.araro.util.AraroConstants
import com.araro.ui.screen.*
import com.araro.ui.strings.Strings
import com.araro.analytics.AppAnalytics
import com.araro.ui.viewmodel.AuthViewModel
import com.araro.ui.viewmodel.ChildViewModel
import com.araro.ui.viewmodel.SettingsViewModel
import com.araro.ui.viewmodel.StoryViewModel
import com.araro.ui.viewmodel.SubscriptionViewModel
import com.araro.ui.viewmodel.AvatarViewModel
import com.araro.ui.viewmodel.VoiceViewModel
import com.araro.ui.AppMessageNotifier
import com.araro.ui.theme.AraroColors
import androidx.compose.ui.Alignment

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

    val effectiveLanguage = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE }
    LaunchedEffect(effectiveLanguage) {
        Strings.setLanguage(effectiveLanguage)
    }

    // First-time users: show language selection; returning users go straight to dashboard (only when settings loaded)
    val postLoginDestination = when {
        !settingsState.settingsLoaded -> null
        !settingsState.hasCompletedLanguageSelection -> Screen.LanguageSelection.route
        else -> Screen.Dashboard.route
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val appMessageNotifier: AppMessageNotifier = koinInject()
    val appMessage by appMessageNotifier.message.collectAsState()
    LaunchedEffect(appMessage) {
        appMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.text)
            appMessageNotifier.clear()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                otpDevCode = otpDevCode,
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
            val curated by storyViewModel.curatedStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val recentPlaybackWithStories by storyViewModel.recentPlaybackWithStories.collectAsState()
            val recommendedWithStories by storyViewModel.recommendedWithStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE }
            val firstChildId = (childrenState as? UiState.Success)?.data?.firstOrNull()?.id
            LaunchedEffect(prefLang) {
                storyViewModel.loadCuratedStories(prefLang)
                storyViewModel.loadRecentPlayback(AraroConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                storyViewModel.loadFavorites(prefLang)
                storyViewModel.loadRecommended(firstChildId, prefLang, AraroConstants.RECOMMENDED_LIMIT)
            }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            val favorites by storyViewModel.favorites.collectAsState()
            val favoriteIds = favorites.map { it.id }.toSet()
            val vmStories = storyViewModel.allStories()
            val stories = if (vmStories.isEmpty()) SampleData.sampleStories() else vmStories
            val firstChildName = (childrenState as? UiState.Success)?.data?.firstOrNull()?.name
            val recentPlayback = recentPlaybackWithStories.map { (dto, story) ->
                com.araro.ui.screen.RecentPlaybackItem(
                    story = story,
                    positionSeconds = dto.positionSeconds,
                    storySource = dto.storySource
                )
            }
            val recommended = recommendedWithStories.map { (dto, story) ->
                com.araro.ui.screen.RecommendedItem(
                    story = story,
                    title = dto.title,
                    theme = dto.theme,
                    reason = dto.reason,
                    storyId = dto.storyId,
                    storySource = dto.storySource
                )
            }
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
                    storyViewModel.loadRecentPlayback(AraroConstants.RECENT_PLAYBACK_LIMIT, prefLang)
                    storyViewModel.loadFavorites(prefLang)
                    storyViewModel.loadRecommended(firstChildId, prefLang, AraroConstants.RECOMMENDED_LIMIT)
                },
                isRefreshing = childrenState is UiState.Loading,
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                recentPlayback = recentPlayback,
                recommended = recommended,
                apiBaseUrl = apiBaseUrl,
                languageCode = prefLang,
                favoriteStoryIds = favoriteIds,
                onRecommendedStoryClick = { id, source -> navController.navigate(Screen.AudioPlayer.withId(id, source)) },
                onFavoriteToggle = { story, add ->
                    if (add) {
                        storyViewModel.addFavorite(story.id, if (story.parentId == 0L) AraroConstants.STORY_SOURCE_CURATED else AraroConstants.STORY_SOURCE_GENERATED)
                        appAnalytics.trackFavoriteAdd(story.id, if (story.parentId == 0L) AraroConstants.STORY_SOURCE_CURATED else AraroConstants.STORY_SOURCE_GENERATED)
                    } else {
                        storyViewModel.removeFavorite(story.id)
                    }
                }
            )
        }
        composable(Screen.ChildList.route) {
            LaunchedEffect(Unit) { childViewModel.loadChildren() }
            ChildListScreen(
                childrenState = childrenState,
                onAddChild = { navController.navigate(Screen.ChildCreate.route) },
                onRetry = { childViewModel.loadChildren() },
                onChildClick = { navController.navigate(Screen.Dashboard.route) },
                onBack = { navController.popBackStack() }
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
        composable(Screen.Categories.route) {
            val prefLangCat = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(prefLangCat) { storyViewModel.loadCuratedStories(prefLangCat) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            StorySelectionScreen(
                cachedStories = storyViewModel.allStories(),
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val appAnalytics: AppAnalytics = koinInject()
            val prefLang = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(prefLang) { storyViewModel.loadFavorites(prefLang) }
            val searchResults by storyViewModel.searchResults.collectAsState()
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
                }
            )
        }
        composable(Screen.Favorites.route) {
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val appAnalytics: AppAnalytics = koinInject()
            val prefLang = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(Unit) { appAnalytics.trackScreenView("favorites") }
            FavoritesScreen(
                storyViewModel = storyViewModel,
                onBack = { navController.popBackStack() },
                onStoryClick = { story ->
                    val source = if (story.parentId == 0L) AraroConstants.STORY_SOURCE_CURATED else AraroConstants.STORY_SOURCE_GENERATED
                    navController.navigate(Screen.AudioPlayer.withId(story.id, source))
                },
                languageCode = prefLang,
                apiBaseUrl = apiBaseUrl,
                onFavoriteRemoved = { id, source -> appAnalytics.trackFavoriteRemove(id, source) }
            )
        }
        composable(Screen.StorySelection.route) {
            val curated by storyViewModel.curatedStories.collectAsState()
            val myStories by storyViewModel.myStories.collectAsState()
            val prefLang = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE }
            LaunchedEffect(prefLang) { storyViewModel.loadCuratedStories(prefLang) }
            LaunchedEffect(Unit) { storyViewModel.loadMyStories() }
            StorySelectionScreen(
                cachedStories = storyViewModel.allStories(),
                onGenerateStory = { navController.navigate(Screen.StoryGeneration.route) },
                onStoryClick = { story -> navController.navigate(Screen.AudioPlayer.withId(story.id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.StoryGeneration.route) {
            val children by childViewModel.children.collectAsState()
            val generateState by storyViewModel.generateState.collectAsState()
            var showSuccessModal by remember { mutableStateOf(false) }
            LaunchedEffect(generateState) {
                when (generateState) {
                    is UiState.Success<*> -> {
                        storyViewModel.loadMyStories()
                        showSuccessModal = true
                    }
                    else -> {}
                }
            }
            val list = (children as? UiState.Success)?.data ?: emptyList()
            StoryGenerationScreen(
                generateState = generateState,
                children = list,
                onGenerate = { storyViewModel.generateStory(it) },
                onBack = { navController.popBackStack() },
                onStoryGenerated = { },
                showSuccessModal = showSuccessModal,
                onDismissSuccess = {
                    showSuccessModal = false
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.StoryGeneration.route) { inclusive = true } }
                }
            )
        }
        composable(
            "audio/{storyId}?storySource={storySource}",
            arguments = listOf(
                navArgument("storyId") { type = NavType.LongType },
                navArgument("storySource") { defaultValue = AraroConstants.STORY_SOURCE_GENERATED }
            )
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getLong("storyId") ?: 0L
            val storySource = backStackEntry.arguments?.getString("storySource") ?: AraroConstants.STORY_SOURCE_GENERATED
            val scope = rememberCoroutineScope()
            val storyApi: StoryApi = koinInject()
            val apiBaseUrl: String = koinInject(named("apiBaseUrl"))
            val prefLang = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE }
            val curated by storyViewModel.curatedStories.collectAsState()
            val allStories = storyViewModel.allStories()
            var story by remember(storyId) { mutableStateOf<com.araro.domain.Story?>(null) }
            var streamUrl by remember(storyId) { mutableStateOf<String?>(null) }
            var streamAvatarUrl by remember(storyId) { mutableStateOf<String?>(null) }
            var streamAvatarVideoUrl by remember(storyId) { mutableStateOf<String?>(null) }
            var streamWordTimings by remember(storyId) { mutableStateOf<List<com.araro.network.WordTiming>?>(null) }
            var streamUrlFailed by remember(storyId) { mutableStateOf(false) }
            var avatarVideoSurfaceFailed by remember(storyId) { mutableStateOf(false) }
            var synthesizedUri by remember(storyId) { mutableStateOf<String?>(null) }
            var conversationalTtsUri by remember(storyId) { mutableStateOf<String?>(null) }
            val preferredVoice = settingsState.preferredVoiceProfile.ifEmpty { AraroConstants.VOICE_PROFILE_DEFAULT }
            var selectedVoice by remember(storyId) { mutableStateOf(preferredVoice) }
            var availableVoices by remember(storyId) { mutableStateOf<List<com.araro.network.VoiceOptionDto>>(emptyList()) }
            var showUpgradeDialog by remember { mutableStateOf(false) }
            var showSleepTimerDialog by remember { mutableStateOf(false) }
            var showMoralDialog by remember { mutableStateOf(false) }
            var showFamilyVoiceRecordDialog by remember { mutableStateOf(false) }
            LaunchedEffect(storyId, allStories, curated, prefLang, storySource) {
                story = if (storyId > 0) {
                    storyViewModel.fetchStoryById(storyId, prefLang, storySource, forceRefresh = true)
                } else {
                    null
                } ?: allStories.find { it.id == storyId }
                    ?: SampleData.sampleStories().find { it.id == storyId }
            }
            var familyVoiceRefreshTrigger by remember(storyId) { mutableStateOf(0) }
            LaunchedEffect(story?.id, familyVoiceRefreshTrigger, preferredVoice) {
                story?.id?.let { id -> availableVoices = storyApi.getAvailableVoices(id, prefLang) }
                if (availableVoices.isEmpty()) availableVoices = listOf(
                    com.araro.network.VoiceOptionDto(AraroConstants.VOICE_PROFILE_DEFAULT, false),
                    com.araro.network.VoiceOptionDto("calm", true)
                )
                val voices = availableVoices
                val hasPreferred = voices.any { it.voiceProfile.equals(preferredVoice, ignoreCase = true) }
                if (hasPreferred) selectedVoice = preferredVoice
                else selectedVoice = voices.firstOrNull()?.voiceProfile ?: AraroConstants.VOICE_PROFILE_DEFAULT
            }
            LaunchedEffect(story?.id, story?.audioFileUrl, story?.content, apiBaseUrl, prefLang, selectedVoice) {
                val s = story ?: return@LaunchedEffect
                streamUrlFailed = false  // Reset so we retry stream when URL changes (e.g. after regeneration)
                val isPlaceholder = s.audioFileUrl.isNullOrBlank() || s.audioFileUrl.startsWith(AraroConstants.PLACEHOLDER_AUDIO_PREFIX)
                val voiceParam = if (selectedVoice == AraroConstants.VOICE_PROFILE_DEFAULT) null else selectedVoice
                if (isPlaceholder && !s.content.isNullOrBlank()) {
                    streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, s.audioFileUrl)
                    streamAvatarUrl = null
                    streamWordTimings = null
                } else {
                    when (val result = storyApi.getStreamUrl(s.id, prefLang, voiceParam, storySource)) {
                        is StoryApi.StreamUrlResult.Url -> {
                            streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, result.url) ?: result.url
                            streamAvatarUrl = result.avatarUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                            streamAvatarVideoUrl = result.avatarVideoUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                            streamWordTimings = result.wordTimings
                        }
                        is StoryApi.StreamUrlResult.UpgradeRequired -> {
                            showUpgradeDialog = true
                            streamUrl = null
                            streamAvatarUrl = null
                            streamAvatarVideoUrl = null
                            streamWordTimings = null
                        }
                        is StoryApi.StreamUrlResult.NotFound -> {
                            if (voiceParam != null) {
                                val fallback = storyApi.getStreamUrl(s.id, prefLang, null, storySource)
                                if (fallback is StoryApi.StreamUrlResult.Url) {
                                    streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, fallback.url) ?: fallback.url
                                    streamAvatarUrl = fallback.avatarUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                                    streamAvatarVideoUrl = fallback.avatarVideoUrl?.let { ApiConfig.resolveCoverUrl(apiBaseUrl, it) ?: it }
                                    streamWordTimings = fallback.wordTimings
                                } else {
                                    streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, s.audioFileUrl)
                                    streamAvatarUrl = null
                                    streamAvatarVideoUrl = null
                                }
                            } else {
                                streamUrl = ApiConfig.resolveAudioUrl(apiBaseUrl, s.audioFileUrl)
                                streamAvatarUrl = null
                                streamAvatarVideoUrl = null
                                streamWordTimings = null
                            }
                        }
                    }
                }
            }
            val useTts = !story?.content.isNullOrBlank()
            LaunchedEffect(story?.id, story?.content, prefLang, useTts) {
                if (useTts) {
                    val s = story ?: return@LaunchedEffect
                    val content = s.content?.trim() ?: return@LaunchedEffect
                    synthesizedUri = withContext(Dispatchers.Default) {
                        com.araro.platform.synthesizeStoryToFile(content, prefLang)
                    }
                    val script = storyApi.getNarrationScript(s.id, prefLang)
                    if (!script.isNullOrBlank()) {
                        conversationalTtsUri = withContext(Dispatchers.Default) {
                            com.araro.platform.synthesizeStoryToFile(script, prefLang)
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
            val hasRealAudio = !currentStreamUrl.isNullOrBlank() && !currentStreamUrl.startsWith(AraroConstants.PLACEHOLDER_AUDIO_PREFIX)
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
                    onPlaybackError = { avatarVideoSurfaceFailed = true }
                )
            } else null
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
            LaunchedEffect(playbackUrl) {
                if (playbackUrl != null && !controller.isPlaying) {
                    controller.playPause()
                }
            }
            val analytics = remember(tracker) {
                com.araro.ui.screen.StoryAnalyticsCallbacks(
                    onStoryStarted = { s, pos -> tracker.onStoryStarted(s, pos) },
                    onProgress = { s, p, pos -> tracker.onProgress(s, p, pos) },
                    onCompleted = { s, pos -> tracker.onCompleted(s, pos) },
                    onStoppedEarly = { s, pos -> tracker.onStoppedEarly(s, pos) }
                )
            }
            if (showFamilyVoiceRecordDialog) {
                com.araro.platform.FamilyVoiceRecordDialog(
                    onDismiss = { showFamilyVoiceRecordDialog = false },
                    onRecordingComplete = { bytes ->
                        scope.launch {
                            story?.id?.let { id ->
                                if (storyApi.uploadFamilyVoice(id, prefLang, bytes, isAac = true)) {
                                    familyVoiceRefreshTrigger++
                                    selectedVoice = "family"
                                    appMessageNotifier.show(Strings.familyVoiceUploaded(), "family-voice")
                                }
                            }
                        }
                    }
                )
            }
            if (showUpgradeDialog) {
                AlertDialog(
                    onDismissRequest = { showUpgradeDialog = false },
                    shape = AraroDialogDefaults.shape,
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
                            shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)
                        ) { Text(Strings.upgrade()) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showUpgradeDialog = false
                            selectedVoice = AraroConstants.VOICE_PROFILE_DEFAULT
                        }) { Text(Strings.cancel()) }
                    }
                )
            }
            story?.let { currentStory ->
            if (showMoralDialog) {
                AlertDialog(
                    onDismissRequest = { showMoralDialog = false },
                    shape = AraroDialogDefaults.shape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            Strings.moralOfStory(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column {
                            Text(
                                Strings.moralDialogIntro(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                currentStory.moral ?: Strings.noMoralAvailable(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showMoralDialog = false },
                            shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)
                        ) { Text(Strings.continueWith()) }
                    }
                )
            }
            }
            com.araro.platform.PlatformBackHandler {
                story?.let { s ->
                    val prog = controller.progress
                    if (prog < 0.99f) {
                        val pos = (prog * s.readingTimeMinutes * 60).toInt()
                        tracker.onStoppedEarly(s, pos)
                    }
                }
                navController.popBackStack()
            }
            if (showSleepTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    shape = AraroDialogDefaults.shape,
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
                            AraroConstants.SLEEP_TIMER_PRESETS.forEach { min ->
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
            AudioPlayerScreen(
                story = story,
                wordTimings = streamWordTimings,
                isPlaying = controller.isPlaying,
                onPlayPause = controller::playPause,
                onRewind = controller::rewind,
                onFastForward = controller::fastForward,
                apiBaseUrl = apiBaseUrl,
                storytellingAvatarUrl = if (avatarVideoResult == null || avatarVideoSurfaceFailed) streamAvatarUrl else null,
                storytellingAvatarVideoContent = if (!avatarVideoSurfaceFailed && avatarVideoResult != null) {
                    avatarVideoResult.let { r ->
                        { com.araro.platform.AvatarVideoSurface(player = r.player, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) }
                    }
                } else null,
                onRecordFamilyVoice = { showFamilyVoiceRecordDialog = true },
                onBack = {
                    story?.let { s ->
                        val prog = controller.progress
                        if (prog < 0.99f) {
                            val pos = (prog * s.readingTimeMinutes * 60).toInt()
                            tracker.onStoppedEarly(s, pos)
                        }
                    }
                    navController.popBackStack()
                },
                progress = controller.progress,
                onSleepTimer = { showSleepTimerDialog = true },
                onDownload = if (hasAvatarVideo) {
                    { controller.download(streamAvatarVideoUrl!!, "story_${story?.id ?: 0}_avatar", "video/mp4") }
                } else if (hasRealAudio) {
                    { controller.download(currentStreamUrl!!, "story_${story?.id ?: 0}", "audio/mpeg") }
                } else null,
                onShare = { controller.share() },
                onMoralClick = { showMoralDialog = true },
                onRemix = { id, instruction ->
                    storyViewModel.remixStory(id, instruction) { newStory ->
                        navController.popBackStack()
                        navController.navigate(Screen.AudioPlayer.withId(newStory.id, AraroConstants.STORY_SOURCE_GENERATED))
                    }
                },
                analytics = analytics,
                isLoading = (story == null && storyId > 0) || (useTts && synthesizedUri == null && story != null),
                selectedVoice = selectedVoice,
                onVoiceChange = { selectedVoice = it },
                availableVoices = availableVoices
            )
        }
        composable(Screen.VoiceUpload.route) {
            val launchAudioPicker = rememberAudioPickerLauncher { bytes, name ->
                bytes?.let { voiceViewModel.uploadVoice(it, name ?: "voice_audio") }
            }
            VoiceUploadScreen(
                uploadState = uploadState,
                profilesState = profilesState,
                onUpload = { bytes, name -> voiceViewModel.uploadVoice(bytes, name) },
                onPickAudio = launchAudioPicker,
                onLoadProfiles = { voiceViewModel.loadProfiles() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AvatarUpload.route) {
            val launchImagePicker = rememberImagePickerLauncher { bytes, contentType ->
                bytes?.let { avatarViewModel.uploadAvatar(it, contentType ?: "image/jpeg") }
            }
            AvatarUploadScreen(
                uploadState = avatarUploadState,
                avatarUrl = avatarUrl,
                onUpload = { bytes, contentType -> avatarViewModel.uploadAvatar(bytes, contentType) },
                onPickImage = launchImagePicker,
                onDelete = { avatarViewModel.deleteAvatar() },
                onLoadAvatar = { avatarViewModel.loadAvatar() },
                onBack = { navController.popBackStack() },
                apiBaseUrl = koinInject(named("apiBaseUrl"))
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
                    com.araro.platform.openUrl(com.araro.platform.getSubscriptionWebUrl())
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            LaunchedEffect(Unit) { settingsViewModel.loadSettings() }
            SettingsScreen(
                languageCode = settingsState.languageCode.ifEmpty { AraroConstants.DEFAULT_LANGUAGE },
                useSystemTheme = settingsState.useSystemTheme,
                darkMode = settingsState.darkMode,
                preferredVoiceProfile = settingsState.preferredVoiceProfile.ifEmpty { AraroConstants.VOICE_PROFILE_DEFAULT },
                consentRecords = settingsState.consentRecords,
                exportJobs = settingsState.exportJobs,
                listeningProgress = settingsState.listeningProgress,
                settingsLoading = settingsState.settingsLoading,
                exporting = settingsState.exporting,
                onPreferredVoiceChange = { settingsViewModel.setPreferredVoiceProfile(it) },
                onLanguageChange = { code ->
                    settingsViewModel.setLanguage(code)
                    Strings.setLanguage(code)
                },
                onUseSystemThemeChange = { settingsViewModel.setUseSystemTheme(it) },
                onDarkModeChange = { settingsViewModel.setDarkMode(it) },
                onLoadSettings = { settingsViewModel.loadSettings() },
                onRequestDataExport = { settingsViewModel.requestDataExport() },
                onOpenUrl = { url -> com.araro.platform.openUrl(url) },
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
