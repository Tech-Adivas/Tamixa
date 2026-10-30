package com.tamixa.application.library

import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.port.ImageStoragePort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.LibraryStoryStatus
import com.tamixa.domain.StoryTranslation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

/** A cover must show the story as it is now, not an older version kept in a stale English translation. */
class LibraryStoryCoverTextTest {

    private val translations: StoryTranslationRepositoryPort = mock()
    private val service = LibraryStoryIllustrationService(
        mock<StoryLibraryRepositoryPort>(), translations, mock<CoverImageUrlResolver>(),
        mock<ImageGenerationPort>(), mock<ImageStoragePort>(), null, null, null,
    )
    private val edited = Instant.parse("2026-10-17T10:00:00Z")

    private fun story(language: String = "ta") = LibraryStory(
        id = 72, title = "புதிய கதை", content = "புதிய உள்ளடக்கம்", theme = "bedtime", language = language,
        age = 5, childName = "Child", wordCount = 2, readingTimeMinutes = 1.0, moral = null, audioFileUrl = null,
        status = LibraryStoryStatus.DRAFT, coverImageUrl = null, createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        updatedAt = edited,
    )

    private fun english(createdAt: Instant) = StoryTranslation(
        id = 1, masterStoryId = 72, language = "en", title = "Old story", content = "Old content", moral = null,
        wordCount = 2, readingTimeMinutes = 1.0, createdAt = createdAt,
    )

    @Test
    fun `english translation older than the story's last save is not used`() {
        whenever(translations.findByMasterStoryIdAndLanguage(72, "en")).thenReturn(english(edited.minusSeconds(86_400)))

        assertEquals(CoverText("புதிய கதை", "புதிய உள்ளடக்கம்"), service.resolveCoverText(story(), null))
    }

    @Test
    fun `english translation made after the last save is used`() {
        whenever(translations.findByMasterStoryIdAndLanguage(72, "en")).thenReturn(english(edited.plusSeconds(60)))

        assertEquals(CoverText("Old story", "Old content"), service.resolveCoverText(story(), null))
    }

    @Test
    fun `the editor's on-screen text wins, even before it is saved`() {
        whenever(translations.findByMasterStoryIdAndLanguage(72, "en")).thenReturn(english(edited.plusSeconds(60)))

        assertEquals(
            CoverText("Unsaved title", "Unsaved text"),
            service.resolveCoverText(story(), CoverText("Unsaved title", "Unsaved text")),
        )
        // Blank on-screen title falls back to the story's title.
        assertEquals(CoverText("புதிய கதை", "Unsaved text"), service.resolveCoverText(story(), CoverText(" ", "Unsaved text")))
    }

    @Test
    fun `story without english translation uses its own text`() {
        whenever(translations.findByMasterStoryIdAndLanguage(72, "en")).thenReturn(null)

        assertEquals(CoverText("புதிய கதை", "புதிய உள்ளடக்கம்"), service.resolveCoverText(story(), null))
    }
}
