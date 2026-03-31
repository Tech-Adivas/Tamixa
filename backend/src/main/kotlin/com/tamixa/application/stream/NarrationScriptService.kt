package com.tamixa.application.stream

import com.tamixa.application.narration.EmotionToToneMapper
import com.tamixa.application.narration.RewriteService
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationScriptRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Returns conversational (rewritten) narration script for TTS fallback.
 * When mobile can't play backend audio, it uses on-device TTS. This service provides
 * the conversational script so device TTS sounds warm instead of dry reading.
 */
@Service
class NarrationScriptService(
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationScriptRepository: StoryNarrationScriptRepositoryPort,
    private val rewriteService: RewriteService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Returns conversational script for the story in the given language.
     * Curated: from story_narration_scripts if present, else run rewrite ([requestingParentId] ignored).
     * Generated: run rewrite on story.content — requires [requestingParentId] to match [Story.parentId].
     */
    fun getNarrationScript(storyId: Long, language: String, requestingParentId: Long? = null): String? {
        val effectiveLang = StreamLanguageUtils.normalize(language)

        storyLibraryRepository.findById(storyId)?.let { curated ->
            val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
            return if (translation != null) {
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
        } ?: run {
            val story = storyRepository.findById(storyId) ?: return null
            if (requestingParentId == null || story.parentId != requestingParentId) {
                log.warn(
                    "Narration script denied: generated storyId={} requester={} owner={}",
                    storyId,
                    requestingParentId,
                    story.parentId
                )
                return null
            }
            log.info("Generating narration script for generated storyId={} lang={}", storyId, effectiveLang)
            return try {
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
