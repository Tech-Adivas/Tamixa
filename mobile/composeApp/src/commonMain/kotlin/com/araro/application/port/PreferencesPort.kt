package com.araro.application.port

/**
 * Platform-agnostic interface for persisting user preferences (e.g. dark mode, language).
 * Android: DataStore; iOS: UserDefaults/NSUserDefaults.
 */
interface PreferencesPort {

    /** When true, theme follows system day/night; when false, use [getDarkMode]. */
    suspend fun getUseSystemTheme(): Boolean

    suspend fun setUseSystemTheme(use: Boolean)

    suspend fun getDarkMode(): Boolean

    suspend fun setDarkMode(enabled: Boolean)

    suspend fun getLanguageCode(): String

    suspend fun setLanguageCode(code: String)

    suspend fun getHasCompletedLanguageSelection(): Boolean

    suspend fun setHasCompletedLanguageSelection(completed: Boolean)

    /** Preferred narration voice applied globally: "default", "calm", or "family". */
    suspend fun getPreferredVoiceProfile(): String

    suspend fun setPreferredVoiceProfile(profile: String)
}
