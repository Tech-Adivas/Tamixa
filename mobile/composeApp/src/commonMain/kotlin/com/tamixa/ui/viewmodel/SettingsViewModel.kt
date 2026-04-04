package com.tamixa.ui.viewmodel

import com.tamixa.application.port.PreferencesPort
import com.tamixa.runtime.ServerEnvironmentCache
import com.tamixa.network.ConsentRecordDto
import com.tamixa.repository.AuthRepository
import com.tamixa.network.ExportJobDto
import com.tamixa.network.ListeningProgressDto
import com.tamixa.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsState(
    val languageCode: String = "",
    val useSystemTheme: Boolean = true,
    val darkMode: Boolean = false,
    val hasCompletedLanguageSelection: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val preferredVoiceProfile: String = com.tamixa.util.TamixaConstants.VOICE_PROFILE_DEFAULT,
    val preferredThemes: String = "",
    val settingsLoaded: Boolean = false,
    val consentRecords: List<ConsentRecordDto> = emptyList(),
    val exportJobs: List<ExportJobDto> = emptyList(),
    val listeningProgress: ListeningProgressDto? = null,
    /** Consecutive days with at least one story play. For Dashboard. */
    val listeningStreakDays: Int? = null,
    val settingsLoading: Boolean = false,
    val settingsLoadError: String? = null,
    val exporting: Boolean = false,
    val storyArtPersonalizationOptIn: Boolean = false,
    /** Runtime API base override (staging / Railway). Empty = build default at next launch. */
    val apiBaseUrlOverride: String = "",
    val subscriptionWebUrlOverride: String = "",
    val serverEnvironmentMessage: String? = null,
    val serverEnvironmentError: String? = null
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val preferencesPort: PreferencesPort,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        scope.launch {
            val useSystemTheme = preferencesPort.getUseSystemTheme()
            val darkMode = preferencesPort.getDarkMode()
            val languageCode = preferencesPort.getLanguageCode()
            val hasCompletedLanguageSelection = preferencesPort.getHasCompletedLanguageSelection()
            val hasCompletedOnboarding = preferencesPort.getHasCompletedOnboarding()
            val preferredVoiceProfile = preferencesPort.getPreferredVoiceProfile()
            val preferredThemes = preferencesPort.getPreferredThemes()
            val storyArtOptIn = preferencesPort.getStoryArtPersonalizationOptIn()
            val apiOverride = preferencesPort.getApiBaseUrlOverride()
            val subOverride = preferencesPort.getSubscriptionWebUrlOverride()
            _state.value = _state.value.copy(
                useSystemTheme = useSystemTheme,
                darkMode = darkMode,
                languageCode = languageCode,
                hasCompletedLanguageSelection = hasCompletedLanguageSelection,
                hasCompletedOnboarding = hasCompletedOnboarding,
                preferredVoiceProfile = preferredVoiceProfile.ifEmpty { com.tamixa.util.TamixaConstants.VOICE_PROFILE_DEFAULT },
                preferredThemes = preferredThemes,
                storyArtPersonalizationOptIn = storyArtOptIn,
                apiBaseUrlOverride = apiOverride,
                subscriptionWebUrlOverride = subOverride,
                settingsLoaded = true
            )
        }
    }

    fun setUseSystemTheme(use: Boolean) {
        _state.value = _state.value.copy(useSystemTheme = use)
        scope.launch { preferencesPort.setUseSystemTheme(use) }
    }

    fun setLanguage(code: String) {
        _state.value = _state.value.copy(languageCode = code)
        scope.launch { preferencesPort.setLanguageCode(code) }
    }

    fun setDarkMode(enabled: Boolean) {
        _state.value = _state.value.copy(darkMode = enabled)
        scope.launch { preferencesPort.setDarkMode(enabled) }
    }

    /** Persists language selection. Call from main thread; do navigation after this returns (on main). */
    suspend fun persistLanguageSelection(code: String) {
        _state.value = _state.value.copy(
            languageCode = code,
            hasCompletedLanguageSelection = true
        )
        preferencesPort.setLanguageCode(code)
        preferencesPort.setHasCompletedLanguageSelection(true)
    }

    /** @deprecated Prefer calling [persistLanguageSelection] from UI and then navigating on main thread. */
    fun completeLanguageSelection(code: String, onDone: (() -> Unit)? = null) {
        _state.value = _state.value.copy(
            languageCode = code,
            hasCompletedLanguageSelection = true
        )
        scope.launch {
            preferencesPort.setLanguageCode(code)
            preferencesPort.setHasCompletedLanguageSelection(true)
            onDone?.let { callback ->
                withContext(Dispatchers.Main) { callback() }
            }
        }
    }

    fun setPreferredVoiceProfile(profile: String) {
        _state.value = _state.value.copy(preferredVoiceProfile = profile)
        scope.launch { preferencesPort.setPreferredVoiceProfile(profile) }
    }

    /** Mark onboarding complete and persist. Call before navigating to Login. */
    suspend fun completeOnboarding() {
        _state.value = _state.value.copy(hasCompletedOnboarding = true)
        preferencesPort.setHasCompletedOnboarding(true)
    }

    fun setPreferredThemes(themes: String) {
        _state.value = _state.value.copy(preferredThemes = themes)
        scope.launch { preferencesPort.setPreferredThemes(themes) }
    }

    fun setBedtimeReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        scope.launch {
            preferencesPort.setBedtimeReminderEnabled(enabled)
            preferencesPort.setBedtimeReminderHour(hour)
            preferencesPort.setBedtimeReminderMinute(minute)
        }
    }

    /** Apply value from GET /auth/me (server source of truth when logged in). */
    fun applyServerStoryArtOptIn(optInFromServer: Boolean) {
        if (_state.value.storyArtPersonalizationOptIn == optInFromServer) return
        _state.value = _state.value.copy(storyArtPersonalizationOptIn = optInFromServer)
        scope.launch { preferencesPort.setStoryArtPersonalizationOptIn(optInFromServer) }
    }

    fun setStoryArtPersonalizationOptIn(optIn: Boolean) {
        if (!authRepository.isLoggedIn()) {
            _state.value = _state.value.copy(storyArtPersonalizationOptIn = optIn)
            scope.launch { preferencesPort.setStoryArtPersonalizationOptIn(optIn) }
            return
        }
        val previous = _state.value.storyArtPersonalizationOptIn
        _state.value = _state.value.copy(storyArtPersonalizationOptIn = optIn)
        scope.launch {
            authRepository.updateStoryArtPersonalizationOptIn(optIn).fold(
                onSuccess = { preferencesPort.setStoryArtPersonalizationOptIn(optIn) },
                onFailure = {
                    _state.value = _state.value.copy(storyArtPersonalizationOptIn = previous)
                }
            )
        }
    }

    fun loadSettings() {
        scope.launch {
            _state.value = _state.value.copy(settingsLoading = true, settingsLoadError = null)
            try {
                val consent = repository.getConsentRecords()
                val jobs = repository.getDataExportJobs()
                val progress = repository.getListeningProgress(30)
                val streak = repository.getListeningStreak()
                _state.value = _state.value.copy(
                    consentRecords = consent,
                    exportJobs = jobs,
                    listeningProgress = progress,
                    listeningStreakDays = streak,
                    settingsLoading = false,
                    settingsLoadError = null
                )
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    settingsLoading = false,
                    settingsLoadError = e.message ?: "Failed to load settings"
                )
            }
        }
    }

    /** Fetches streak + 30-day listening stats for the home dashboard (no full settings payload). */
    fun loadListeningStreak() {
        scope.launch {
            val streak = repository.getListeningStreak()
            val progress = repository.getListeningProgress(30)
            _state.value = _state.value.copy(listeningStreakDays = streak, listeningProgress = progress)
        }
    }

    fun requestDataExport(onComplete: () -> Unit = {}) {
        scope.launch {
            _state.value = _state.value.copy(exporting = true)
            val job = repository.requestDataExport()
            if (job != null) {
                val jobs = repository.getDataExportJobs()
                _state.value = _state.value.copy(exportJobs = jobs)
            }
            _state.value = _state.value.copy(exporting = false)
            onComplete()
        }
    }

    fun setApiBaseUrlOverrideDraft(value: String) {
        _state.value = _state.value.copy(apiBaseUrlOverride = value, serverEnvironmentError = null)
    }

    fun setSubscriptionWebUrlOverrideDraft(value: String) {
        _state.value = _state.value.copy(subscriptionWebUrlOverride = value, serverEnvironmentError = null)
    }

    fun dismissServerEnvironmentMessage() {
        _state.value = _state.value.copy(serverEnvironmentMessage = null, serverEnvironmentError = null)
    }

    fun saveServerEnvironment() {
        scope.launch {
            val api = _state.value.apiBaseUrlOverride.trim()
            val sub = _state.value.subscriptionWebUrlOverride.trim()
            if (api.isNotEmpty() && !isPlausibleHttpUrl(api)) {
                _state.value = _state.value.copy(serverEnvironmentError = com.tamixa.ui.strings.Strings.serverEnvironmentInvalidUrl())
                return@launch
            }
            if (sub.isNotEmpty() && !isPlausibleHttpUrl(sub)) {
                _state.value = _state.value.copy(serverEnvironmentError = com.tamixa.ui.strings.Strings.serverEnvironmentInvalidUrl())
                return@launch
            }
            preferencesPort.setApiBaseUrlOverride(api)
            preferencesPort.setSubscriptionWebUrlOverride(sub)
            ServerEnvironmentCache.subscriptionWebUrlOverride = sub
            _state.value = _state.value.copy(
                serverEnvironmentError = null,
                serverEnvironmentMessage = com.tamixa.ui.strings.Strings.serverEnvironmentSavedHint()
            )
        }
    }

    fun clearServerEnvironment() {
        scope.launch {
            preferencesPort.setApiBaseUrlOverride("")
            preferencesPort.setSubscriptionWebUrlOverride("")
            ServerEnvironmentCache.subscriptionWebUrlOverride = ""
            _state.value = _state.value.copy(
                apiBaseUrlOverride = "",
                subscriptionWebUrlOverride = "",
                serverEnvironmentError = null,
                serverEnvironmentMessage = com.tamixa.ui.strings.Strings.serverEnvironmentClearedHint()
            )
        }
    }

    private fun isPlausibleHttpUrl(s: String): Boolean {
        val t = s.trim()
        return t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)
    }
}
