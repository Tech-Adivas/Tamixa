package com.araro.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.araro.application.port.PreferencesPort
import com.araro.util.AraroConstants
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "araro_preferences")

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
        return context.dataStore.data.first()[PREFERRED_VOICE_PROFILE_KEY] ?: AraroConstants.VOICE_PROFILE_DEFAULT
    }

    override suspend fun setPreferredVoiceProfile(profile: String) {
        context.dataStore.edit { prefs ->
            prefs[PREFERRED_VOICE_PROFILE_KEY] = profile
        }
    }

    companion object {
        private val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LANGUAGE_CODE_KEY = stringPreferencesKey("language_code")
        private val HAS_COMPLETED_LANGUAGE_SELECTION_KEY = booleanPreferencesKey("has_completed_language_selection")
        private val PREFERRED_VOICE_PROFILE_KEY = stringPreferencesKey("preferred_voice_profile")
    }
}
