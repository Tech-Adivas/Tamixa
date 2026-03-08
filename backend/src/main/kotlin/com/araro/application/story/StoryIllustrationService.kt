package com.araro.application.story

import com.araro.application.port.ImageGenerationPort
import com.araro.application.port.ImageStoragePort
import com.araro.application.port.StoryRepositoryPort
import com.araro.domain.Story
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Generates AI cover illustrations for stories and stores them.
 */
@Service
class StoryIllustrationService(
    private val storyRepository: StoryRepositoryPort,
    private val imageGeneration: ImageGenerationPort,
    private val imageStorage: ImageStoragePort
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Generate and store a cover image for the story. Idempotent: skips if already has cover.
     */
    fun generateCoverForStory(story: Story): Story? {
        if (!story.coverImageUrl.isNullOrBlank()) {
            log.debug("Story {} already has cover, skipping", story.id)
            return storyRepository.findById(story.id)
        }
        val prompt = buildCoverPrompt(story)
        val imageBytes = imageGeneration.generateImage(prompt) ?: return null
        val path = imageStorage.storeCoverImage(story.id, imageBytes) ?: return null
        storyRepository.updateCoverImage(story.id, path)
        log.info("Generated cover for story {}", story.id)
        return storyRepository.findById(story.id)
    }

    private fun buildCoverPrompt(story: Story): String {
        val title = story.title?.takeIf { it.isNotBlank() } ?: story.theme
        return "Children's story illustration: $title. Child-friendly, colorful, warm."
    }
}
