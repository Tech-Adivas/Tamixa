package com.tamixa.application.narration

import com.tamixa.IntegrationTestBase
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.ToneMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

/**
 * Integration test for StoryProcessingOrchestrator.
 * Mocks external services (LLM, TTS, storage) to test orchestration flow.
 */
@ActiveProfiles("test")
class StoryProcessingOrchestratorIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var orchestrator: StoryProcessingOrchestrator

    @Autowired
    private lateinit var translationRepository: StoryTranslationRepositoryPort

    @Autowired
    private lateinit var storyLibraryRepository: StoryLibraryRepositoryPort

    @Autowired
    private lateinit var narrationAudioRepository: StoryNarrationAudioRepositoryPort

    @MockBean
    private lateinit var narrationFormatter: NarrationFormatterService

    /** [NarrationFormatterServiceImpl] implements both formatter and [RewriteService]; mocking the formatter removes that bean. */
    @MockBean
    private lateinit var rewriteService: RewriteService

    @MockBean
    private lateinit var safetyValidator: SafetyValidatorService

    @MockBean
    private lateinit var ssmlBuilder: SSMLBuilderService

    @MockBean
    private lateinit var ttsService: TTSService

    @MockBean
    private lateinit var audioStorage: AudioStorageService

    @MockBean
    private lateinit var premiumVoiceValidator: NarrationPremiumVoiceValidator

    /** Real limiter avoids Kotlin-default-parameter stubbing issues with [NarrationConcurrencyLimiter.tryAcquire]. */

    @org.junit.jupiter.api.BeforeEach
    fun setUpMocks() {
        whenever(premiumVoiceValidator.canUseVoice(org.mockito.kotlin.any(), anyOrNull())).thenReturn(true)
        whenever(
            rewriteService.rewrite(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()
            )
        ).thenReturn(RewriteResult("Formatted story", 10, 5))
    }

    @Test
    fun `orchestrator processes translation and saves READY audio`() {
        val libraryStory = LibraryStory(
            id = 0,
            title = "Test Story",
            content = "Tamil content",
            theme = "bedtime",
            language = "ta",
            age = 5,
            childName = "Child",
            wordCount = 10,
            readingTimeMinutes = 1.0,
            moral = "Be kind",
            audioFileUrl = null,
            status = "DRAFT",
            coverImageUrl = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val savedLibraryStory = storyLibraryRepository.save(libraryStory)
        val translation = StoryTranslation(
            id = 0,
            masterStoryId = savedLibraryStory.id,
            language = "en",
            title = "Test",
            content = "Once upon a time.",
            moral = "Be kind",
            wordCount = 4,
            readingTimeMinutes = 0.1,
            createdAt = Instant.now()
        )
        val savedTranslation = translationRepository.save(translation)

        val calm = ToneMode.CALM
        whenever(narrationFormatter.formatNarration(org.mockito.kotlin.any(), org.mockito.kotlin.any(), eq(calm), org.mockito.kotlin.any()))
            .thenReturn(NarrationFormatResult("Formatted story", 10, 5))
        whenever(safetyValidator.validate(org.mockito.kotlin.any())).thenReturn(ValidationResult(true, 100))
        whenever(
            ssmlBuilder.buildSSMLFromEmotionTagged(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), eq(calm))
        ).thenReturn("<speak>Formatted</speak>")
        whenever(ssmlBuilder.buildSSML(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any(), eq(calm)))
            .thenReturn("<speak>Formatted</speak>")
        whenever(ttsService.synthesize(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn(byteArrayOf(1, 2, 3))
        whenever(
            audioStorage.uploadNarrationAudio(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()
            )
        ).thenReturn("stories/${savedLibraryStory.id}/en/v1.mp3")

        orchestrator.process(savedTranslation)

        val audio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(savedTranslation.id, "default")
        assertNotNull(audio)
        assertEquals(NarrationAudioStatus.READY, audio!!.status)
        assertTrue(audio.audioUrl.endsWith("/v1.mp3"))
    }
}
