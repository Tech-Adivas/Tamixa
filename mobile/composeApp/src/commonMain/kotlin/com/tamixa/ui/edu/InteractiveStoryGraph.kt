package com.tamixa.ui.edu

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val graphJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
data class InteractiveStoryGraph(
    val startSegmentId: String,
    val segments: Map<String, InteractiveSegment> = emptyMap(),
    val overlayStyle: String? = null,
)

@Serializable
data class InteractiveSegment(
    val audioUrl: String,
    /** Spoken narration for this node; matches segment TTS / what the player should show while audio plays. */
    val text: String = "",
    val choices: List<InteractiveChoice> = emptyList(),
    /** Picture shown with this segment's question (a `/api/v1/covers/interactive_images/...` proxy path). */
    val imageUrl: String? = null,
    /** Question shown in the popup; when absent it is the narration's last question sentence. */
    val question: String? = null,
)

private val sentenceEnd = Regex("""(?<=[.!?।])\s+""")

/** Question asked at the end of this segment, or null when the narration has none. */
fun InteractiveSegment.questionText(): String? {
    question?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    // Only the closing lines: an earlier rhetorical question isn't the one the child answers.
    return text.trim().split(sentenceEnd).takeLast(2).lastOrNull { it.trim().endsWith("?") }?.trim()
}

@Serializable
data class InteractiveChoice(
    val id: String,
    val label: String,
    val nextSegmentId: String,
    val skillDeltas: Map<String, Int>? = null,
)

fun parseInteractiveStoryGraph(obj: JsonObject?): InteractiveStoryGraph? =
    obj?.let {
        runCatching {
            graphJson.decodeFromJsonElement(InteractiveStoryGraph.serializer(), it)
        }.getOrNull()
    }
