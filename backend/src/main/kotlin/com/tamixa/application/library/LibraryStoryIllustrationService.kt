package com.tamixa.application.library

import com.tamixa.application.narration.NarrationTextUtils
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
 * Generates AI cover for library stories: still image (DALL·E or Gemini per config), then optional animated GIF when a
 * [CoverVideoGenerationPort] exists (FFmpeg MP4→GIF). Drawn from the text the editor has on screen when sent, else the
 * saved story (its English translation only when that is not older than the story's last save; see [resolveCoverText]).
 * Static image is always stored when generation succeeds; serves as fallback when GIF is absent.
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
     * When storage keys change between two saves (e.g. submit for review after editing), delete the **previous**
     * S3 objects so stale cover/animation files are not left orphaned.
     */
    fun deleteReplacedCoverAssets(before: LibraryStory, after: LibraryStory) {
        val oldImageKey = coverImageUrlResolver.normalizeForStorage(before.coverImageUrl)
        val newImageKey = coverImageUrlResolver.normalizeForStorage(after.coverImageUrl)
        if (!oldImageKey.isNullOrBlank() && oldImageKey != newImageKey) {
            imageStorage.deleteCuratedCoverImage(oldImageKey)
            log.info("Deleted replaced curated cover image key={} storyId={}", oldImageKey, before.id)
        }
        val oldVideoKey = coverImageUrlResolver.normalizeForStorage(before.coverVideoUrl)
        val newVideoKey = coverImageUrlResolver.normalizeForStorage(after.coverVideoUrl)
        if (!oldVideoKey.isNullOrBlank() && oldVideoKey != newVideoKey) {
            coverVideoStorage?.deleteCuratedCoverVideo(oldVideoKey)
            log.info("Deleted replaced curated cover video key={} storyId={}", oldVideoKey, before.id)
        }
    }

    companion object {
        private const val COVER_CUSTOM_PROMPT_MAX_CHARS = 8000
    }

    /**
     * Generate cover: image bytes from [ImageGenerationPort], try animated GIF upload first, then static image upload.
     * When force=true, clears existing cover and video then regenerates.
     * Otherwise idempotent: skips if already has cover.
     * Optional [customPrompt] is trimmed and capped; appended after the standard cover template (image + fallback + motion hints).
     * Optional [onScreen] is the title/text in the editor (possibly unsaved); the cover is drawn from it instead of the saved story.
     */
    fun generateCoverForStory(
        story: LibraryStory,
        force: Boolean = false,
        customPrompt: String? = null,
        onScreen: CoverText? = null,
    ): LibraryStory? {
        val editorCustom = normalizeCoverCustomPrompt(story.id, customPrompt)
        log.info(
            "Generating cover for library story id={} theme={} force={} customPromptChars={}",
            story.id,
            story.theme,
            force,
            editorCustom?.length ?: 0
        )
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
        val coverText = resolveCoverText(current, onScreen)
        val prompt = appendCoverEditorInstructions(buildAnimatedHdPrompt(coverText), editorCustom)
        log.debug("Cover image primary prompt length={} excerpt={}", prompt.length, prompt.take(80))
        val primaryImageBytes = imageGeneration.generateImage(prompt)
        val imageBytes = if (primaryImageBytes != null) {
            primaryImageBytes
        } else {
            val fallbackPrompt = buildPolicySafeFallbackPrompt(current, coverText, editorCustom)
            log.warn(
                "Primary cover image prompt failed for story {}. Retrying with policy-safe fallback prompt length={}",
                current.id,
                fallbackPrompt.length
            )
            imageGeneration.generateImage(fallbackPrompt)
        }
        if (imageBytes == null) {
            log.warn(
                "Library story {} cover generation failed after primary + fallback prompt (check provider policy, API keys, and status)",
                current.id
            )
            return null
        }
        log.info("Cover image provider returned {} bytes for story {}", imageBytes.size, current.id)
        var coverAnimationPath: String? = null
        if (coverVideoGeneration != null && coverVideoStorage != null) {
            val motionPrompt = buildCoverVideoMotionPrompt(coverText, editorCustom)
            log.info("Cover animation: generating GIF for library story {} before static upload", current.id)
            log.debug("Cover motion prompt length={} excerpt={}", motionPrompt.length, motionPrompt.take(80))
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
                    log.warn("Library story {} MP4-to-GIF conversion failed (install FFmpeg; app.cover-animation.convert-mp4-to-gif=true)", current.id)
                }
            } else {
                log.warn("Library story {} cover video generation failed (adapter returned null); continuing with static cover only", current.id)
            }
        } else {
            log.debug("Cover animation skipped: image-to-video or storage not configured — generating static cover only")
        }
        val path = imageStorage.storeCuratedCoverImage(current.id, imageBytes)
        if (path == null) {
            log.warn("Library story {} cover storage failed: imageStorage.storeCuratedCoverImage returned null (check S3/GCS config)", current.id)
            if (coverAnimationPath != null) {
                coverVideoStorage?.deleteCuratedCoverVideo(coverAnimationPath)
            }
            return null
        }
        val updated = current.copy(coverImageUrl = path, coverVideoUrl = coverAnimationPath)
        repository.update(updated)
        log.info("Generated cover for library story {} (static + {})", current.id, if (coverAnimationPath != null) "GIF" else "no GIF")
        return repository.findById(current.id)
    }

    private fun buildAnimatedHdPrompt(text: CoverText): String {
        val title = text.title
        val contentExcerpt = promptSafe(text.content.take(500))
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

    private fun normalizeCoverCustomPrompt(storyId: Long, customPrompt: String?): String? {
        val raw = customPrompt?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (raw.length > COVER_CUSTOM_PROMPT_MAX_CHARS) {
            log.warn(
                "Cover customPrompt truncated for library story id={} from {} to {} chars",
                storyId,
                raw.length,
                COVER_CUSTOM_PROMPT_MAX_CHARS
            )
        }
        return raw.take(COVER_CUSTOM_PROMPT_MAX_CHARS)
    }

    private fun appendCoverEditorInstructions(base: String, editorCustom: String?): String {
        val trimmed = editorCustom ?: return base
        return buildString {
            append(base)
            append("\n\n")
            append("## Additional instructions from the editor\n")
            append(
                "The following notes were added in the admin UI. Apply them to the cover illustration and, where relevant, to subtle motion, " +
                    "while still honoring all child-safety rules, no text or letters in the image, and the Tamixa storybook style described above. " +
                    "If anything conflicts, prefer child safety.\n\n"
            )
            append(trimmed)
        }
    }

    private fun buildCoverVideoMotionPrompt(text: CoverText, editorCustom: String? = null): String {
        val title = text.title
        val sceneHint = promptSafe(text.content.take(200)).take(100)
        val motionCustom = editorCustom
            ?.replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            ?.trim()
            ?.take(220)
            ?.takeIf { it.isNotBlank() }
        val base = (
            "CRITICAL: Do not zoom in, zoom out, or pan. The camera must stay completely fixed. No Ken Burns effect. No movement of the frame or background. " +
                "Only the characters, people, and animals inside the scene may move: natural eye blinks, hands and arms gesturing, legs when walking or running, lip sync. " +
                "Ears twitch, tail sways. Horse or vehicle moves in place; do not move the camera. " +
                "The image must never zoom or pan. Story: $title. $sceneHint." +
                (if (motionCustom != null) " Editor motion notes: $motionCustom." else "") +
                " Child-friendly, seamless loop, no sound."
            )
        return base.take(800)
    }

    private fun buildPolicySafeFallbackPrompt(story: LibraryStory, text: CoverText, editorCustom: String? = null): String {
        val safeTitle = promptSafe(text.title.take(100))
        val safeTheme = story.theme
            .take(80)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
            .trim()
        val base = (
            "Gentle children's storybook cover illustration, suitable for ages 3-12. " +
                "Use a clean, emotionally safe scene based on the given title and theme; avoid generic defaults. " +
                "No fear, danger, conflict, or violence. " +
                "Use a balanced palette suitable for dark app UI with moderate contrast and soft accents; do not overuse gold tones unless the story explicitly suggests it. " +
                "Single static scene, no text or letters, high-definition. " +
                "Title inspiration: $safeTitle. Theme: $safeTheme."
            )
        val merged = appendCoverEditorInstructions(base, editorCustom)
        val maxLen = if (editorCustom.isNullOrBlank()) 700 else 3200
        return merged.take(maxLen)
    }

    /**
     * What the cover is drawn from. The editor's on-screen text wins (they may not have saved yet). Otherwise the saved
     * story — its English translation only when that translation is not older than the story's last save: a translation
     * made before the story was edited (or never refreshed, e.g. when only Tamil is generated) would draw the old story.
     */
    internal fun resolveCoverText(story: LibraryStory, onScreen: CoverText?): CoverText {
        val fallbackTitle = story.title?.takeIf { it.isNotBlank() } ?: story.theme
        onScreen?.takeIf { it.content.isNotBlank() }?.let { screen ->
            log.info("Cover for library story {} drawn from the editor's on-screen text ({} chars)", story.id, screen.content.length)
            return CoverText(screen.title.ifBlank { fallbackTitle }, screen.content)
        }
        val en = translationRepository.findByMasterStoryIdAndLanguage(story.id, "en")
        val enIsCurrent = en != null && en.content.isNotBlank() && !en.createdAt.isBefore(story.updatedAt)
        return if (en != null && enIsCurrent && story.language.lowercase() != "en") {
            CoverText(en.title?.takeIf { it.isNotBlank() } ?: fallbackTitle, en.content)
        } else {
            if (en != null && !enIsCurrent) {
                log.info("Cover for library story {}: English translation is older than the story, using the story itself", story.id)
            }
            CoverText(fallbackTitle, story.content)
        }
    }

    /** Prompt-safe text: narration markers removed; letters (with Indic vowel signs), digits and basic punctuation kept. */
    private fun promptSafe(text: String): String =
        NarrationTextUtils.stripRemainingMarkers(text)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s.,'-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

/** Title and story text a cover is drawn from. */
data class CoverText(val title: String, val content: String)
