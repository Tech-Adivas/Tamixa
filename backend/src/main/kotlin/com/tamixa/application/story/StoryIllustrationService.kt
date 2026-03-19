package com.tamixa.application.story

import com.tamixa.application.port.CoverVideoGenerationPort
import com.tamixa.application.port.CoverVideoStoragePort
import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.port.ImageStoragePort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.VideoToGifConverterPort
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.domain.Story
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Generates AI cover illustrations for generated stories. Produces static DALL-E image, then
 * optionally animated GIF (Sora image-to-video) when SORA_ENABLED. Theme: Tamil Nadu / South India,
 * culturally neutral (no religion/caste). HD quality.
 */
@Service
class StoryIllustrationService(
    private val storyRepository: StoryRepositoryPort,
    private val imageGeneration: ImageGenerationPort,
    private val imageStorage: ImageStoragePort,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    @Autowired(required = false) private val coverVideoGeneration: CoverVideoGenerationPort?,
    @Autowired(required = false) private val coverVideoStorage: CoverVideoStoragePort?,
    @Autowired(required = false) private val videoToGifConverter: VideoToGifConverterPort?
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Generate and store cover image (DALL-E), then optionally animated GIF (Sora image-to-video).
     * Idempotent: skips if already has cover. When force=true, deletes existing and regenerates.
     */
    fun generateCoverForStory(story: Story, force: Boolean = false): Story? {
        var current = story
        if (force && (!story.coverImageUrl.isNullOrBlank() || !story.coverVideoUrl.isNullOrBlank())) {
            log.info("Force regenerate: deleting old cover image and video for story {}", story.id)
            val oldImageKey = coverImageUrlResolver.normalizeForStorage(story.coverImageUrl)
            val oldVideoKey = coverImageUrlResolver.normalizeForStorage(story.coverVideoUrl)
            if (!oldImageKey.isNullOrBlank()) imageStorage.deleteCoverImage(oldImageKey)
            coverVideoStorage?.deleteGeneratedCoverVideo(oldVideoKey)
            current = story.copy(coverImageUrl = null, coverVideoUrl = null)
        } else if (!current.coverImageUrl.isNullOrBlank()) {
            log.debug("Story {} already has cover, skipping", story.id)
            return storyRepository.findById(story.id)
        }

        val prompt = buildCoverPrompt(current)
        log.debug("DALL-E prompt length={} excerpt={}", prompt.length, prompt.take(80))
        val imageBytes = imageGeneration.generateImage(prompt) ?: run {
            val fallbackPrompt = buildPolicySafeFallbackPrompt(current)
            log.warn("Primary DALL-E failed for story {}, retrying with fallback", current.id)
            imageGeneration.generateImage(fallbackPrompt)
        } ?: run {
            log.warn("Story {} cover generation failed (OpenAI content policy or API)", current.id)
            return null
        }

        val path = imageStorage.storeCoverImage(current.id, imageBytes) ?: run {
            log.warn("Story {} cover storage failed", current.id)
            return null
        }

        var coverAnimationPath: String? = null
        if (coverVideoGeneration != null && coverVideoStorage != null) {
            val motionPrompt = buildCoverVideoMotionPrompt(current)
            log.info("Sora: generating cover animation for story {} (image-to-video, then GIF)", current.id)
            val videoBytes = coverVideoGeneration.generateVideoFromImage(imageBytes, motionPrompt)
            if (videoBytes != null) {
                val gifBytes = videoToGifConverter?.convertMp4ToGif(videoBytes)
                if (gifBytes != null) {
                    coverAnimationPath = coverVideoStorage.storeGeneratedCoverVideo(current.id, gifBytes)
                    if (coverAnimationPath != null) {
                        log.info("Stored cover GIF for story {} ({} bytes)", current.id, gifBytes.size)
                    } else {
                        log.warn("Story {} cover GIF storage failed", current.id)
                    }
                } else {
                    log.warn("Story {} MP4-to-GIF conversion failed (install FFmpeg, SORA_CONVERT_TO_GIF=true)", current.id)
                }
            } else {
                log.warn("Story {} cover video generation failed (Sora returned null)", current.id)
            }
        } else {
            log.debug("Cover animation skipped: Sora or storage not configured (SORA_ENABLED=true for animated GIF)")
        }

        storyRepository.updateCover(current.id, path, coverAnimationPath)
        log.info("Generated cover for story {} (image + {})", current.id, if (coverAnimationPath != null) "GIF" else "image only")
        return storyRepository.findById(current.id)
    }

    private fun buildCoverPrompt(story: Story): String {
        val title = story.title?.takeIf { it.isNotBlank() } ?: story.theme
        val contentExcerpt = story.content
            .take(400)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
            .take(250)
            .let { if (it.length >= 250) "$it..." else it }
        return (
            "Premium children's storybook illustration for Tamixa app. " +
                "Story set in Tamil Nadu / South India: backgrounds, settings, and objects reflect the region – villages, fields, temples as ancient architecture (non-religious), coastal scenery, traditional crafts, local flora and fauna. " +
                "Culturally inclusive and neutral: avoid any religious, caste, or sectarian symbolism. Universal, warm, child-friendly. " +
                "Story-first composition: illustrate a specific moment from the story, not a generic scene. " +
                "Characters, setting, props, and mood must come from: $title. $contentExcerpt. " +
                "Refined, calm, emotionally safe style; not cartoonish and not photoreal. " +
                "Color: palette suited to dark app UI; deep base tones with 1–2 tasteful accents; no harsh neon, no overly bright whites. " +
                "No text, words, or letters in the image. Single cohesive square cover, high-definition."
        )
    }

    private fun buildCoverVideoMotionPrompt(story: Story): String {
        val title = story.title?.takeIf { it.isNotBlank() } ?: story.theme
        val sceneHint = story.content.take(150).replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ").trim().take(80)
        return (
            "CRITICAL: Do not zoom in, zoom out, or pan. Camera stays completely fixed. No Ken Burns effect. " +
                "Only characters, people, and animals may move: natural eye blinks, hand gestures, lip sync. Ears twitch, tail sways. " +
                "Story: $title. $sceneHint. Child-friendly, seamless loop, no sound."
        ).take(800)
    }

    private fun buildPolicySafeFallbackPrompt(story: Story): String {
        val safeTitle = (story.title?.takeIf { it.isNotBlank() } ?: story.theme)
            .take(100)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
        return (
            "Gentle children's storybook cover, Tamil Nadu / South Indian region theme. " +
                "Culturally neutral: no religion, caste, or sectarian symbols. Universal, warm. " +
                "Scene based on: $safeTitle. " +
                "Clean, emotionally safe; no fear, danger, or violence. " +
                "Balanced palette for dark app UI. Single static scene, no text, high-definition."
        ).take(500)
    }
}
