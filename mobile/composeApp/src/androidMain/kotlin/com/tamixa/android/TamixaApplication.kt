package com.tamixa.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.tamixa.di.androidPlatformModule
import com.tamixa.di.sharedModule
import com.tamixa.di.viewModelModule
import com.tamixa.platform.setPlatformAppContext
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class TamixaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setPlatformAppContext(this)
        // Only enable Crashlytics in release; debug uses placeholder google-services.json (see FIREBASE_SETUP.md)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        val baseUrl = BuildConfig.BASE_URL ?: "http://10.0.2.2:8080"
        startKoin {
            androidContext(this@TamixaApplication)
            modules(
                androidPlatformModule(this@TamixaApplication),
                sharedModule(baseUrl),
                viewModelModule()
            )
        }
        // Coil: use Ktor for network image loading (HD covers)
        val httpClient = GlobalContext.get().get<HttpClient>()
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(it)
                .components {
                    add(KtorNetworkFetcherFactory(httpClient))
                }
                .crossfade(100)
                .build()
        }
    }
}
