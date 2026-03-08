package com.araro.di

import com.araro.analytics.StoryPlaybackTracker
import com.araro.ui.viewmodel.AuthViewModel
import com.araro.ui.viewmodel.ChildViewModel
import com.araro.ui.viewmodel.SettingsViewModel
import com.araro.ui.viewmodel.SubscriptionViewModel
import com.araro.ui.viewmodel.StoryViewModel
import com.araro.ui.viewmodel.AvatarViewModel
import com.araro.ui.viewmodel.VoiceViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun viewModelModule() = module {
    factory { AuthViewModel(get(), get(named("appScope"))) }
    factory { ChildViewModel(get(), get(named("appScope"))) }
    factory { StoryPlaybackTracker(get(), get(), get(named("appScope"))) }
    factory {
        StoryViewModel(get(), get(), get(named("appScope")), get())
    }
    factory { VoiceViewModel(get(), get(named("appScope"))) }
    factory { AvatarViewModel(get(), get(named("appScope"))) }
    factory { SettingsViewModel(get(), get(), get(named("appScope"))) }
    factory { SubscriptionViewModel(get(), get(named("appScope"))) }
}
