package com.tamixa.di

import android.content.Context
import com.tamixa.application.port.OnboardingReminderPort
import com.tamixa.application.port.PreferencesPort
import com.tamixa.platform.DataStorePreferences
import com.tamixa.platform.OnboardingReminderAdapter
import com.tamixa.repository.StoryCache
import com.tamixa.repository.StoryCacheDataStore
import com.tamixa.security.AndroidTokenStorage
import com.tamixa.security.TokenStorage
import com.tamixa.util.TamixaConstants
import org.koin.dsl.module

/**
 * Android platform bindings: TokenStorage, PreferencesPort, StoryCache.
 */
fun androidPlatformModule(context: Context) = module {
    single<TokenStorage> { AndroidTokenStorage(context) }
    single<PreferencesPort> { DataStorePreferences(context) }
    single<StoryCache> { StoryCacheDataStore(context, maxStories = TamixaConstants.CACHE_MAX_STORIES) }
    single<OnboardingReminderPort> { OnboardingReminderAdapter(context) }
}
