package com.tamixa.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tamixa.application.port.PreferencesPort
import com.tamixa.util.TamixaConstants
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tamixa_preferences")

class DataStorePreferences(private val context: Context) : PreferencesPort {

    override suspend fun getUseSystemTheme(): Boolean {
        return context.dataStore.data.first()[USE_SYSTEM_THEME_KEY] ?: true
    }

    override suspend fun setUseSystemTheme(use: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_SYSTEM_THEME_KEY] = use
        }
    }

    override suspend fun getDarkMode(): Boolean {
        return context.dataStore.data.first()[DARK_MODE_KEY] ?: false
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }

    override suspend fun getLanguageCode(): String {
        return context.dataStore.data.first()[LANGUAGE_CODE_KEY] ?: ""
    }

    override suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_CODE_KEY] = code
        }
    }

    override suspend fun getHasCompletedLanguageSelection(): Boolean {
        return context.dataStore.data.first()[HAS_COMPLETED_LANGUAGE_SELECTION_KEY] ?: false
    }

    override suspend fun setHasCompletedLanguageSelection(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_COMPLETED_LANGUAGE_SELECTION_KEY] = completed
        }
    }

    override suspend fun getPreferredVoiceProfile(): String {
        return context.dataStore.data.first()[PREFERRED_VOICE_PROFILE_KEY] ?: TamixaConstants.VOICE_PROFILE_DEFAULT
    }

    override suspend fun setPreferredVoiceProfile(profile: String) {
        context.dataStore.edit { prefs ->
            prefs[PREFERRED_VOICE_PROFILE_KEY] = profile
        }
    }

    override suspend fun getHasCompletedOnboarding(): Boolean {
        return context.dataStore.data.first()[HAS_COMPLETED_ONBOARDING_KEY] ?: false
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_COMPLETED_ONBOARDING_KEY] = completed
        }
    }

    override suspend fun getPreferredThemes(): String {
        return context.dataStore.data.first()[PREFERRED_THEMES_KEY] ?: ""
    }

    override suspend fun setPreferredThemes(themes: String) {
        context.dataStore.edit { prefs ->
            prefs[PREFERRED_THEMES_KEY] = themes
        }
    }

    override suspend fun getBedtimeReminderEnabled(): Boolean {
        return context.dataStore.data.first()[BEDTIME_REMINDER_ENABLED_KEY] ?: false
    }

    override suspend fun setBedtimeReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BEDTIME_REMINDER_ENABLED_KEY] = enabled
        }
    }

    override suspend fun getBedtimeReminderHour(): Int {
        return context.dataStore.data.first()[BEDTIME_REMINDER_HOUR_KEY] ?: 20
    }

    override suspend fun setBedtimeReminderHour(hour: Int) {
        context.dataStore.edit { prefs ->
            prefs[BEDTIME_REMINDER_HOUR_KEY] = hour.coerceIn(0, 23)
        }
    }

    override suspend fun getBedtimeReminderMinute(): Int {
        return context.dataStore.data.first()[BEDTIME_REMINDER_MINUTE_KEY] ?: 0
    }

    override suspend fun setBedtimeReminderMinute(minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[BEDTIME_REMINDER_MINUTE_KEY] = minute.coerceIn(0, 59)
        }
    }

    override suspend fun getStoryArtPersonalizationOptIn(): Boolean =
        context.dataStore.data.first()[STORY_ART_PERSONALIZATION_OPT_IN_KEY] ?: false

    override suspend fun setStoryArtPersonalizationOptIn(optIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[STORY_ART_PERSONALIZATION_OPT_IN_KEY] = optIn
        }
    }

    companion object {
        private val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LANGUAGE_CODE_KEY = stringPreferencesKey("language_code")
        private val HAS_COMPLETED_LANGUAGE_SELECTION_KEY = booleanPreferencesKey("has_completed_language_selection")
        private val PREFERRED_VOICE_PROFILE_KEY = stringPreferencesKey("preferred_voice_profile")
        private val HAS_COMPLETED_ONBOARDING_KEY = booleanPreferencesKey("has_completed_onboarding")
        private val PREFERRED_THEMES_KEY = stringPreferencesKey("preferred_themes")
        private val BEDTIME_REMINDER_ENABLED_KEY = booleanPreferencesKey("bedtime_reminder_enabled")
        private val BEDTIME_REMINDER_HOUR_KEY = intPreferencesKey("bedtime_reminder_hour")
        private val BEDTIME_REMINDER_MINUTE_KEY = intPreferencesKey("bedtime_reminder_minute")
        private val STORY_ART_PERSONALIZATION_OPT_IN_KEY = booleanPreferencesKey("story_art_personalization_opt_in")
    }
}
