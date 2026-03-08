package com.araro.application.curated

import com.araro.application.port.CoverVideoGenerationPort
import com.araro.application.port.CoverVideoStoragePort
import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.VideoToGifConverterPort
import com.araro.application.port.ImageGenerationPort
import com.araro.application.port.ImageStoragePort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.stream.CoverImageUrlResolver
import com.araro.domain.CuratedStory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Generates AI cover for curated stories: DALL-E image, then optional Sora image-to-video
 * converted to GIF for app. Uses English content when available. Keeps static image as fallback.
 */
@Service
class CuratedStoryIllustrationService(
    private val repository: CuratedStoryRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val imageGeneration: ImageGenerationPort,
    private val imageStorage: ImageStoragePort,
    @Autowired(required = false) private val coverVideoGeneration: CoverVideoGenerationPort?,
    @Autowired(required = false) private val coverVideoStorage: CoverVideoStoragePort?,
    @Autowired(required = false) private val videoToGifConverter: VideoToGifConverterPort?
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Generate and store cover image (DALL-E), then optionally cover video (Sora image-to-video).
     * When force=true, clears existing cover and video then regenerates (for alternate covers).
     * Otherwise idempotent: skips if already has cover.
     */
    fun generateCoverForStory(story: CuratedStory, force: Boolean = false): CuratedStory? {
        log.info("Generating cover for curated story id={} theme={} force={}", story.id, story.theme, force)
        var current = story
        if (force && (!story.coverImageUrl.isNullOrBlank() || !story.coverVideoUrl.isNullOrBlank())) {
            log.info("Force regenerate: deleting old cover and video from storage, then clearing for story {}", story.id)
            val oldImageKey = coverImageUrlResolver.normalizeForStorage(story.coverImageUrl)
            val oldVideoKey = coverImageUrlResolver.normalizeForStorage(story.coverVideoUrl)
            if (!oldImageKey.isNullOrBlank()) {
                imageStorage.deleteCuratedCoverImage(oldImageKey)
            }
            if (!oldVideoKey.isNullOrBlank()) {
                coverVideoStorage?.deleteCuratedCoverVideo(oldVideoKey)
            }
            repository.update(story.copy(coverImageUrl = null, coverVideoUrl = null))
            current = story.copy(coverImageUrl = null, coverVideoUrl = null)
        } else if (!current.coverImageUrl.isNullOrBlank()) {
            log.debug("Curated story {} already has cover, skipping (use force=true for alternate)", story.id)
            return repository.findById(story.id)
        }
        val prompt = buildAnimatedHdPrompt(current)
        log.debug("DALL-E prompt length={} excerpt={}", prompt.length, prompt.take(80))
        val imageBytes = imageGeneration.generateImage(prompt)
        if (imageBytes == null) {
            log.warn("Curated story {} cover generation failed: DALL-E returned null (check OPENAI_API_KEY and API status)", current.id)
            return null
        }
        log.info("DALL-E returned {} bytes for story {}", imageBytes.size, current.id)
        val path = imageStorage.storeCuratedCoverImage(current.id, imageBytes)
        if (path == null) {
            log.warn("Curated story {} cover storage failed: imageStorage.storeCuratedCoverImage returned null (check S3/GCS config)", current.id)
            return null
        }
        var coverAnimationPath: String? = null
        if (coverVideoGeneration != null && coverVideoStorage != null) {
            val motionPrompt = buildCoverVideoMotionPrompt(current)
            log.info("Sora: generating cover animation for story {} (image-to-video, then GIF)", current.id)
            log.debug("Sora motion prompt length={} excerpt={}", motionPrompt.length, motionPrompt.take(80))
            val videoBytes = coverVideoGeneration.generateVideoFromImage(imageBytes, motionPrompt)
            if (videoBytes != null) {
                val gifBytes = videoToGifConverter?.convertMp4ToGif(videoBytes)
                if (gifBytes != null) {
                    coverAnimationPath = coverVideoStorage.storeCuratedCoverVideo(current.id, gifBytes)
                    if (coverAnimationPath != null) {
                        log.info("Stored cover GIF for curated story {} ({} bytes)", current.id, gifBytes.size)
                    } else {
                        log.warn("Curated story {} cover GIF storage failed", current.id)
                    }
                } else {
                    log.warn("Curated story {} MP4-to-GIF conversion failed (install FFmpeg and set SORA_CONVERT_TO_GIF=true for animated cover)", current.id)
                }
            } else {
                log.warn("Curated story {} cover video generation failed (Sora returned null; check SORA_ENABLED and Sora API access)", current.id)
            }
        } else {
            log.debug("Cover animation skipped: Sora or storage not configured (set SORA_ENABLED=true for animated GIF cover)")
        }
        val updated = current.copy(coverImageUrl = path, coverVideoUrl = coverAnimationPath)
        repository.update(updated)
        log.info("Generated cover for curated story {} (image + {})", current.id, if (coverAnimationPath != null) "GIF" else "image only")
        return repository.findById(current.id)
    }

    /**
     * Builds prompt from English content when available; otherwise uses original.
     * Style matches Araro app: premium storybook look, calm/kid-friendly, warm palette that fits
     * the dark-themed app (soft gold, cream, muted tones). No text in image.
     */
    private fun buildAnimatedHdPrompt(story: CuratedStory): String {
        val (title, contentForPrompt) = resolveEnglishContent(story)
        val contentExcerpt = contentForPrompt
            .take(500)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
        val description = if (contentExcerpt.length > 250) {
            contentExcerpt.take(250) + "..."
        } else contentExcerpt
        val styleGuide = (
            "Premium children's storybook illustration, soft digital art style. " +
            "Calm, warm, and child-friendly. " +
            "Color palette: warm and rich (soft gold, cream, muted earth tones, gentle blues); works on dark backgrounds; no harsh neon or bright white. " +
            "No text, words, or letters in the image. " +
            "Single cohesive scene, high-definition, suitable for a bedtime storytelling app."
        )
        return "$styleGuide Scene: $title. $description."
    }

    /**
     * Motion prompt for Sora: locked camera, no zoom/pan; only characters and objects in scene move.
     * Cover video is always played without sound (muted) in app and admin.
     */
    private fun buildCoverVideoMotionPrompt(story: CuratedStory): String {
        val (title, contentForPrompt) = resolveEnglishContent(story)
        val sceneHint = contentForPrompt
            .take(200)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
            .take(100)
        return (
            "CRITICAL: Do not zoom in, zoom out, or pan. The camera must stay completely fixed. No Ken Burns effect. No movement of the frame or background. " +
            "Only the characters, people, and animals inside the scene may move: natural eye blinks, hands and arms gesturing, legs when walking or running, lip sync. " +
            "Ears twitch, tail sways. Horse or vehicle moves in place; do not move the camera. " +
            "The image must never zoom or pan. Story: $title. $sceneHint. Child-friendly, seamless loop, no sound."
        ).take(800)
    }

    private fun resolveEnglishContent(story: CuratedStory): Pair<String, String> {
        val enTranslation = translationRepository.findByMasterStoryIdAndLanguage(story.id, "en")
        return if (enTranslation != null && enTranslation.content.isNotBlank()) {
            val title = enTranslation.title?.takeIf { it.isNotBlank() } ?: story.theme
            title to enTranslation.content
        } else {
            val title = story.title?.takeIf { it.isNotBlank() } ?: story.theme
            title to story.content
        }
    }
}
