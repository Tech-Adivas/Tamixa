package com.tamixa.android

import android.app.Application
import com.tamixa.di.sharedModule
import com.tamixa.di.viewModelModule
import com.tamixa.security.AndroidTokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class TamixaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val baseUrl = BuildConfig.BASE_URL ?: "http://10.0.2.2:8080"
        startKoin {
            androidContext(this@TamixaApplication)
            modules(
                module {
                    single<com.tamixa.security.TokenStorage> { AndroidTokenStorage(androidContext()) }
                },
                sharedModule(baseUrl),
                viewModelModule()
            )
        }
    }
}
