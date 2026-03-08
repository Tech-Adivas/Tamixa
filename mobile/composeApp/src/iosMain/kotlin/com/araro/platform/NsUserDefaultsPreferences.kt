package com.araro.platform

import com.araro.application.port.PreferencesPort
import com.araro.util.AraroConstants
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PreferencesPort using NSUserDefaults.
 */
class NsUserDefaultsPreferences : PreferencesPort {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val prefix = "araro_pref_"

    override suspend fun getUseSystemTheme(): Boolean =
        defaults.stringForKey("${prefix}use_system_theme") != "false"

    override suspend fun setUseSystemTheme(use: Boolean) {
        defaults.setObject(if (use) "true" else "false", forKey = "${prefix}use_system_theme")
        defaults.synchronize()
    }

    override suspend fun getDarkMode(): Boolean =
        defaults.boolForKey("${prefix}dark_mode")

    override suspend fun setDarkMode(enabled: Boolean) {
        defaults.setBool(enabled, forKey = "${prefix}dark_mode")
        defaults.synchronize()
    }

    override suspend fun getLanguageCode(): String =
        defaults.stringForKey("${prefix}language_code") ?: ""

    override suspend fun setLanguageCode(code: String) {
        defaults.setObject(code, forKey = "${prefix}language_code")
        defaults.synchronize()
    }

    override suspend fun getHasCompletedLanguageSelection(): Boolean =
        defaults.boolForKey("${prefix}has_completed_language_selection")

    override suspend fun setHasCompletedLanguageSelection(completed: Boolean) {
        defaults.setBool(completed, forKey = "${prefix}has_completed_language_selection")
        defaults.synchronize()
    }

    override suspend fun getPreferredVoiceProfile(): String =
        defaults.stringForKey("${prefix}preferred_voice_profile") ?: AraroConstants.VOICE_PROFILE_DEFAULT

    override suspend fun setPreferredVoiceProfile(profile: String) {
        defaults.setObject(profile, forKey = "${prefix}preferred_voice_profile")
        defaults.synchronize()
    }
}
