package com.tamixa.ui.navigation

import com.tamixa.util.TamixaConstants

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object OnboardingHook : Screen("onboarding/hook")
    data object OnboardingDemo : Screen("onboarding/demo")
    data object OnboardingInterests : Screen("onboarding/interests")
    data object OnboardingHomePreview : Screen("onboarding/home-preview")
    data object OnboardingBedtimeReminder : Screen("onboarding/bedtime-reminder")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object LanguageSelection : Screen("language")
    data object Dashboard : Screen("dashboard")
    data object Profile : Screen("profile")
    data object Library : Screen("library")
    data object StorySelection : Screen("stories")
    data object StoryGeneration : Screen("stories/generate")
    data object AudioPlayer : Screen("audio/{storyId}") {
        fun withId(storyId: Long, storySource: String = TamixaConstants.STORY_SOURCE_GENERATED) = "audio/$storyId?storySource=$storySource"
    }
    data object VoiceUpload : Screen("voice")
    data object AvatarUpload : Screen("avatar")
    /** Hub for voice + avatar; single bottom tab "My voice & Avatar". */
    data object MyVoiceAndAvatar : Screen("my-voice-avatar")
    data object Subscription : Screen("subscription")
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
    data object Favorites : Screen("favorites")
    data object Search : Screen("search")
    data object ListeningHistory : Screen("listening-history")
    data object Achievements : Screen("achievements")
    data object OnboardingVoiceInvitation : Screen("onboarding/voice-invitation")
    data object OnboardingAvatarInvitation : Screen("onboarding/avatar-invitation")
    data object ShortContent : Screen("short-content")
}
