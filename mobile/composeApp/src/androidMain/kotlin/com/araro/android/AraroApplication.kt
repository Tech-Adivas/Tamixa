package com.araro.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.araro.di.androidPlatformModule
import com.araro.di.sharedModule
import com.araro.di.viewModelModule
import com.araro.platform.setPlatformAppContext
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class AraroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setPlatformAppContext(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        val baseUrl = BuildConfig.BASE_URL ?: "http://10.0.2.2:8080"
        startKoin {
            androidContext(this@AraroApplication)
            modules(
                androidPlatformModule(this@AraroApplication),
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
