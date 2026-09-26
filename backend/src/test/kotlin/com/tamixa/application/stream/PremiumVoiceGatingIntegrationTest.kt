package com.tamixa.application.stream

import com.tamixa.IntegrationTestBase
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.LibraryStory
import com.tamixa.infrastructure.persistence.NarrationVoiceCatalogEntity
import com.tamixa.infrastructure.persistence.NarrationVoiceCatalogJpaRepository
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.narration.NarrationAudioStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * Integration test for premium voice gating.
 * Verifies SubscriptionGuard returns 402 when premium voice requested without entitlement.
 */
@ActiveProfiles("test")
class PremiumVoiceGatingIntegrationTest : IntegrationTestBase() {

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

    @org.junit.jupiter.api.BeforeEach
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
    fun `premium voice stream returns 402 when user not entitled`() {
        val libraryStory = storyLibraryRepository.save(
            LibraryStory(
                id = 0,
                title = "Test",
                content = "Story",
                theme = "bedtime",
                language = "ta",
                age = 5,
                childName = "Child",
                wordCount = 5,
                readingTimeMinutes = 1.0,
                moral = "Be kind",
                audioFileUrl = null,
                status = com.tamixa.domain.LibraryStoryStatus.DRAFT,
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
                title = "Test",
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
                voiceProfile = "calm",
                audioUrl = "stories/${libraryStory.id}/en/calm.mp3",
                durationSeconds = 10,
                status = NarrationAudioStatus.READY,
                createdAt = Instant.now()
            )
        )

        // resolveParentId returns null in controller -> no premium entitlement
        // calm is premium per seed data -> expect 402
        mockMvc.perform(
            get("/api/v1/stories/library/${libraryStory.id}/stream-url")
                .param("language", "en")
                .param("voiceProfile", "calm")
        ).andExpect(status().isPaymentRequired)
    }
}
