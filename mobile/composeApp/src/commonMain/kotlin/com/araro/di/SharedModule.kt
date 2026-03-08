package com.araro.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.araro.analytics.AppAnalytics
import com.araro.network.AchievementApi
import com.araro.network.AnalyticsApi
import com.araro.network.ApiConfig
import com.araro.network.AvatarApi
import com.araro.network.AuthApi
import com.araro.network.ChildApi
import com.araro.network.createKtorClient
import com.araro.network.SettingsApi
import com.araro.network.StoryApi
import com.araro.network.SubscriptionApi
import com.araro.network.VoiceApi
import com.araro.repository.AvatarRepository
import com.araro.repository.AuthRepository
import com.araro.repository.ChildRepository
import com.araro.repository.StoryRepository
import com.araro.repository.SubscriptionRepository
import com.araro.repository.VoiceRepository
import com.araro.ui.AppMessageNotifier
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun sharedModule(baseUrl: String = ApiConfig.DEFAULT_BASE_URL) = module {
    single(named("apiBaseUrl")) { baseUrl }
    // Default dispatcher: avoid Dispatchers.Main at Koin init (on iOS Main may not be set yet).
    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { createKtorClient(baseUrl, get(), enableLogging = true) }
    single { AuthApi(get()) }
    single { ChildApi(get()) }
    single { StoryApi(get()) }
    single { AnalyticsApi(get()) }
    single {
        AppAnalytics(
            get(),
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
        )
    }
    single { AchievementApi(get()) }
    single { VoiceApi(get()) }
    single { AvatarApi(get()) }
    single { SubscriptionApi(get()) }
    single { SettingsApi(get()) }
    single { com.araro.repository.SettingsRepository(get()) }
    // StoryCache provided by platform module (DataStore/NSUserDefaults)
    single { AuthRepository(get(), get()) }
    single { ChildRepository(get()) }
    single { StoryRepository(get(), get()) }
    single { VoiceRepository(get()) }
    single { AvatarRepository(get()) }
    single { SubscriptionRepository(get()) }
    single { AppMessageNotifier() }
}
