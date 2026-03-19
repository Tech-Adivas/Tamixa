package com.tamixa.application.narration

import com.tamixa.application.port.GeneratedStorySceneRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.domain.StoryStatus
import com.tamixa.domain.narration.ToneMode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Generates conversational narration audio for user AI-generated stories.
 * Pipeline: LLM rewrite (real-time conversation style) → SSML → Neural TTS → S3.
 * Persists structured scenes/segments for playback manifest parity with library stories.
 */
@Service
class GeneratedStoryNarrationService(
    private val storyRepository: StoryRepositoryPort,
    private val rewriteService: RewriteService,
    private val emotionTaggingService: EmotionTaggingService,
    private val ssmlBuilder: SSMLBuilderService,
    private val ttsService: TTSService,
    private val audioStorage: AudioStorageService,
    private val ttsConcurrencyLimiter: TtsConcurrencyLimiter,
    private val generatedStorySceneRepository: GeneratedStorySceneRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val defaultVoice = "default"

    /**
     * Processes story content into conversational narration audio and stores it.
     * @return Audio path for story.audioFileUrl, or null on failure
     */
    fun processAndStore(storyId: Long): String? {
        val story = storyRepository.findById(storyId) ?: run {
            log.warn("Story storyId={} not found for narration", storyId)
            return null
        }
        if (story.content.isBlank()) {
            log.warn("Story storyId={} has no content, skipping narration", storyId)
            return null
        }
        val language = story.language.trim().lowercase().take(10).ifEmpty { "en" }
        val age = story.age.coerceIn(3, 12)
        val toneMode: ToneMode = EmotionToToneMapper.toToneMode(story.emotionMode)

        return try {
            val rewriteResult = rewriteService.rewrite(
                storyText = story.content,
                age = age,
                toneMode = toneMode,
                language = language
            )
            val scriptText = rewriteResult.scriptText
            val emotionTagged = emotionTaggingService.tagEmotions(scriptText, language)
            val emotionTaggedForScenes = if (emotionTagged.validateWordCountTolerance(15)) emotionTagged else null
            val ssml = if (emotionTaggedForScenes != null) {
                ssmlBuilder.buildSSMLFromEmotionTagged(emotionTaggedForScenes, language, age, toneMode)
            } else {
                ssmlBuilder.buildSSML(scriptText, language, age, toneMode)
            }
            val audioBytes = ttsConcurrencyLimiter.withPermit(
                block = { ttsService.synthesize(ssml, language, defaultVoice) }
            )
            if (audioBytes == null || audioBytes.isEmpty()) {
                log.error("TTS returned null or empty for storyId={}", storyId)
                return null
            }
            val audioPath = audioStorage.uploadNarrationAudio(
                storyId = storyId,
                language = language,
                voiceProfile = "audio",
                mp3Bytes = audioBytes
            )
            val durationSeconds = (audioBytes.size / 16000).coerceAtLeast(1)
            // Persist scenes + segments for structured playback
            generatedStorySceneRepository.saveSceneWithSegments(
                storyId = storyId,
                language = language,
                script = scriptText,
                totalDurationSeconds = durationSeconds,
                audioUrl = audioPath,
                theme = story.theme,
                emotionTaggedScript = emotionTaggedForScenes
            )
            // Persist narrated script so displayed content matches audio. Use actual duration from audio.
            val scriptWordCount = scriptText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val scriptReadingTime = (durationSeconds / 60.0).coerceIn(0.5, 30.0)
            storyRepository.updateNarratedContent(storyId, scriptText, scriptWordCount, scriptReadingTime)
            log.info("Generated story narration OK storyId={} path={} bytes={} scenes persisted", storyId, audioPath, audioBytes.size)
            audioPath
        } catch (e: Exception) {
            log.error("Narration pipeline failed for storyId={}: {}", storyId, e.message, e)
            null
        }
    }
}
