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
 * Generates AI cover illustrations for generated stories: still image (DALL·E or Gemini per config), then optional animated GIF when a
 * [CoverVideoGenerationPort] bean exists (FFmpeg MP4→GIF). Falls back to static-only when image-to-video is absent
 * or fails. Theme: Tamil Nadu / South India, culturally neutral (no religion/caste). HD quality.
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
     * Generate cover: image bytes from [ImageGenerationPort], then try animated GIF first when image-to-video is configured (upload), then static image (upload).
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
        log.debug("Cover image prompt length={} excerpt={}", prompt.length, prompt.take(80))
        val imageBytes = imageGeneration.generateImage(prompt) ?: run {
            val fallbackPrompt = buildPolicySafeFallbackPrompt(current)
            log.warn("Primary cover image generation failed for story {}, retrying with fallback", current.id)
            imageGeneration.generateImage(fallbackPrompt)
        } ?: run {
            log.warn("Story {} cover generation failed (content policy, missing API key, or provider error)", current.id)
            return null
        }

        var coverAnimationPath: String? = null
        if (coverVideoGeneration != null && coverVideoStorage != null) {
            val motionPrompt = buildCoverVideoMotionPrompt(current)
            log.info("Cover animation: generating GIF for story {} before static upload", current.id)
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
                    log.warn("Story {} MP4-to-GIF conversion failed (install FFmpeg; app.cover-animation.convert-mp4-to-gif=true)", current.id)
                }
            } else {
                log.warn("Story {} cover video generation failed (adapter returned null); continuing with static cover only", current.id)
            }
        } else {
            log.debug("Cover animation skipped: image-to-video or storage not configured — generating static cover only")
        }

        val path = imageStorage.storeCoverImage(current.id, imageBytes) ?: run {
            log.warn("Story {} cover image storage failed", current.id)
            if (coverAnimationPath != null) {
                coverVideoStorage?.deleteGeneratedCoverVideo(coverAnimationPath)
            }
            return null
        }

        storyRepository.updateCover(current.id, path, coverAnimationPath)
        log.info("Generated cover for story {} (static + {})", current.id, if (coverAnimationPath != null) "GIF" else "no GIF")
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
