package com.tamixa.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tamixa.navigation.TamixaNavHost
import com.tamixa.ui.theme.TamixaTheme
import org.koin.compose.koinInject

/**
 * Shared app root. Used by Android MainActivity and iOS ComposeUIViewController.
 * ViewModels use a Koin-provided app scope (no parameters), which avoids parameter resolution issues on iOS.
 *
 * @param onSensitiveScreen Called when navigating to/from login/register. Android uses for FLAG_SECURE; iOS passes null.
 */
@androidx.compose.runtime.Composable
fun TamixaApp(onSensitiveScreen: ((Boolean) -> Unit)? = null) {
    val settingsViewModel: com.tamixa.ui.viewmodel.SettingsViewModel = koinInject()
    val settingsState by settingsViewModel.state.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = if (settingsState.useSystemTheme) systemDark else settingsState.darkMode
    TamixaTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            TamixaNavHost(
                authViewModel = koinInject(),
                storyViewModel = koinInject(),
                voiceViewModel = koinInject(),
                avatarViewModel = koinInject(),
                subscriptionViewModel = koinInject(),
                settingsViewModel = settingsViewModel,
                shortContentViewModel = koinInject(),
                onSensitiveScreen = onSensitiveScreen
            )
        }
    }
}

