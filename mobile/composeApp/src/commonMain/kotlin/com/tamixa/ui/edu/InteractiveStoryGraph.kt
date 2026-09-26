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
)

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
