package com.araro.application.stream

import com.araro.application.narration.EmotionToToneMapper
import com.araro.application.narration.RewriteService
import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.StoryRepositoryPort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.port.narration.StoryNarrationScriptRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Returns conversational (rewritten) narration script for TTS fallback.
 * When mobile can't play backend audio, it uses on-device TTS. This service provides
 * the conversational script so device TTS sounds warm instead of dry reading.
 */
@Service
class NarrationScriptService(
    private val curatedStoryRepository: CuratedStoryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationScriptRepository: StoryNarrationScriptRepositoryPort,
    private val rewriteService: RewriteService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Returns conversational script for the story in the given language.
     * Curated: from story_narration_scripts if present, else run rewrite.
     * Generated: run rewrite on story.content.
     */
    fun getNarrationScript(storyId: Long, language: String): String? {
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }

        return curatedStoryRepository.findById(storyId)?.let { curated ->
            val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
            if (translation != null) {
                narrationScriptRepository.findByTranslationId(translation.id)?.scriptText
                    ?: run {
                        log.info("Narration script not cached for curated storyId={} lang={}, running rewrite", storyId, effectiveLang)
                        try {
                            rewriteService.rewrite(
                                translation.content,
                                curated.age,
                                EmotionToToneMapper.toToneMode(curated.emotionMode),
                                effectiveLang
                            ).scriptText
                        } catch (e: Exception) {
                            log.warn("Rewrite failed for curated storyId={}, returning translation content: {}", storyId, e.message)
                            translation.content
                        }
                    }
            } else {
                log.debug("No translation for curated storyId={} lang={}, using raw content", storyId, effectiveLang)
                curated.content
            }
        } ?: storyRepository.findById(storyId)?.let { story ->
            log.info("Generating narration script for generated storyId={} lang={}", storyId, effectiveLang)
            try {
                rewriteService.rewrite(
                    story.content,
                    story.age,
                    EmotionToToneMapper.toToneMode(story.emotionMode),
                    effectiveLang
                ).scriptText
            } catch (e: Exception) {
                log.warn("Rewrite failed for generated storyId={}, returning raw content: {}", storyId, e.message)
                story.content
            }
        }
    }
}
