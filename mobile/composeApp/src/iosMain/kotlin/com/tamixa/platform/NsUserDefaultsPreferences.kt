package com.tamixa.platform

import com.tamixa.application.port.PreferencesPort
import com.tamixa.util.TamixaConstants
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PreferencesPort using NSUserDefaults.
 */
class NsUserDefaultsPreferences : PreferencesPort {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val prefix = "tamixa_pref_"

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
        defaults.stringForKey("${prefix}preferred_voice_profile") ?: TamixaConstants.VOICE_PROFILE_DEFAULT

    override suspend fun setPreferredVoiceProfile(profile: String) {
        defaults.setObject(profile, forKey = "${prefix}preferred_voice_profile")
        defaults.synchronize()
    }

    override suspend fun getHasCompletedOnboarding(): Boolean =
        defaults.boolForKey("${prefix}has_completed_onboarding")

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        defaults.setBool(completed, forKey = "${prefix}has_completed_onboarding")
        defaults.synchronize()
    }

    override suspend fun getPreferredThemes(): String =
        defaults.stringForKey("${prefix}preferred_themes") ?: ""

    override suspend fun setPreferredThemes(themes: String) {
        defaults.setObject(themes, forKey = "${prefix}preferred_themes")
        defaults.synchronize()
    }

    override suspend fun getBedtimeReminderEnabled(): Boolean =
        defaults.boolForKey("${prefix}bedtime_reminder_enabled")

    override suspend fun setBedtimeReminderEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = "${prefix}bedtime_reminder_enabled")
        defaults.synchronize()
    }

    override suspend fun getBedtimeReminderHour(): Int {
        val v = defaults.integerForKey("${prefix}bedtime_reminder_hour").toInt()
        return if (v != 0) v else 20
    }

    override suspend fun setBedtimeReminderHour(hour: Int) {
        defaults.setInteger(hour.coerceIn(0, 23).toLong(), forKey = "${prefix}bedtime_reminder_hour")
        defaults.synchronize()
    }

    override suspend fun getBedtimeReminderMinute(): Int =
        defaults.integerForKey("${prefix}bedtime_reminder_minute").toInt()

    override suspend fun setBedtimeReminderMinute(minute: Int) {
        defaults.setInteger(minute.coerceIn(0, 59).toLong(), forKey = "${prefix}bedtime_reminder_minute")
        defaults.synchronize()
    }
}
