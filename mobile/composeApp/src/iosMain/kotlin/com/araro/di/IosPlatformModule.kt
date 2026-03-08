package com.araro.di

import com.araro.application.port.PreferencesPort
import com.araro.platform.NsUserDefaultsPreferences
import com.araro.repository.StoryCache
import com.araro.repository.StoryCacheNsUserDefaults
import com.araro.security.IosTokenStorage
import com.araro.security.TokenStorage
import com.araro.util.AraroConstants
import org.koin.dsl.module

/**
 * iOS platform bindings: TokenStorage, PreferencesPort, StoryCache.
 */
fun iosPlatformModule() = module {
    single<TokenStorage> { IosTokenStorage() }
    single<PreferencesPort> { NsUserDefaultsPreferences() }
    single<StoryCache> { StoryCacheNsUserDefaults(maxStories = AraroConstants.CACHE_MAX_STORIES) }
}
