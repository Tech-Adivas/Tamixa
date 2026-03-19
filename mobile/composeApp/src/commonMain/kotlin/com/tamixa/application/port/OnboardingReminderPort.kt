package com.tamixa.application.port

/**
 * Platform-specific scheduling of bedtime reminder notifications.
 * Used during onboarding for "Remind me at bedtime" to improve Day-1 retention.
 */
interface OnboardingReminderPort {

    /**
     * Request notification permission (if required by platform).
     * Call before [scheduleBedtimeReminder].
     */
    suspend fun requestNotificationPermission(): Boolean

    /**
     * Schedule a daily bedtime reminder at the given hour:minute.
     * Overwrites any existing reminder.
     */
    suspend fun scheduleBedtimeReminder(hour: Int, minute: Int)

    /**
     * Cancel the bedtime reminder.
     */
    suspend fun cancelBedtimeReminder()
}
