package com.tamixa.application.narration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.tamixa.application.narration.EmotionToToneMapper
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.narration.NarrationLLMPort
import com.tamixa.application.storylibrary.StoryLibraryValidation
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InteractiveEpisodeAdminService(
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val ttsService: TTSService,
    private val ssmlBuilder: SSMLBuilderService,
    private val audioStorageService: AudioStorageService,
    private val narrationLLMPort: NarrationLLMPort,
    private val objectMapper: ObjectMapper,
    private val appProperties: AppProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun interactiveCfg() = appProperties.interactiveEpisode

    private fun cappedLlmTokens(): Int = interactiveCfg().llmMaxOutputTokens.coerceIn(256, 32_768)

    private fun requireGraphSizeForLlm(json: String, label: String) {
        val max = interactiveCfg().maxInteractiveGraphCharsForLlm.coerceAtLeast(4_000)
        if (json.length > max) {
            throw IllegalArgumentException("$label is too large for LLM processing (max $max characters)")
        }
    }

    private fun requireStoryTextForLlm(text: String) {
        val max = interactiveCfg().maxStoryCharsForInteractiveLlm.coerceAtLeast(2_000)
        if (text.length > max) {
            throw IllegalArgumentException("Story text is too large for interactive LLM (max $max characters)")
        }
    }

    private fun defaultInteractiveLang(requested: String, storyFallback: String): String {
        val fb = interactiveCfg().defaultLanguageFallback.trim().lowercase().take(10).ifBlank { "ta" }
        return requested.trim().lowercase().take(10).ifBlank {
            storyFallback.trim().lowercase().take(10).ifBlank { fb }
        }.ifBlank { fb }
    }

    data class SegmentAudioInput(
        val segmentId: String,
        val text: String,
        val overwriteExisting: Boolean = false,
    )

    data class SegmentAudioResult(
        val segmentId: String,
        val audioUrl: String? = null,
        val status: String,
        val message: String? = null,
    )

    data class SegmentAudioBatchResult(
        val updatedGraphJson: String,
        val generated: List<SegmentAudioResult>,
        val skipped: List<SegmentAudioResult>,
        val failed: List<SegmentAudioResult>,
    )

    data class SegmentScriptsFillResult(
        val interactiveGraph: String,
        val filledSegmentIds: List<String>,
    )

    fun generateSegmentAudio(
        storyId: Long,
        language: String,
        voiceProfile: String,
        interactiveGraphJson: String,
        segments: List<SegmentAudioInput>,
    ): SegmentAudioBatchResult {
        val story = storyLibraryRepository.findById(storyId)
            ?: throw IllegalArgumentException("Story not found")
        val normalizedGraph = StoryLibraryValidation.normalizeInteractiveGraphJson(interactiveGraphJson)
            ?: throw IllegalArgumentException("Interactive graph is required")
        requireGraphSizeForLlm(normalizedGraph, "Interactive graph")
        StoryLibraryValidation.validateInteractiveGraphThemeAlignment(story.theme, story.category, normalizedGraph)
        val root = objectMapper.readTree(normalizedGraph) as? ObjectNode
            ?: throw IllegalArgumentException("Interactive graph must be a JSON object")
        val segmentsNode = root.path("segments")
        if (!segmentsNode.isObject) {
            throw IllegalArgumentException("Interactive graph must include a non-empty 'segments' object")
        }
        val toneMode = EmotionToToneMapper.toToneMode(story.emotionMode)
        val safeLang = defaultInteractiveLang(language, story.language)
        val safeVoiceProfile = voiceProfile.trim().take(120).ifBlank { "default" }
        val maxBatch = interactiveCfg().maxSegmentBatchSize.coerceIn(1, 200)
        if (segments.size > maxBatch) {
            throw IllegalArgumentException("Too many segments in one request (max $maxBatch)")
        }
        if (segments.isEmpty()) {
            throw IllegalArgumentException("At least one segment is required for audio generation")
        }
        log.info("Interactive segment audio batch storyId={} lang={} segmentCount={}", storyId, safeLang, segments.size)
        val generated = mutableListOf<SegmentAudioResult>()
        val skipped = mutableListOf<SegmentAudioResult>()
        val failed = mutableListOf<SegmentAudioResult>()

        for (input in segments) {
            val sid = input.segmentId.trim()
            if (sid.isBlank()) {
                failed += SegmentAudioResult(segmentId = "", status = "FAILED", message = "segmentId is required")
                continue
            }
            val segmentNode = segmentsNode.path(sid)
            if (!segmentNode.isObject) {
                failed += SegmentAudioResult(segmentId = sid, status = "FAILED", message = "Segment not found in graph")
                continue
            }
            val existingAudioUrl = segmentNode.path("audioUrl").asText("").trim()
            if (existingAudioUrl.isNotBlank() && !input.overwriteExisting) {
                skipped += SegmentAudioResult(segmentId = sid, audioUrl = existingAudioUrl, status = "SKIPPED", message = "audioUrl already present")
                continue
            }
            val rawText = input.text.trim()
            if (rawText.isBlank()) {
                failed += SegmentAudioResult(segmentId = sid, status = "FAILED", message = "Segment script is empty")
                continue
            }
            try {
                val ssml = ssmlBuilder.buildSSML(rawText, safeLang, story.age.coerceIn(1, 21), toneMode)
                val bytes = ttsService.synthesize(ssml, safeLang, safeVoiceProfile)
                    ?: throw IllegalStateException("TTS provider returned empty audio")
                val key = audioStorageService.uploadNarrationAudio(
                    storyId,
                    safeLang,
                    "interactive_${slug(sid)}_${slug(safeVoiceProfile)}",
                    bytes
                )
                // Store the full CDN URL (e.g. https://d1ga398w5nxxhc.cloudfront.net/stories/...)
                // when INTERACTIVE_AUDIO_CDN_BASE_URL is configured, otherwise fall back to the
                // backend audio proxy URL. CloudFront URLs don't expire so they are safe to store in DB.
                val publicUrl = resolvePublicAudioUrl(key)
                (segmentNode as ObjectNode).put("audioUrl", publicUrl)
                generated += SegmentAudioResult(segmentId = sid, audioUrl = publicUrl, status = "GENERATED")
            } catch (e: Exception) {
                log.warn("Interactive segment audio generation failed storyId={} segmentId={} lang={} err={}", storyId, sid, safeLang, e.message)
                failed += SegmentAudioResult(segmentId = sid, status = "FAILED", message = e.message ?: "Failed")
            }
        }
        return SegmentAudioBatchResult(
            updatedGraphJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
            generated = generated,
            skipped = skipped,
            failed = failed,
        )
    }

    /**
     * Builds the public audio URL for a segment storage key.
     * Uses CloudFront CDN base URL when [AppProperties.CdnStreamProperties.interactiveAudioCdnBaseUrl] is set
     * (e.g. https://d1ga398w5nxxhc.cloudfront.net/stories/...).
     * Falls back to the backend audio proxy URL (AUDIO_PUBLIC_BASE_URL/audio/...) for dev/local.
     */
    private fun resolvePublicAudioUrl(storageKey: String): String {
        val cdnBase = appProperties.cdn.interactiveAudioCdnBaseUrl.trim().trimEnd('/')
        return if (cdnBase.isNotBlank()) {
            "$cdnBase/$storageKey"
        } else {
            val base = appProperties.audio.publicBaseUrl.trimEnd('/')
            "$base/audio/$storageKey"
        }
    }

    /**
     * Writes spoken narration into each segment's `text` when it is missing or blank, using the story as context.
     * Does not change graph structure or choice wiring.
     */
    fun fillMissingSegmentNarrationScripts(
        storyId: Long,
        language: String,
        interactiveGraphJson: String,
        storyText: String?,
    ): SegmentScriptsFillResult {
        val story = storyLibraryRepository.findById(storyId)
            ?: throw IllegalArgumentException("Story not found")
        val normalized = StoryLibraryValidation.normalizeInteractiveGraphJson(interactiveGraphJson)
            ?: throw IllegalArgumentException("Interactive graph is required")
        requireGraphSizeForLlm(normalized, "Interactive graph")
        StoryLibraryValidation.validateInteractiveGraphThemeAlignment(story.theme, story.category, normalized)
        validateGraphConnectivity(normalized)
        val sourceText = storyText?.trim().takeUnless { it.isNullOrBlank() } ?: story.content.trim()
        if (sourceText.isBlank()) throw IllegalArgumentException("Story content is empty")
        requireStoryTextForLlm(sourceText)
        val lang = defaultInteractiveLang(language, story.language)
        log.info("Interactive segment script fill storyId={} lang={}", storyId, lang)
        val pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(normalized))
        val (graph, filled) = fillMissingSegmentNarrationScriptsInternal(
            pretty,
            sourceText,
            story.age.coerceIn(1, 21),
            lang,
        )
        return SegmentScriptsFillResult(interactiveGraph = graph, filledSegmentIds = filled)
    }

    fun generateInteractiveGraphFromStory(
        storyId: Long,
        language: String,
        storyText: String?,
    ): String {
        val story = storyLibraryRepository.findById(storyId)
            ?: throw IllegalArgumentException("Story not found")
        val sourceText = storyText?.trim().takeUnless { it.isNullOrBlank() } ?: story.content.trim()
        if (sourceText.isBlank()) throw IllegalArgumentException("Story content is empty")
        requireStoryTextForLlm(sourceText)
        val lang = defaultInteractiveLang(language, story.language)
        log.info("Interactive graph from story LLM storyId={} lang={}", storyId, lang)
        val prompt = """
Create an interactive episode graph in STRICT JSON only.
No markdown, no commentary, no code fences.

Schema:
{
  "startSegmentId": "hook",
  "segments": {
    "hook": { "text": "...", "audioUrl": "", "choices": [ { "id": "c1", "label": "...", "nextSegmentId": "..." } ] },
    "outcome_safe": { "text": "...", "audioUrl": "", "choices": [] },
    "outcome_risky": { "text": "...", "audioUrl": "", "choices": [] }
  }
}

Rules:
- Keep 5-9 segments total.
- Keep language in $lang.
- Preserve child-safe, practical tone for age ${story.age}.
- Include exactly one start segment and at least two branch outcomes.
- Every choice.nextSegmentId must exist in segments.
- Use concise labels for choices.
- Set every audioUrl to an empty string.
- Every segment MUST include non-empty "text": 8-12 sentences of rich, engaging spoken narration for TTS (no stage directions, no markdown).
- Each segment should be 100-150 words to create an immersive, detailed story experience.
- Use vivid descriptions, dialogue, and sensory details to bring the story to life.
- Make each choice point meaningful with clear consequences in the following segments.
""".trimIndent()
        val modelOut = narrationLLMPort.transformWithCustomPrompt(sourceText, prompt, cappedLlmTokens()).formattedText.trim()
        val jsonOnly = stripCodeFence(modelOut)
        val normalized = StoryLibraryValidation.normalizeInteractiveGraphJson(jsonOnly)
            ?: throw IllegalArgumentException("Model did not return valid interactive graph JSON")
        requireGraphSizeForLlm(normalized, "Model interactive graph")
        validateGraphConnectivity(normalized)
        val pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(normalized))
        val (withScripts, _) = fillMissingSegmentNarrationScriptsInternal(
            pretty,
            sourceText,
            story.age.coerceIn(1, 21),
            lang,
        )
        return withScripts
    }


    private fun slug(value: String): String =
        value.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifBlank { "segment" }

    private fun stripCodeFence(raw: String): String {
        var out = raw.trim()
        out = out.removePrefix("```json").removePrefix("```JSON").trim()
        out = out.removePrefix("```").trim()
        out = out.removeSuffix("```").trim()
        return out
    }

    private fun fillMissingSegmentNarrationScriptsInternal(
        graphJson: String,
        sourceText: String,
        age: Int,
        lang: String,
    ): Pair<String, List<String>> {
        val root = objectMapper.readTree(graphJson) as? ObjectNode
            ?: throw IllegalArgumentException("Interactive graph must be a JSON object")
        val segments = root.path("segments") as? ObjectNode
            ?: throw IllegalArgumentException("Interactive graph must include a segments object")
        val missing = mutableListOf<String>()
        segments.fields().forEach { (id, node) ->
            if (node.isObject && node.path("text").asText("").isBlank()) {
                missing.add(id)
            }
        }
        if (missing.isEmpty()) {
            return graphJson to emptyList()
        }
        log.info("Interactive segment scripts: LLM fill for {} segment id(s)", missing.joinToString(", "))
        val graphSnapshot = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
        val prompt = """
You write spoken narration for a child-safe interactive learning episode.

Master story (context for tone and facts):
---
$sourceText
---

Current interactive graph JSON (do not change structure, ids, or choices—only supply missing narration):
$graphSnapshot

These segment ids have blank or missing "text" and need spoken lines for text-to-speech. Audience age about $age. Language/locale code: $lang.

Return STRICT JSON only—no markdown, no code fences. Return one JSON object whose keys are exactly these segment ids:
${missing.joinToString(", ")}

Each value must be a single string: 8-12 sentences (100-150 words) that a narrator would read aloud. Use rich, engaging language with vivid descriptions, dialogue, and sensory details. Make the narration immersive and detailed while remaining child-safe and age-appropriate. No bullet lists, no speaker labels, no *italics*.

Example shape: { "intro": "First line. Second line. Third line...", "outcome_a": "..." }
""".trimIndent()
        val modelOut = narrationLLMPort.transformWithCustomPrompt(sourceText, prompt, cappedLlmTokens()).formattedText.trim()
        val jsonOnly = stripCodeFence(modelOut)
        val mapNode = runCatching { objectMapper.readTree(jsonOnly) }.getOrElse {
            throw IllegalArgumentException("Model did not return valid JSON for segment scripts")
        }
        if (!mapNode.isObject) {
            throw IllegalArgumentException("Segment scripts response must be a JSON object")
        }
        val filled = mutableListOf<String>()
        for (sid in missing) {
            var narration = mapNode.path(sid).asText("").trim()
            if (narration.isBlank()) {
                narration = mapNode.path("segments").path(sid).asText("").trim()
            }
            if (narration.isBlank()) {
                throw IllegalArgumentException("Model returned empty narration for segment: $sid")
            }
            val seg = segments.get(sid) as? ObjectNode
                ?: throw IllegalArgumentException("Segment not found in graph: $sid")
            seg.put("text", narration)
            filled.add(sid)
        }
        val out = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
        return out to filled
    }

    private fun validateGraphConnectivity(graphJson: String) {
        val root = objectMapper.readTree(graphJson)
        val startSegmentId = root.path("startSegmentId").asText("").trim()
        val segments = root.path("segments")
        if (startSegmentId.isBlank() || !segments.isObject || segments.size() == 0) {
            throw IllegalArgumentException("Interactive graph must include startSegmentId and segments")
        }
        if (!segments.has(startSegmentId)) {
            throw IllegalArgumentException("startSegmentId is not present inside segments")
        }
        val ids = segments.fieldNames().asSequence().toSet()
        val missingTargets = mutableListOf<String>()
        segments.fields().forEach { (id, node) ->
            val choices = node.path("choices")
            if (!choices.isArray) return@forEach
            choices.forEach { c: JsonNode ->
                val next = c.path("nextSegmentId").asText("").trim()
                if (next.isNotBlank() && !ids.contains(next)) {
                    missingTargets += "$id->$next"
                }
            }
        }
        if (missingTargets.isNotEmpty()) {
            throw IllegalArgumentException("Invalid graph links: ${missingTargets.joinToString(", ")}")
        }
    }
}
