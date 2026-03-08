package com.araro.ui.navigation

import com.araro.util.AraroConstants

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object LanguageSelection : Screen("language")
    data object Dashboard : Screen("dashboard")
    data object ChildList : Screen("children")
    data object ChildCreate : Screen("children/create")
    data object StorySelection : Screen("stories")
    data object StoryGeneration : Screen("stories/generate")
    data object AudioPlayer : Screen("audio/{storyId}") {
        fun withId(storyId: Long, storySource: String = AraroConstants.STORY_SOURCE_GENERATED) = "audio/$storyId?storySource=$storySource"
    }
    data object VoiceUpload : Screen("voice")
    data object AvatarUpload : Screen("avatar")
    data object Subscription : Screen("subscription")
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
    data object Favorites : Screen("favorites")
    data object Search : Screen("search")
}
