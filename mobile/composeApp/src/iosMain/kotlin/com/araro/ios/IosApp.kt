package com.araro.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.araro.di.initKoin
import com.araro.di.iosPlatformModule
import com.araro.ui.AraroApp
import platform.Foundation.NSLog
import platform.UIKit.UIViewController

private var koinInitialized = false

/**
 * Call from Swift in App.init() on the main thread. Ensures the Kotlin framework is loaded
 * on the main thread so Dispatchers.Main can bind to the iOS main queue before any UI runs.
 */
fun warmupOnMainThread() {
    // No-op; the call itself loads the framework on the calling (main) thread.
}

/**
 * Idempotent Koin init. Called from [IosAppContent] so it runs after Compose has set the Main
 * dispatcher (avoids Preconditions.kt crash on iOS). Still safe to call from Swift if needed.
 */
fun doInitKoin(baseUrl: String = "http://127.0.0.1:8080") {
    if (koinInitialized) return
    koinInitialized = true
    initKoin(baseUrl, iosPlatformModule())
}

/**
 * iOS root composable: initializes Koin from the Compose tree, then shows [AraroApp].
 * Catches any throwable and logs via NSLog so the real error is visible in Xcode console.
 */
@androidx.compose.runtime.Composable
private fun IosAppContent(baseUrl: String = "http://127.0.0.1:8080") {
    var ready by remember { mutableStateOf(koinInitialized) }
    var initError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            doInitKoin(baseUrl)
            ready = true
        } catch (e: Throwable) {
            val msg = "Koin init failed: ${e.message}"
            NSLog("Araro: $msg\n${e.stackTraceToString()}")
            initError = msg
        }
    }

    when {
        initError != null -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("$initError")
            }
        }
        ready -> {
            AraroApp(onSensitiveScreen = null)
        }
        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Returns the main Compose UIViewController for the iOS app.
 * Call from Swift: IosAppKt.MainViewController() or IosAppKt.MainViewController(baseUrl: "http://192.168.x.x:8080") for physical device.
 * Koin is initialized inside the Compose tree (no need to call doInitKoin from Swift).
 */
fun MainViewController(baseUrl: String = "http://127.0.0.1:8080"): UIViewController = ComposeUIViewController {
    IosAppContent(baseUrl = baseUrl)
}
