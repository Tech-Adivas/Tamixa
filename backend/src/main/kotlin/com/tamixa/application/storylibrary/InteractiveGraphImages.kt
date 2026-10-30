package com.tamixa.application.storylibrary

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Question pictures of an interactive story live in the master graph as `segments.{id}.imageUrl`. The picture has no
 * words in it, so every language shows the same one.
 *
 * - [preserveImageUrls]: an admin save that sends a segment without an `imageUrl` key keeps the stored picture
 *   (the editor may still hold the graph from before the pictures were made). An explicit `""` removes it.
 * - [withMasterImageUrls]: a translation's own graph gets the master's picture for each segment id it shares.
 */
object InteractiveGraphImages {

    const val IMAGE_URL_FIELD = "imageUrl"

    private val mapper = ObjectMapper()

    fun preserveImageUrls(incomingJson: String?, existingJson: String?): String? {
        val incoming = incomingJson?.trim()?.takeIf { it.isNotBlank() } ?: return incomingJson
        val existingImages = imageUrlsBySegment(parse(existingJson))
        if (existingImages.isEmpty()) return incomingJson
        val root = parse(incoming) as? ObjectNode ?: return incomingJson
        val segments = root.get("segments") as? ObjectNode ?: return incomingJson
        var changed = false
        existingImages.forEach { (segmentId, url) ->
            val seg = segments.get(segmentId) as? ObjectNode ?: return@forEach
            if (!seg.has(IMAGE_URL_FIELD)) {
                seg.put(IMAGE_URL_FIELD, url)
                changed = true
            }
        }
        return if (changed) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) else incomingJson
    }

    fun withMasterImageUrls(graph: JsonNode, masterJson: String?): JsonNode {
        val masterImages = imageUrlsBySegment(parse(masterJson))
        if (masterImages.isEmpty()) return graph
        val copy = graph.deepCopy<JsonNode>()
        val segments = copy.get("segments") as? ObjectNode ?: return graph
        masterImages.forEach { (segmentId, url) ->
            val seg = segments.get(segmentId) as? ObjectNode ?: return@forEach
            if (seg.path(IMAGE_URL_FIELD).asText("").isBlank()) seg.put(IMAGE_URL_FIELD, url)
        }
        return copy
    }

    /** Non-blank `imageUrl` per segment id. */
    fun imageUrlsBySegment(graph: JsonNode?): Map<String, String> {
        val segments = graph?.path("segments")?.takeIf { it.isObject } ?: return emptyMap()
        return segments.fields().asSequence()
            .mapNotNull { (id, seg) -> seg.path(IMAGE_URL_FIELD).asText("").trim().takeIf { it.isNotBlank() }?.let { id to it } }
            .toMap()
    }

    private fun parse(raw: String?): JsonNode? =
        raw?.trim()?.takeIf { it.isNotBlank() }?.let { runCatching { mapper.readTree(it) }.getOrNull() }
}
