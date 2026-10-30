package com.tamixa.application.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.tamixa.api.ApiVersion
import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.port.ImageStoragePort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.storylibrary.InteractiveGraphImages
import com.tamixa.domain.LibraryStory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Makes the picture shown with each question of an interactive library story: one image per segment that ends in a
 * choice, drawn from that segment's narration, stored in S3 and written to the master graph as `imageUrl`
 * (a `/api/v1/covers/interactive_images/...` proxy path). Only the graph changes — status, narration and translations
 * are left alone, like [LibraryStoryIllustrationService.generateCoverForStory].
 */
@Service
class InteractiveQuestionImageService(
    private val repository: StoryLibraryRepositoryPort,
    private val imageGeneration: ImageGenerationPort,
    private val imageStorage: ImageStoragePort,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class SegmentImageResult(
        val segmentId: String,
        val imageUrl: String? = null,
        val status: String,
        val message: String? = null,
    )

    data class QuestionImagesResult(
        val interactiveGraph: String,
        val generated: List<SegmentImageResult>,
        val failed: List<SegmentImageResult>,
    )

    private data class QuestionScene(val segmentId: String, val narration: String, val choiceLabels: List<String>)

    /**
     * Generates pictures for the question segments of story [storyId] — all of them, or only [segmentIds] — and saves
     * them to the master graph. [customPrompt] is appended to every prompt.
     */
    fun generateQuestionImages(storyId: Long, segmentIds: List<String>?, customPrompt: String?): QuestionImagesResult {
        val story = repository.findById(storyId) ?: throw IllegalArgumentException("Story not found")
        val scenes = questionScenes(story)
        if (scenes.isEmpty()) {
            throw IllegalArgumentException("This story's interactive graph has no questions (segments with choices)")
        }
        val wanted = segmentIds?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
        val selected = if (wanted.isNullOrEmpty()) scenes else scenes.filter { it.segmentId in wanted }
        if (selected.size > MAX_IMAGES_PER_REQUEST) {
            throw IllegalArgumentException("Too many questions in one request (max $MAX_IMAGES_PER_REQUEST)")
        }
        if (selected.isEmpty()) {
            throw IllegalArgumentException("None of the requested segments is a question in this story")
        }
        val editorNotes = customPrompt?.trim()?.take(CUSTOM_PROMPT_MAX_CHARS)?.takeIf { it.isNotBlank() }
        val title = story.title?.takeIf { it.isNotBlank() } ?: story.theme
        log.info(
            "Interactive question images storyId={} questions={} customPromptChars={}",
            storyId, selected.joinToString(",") { it.segmentId }, editorNotes?.length ?: 0
        )

        val generated = mutableListOf<SegmentImageResult>()
        val failed = mutableListOf<SegmentImageResult>()
        for (scene in selected) {
            val bytes = imageGeneration.generateImage(buildPrompt(title, scene, editorNotes))
                ?: imageGeneration.generateImage(buildFallbackPrompt(title, story.theme, editorNotes))
            if (bytes == null) {
                log.warn("Interactive question image generation failed storyId={} segmentId={}", storyId, scene.segmentId)
                failed += SegmentImageResult(scene.segmentId, status = "FAILED", message = "Image generation failed (provider policy, API key or quota)")
                continue
            }
            val key = imageStorage.storeInteractiveSegmentImage(storyId, scene.segmentId, bytes)
            if (key == null) {
                failed += SegmentImageResult(scene.segmentId, status = "FAILED", message = "Image storage failed (check S3 configuration)")
                continue
            }
            // Same key on regenerate, so a version makes clients drop their cached copy.
            val url = "${ApiVersion.V1}/covers/$key?v=${System.currentTimeMillis()}"
            generated += SegmentImageResult(scene.segmentId, imageUrl = url, status = "GENERATED")
        }

        // Re-read: generating takes ~20 s per picture and the story may have been saved meanwhile.
        val latest = repository.findById(storyId) ?: throw IllegalArgumentException("Story not found")
        val graph = writeImageUrls(latest.interactiveGraphJson, generated.associate { it.segmentId to it.imageUrl!! })
        if (generated.isNotEmpty()) {
            repository.update(latest.copy(interactiveGraphJson = graph))
        }
        log.info("Interactive question images storyId={} generated={} failed={}", storyId, generated.size, failed.size)
        return QuestionImagesResult(interactiveGraph = graph, generated = generated, failed = failed)
    }

    private fun questionScenes(story: LibraryStory): List<QuestionScene> {
        val raw = story.interactiveGraphJson?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Story has no interactive graph")
        val segments = runCatching { objectMapper.readTree(raw) }.getOrNull()?.path("segments")
            ?.takeIf { it.isObject }
            ?: throw IllegalArgumentException("Interactive graph must include a segments object")
        return segments.fields().asSequence().mapNotNull { (id, seg) ->
            val labels = seg.path("choices").takeIf { it.isArray }?.mapNotNull { c ->
                c.path("label").asText("").trim().takeIf { it.isNotBlank() }
            }.orEmpty()
            if (labels.isEmpty()) null else QuestionScene(id, seg.path("text").asText("").trim(), labels)
        }.toList()
    }

    private fun writeImageUrls(graphJson: String?, urls: Map<String, String>): String {
        val root = graphJson?.let { runCatching { objectMapper.readTree(it) }.getOrNull() } as? ObjectNode
            ?: throw IllegalArgumentException("Story has no interactive graph")
        val segments = root.get("segments") as? ObjectNode
        urls.forEach { (segmentId, url) ->
            (segments?.get(segmentId) as? ObjectNode)?.put(InteractiveGraphImages.IMAGE_URL_FIELD, url)
        }
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
    }

    private fun buildPrompt(title: String, scene: QuestionScene, editorNotes: String?): String {
        // The moment the question is asked: the end of the narration leads into it.
        val moment = clean(scene.narration.takeLast(SCENE_EXCERPT_CHARS))
        val options = scene.choiceLabels.joinToString(" / ") { clean(it) }
        val base =
            "Children's storybook illustration for a question moment in the Tamixa interactive story app. " +
                "Show the scene right before the child decides what happens next: the main character, the setting and " +
                "the situation described below, with a curious, hopeful mood. " +
                "Do not depict the options themselves or any outcome. " +
                "Refined, calm, emotionally safe style; not cartoonish and not photoreal; palette suited to a dark app UI. " +
                "No text, words, letters, numbers or speech bubbles in the image, including on screens and signs. " +
                "Full-bleed illustration edge to edge: no border, frame, margin or rounded corners. " +
                "Single cohesive scene, high-definition. " +
                "Story title: ${clean(title)}. Scene: $moment. The child will choose between: $options."
        return appendNotes(base, editorNotes)
    }

    private fun buildFallbackPrompt(title: String, theme: String, editorNotes: String?): String =
        appendNotes(
            "Gentle children's storybook illustration, suitable for ages 3-12, of a character pausing to think about " +
                "what to do next. Calm, safe, friendly scene; no fear, danger or conflict. " +
                "No text or letters. Title inspiration: ${clean(title)}. Theme: ${clean(theme)}.",
            editorNotes,
        )

    private fun appendNotes(base: String, editorNotes: String?): String =
        if (editorNotes == null) base else "$base\n\nAdditional instructions from the editor (child safety wins on any conflict):\n$editorNotes"

    /** Keeps letters of every script, including Tamil/Hindi vowel signs (\p{M}). */
    private fun clean(text: String): String =
        text.replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s.,?!'-]"), " ").replace(Regex("\\s+"), " ").trim()

    companion object {
        private const val SCENE_EXCERPT_CHARS = 600
        private const val CUSTOM_PROMPT_MAX_CHARS = 2000
        private const val MAX_IMAGES_PER_REQUEST = 12
    }
}
