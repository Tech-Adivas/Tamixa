package com.araro.di

import android.content.Context
import com.araro.application.port.PreferencesPort
import com.araro.platform.DataStorePreferences
import com.araro.repository.StoryCache
import com.araro.repository.StoryCacheDataStore
import com.araro.security.AndroidTokenStorage
import com.araro.security.TokenStorage
import com.araro.util.AraroConstants
import org.koin.dsl.module

/**
 * Android platform bindings: TokenStorage, PreferencesPort, StoryCache.
 */
fun androidPlatformModule(context: Context) = module {
    single<TokenStorage> { AndroidTokenStorage(context) }
    single<PreferencesPort> { DataStorePreferences(context) }
    single<StoryCache> { StoryCacheDataStore(context, maxStories = AraroConstants.CACHE_MAX_STORIES) }
}
