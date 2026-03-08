package com.araro.application.narration

import com.araro.application.port.StoryRepositoryPort
import com.araro.domain.StoryStatus
import com.araro.domain.narration.ToneMode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Generates conversational narration audio for user AI-generated stories.
 * Pipeline: LLM rewrite (real-time conversation style) → SSML → Neural TTS → S3.
 * Uses same RewriteService/NarrationOpenAIAdapter prompts as curated stories.
 */
@Service
class GeneratedStoryNarrationService(
    private val storyRepository: StoryRepositoryPort,
    private val rewriteService: RewriteService,
    private val emotionTaggingService: EmotionTaggingService,
    private val ssmlBuilder: SSMLBuilderService,
    private val ttsService: TTSService,
    private val audioStorage: AudioStorageService,
    private val ttsConcurrencyLimiter: TtsConcurrencyLimiter
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
            val ssml = run {
                val emotionTagged = emotionTaggingService.tagEmotions(scriptText, language)
                if (emotionTagged.validateWordCountTolerance(15)) {
                    ssmlBuilder.buildSSMLFromEmotionTagged(emotionTagged, language, age, toneMode)
                } else {
                    ssmlBuilder.buildSSML(scriptText, language, age, toneMode)
                }
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
            // Persist narrated script so displayed content matches audio
            val scriptWordCount = scriptText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val scriptReadingTime = (scriptWordCount / 150.0).coerceAtMost(5.0)
            storyRepository.updateNarratedContent(storyId, scriptText, scriptWordCount, scriptReadingTime)
            log.info("Generated story narration OK storyId={} path={} bytes={}", storyId, audioPath, audioBytes.size)
            audioPath
        } catch (e: Exception) {
            log.error("Narration pipeline failed for storyId={}: {}", storyId, e.message, e)
            null
        }
    }
}
