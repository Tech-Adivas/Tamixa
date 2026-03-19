package com.tamixa.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.tamixa.analytics.AppAnalytics
import com.tamixa.network.AchievementApi
import com.tamixa.network.AnalyticsApi
import com.tamixa.network.ApiConfig
import com.tamixa.network.AvatarApi
import com.tamixa.network.AuthApi
import com.tamixa.network.createKtorClient
import com.tamixa.network.SettingsApi
import com.tamixa.network.ShortContentApi
import com.tamixa.network.StoryApi
import com.tamixa.network.SubscriptionApi
import com.tamixa.network.VoiceApi
import com.tamixa.security.SessionExpiredNotifier
import com.tamixa.repository.AvatarRepository
import com.tamixa.repository.AuthRepository
import com.tamixa.repository.StoryRepository
import com.tamixa.repository.SubscriptionRepository
import com.tamixa.repository.VoiceRepository
import com.tamixa.ui.AppMessageNotifier
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun sharedModule(baseUrl: String = ApiConfig.DEFAULT_BASE_URL) = module {
    single(named("apiBaseUrl")) { baseUrl }
    // Default dispatcher: avoid Dispatchers.Main at Koin init (on iOS Main may not be set yet).
    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { SessionExpiredNotifier() }
    single { createKtorClient(baseUrl, get(), get<SessionExpiredNotifier>()::notifySessionExpired, enableLogging = true) }
    single { AuthApi(get()) }
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
    single { ShortContentApi(get()) }
    single { com.tamixa.repository.SettingsRepository(get()) }
    // StoryCache provided by platform module (DataStore/NSUserDefaults)
    single { AuthRepository(get(), get()) }
    single { StoryRepository(get(), get()) }
    single { VoiceRepository(get()) }
    single { AvatarRepository(get()) }
    single { SubscriptionRepository(get()) }
    single { AppMessageNotifier() }
}
