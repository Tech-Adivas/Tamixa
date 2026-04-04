package com.tamixa.ui.data

import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus

/**
 * Sample data inspired by Tamil stories from tamilsirukathaigal.com
 * Categories: பஞ்சதந்திர கதைகள், ஈசாப் கதைகள், தெனாலிராமன் கதைகள், நீதிக்கதைகள்
 *
 * categories: Must stay aligned with backend StoryCategories.canonical (same order; "All" is app-only for filter).
 * Admin STORY_CATEGORIES and bulk generation use the same list excluding "All".
 */
object SampleData {
    val categories = listOf(
        "All",
        "Animals",
        "Friendship",
        "Adventure",
        "Village Life",
        "Moral Stories",
        "Fun stories",
        "Funny Stories",
        "Family Stories",
        "Fantasy",
        "Nature",
        "Bravery",
        "Learn · History",
        "Learn · Science & Nature",
        "Learn · Culture & Heritage",
        "Learn · Life Skills",
        "Learn · Digital Safety",
        "Learn · Simulator · Digital Safety",
    )

    /** English fallback for TTS when Tamil/other language packs are not installed. */
    private fun foxAndShadowEnglish(): String =
        """One day a fox was walking along a forest path. The sunlight fell on him. The fox saw his shadow and thought, "Wow! My shadow is so big! I must be the biggest animal in the world!" He walked through the forest with great pride. When the sun set, the shadow grew small and vanished. The fox understood — pride leads to downfall.
Moral: Pride has no reward."""

    fun sampleStories(): List<Story> = listOf(
        Story(
            id = 1L,
            parentId = 1L,
            childId = 1L,
            content = foxAndShadowEnglish(),
            theme = "The Fox and His Shadow",
            language = "en",
            age = 5,
            childName = "Kavi",
            wordCount = 72,
            readingTimeMinutes = 0.7,
            status = StoryStatus.READY,
            audioFileUrl = "https://example.com/audio/1.mp3",
            createdAt = "2025-02-20T10:00:00Z"
        ),
        Story(
            id = 2L,
            parentId = 1L,
            childId = 1L,
            content = """A monkey lived in a tree. nearby in the forest river lived a crocodile. The crocodile's mother wished to eat the monkey's heart. The crocodile's son befriended the monkey and brought him home. The monkey said, "My heart is in the tree. Go and get it!" The monkey climbed the tree and escaped.
Moral: Cleverness wins over strength.""",
            theme = "The Monkey and The Crocodile",
            language = "en",
            age = 5,
            childName = "Kavi",
            wordCount = 68,
            readingTimeMinutes = 0.7,
            status = StoryStatus.READY,
            audioFileUrl = "https://example.com/audio/2.mp3",
            createdAt = "2025-02-21T10:00:00Z"
        ),
        Story(
            id = 3L,
            parentId = 1L,
            childId = 1L,
            content = """It was summer. The ants were gathering food. The grasshopper sang and wasted the days. "Work during the year," said the ant. The grasshopper laughed. Winter came. The grasshopper had no food. He begged the ant. The ant said, "You sang in summer; now dance in winter!"
Moral: Those who work hard will always have plenty.""",
            theme = "The Ant and the Grasshopper",
            language = "en",
            age = 5,
            childName = "Kavi",
            wordCount = 58,
            readingTimeMinutes = 0.6,
            status = StoryStatus.READY,
            audioFileUrl = "https://example.com/audio/3.mp3",
            createdAt = "2025-02-22T10:00:00Z"
        ),
        Story(
            id = 4L,
            parentId = 1L,
            childId = 1L,
            content = """A lion lived in the forest. Each day he asked one animal for food. The rabbits made a plan. One rabbit told the lion, "Your Majesty, there is another lion in the well!" The lion was angry and looked into the well. He saw his reflection and jumped in. He fell and died.
Moral: Cleverness wins over strength.""",
            theme = "The Foolish Lion and the Clever Rabbit",
            language = "en",
            age = 5,
            childName = "Kavi",
            wordCount = 65,
            readingTimeMinutes = 0.7,
            status = StoryStatus.READY,
            audioFileUrl = "https://example.com/audio/4.mp3",
            createdAt = "2025-02-23T10:00:00Z"
        ),
        Story(
            id = 5L,
            parentId = 1L,
            childId = 1L,
            content = "",
            theme = "விவசாயி, மகன், கழுதை | The Man, the Boy, and the Donkey",
            language = "ta",
            age = 5,
            childName = "கவி",
            wordCount = 0,
            readingTimeMinutes = 0.0,
            status = StoryStatus.GENERATING,
            audioFileUrl = null,
            createdAt = "2025-02-24T10:00:00Z"
        ),
        Story(
            id = 6L,
            parentId = 1L,
            childId = 1L,
            content = """King Krishnadevaraya's favorite courtier was Tenali Raman. One day two thieves came to test Tenali. One said, "One of us is good, one is bad. Whom will you choose?" Tenali laughed and said, "I choose the bad one. The good one will save me!" The thieves saw their plan fail and ran away.
Moral: With wit you can win anything.""",
            theme = "Tenali Raman and The Two Thieves",
            language = "en",
            age = 6,
            childName = "Kavi",
            wordCount = 82,
            readingTimeMinutes = 0.8,
            status = StoryStatus.READY,
            audioFileUrl = "https://example.com/audio/6.mp3",
            createdAt = "2025-02-25T10:00:00Z"
        )
    )
}
