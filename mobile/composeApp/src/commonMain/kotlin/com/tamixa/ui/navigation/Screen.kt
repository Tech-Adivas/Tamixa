package com.tamixa.ui.navigation

import com.tamixa.util.TamixaConstants

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object OnboardingHook : Screen("onboarding/hook")
    data object OnboardingDemo : Screen("onboarding/demo")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object LanguageSelection : Screen("language")
    data object Dashboard : Screen("dashboard")
    data object Profile : Screen("profile")
    /** Library hub tabs: [HUB_FUN] opens the Fun corner filter; [HUB_LEARN] kept for deep-link compatibility. */
    data object Library : Screen("library?hub={hub}") {
        const val HUB_BROWSE = "browse"
        const val HUB_FUN = "fun"
        const val HUB_LEARN = "learn"
        fun withHub(hub: String = HUB_BROWSE): String = "library?hub=$hub"
    }
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
    /** Alias for [StorySelection]; navigates to the same browse/generate experience (legacy deep links). */
    data object Categories : Screen("categories")
    data object Favorites : Screen("favorites")
    data object Search : Screen("search")
    data object ListeningHistory : Screen("listening-history")
    data object Achievements : Screen("achievements")
    data object OnboardingVoiceInvitation : Screen("onboarding/voice-invitation")
    data object OnboardingAvatarInvitation : Screen("onboarding/avatar-invitation")
    data object ShortContent : Screen("short-content")
    data object ReadingLevel : Screen("reading-level/{childId}") {
        fun withId(childId: Long) = "reading-level/$childId"
    }
    data object Streak : Screen("streak/{childId}") {
        fun withId(childId: Long) = "streak/$childId"
    }
    data object Vocabulary : Screen("vocabulary/{childId}") {
        fun withId(childId: Long) = "vocabulary/$childId"
    }
    data object Classroom : Screen("classroom/{childId}") {
        fun withId(childId: Long) = "classroom/$childId"
    }
    data object Quiz : Screen("quiz/{storyId}/{childId}") {
        fun withIds(storyId: Long, childId: Long) = "quiz/$storyId/$childId"
    }
}
