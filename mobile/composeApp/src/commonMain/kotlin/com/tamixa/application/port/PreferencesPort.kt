package com.tamixa.application.port

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

    // --- Onboarding ---

    suspend fun getHasCompletedOnboarding(): Boolean

    suspend fun setHasCompletedOnboarding(completed: Boolean)

    /** Comma-separated preferred story themes (e.g. "Animals,Adventure,Friendship"). */
    suspend fun getPreferredThemes(): String

    suspend fun setPreferredThemes(themes: String)

    suspend fun getBedtimeReminderEnabled(): Boolean

    suspend fun setBedtimeReminderEnabled(enabled: Boolean)

    /** Hour (0–23) for bedtime reminder. Default 20 (8 PM). */
    suspend fun getBedtimeReminderHour(): Int

    suspend fun setBedtimeReminderHour(hour: Int)

    /** Minute (0–59) for bedtime reminder. */
    suspend fun getBedtimeReminderMinute(): Int

    suspend fun setBedtimeReminderMinute(minute: Int)

    /** Parent opt-in for future cautious story-art personalization (Phase 3). Default false. */
    suspend fun getStoryArtPersonalizationOptIn(): Boolean

    suspend fun setStoryArtPersonalizationOptIn(optIn: Boolean)
}
