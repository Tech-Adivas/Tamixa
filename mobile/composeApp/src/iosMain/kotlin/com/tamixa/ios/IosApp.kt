package com.tamixa.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.tamixa.di.initKoin
import com.tamixa.di.iosPlatformModule
import com.tamixa.ui.TamixaApp
import com.tamixa.ui.theme.TamixaTheme
import platform.Foundation.NSLog
import platform.UIKit.UIViewController

private var koinInitialized = false

/** Set by Swift so pickers (audio/image) can be presented. Optional; if null, pickers are no-op. */
var hostViewControllerForPickers: UIViewController? = null

fun setHostViewControllerForPickers(vc: UIViewController?) {
    hostViewControllerForPickers = vc
}

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
 * iOS root composable: initializes Koin from the Compose tree, then shows [TamixaApp].
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
            NSLog("Tamixa: $msg\n${e.stackTraceToString()}")
            initError = msg
        }
    }

    TamixaTheme(darkTheme = true) {
        when {
            initError != null -> {
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = initError!!,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            ready -> {
                TamixaApp(onSensitiveScreen = null)
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Loading…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
