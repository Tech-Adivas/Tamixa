package com.tamixa.application.storylibrary

import com.tamixa.application.narration.StoryProcessingOrchestrator
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.LibraryStoryStatus
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.TranslationPipelineStatus
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationAudio
import com.tamixa.infrastructure.config.AppProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.*
import java.time.Instant

/**
 * Unit tests for AudioGenerationService.
 * 
 * Tests cover:
 * - Audio generation for all languages
 * - Audio generation for specific language
 * - Audio status retrieval
 * - Failed audio retry
 * - Status validation
 * - Error handling
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AudioGenerationServiceTest {
    
    private lateinit var service: AudioGenerationService
    
    @Mock
    private lateinit var storyLibraryRepository: StoryLibraryRepositoryPort
    
    @Mock
    private lateinit var storyTranslationRepository: StoryTranslationRepositoryPort
    
    @Mock
    private lateinit var narrationAudioRepository: StoryNarrationAudioRepositoryPort
    
    @Mock
    private lateinit var storyProcessingOrchestrator: StoryProcessingOrchestrator
    
    @Mock
    private lateinit var appProperties: AppProperties
    
    @BeforeEach
    fun setup() {
        // Mock app properties
        val translationPipelineProps = AppProperties.TranslationPipelineProperties(
            sourceLanguage = "ta",
            targetLanguages = "en,hi,te,kn,ml"
        )
        lenient().`when`(appProperties.translationPipeline).thenReturn(translationPipelineProps)
        
        service = AudioGenerationService(
            storyLibraryRepository,
            storyTranslationRepository,
            narrationAudioRepository,
            storyProcessingOrchestrator,
            appProperties
        )
    }
    
    @Test
    fun `generateAudioForAllLanguages should start generation for approved story`() {
        // Given
        val storyId = 1L
        val story = createTestStory(storyId, LibraryStoryStatus.APPROVED)
        val translations = listOf(
            createTestTranslation(1L, storyId, "ta"),
            createTestTranslation(2L, storyId, "en"),
            createTestTranslation(3L, storyId, "hi")
        )
        
        whenever(storyLibraryRepository.findById(storyId)).thenReturn(story)
        whenever(storyTranslationRepository.findByMasterStoryId(storyId)).thenReturn(translations)
        doNothing().whenever(narrationAudioRepository).deleteByTranslationIdAndVoiceProfile(any(), any())
        doNothing().whenever(storyLibraryRepository).updateStatus(any(), any())
        
        // When
        val result = service.generateAudioForAllLanguages(storyId)
        
        // Then
        assertTrue(result.success)
        assertEquals(storyId, result.storyId)
        assertEquals(3, result.languageResults.size)
        assertTrue(result.languageResults.all { it.status == "PENDING" })
        
        verify(storyLibraryRepository).updateStatus(storyId, LibraryStoryStatus.AUDIO_GENERATING)
    }
    
    @Test
    fun `generateAudioForAllLanguages should throw exception for non-approved story`() {
        // Given
        val storyId = 1L
        val story = createTestStory(storyId, LibraryStoryStatus.DRAFT)
        
        whenever(storyLibraryRepository.findById(storyId)).thenReturn(story)
        
        // When/Then
        val exception = assertThrows(IllegalStateException::class.java) {
            service.generateAudioForAllLanguages(storyId)
        }
        
        assertTrue(exception.message!!.contains("APPROVED status"))
    }
    
    @Test
    fun `generateAudioForLanguage should start generation for specific language`() {
        // Given
        val storyId = 1L
        val language = "en"
        val story = createTestStory(storyId, LibraryStoryStatus.APPROVED)
        val translation = createTestTranslation(1L, storyId, language)
        
        whenever(storyLibraryRepository.findById(storyId)).thenReturn(story)
        whenever(storyTranslationRepository.findByMasterStoryIdAndLanguage(storyId, language)).thenReturn(translation)
        doNothing().whenever(narrationAudioRepository).deleteByTranslationIdAndVoiceProfile(any(), any())
        doNothing().whenever(storyLibraryRepository).updateStatus(any(), any())
        
        // When
        val result = service.generateAudioForLanguage(storyId, language)
        
        // Then
        assertTrue(result.success)
        assertEquals(storyId, result.storyId)
        assertEquals(1, result.languageResults.size)
        assertEquals(language, result.languageResults[0].language)
        assertEquals("PENDING", result.languageResults[0].status)
        
        verify(storyLibraryRepository).updateStatus(storyId, LibraryStoryStatus.AUDIO_GENERATING)
    }
    
    @Test
    fun `generateAudioForLanguage should throw exception for unsupported language`() {
        // Given
        val storyId = 1L
        val unsupportedLanguage = "fr"
        val story = createTestStory(storyId, LibraryStoryStatus.APPROVED)
        
        whenever(storyLibraryRepository.findById(storyId)).thenReturn(story)
        
        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.generateAudioForLanguage(storyId, unsupportedLanguage)
        }
        
        assertTrue(exception.message!!.contains("Unsupported language"))
    }
    
    @Test
    fun `getAudioStatus should return status for all languages`() {
        // Given
        val storyId = 1L
        val story = createTestStory(storyId, LibraryStoryStatus.AUDIO_GENERATING)
        val translations = listOf(
            createTestTranslation(1L, storyId, "ta"),
            createTestTranslation(2L, storyId, "en"),
            createTestTranslation(3L, storyId, "hi")
        )
        val audioTa = createTestNarrationAudio(1L, 1L, NarrationAudioStatus.READY)
        val audioEn = createTestNarrationAudio(2L, 2L, NarrationAudioStatus.PENDING)
        
        whenever(storyLibraryRepository.findById(storyId)).thenReturn(story)
        whenever(storyTranslationRepository.findByMasterStoryId(storyId)).thenReturn(translations)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(1L, "default")).thenReturn(audioTa)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(2L, "default")).thenReturn(audioEn)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(3L, "default")).thenReturn(null)
        doNothing().whenever(storyLibraryRepository).updateStatus(any(), any())
        
        // When
        val result = service.getAudioStatus(storyId)
        
        // Then
        assertEquals(storyId, result.storyId)
        assertTrue(result.languageAudioStatuses.isNotEmpty())
        
        val taStatus = result.languageAudioStatuses.find { it.language == "ta" }
        assertNotNull(taStatus)
        assertEquals("READY", taStatus?.status)
        assertTrue(taStatus?.hasAudio == true)
        
        val enStatus = result.languageAudioStatuses.find { it.language == "en" }
        assertNotNull(enStatus)
        assertEquals("PENDING", enStatus?.status)
        assertFalse(enStatus?.hasAudio == true)
    }
    
    @Test
    fun `retryFailedAudio should retry only failed languages`() {
        // Given
        val storyId = 1L
        val story = createTestStory(storyId, LibraryStoryStatus.AUDIO_FAILED)
        val translations = listOf(
            createTestTranslation(1L, storyId, "ta"),
            createTestTranslation(2L, storyId, "en"),
            createTestTranslation(3L, storyId, "hi")
        )
        val audioTa = createTestNarrationAudio(1L, 1L, NarrationAudioStatus.READY)
        val audioEn = createTestNarrationAudio(2L, 2L, NarrationAudioStatus.FAILED)
        val audioHi = createTestNarrationAudio(3L, 3L, NarrationAudioStatus.FAILED)
        
        whenever(storyLibraryRepository.findById(storyId)).thenReturn(story)
        whenever(storyTranslationRepository.findByMasterStoryId(storyId)).thenReturn(translations)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(1L, "default")).thenReturn(audioTa)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(2L, "default")).thenReturn(audioEn)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(3L, "default")).thenReturn(audioHi)
        doNothing().whenever(narrationAudioRepository).deleteByTranslationIdAndVoiceProfile(any(), any())
        doNothing().whenever(storyLibraryRepository).updateStatus(any(), any())
        
        // When
        val result = service.retryFailedAudio(storyId)
        
        // Then
        assertTrue(result.success)
        assertEquals(2, result.languageResults.size) // Only en and hi should be retried
        assertTrue(result.languageResults.all { it.language in listOf("en", "hi") })
        assertTrue(result.languageResults.all { it.status == "PENDING" })
        
        verify(narrationAudioRepository, atLeastOnce()).deleteByTranslationIdAndVoiceProfile(2L, "default")
        verify(narrationAudioRepository, atLeastOnce()).deleteByTranslationIdAndVoiceProfile(3L, "default")
        verify(storyLibraryRepository, atLeastOnce()).updateStatus(storyId, LibraryStoryStatus.AUDIO_GENERATING)
    }
    
    @Test
    fun `retryFailedAudio should return empty result when no failed audio`() {
        // Given
        val storyId = 1L
        val story = createTestStory(storyId, LibraryStoryStatus.APPROVED)
        val translations = listOf(
            createTestTranslation(1L, storyId, "ta"),
            createTestTranslation(2L, storyId, "en")
        )
        val audioTa = createTestNarrationAudio(1L, 1L, NarrationAudioStatus.READY)
        val audioEn = createTestNarrationAudio(2L, 2L, NarrationAudioStatus.READY)
        
        whenever(storyLibraryRepository.findById(storyId)).thenReturn(story)
        whenever(storyTranslationRepository.findByMasterStoryId(storyId)).thenReturn(translations)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(1L, "default")).thenReturn(audioTa)
        whenever(narrationAudioRepository.findByTranslationIdAndVoiceProfile(2L, "default")).thenReturn(audioEn)
        
        // When
        val result = service.retryFailedAudio(storyId)
        
        // Then
        assertTrue(result.success)
        assertEquals(0, result.languageResults.size)
        assertTrue(result.message.contains("No failed audio"))
    }
    
    // Helper methods
    
    private fun createTestStory(id: Long, status: LibraryStoryStatus): LibraryStory {
        return LibraryStory(
            id = id,
            title = "Test Story",
            content = "Test content",
            theme = "adventure",
            category = "adventure",
            language = "ta",
            age = 8,
            childName = "Test Child",
            wordCount = 100,
            readingTimeMinutes = 5.0,
            moral = "Test moral",
            audioFileUrl = null,
            status = status,
            coverImageUrl = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
    
    private fun createTestTranslation(id: Long, masterStoryId: Long, language: String): StoryTranslation {
        return StoryTranslation(
            id = id,
            masterStoryId = masterStoryId,
            language = language,
            title = "Test Story",
            content = "Test content",
            moral = "Test moral",
            wordCount = 100,
            readingTimeMinutes = 5.0,
            createdAt = Instant.now(),
            status = TranslationPipelineStatus.COMPLETED
        )
    }
    
    private fun createTestNarrationAudio(
        id: Long,
        translationId: Long,
        status: NarrationAudioStatus
    ): StoryNarrationAudio {
        return StoryNarrationAudio(
            id = id,
            translationId = translationId,
            voiceProfile = "default",
            audioUrl = if (status == NarrationAudioStatus.READY) "https://example.com/audio.mp3" else "",
            durationSeconds = if (status == NarrationAudioStatus.READY) 300 else 0,
            status = status,
            createdAt = Instant.now()
        )
    }
}
