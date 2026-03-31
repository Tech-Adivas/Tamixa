package com.tamixa.di

import com.tamixa.analytics.StoryPlaybackTracker
import com.tamixa.ui.viewmodel.AuthViewModel
import com.tamixa.ui.viewmodel.SettingsViewModel
import com.tamixa.ui.viewmodel.SubscriptionViewModel
import com.tamixa.ui.viewmodel.StoryViewModel
import com.tamixa.ui.viewmodel.AvatarViewModel
import com.tamixa.ui.viewmodel.EducationViewModel
import com.tamixa.ui.viewmodel.ShortContentViewModel
import com.tamixa.ui.viewmodel.VoiceViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun viewModelModule() = module {
    factory { AuthViewModel(get(), get(), get(), get(named("appScope"))) }
    factory { StoryPlaybackTracker(get(), get(), get(named("appScope"))) }
    factory {
        StoryViewModel(get(), get(), get(named("appScope")), get())
    }
    factory { VoiceViewModel(get(), get(), get(named("appScope"))) }
    factory { AvatarViewModel(get(), get(), get(named("appScope"))) }
    factory { SettingsViewModel(get(), get(), get(), get(named("appScope"))) }
    factory { SubscriptionViewModel(get(), get(named("appScope"))) }
    factory { ShortContentViewModel(get(), get(named("appScope"))) }
    factory { EducationViewModel(get(), get(named("appScope")), get()) }
}
