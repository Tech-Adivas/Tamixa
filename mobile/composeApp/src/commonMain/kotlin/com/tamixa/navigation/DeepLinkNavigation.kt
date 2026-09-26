package com.tamixa.navigation

import androidx.navigation.NavHostController
import com.tamixa.ui.navigation.Screen
import com.tamixa.ui.viewmodel.AuthViewModel
import com.tamixa.util.AuthValidation

/** Parses `tamixa://story/{id}` or paths containing `/s/{id}`. */
fun parseStoryIdFromDeepLink(uri: String): Long? {
    if (uri.isBlank()) return null
    return when {
        uri.contains("tamixa://story/") ->
            uri.substringAfter("tamixa://story/").substringBefore("?").trim().toLongOrNull()
        uri.contains("/s/") ->
            uri.substringAfterLast("/s/").substringBefore("?").trim().toLongOrNull()
        else -> null
    }
}

/**
 * Handles one launch URI: magic-link passwordless token and/or story player deep link.
 * Safe to call from the main thread when navigation is active.
 */
fun handleExternalLaunchUri(
    uri: String,
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val route = navController.currentDestination?.route

    val token = AuthValidation.extractPasswordlessLoginTokenFromUri(uri)
    if (!token.isNullOrBlank() && !authViewModel.isLoggedIn()) {
        authViewModel.setPendingPasswordlessMagicLinkToken(token)
        if (route != null && route != Screen.Login.route && route != Screen.Splash.route) {
            navController.navigate(Screen.Login.route) {
                launchSingleTop = true
            }
        }
        return
    }

    val storyId = parseStoryIdFromDeepLink(uri) ?: return
    if (route == Screen.Splash.route || route == Screen.Login.route) return
    if (!authViewModel.isLoggedIn()) return
    navController.navigate(Screen.AudioPlayer.withId(storyId)) {
        launchSingleTop = true
    }
}
