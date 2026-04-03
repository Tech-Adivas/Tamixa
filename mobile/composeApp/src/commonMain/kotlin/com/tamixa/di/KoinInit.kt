package com.tamixa.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Initializes Koin with shared and platform-specific modules.
 * Call from Application.onCreate() (Android) or iOS AppDelegate/main.
 *
 * @param baseUrl API base URL (e.g. from BuildConfig or Info.plist)
 * @param platformModule Platform-specific bindings: TokenStorage, PreferencesPort, StoryCache
 * @param buildEnvironment dev | qa | prod — must match the backend you are calling (OTP is stored per environment).
 */
fun initKoin(baseUrl: String, platformModule: Module, buildEnvironment: String = "unknown") {
    startKoin {
        // Platform module first: TokenStorage, PreferencesPort, StoryCache (required by sharedModule)
        modules(
            platformModule,
            sharedModule(baseUrl = baseUrl, buildEnvironment = buildEnvironment),
            viewModelModule()
        )
    }
}
