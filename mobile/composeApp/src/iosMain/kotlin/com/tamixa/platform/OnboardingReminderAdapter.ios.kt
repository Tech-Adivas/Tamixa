package com.tamixa.platform

import com.tamixa.application.port.OnboardingReminderPort

/**
 * iOS placeholder: bedtime reminders require UNUserNotificationCenter.
 * TODO: Implement when iOS app is built.
 */
class OnboardingReminderAdapterIos : OnboardingReminderPort {

    override suspend fun requestNotificationPermission(): Boolean = true

    override suspend fun scheduleBedtimeReminder(hour: Int, minute: Int) {
        // No-op on iOS until implemented
    }

    override suspend fun cancelBedtimeReminder() {
        // No-op on iOS until implemented
    }
}
