package com.araro.ui.viewmodel

import com.araro.application.port.PreferencesPort
import com.araro.network.ConsentRecordDto
import com.araro.network.ExportJobDto
import com.araro.network.ListeningProgressDto
import com.araro.repository.SettingsRepository
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
    val preferredVoiceProfile: String = com.araro.util.AraroConstants.VOICE_PROFILE_DEFAULT,
    val settingsLoaded: Boolean = false,
    val consentRecords: List<ConsentRecordDto> = emptyList(),
    val exportJobs: List<ExportJobDto> = emptyList(),
    val listeningProgress: ListeningProgressDto? = null,
    val settingsLoading: Boolean = false,
    val exporting: Boolean = false
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val preferencesPort: PreferencesPort,
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
            val preferredVoiceProfile = preferencesPort.getPreferredVoiceProfile()
            _state.value = _state.value.copy(
                useSystemTheme = useSystemTheme,
                darkMode = darkMode,
                languageCode = languageCode,
                hasCompletedLanguageSelection = hasCompletedLanguageSelection,
                preferredVoiceProfile = preferredVoiceProfile.ifEmpty { com.araro.util.AraroConstants.VOICE_PROFILE_DEFAULT },
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

    fun loadSettings() {
        scope.launch {
            _state.value = _state.value.copy(settingsLoading = true)
            val consent = repository.getConsentRecords()
            val jobs = repository.getDataExportJobs()
            val progress = repository.getListeningProgress(30)
            _state.value = _state.value.copy(
                consentRecords = consent,
                exportJobs = jobs,
                listeningProgress = progress,
                settingsLoading = false
            )
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
}
