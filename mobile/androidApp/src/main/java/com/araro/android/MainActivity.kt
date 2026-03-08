package com.araro.android

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
import com.araro.ui.theme.AraroTheme
import com.araro.android.navigation.AraroNavHost
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val settingsViewModel = remember(scope) {
                org.koin.core.context.GlobalContext.get().get<com.araro.ui.viewmodel.SettingsViewModel>(parameters = { parametersOf(scope) })
            }
            val settingsState by settingsViewModel.state.collectAsState()
            val darkTheme = settingsState.darkMode
            AraroTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AraroNavHost(
                        authViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.araro.ui.viewmodel.AuthViewModel>(parameters = { parametersOf(scope) })
                        },
                        childViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.araro.ui.viewmodel.ChildViewModel>(parameters = { parametersOf(scope) })
                        },
                        storyViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.araro.ui.viewmodel.StoryViewModel>(parameters = { parametersOf(scope) })
                        },
                        voiceViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.araro.ui.viewmodel.VoiceViewModel>(parameters = { parametersOf(scope) })
                        },
                        avatarViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.araro.ui.viewmodel.AvatarViewModel>(parameters = { parametersOf(scope) })
                        },
                        subscriptionViewModel = remember(scope) {
                            org.koin.core.context.GlobalContext.get().get<com.araro.ui.viewmodel.SubscriptionViewModel>(parameters = { parametersOf(scope) })
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
