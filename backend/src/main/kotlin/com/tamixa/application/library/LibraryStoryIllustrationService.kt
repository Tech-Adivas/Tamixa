package com.tamixa.application.library

import com.tamixa.application.port.CoverVideoGenerationPort
import com.tamixa.application.port.CoverVideoStoragePort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.VideoToGifConverterPort
import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.port.ImageStoragePort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.domain.LibraryStory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Generates AI cover for library stories: DALL-E image, then optional Sora image-to-video
 * converted to GIF for app. Uses English content when available. Keeps static image as fallback.
 */
@Service
class LibraryStoryIllustrationService(
    private val repository: StoryLibraryRepositoryPort,
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
     * When force=true, clears existing cover and video then regenerates.
     * Otherwise idempotent: skips if already has cover.
     */
    fun generateCoverForStory(story: LibraryStory, force: Boolean = false): LibraryStory? {
        log.info("Generating cover for library story id={} theme={} force={}", story.id, story.theme, force)
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
            log.debug("Library story {} already has cover, skipping (use force=true for alternate)", story.id)
            return repository.findById(story.id)
        }
        val prompt = buildAnimatedHdPrompt(current)
        log.debug("DALL-E primary prompt length={} excerpt={}", prompt.length, prompt.take(80))
        val primaryImageBytes = imageGeneration.generateImage(prompt)
        val imageBytes = if (primaryImageBytes != null) {
            primaryImageBytes
        } else {
            val fallbackPrompt = buildPolicySafeFallbackPrompt(current)
            log.warn(
                "Primary DALL-E prompt failed for story {}. Retrying with policy-safe fallback prompt length={}",
                current.id,
                fallbackPrompt.length
            )
            imageGeneration.generateImage(fallbackPrompt)
        }
        if (imageBytes == null) {
            log.warn(
                "Library story {} cover generation failed after primary + fallback prompt (check OpenAI content policy, OPENAI_API_KEY, and API status)",
                current.id
            )
            return null
        }
        log.info("DALL-E returned {} bytes for story {}", imageBytes.size, current.id)
        val path = imageStorage.storeCuratedCoverImage(current.id, imageBytes)
        if (path == null) {
            log.warn("Library story {} cover storage failed: imageStorage.storeCuratedCoverImage returned null (check S3/GCS config)", current.id)
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
                        log.info("Stored cover GIF for library story {} ({} bytes)", current.id, gifBytes.size)
                    } else {
                        log.warn("Library story {} cover GIF storage failed", current.id)
                    }
                } else {
                    log.warn("Library story {} MP4-to-GIF conversion failed (install FFmpeg and set SORA_CONVERT_TO_GIF=true for animated cover)", current.id)
                }
            } else {
                log.warn("Library story {} cover video generation failed (Sora returned null; check SORA_ENABLED and Sora API access)", current.id)
            }
        } else {
            log.debug("Cover animation skipped: Sora or storage not configured (set SORA_ENABLED=true for animated GIF cover)")
        }
        val updated = current.copy(coverImageUrl = path, coverVideoUrl = coverAnimationPath)
        repository.update(updated)
        log.info("Generated cover for library story {} (image + {})", current.id, if (coverAnimationPath != null) "GIF" else "image only")
        return repository.findById(current.id)
    }

    private fun buildAnimatedHdPrompt(story: LibraryStory): String {
        val (title, contentForPrompt) = resolveEnglishContent(story)
        val contentExcerpt = contentForPrompt
            .take(500)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
        val description = if (contentExcerpt.length > 250) {
            contentExcerpt.take(250) + "..."
        } else contentExcerpt
        val styleGuide = (
            "Premium children's storybook illustration for Tamixa app. " +
                "Story-first composition: illustrate a specific moment from the story, not a generic portrait. " +
                "Characters, setting, props, and mood must come from the provided title and excerpt. " +
                "If location/cultural cues are present in the story, reflect them accurately; otherwise keep the setting neutral and relevant to the scene. " +
                "Refined, calm, emotionally safe style; not cartoonish and not photoreal. " +
                "Color direction: choose a palette that matches the story mood while staying app-theme friendly on dark UI (deep base tones + 1-2 tasteful accents, no harsh neon, no overly bright whites, avoid repeating the same gold-heavy palette across stories). " +
                "No text, words, or letters in the image. Single cohesive square cover, high-definition."
        )
        return "$styleGuide Story title: $title. Story excerpt: $description."
    }

    private fun buildCoverVideoMotionPrompt(story: LibraryStory): String {
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

    private fun buildPolicySafeFallbackPrompt(story: LibraryStory): String {
        val safeTitle = (story.title?.takeIf { it.isNotBlank() } ?: story.theme)
            .take(100)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
        val safeTheme = story.theme
            .take(80)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
        return (
            "Gentle children's storybook cover illustration, suitable for ages 3-12. " +
                "Use a clean, emotionally safe scene based on the given title and theme; avoid generic defaults. " +
                "No fear, danger, conflict, or violence. " +
                "Use a balanced palette suitable for dark app UI with moderate contrast and soft accents; do not overuse gold tones unless the story explicitly suggests it. " +
                "Single static scene, no text or letters, high-definition. " +
                "Title inspiration: $safeTitle. Theme: $safeTheme."
            ).take(700)
    }

    private fun resolveEnglishContent(story: LibraryStory): Pair<String, String> {
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
