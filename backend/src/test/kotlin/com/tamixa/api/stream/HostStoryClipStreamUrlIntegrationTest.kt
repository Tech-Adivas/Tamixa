package com.tamixa.api.stream

import com.tamixa.IntegrationTestBase
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.infrastructure.persistence.NarrationVoiceCatalogEntity
import com.tamixa.infrastructure.persistence.NarrationVoiceCatalogJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "app.audio.host-story-clip-url=https://cdn.example.com/host-loop.mp4",
        "app.audio.public-base-url=http://localhost:8080"
    ]
)
class HostStoryClipStreamUrlIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var storyLibraryRepository: StoryLibraryRepositoryPort

    @Autowired
    private lateinit var translationRepository: StoryTranslationRepositoryPort

    @Autowired
    private lateinit var narrationAudioRepository: StoryNarrationAudioRepositoryPort

    @Autowired
    private lateinit var voiceCatalogJpa: NarrationVoiceCatalogJpaRepository

    @BeforeEach
    fun seedVoiceCatalog() {
        if (voiceCatalogJpa.findFirstByToneModeAndIsActiveTrue("calm") == null) {
            voiceCatalogJpa.save(
                NarrationVoiceCatalogEntity(
                    provider = "neural",
                    language = "en",
                    voiceName = "calm",
                    toneMode = "calm",
                    isPremium = true,
                    isActive = true
                )
            )
        }
    }

    @Test
    @WithMockUser(roles = ["PARENT"])
    fun `library stream-url JSON includes configured hostStoryClipUrl`() {
        val libraryStory = storyLibraryRepository.save(
            LibraryStory(
                id = 0,
                title = "HostClip",
                content = "Story",
                theme = "bedtime",
                language = "ta",
                age = 5,
                childName = "Child",
                wordCount = 5,
                readingTimeMinutes = 1.0,
                moral = "Be kind",
                audioFileUrl = null,
                status = "DRAFT",
                coverImageUrl = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val translation = translationRepository.save(
            StoryTranslation(
                id = 0,
                masterStoryId = libraryStory.id,
                language = "en",
                title = "HostClip",
                content = "Content",
                moral = "Be kind",
                wordCount = 1,
                readingTimeMinutes = 0.1,
                createdAt = Instant.now()
            )
        )
        narrationAudioRepository.save(
            com.tamixa.domain.narration.StoryNarrationAudio(
                id = 0,
                translationId = translation.id,
                voiceProfile = "default",
                audioUrl = "stories/${libraryStory.id}/en/default.mp3",
                durationSeconds = 10,
                status = NarrationAudioStatus.READY,
                createdAt = Instant.now()
            )
        )

        mockMvc.perform(
            get("/api/v1/stories/library/${libraryStory.id}/stream-url")
                .param("language", "en")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.streamUrl").isNotEmpty)
            .andExpect(jsonPath("$.hostStoryClipUrl").value("https://cdn.example.com/host-loop.mp4"))
    }
}
