package com.tamixa.application.visuals

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.visuals.PlannerMessage
import com.tamixa.application.port.visuals.StoryVisualPlannerLlmPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Plans a story's slideshow with an LLM: a few base pictures (with where each subject sits) and a timeline of camera
 * shots that start at exact words of the story.
 *
 * The LLM reply is validated (start phrases copied exactly and in order, subject regions, picture limit) and sent back
 * with the problems up to [MAX_ATTEMPTS] times. Even spacing is not left to the LLM: long passages without a shot get
 * extra framings of the current picture here, which costs nothing because no new picture is needed.
 */
@Component
class StoryVisualPlanner(
    private val llm: StoryVisualPlannerLlmPort,
    private val objectMapper: ObjectMapper,
    @Value("\${app.story-visuals.max-images:12}") private val maxImagesCap: Int = 12,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val template: String by lazy {
        javaClass.getResourceAsStream("/prompts/story-visual-planner.txt")!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun plan(storyText: String, language: String, durationSeconds: Int): VisualPlan {
        val text = StoryText(storyText)
        if (text.words.size < 20) throw StoryVisualPlanException("Story text is too short to plan a slideshow")
        val targets = Targets.of(text.words.size, durationSeconds, maxImagesCap)
        val messages = mutableListOf(PlannerMessage("user", buildPrompt(storyText, language, durationSeconds, targets)))
        var lastProblems = emptyList<String>()
        repeat(MAX_ATTEMPTS) { attempt ->
            val reply = llm.completeJson(messages) ?: throw StoryVisualPlanException("The planner did not answer")
            val parsed = try {
                objectMapper.readTree(reply)
            } catch (e: Exception) {
                null
            }
            val result = parsed?.let { validate(it, text, targets) }
            val problems = result?.problems ?: listOf("The reply was not valid JSON")
            if (result != null && problems.isEmpty()) {
                val shots = fillGaps(dropCrowded(result.shots, targets.minShotWords), result.images, text, targets.maxGapWords)
                log.info(
                    "Story visuals planned on attempt {}: images={} shots={} (planner {} + filled {})",
                    attempt + 1, result.images.size, shots.size, result.shots.size, shots.size - result.shots.size
                )
                return VisualPlan(result.charactersJson, result.images, shots, text.words.size)
            }
            log.info("Story visual plan attempt {} rejected: {}", attempt + 1, problems.take(5))
            lastProblems = problems
            messages += PlannerMessage("assistant", reply)
            messages += PlannerMessage(
                "user",
                "Fix these problems and return the complete corrected JSON:\n- " + problems.joinToString("\n- ")
            )
        }
        throw StoryVisualPlanException("The plan still had problems after $MAX_ATTEMPTS attempts: ${lastProblems.take(3)}")
    }

    private fun buildPrompt(storyText: String, language: String, durationSeconds: Int, targets: Targets): String =
        template
            .replace("{{LANGUAGE}}", languageName(language))
            .replace("{{DURATION_MINUTES}}", (durationSeconds / 60.0).roundToInt().coerceAtLeast(1).toString())
            .replace("{{TARGET_IMAGES}}", targets.images.toString())
            .replace("{{MAX_IMAGES}}", targets.maxImages.toString())
            .replace("{{MIN_SHOTS}}", targets.minShots.toString())
            .replace("{{MAX_GAP_WORDS}}", targets.maxGapWords.toString())
            .replace("{{SETTING_HINT}}", settingHint(language))
            .replace("{{STORY}}", storyText.trim())

    internal fun validate(root: JsonNode, text: StoryText, targets: Targets): Validation {
        val problems = mutableListOf<String>()
        val images = root.path("images").mapNotNull { node ->
            val key = node.path("image_id").asText("").trim()
            if (key.isEmpty()) return@mapNotNull null
            val subjects = node.path("subjects").map { Subject(it.path("name").asText(""), it.path("region").asText("")) }
            if (subjects.isEmpty() || subjects.any { it.region !in REGIONS }) {
                problems += "$key: every subject needs a region, one of ${REGIONS.sorted()}"
            } else if (subjects.size > 1 && subjects.map { it.region }.distinct().size == 1) {
                problems += "$key: place the subjects in different regions (e.g. one left, one right) so the camera can move between them"
            }
            val prompt = node.path("image_prompt").asText("").trim()
            if (prompt.length < 40) problems += "$key: image_prompt is missing or too short"
            PlannedImage(key.take(20), node.path("story_moment").asText("").ifBlank { null }, prompt, subjects)
        }
        if (images.isEmpty()) problems += "there are no images"
        if (images.size > targets.maxImages) problems += "${images.size} images is more than the maximum of ${targets.maxImages}"
        val keys = images.map { it.imageKey }.toSet()

        val shots = mutableListOf<PlannedShot>()
        var lastWord = -1
        root.path("timeline").forEachIndexed { i, node ->
            val n = i + 1
            val imageKey = node.path("image_id").asText("")
            if (imageKey !in keys) problems += "shot $n uses unknown image \"$imageKey\""
            val focus = node.path("focus").takeIf { !it.isNull && !it.isMissingNode }?.asText()?.ifBlank { null }
            if (focus != null && focus !in REGIONS) problems += "shot $n focus must be a region or null"
            val start = node.path("narration_start").asText("")
            val word = text.findWordIndex(start, lastWord + 1)
            if (word == null) {
                problems += "shot $n narration_start \"${start.take(60)}\" is not copied exactly from the story (or is out of order)"
            } else {
                lastWord = word
                shots += PlannedShot(
                    imageKey = imageKey,
                    narrationStart = start.trim(),
                    startWord = word,
                    camera = node.path("camera").asText("").takeIf { it in CAMERAS } ?: "wide",
                    focus = focus?.takeIf { it in REGIONS },
                    movement = node.path("movement").asText("").takeIf { it in MOVEMENTS } ?: "none",
                )
            }
        }
        if (shots.isEmpty()) problems += "the timeline is empty"
        else if (shots.first().startWord != 0) problems += "the first shot must start at the first words of the story"

        val characters = root.path("characters")
        if (!characters.isArray || characters.isEmpty) problems += "list every character in \"characters\""
        return Validation(problems, images, shots, objectMapper.writeValueAsString(characters))
    }

    /** Drops shots that would follow the previous one too quickly (too busy for a calm story). */
    internal fun dropCrowded(shots: List<PlannedShot>, minShotWords: Int): List<PlannedShot> {
        val kept = mutableListOf<PlannedShot>()
        for (shot in shots) {
            val previous = kept.lastOrNull()
            if (previous == null || shot.startWord - previous.startWord >= minShotWords) kept += shot
        }
        return kept
    }

    /** Adds framings of the current picture wherever the story runs too long without a shot. */
    internal fun fillGaps(shots: List<PlannedShot>, images: List<PlannedImage>, text: StoryText, maxGapWords: Int): List<PlannedShot> {
        val subjectsByImage = images.associate { it.imageKey to it.subjects }
        val result = mutableListOf<PlannedShot>()
        shots.forEachIndexed { i, shot ->
            result += shot
            val end = shots.getOrNull(i + 1)?.startWord ?: text.words.size
            val gap = end - shot.startWord
            if (gap <= (maxGapWords * GAP_TOLERANCE).toInt()) return@forEachIndexed
            val extra = ceil(gap / maxGapWords.toDouble()).toInt() - 1
            val subjects = subjectsByImage[shot.imageKey].orEmpty()
            var previous = shot.startWord
            for (k in 1..extra) {
                val target = shot.startWord + gap * k / (extra + 1)
                val at = text.nearestSentenceStart(target, previous + MIN_SHOT_WORDS, end - MIN_SHOT_WORDS) ?: continue
                result += framing(k, shot, subjects, at, text)
                previous = at
            }
        }
        return result
    }

    /** Alternates close-ups on the picture's subjects with wider framings, each with a gentle movement. */
    private fun framing(k: Int, base: PlannedShot, subjects: List<Subject>, word: Int, text: StoryText): PlannedShot {
        val subject = subjects.getOrNull((k - 1) % subjects.size.coerceAtLeast(1))
        val closeUp = k % 2 == 1 && subject != null
        return base.copy(
            narrationStart = text.phraseAt(word),
            startWord = word,
            camera = if (closeUp) "close_up" else if (base.camera == "wide") "medium" else "wide",
            focus = if (closeUp) subject?.region else null,
            movement = if (closeUp) "slow_zoom_in" else "slow_zoom_out",
        )
    }

    private fun languageName(code: String): String = when (code.lowercase().take(2)) {
        "ta" -> "Tamil"
        "hi" -> "Hindi"
        "te" -> "Telugu"
        "kn" -> "Kannada"
        "ml" -> "Malayalam"
        "en" -> "English"
        else -> code
    }

    private fun settingHint(code: String): String = when (code.lowercase().take(2)) {
        "ta" -> "This is a Tamil story: set it in Tamil Nadu (village, town or coast as the story suggests). Girls wear pavadai-sattai or simple cotton frocks, women wear sarees or churidars, men wear veshti or shirt and trousers; homes, streets and landscapes look like Tamil Nadu."
        "ml" -> "This is a Malayalam story: set it in Kerala. Girls wear pattu pavadai or cotton frocks, women wear sarees or churidars, men wear mundu or shirt and trousers; homes and landscapes look like Kerala."
        "te" -> "This is a Telugu story: set it in Andhra Pradesh or Telangana, with clothing, homes and landscapes typical of the region."
        "kn" -> "This is a Kannada story: set it in Karnataka, with clothing, homes and landscapes typical of the region."
        "hi" -> "This is a Hindi story: set it in North India, with clothing, homes and landscapes typical of the region."
        else -> "Use the setting, clothing and places the story itself describes."
    }

    data class Targets(val images: Int, val maxImages: Int, val minShots: Int, val maxGapWords: Int, val minShotWords: Int) {
        companion object {
            fun of(words: Int, durationSeconds: Int, cap: Int): Targets {
                val seconds = durationSeconds.takeIf { it > 0 } ?: (words / 1.5).toInt()
                val images = (seconds / 60.0).roundToInt().coerceIn(3, cap)
                val wordsPerSecond = words / seconds.toDouble().coerceAtLeast(1.0)
                return Targets(
                    images = images,
                    maxImages = (images + 2).coerceAtMost(cap),
                    minShots = (seconds / 28.0).roundToInt().coerceAtLeast(4),
                    maxGapWords = (wordsPerSecond * 45).roundToInt().coerceAtLeast(20),
                    // ~8 s of narration: a shot shorter than this feels like a flicker.
                    minShotWords = (wordsPerSecond * 8).roundToInt().coerceAtLeast(4),
                )
            }
        }
    }

    internal data class Validation(
        val problems: List<String>,
        val images: List<PlannedImage>,
        val shots: List<PlannedShot>,
        val charactersJson: String,
    )

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val GAP_TOLERANCE = 1.3
        private const val MIN_SHOT_WORDS = 8
        val REGIONS = setOf("left", "center", "right", "top", "bottom", "top_left", "top_right", "bottom_left", "bottom_right")
        val CAMERAS = setOf("wide", "medium", "close_up")
        val MOVEMENTS = setOf("none", "slow_zoom_in", "slow_zoom_out", "pan_left", "pan_right", "pan_up", "pan_down")
    }
}

