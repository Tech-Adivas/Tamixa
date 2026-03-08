package com.araro.android

import android.app.Application
import com.araro.di.sharedModule
import com.araro.di.viewModelModule
import com.araro.security.AndroidTokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AraroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val baseUrl = BuildConfig.BASE_URL ?: "http://10.0.2.2:8080"
        startKoin {
            androidContext(this@AraroApplication)
            modules(
                module {
                    single<com.araro.security.TokenStorage> { AndroidTokenStorage(androidContext()) }
                },
                sharedModule(baseUrl),
                viewModelModule()
            )
        }
    }
}
