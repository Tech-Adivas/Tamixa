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

    override suspend fun getStoryArtPersonalizationOptIn(): Boolean =
        defaults.boolForKey("${prefix}story_art_personalization_opt_in")

    override suspend fun setStoryArtPersonalizationOptIn(optIn: Boolean) {
        defaults.setBool(optIn, forKey = "${prefix}story_art_personalization_opt_in")
        defaults.synchronize()
    }

    override suspend fun getApiBaseUrlOverride(): String =
        defaults.stringForKey("${prefix}api_base_url_override")?.trim().orEmpty()

    override suspend fun setApiBaseUrlOverride(value: String) {
        val t = value.trim()
        if (t.isEmpty()) {
            defaults.removeObjectForKey("${prefix}api_base_url_override")
        } else {
            defaults.setObject(t, forKey = "${prefix}api_base_url_override")
        }
        defaults.synchronize()
    }

    override suspend fun getSubscriptionWebUrlOverride(): String =
        defaults.stringForKey("${prefix}subscription_web_url_override")?.trim().orEmpty()

    override suspend fun setSubscriptionWebUrlOverride(value: String) {
        val t = value.trim()
        if (t.isEmpty()) {
            defaults.removeObjectForKey("${prefix}subscription_web_url_override")
        } else {
            defaults.setObject(t, forKey = "${prefix}subscription_web_url_override")
        }
        defaults.synchronize()
    }

    override suspend fun getLifeSkillPreferredChildId(): Long? {
        val s = defaults.stringForKey("${prefix}life_skill_preferred_child_id")?.trim().orEmpty()
        if (s.isEmpty()) return null
        return s.toLongOrNull()
    }

    override suspend fun setLifeSkillPreferredChildId(id: Long?) {
        val key = "${prefix}life_skill_preferred_child_id"
        if (id == null || id <= 0L) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(id.toString(), forKey = key)
        }
        defaults.synchronize()
    }

    override suspend fun getCrisisSafetyVaultText(): String =
        defaults.stringForKey("${prefix}crisis_safety_vault")?.trim().orEmpty()

    override suspend fun setCrisisSafetyVaultText(text: String) {
        val key = "${prefix}crisis_safety_vault"
        val t = text.trim()
        if (t.isEmpty()) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(t, forKey = key)
        }
        defaults.synchronize()
    }
}
