package com.tamixa.di

import com.tamixa.application.port.OnboardingReminderPort
import com.tamixa.application.port.PreferencesPort
import com.tamixa.platform.NsUserDefaultsPreferences
import com.tamixa.platform.OnboardingReminderAdapterIos
import com.tamixa.repository.StoryCache
import com.tamixa.repository.StoryCacheNsUserDefaults
import com.tamixa.security.IosTokenStorage
import com.tamixa.security.TokenStorage
import com.tamixa.util.TamixaConstants
import org.koin.dsl.module

/**
 * iOS platform bindings: TokenStorage, PreferencesPort, StoryCache.
 */
fun iosPlatformModule() = module {
    single<TokenStorage> { IosTokenStorage() }
    single<PreferencesPort> { NsUserDefaultsPreferences() }
    single<StoryCache> { StoryCacheNsUserDefaults(maxStories = TamixaConstants.CACHE_MAX_STORIES) }
    single<OnboardingReminderPort> { OnboardingReminderAdapterIos() }
}