data class VisualPlan(
    val charactersJson: String,
    val images: List<PlannedImage>,
    val shots: List<PlannedShot>,
    val sourceWordCount: Int,
)

data class PlannedImage(val imageKey: String, val storyMoment: String?, val prompt: String, val subjects: List<Subject>)

data class Subject(val name: String, val region: String)

data class PlannedShot(
    val imageKey: String,
    val narrationStart: String,
    val startWord: Int,
    val camera: String,
    val focus: String?,
    val movement: String,
)

class StoryVisualPlanException(message: String) : RuntimeException(message)

/**
 * The story as whitespace-separated words (the same tokens the narration word timings use), with quote styles
 * normalized so a phrase copied with different quote marks still matches.
 */
class StoryText(raw: String) {
    val words: List<String> = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
    private val normalized: List<String> = words.map { normalizeQuotes(it) }

    /** Index of the first word of [phrase] at or after [fromWord], matching its first words exactly. */
    fun findWordIndex(phrase: String, fromWord: Int): Int? {
        val needle = phrase.split(Regex("\\s+")).filter { it.isNotBlank() }.map { normalizeQuotes(it) }.take(6)
        if (needle.size < 2) return null
        for (i in fromWord.coerceAtLeast(0)..(normalized.size - needle.size)) {
            if (needle.indices.all { normalized[i + it] == needle[it] }) return i
        }
        return null
    }

    /** Word index of the sentence start closest to [target] within [min]..[max], or null if there is none. */
    fun nearestSentenceStart(target: Int, min: Int, max: Int): Int? {
        if (min > max) return null
        return (min..max).filter { isSentenceStart(it) }.minByOrNull { kotlin.math.abs(it - target) }
    }

    fun phraseAt(word: Int): String = words.subList(word, (word + 8).coerceAtMost(words.size)).joinToString(" ")

    private fun isSentenceStart(i: Int): Boolean {
        if (i <= 0 || i >= words.size) return i == 0
        val previous = words[i - 1].trimEnd('"', '\'', '”', '’', ')')
        return previous.endsWith('.') || previous.endsWith('!') || previous.endsWith('?') || previous.endsWith('…')
    }

    private fun normalizeQuotes(word: String): String =
        word.replace('\'', '"').replace('‘', '"').replace('’', '"').replace('“', '"').replace('”', '"')
}
