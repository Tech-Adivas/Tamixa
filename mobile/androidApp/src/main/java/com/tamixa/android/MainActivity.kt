package com.tamixa.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.tamixa.ui.theme.TamixaTheme
import com.tamixa.android.navigation.TamixaNavHost
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialDeepLinkUri = intent?.data?.toString()
        setContent {
            val scope = rememberCoroutineScope()
            val settingsViewModel = remember(scope) {
                org.koin.core.context.GlobalContext.get().get<com.tamixa.ui.viewmodel.SettingsViewModel>(parameters = { parametersOf(scope) })
            }
            val settingsState by settingsViewModel.state.collectAsState()
            val darkTheme = settingsState.darkMode
            TamixaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TamixaNavHost(
                        initialDeepLinkUri = initialDeepLinkUri,
                        authViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.tamixa.ui.viewmodel.AuthViewModel>(parameters = { parametersOf(scope) })
                        },
                        storyViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.tamixa.ui.viewmodel.StoryViewModel>(parameters = { parametersOf(scope) })
                        },
                        voiceViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.tamixa.ui.viewmodel.VoiceViewModel>(parameters = { parametersOf(scope) })
                        },
                        avatarViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.tamixa.ui.viewmodel.AvatarViewModel>(parameters = { parametersOf(scope) })
                        },
                        subscriptionViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.tamixa.ui.viewmodel.SubscriptionViewModel>(parameters = { parametersOf(scope) })
                        },
                        shortContentViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.tamixa.ui.viewmodel.ShortContentViewModel>()
                        },
                        settingsViewModel = settingsViewModel,
                        onSensitiveScreen = { secure ->
                            if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    )
                }
            }
        }
    }
}
