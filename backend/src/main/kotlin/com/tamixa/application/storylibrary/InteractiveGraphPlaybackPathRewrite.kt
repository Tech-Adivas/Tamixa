package com.tamixa.application.storylibrary

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Rewrites `audioUrl` entries under an interactive graph's `segments` when the API serves a translation that
 * reuses the master's graph JSON: URLs would otherwise keep the master's language directory
 * (e.g. `stories/{id}/ta/...`) while on-screen text is in another locale.
 *
 * Rewrites are applied only when [storageKeyExists] returns true for the **target** storage key
 * (`stories/{id}/{toLang}/...`). Otherwise the original URL is kept so playback does not 404 when
 * locale-specific segment TTS has not been generated yet.
 */
object InteractiveGraphPlaybackPathRewrite {

    /** Extracts `stories/...` key from a full URL, `/audio/stories/...`, or a bare `stories/...` path. */
    fun extractStoriesStorageKey(url: String): String? {
        val u = url.substringBefore('?').trim()
        val ix = u.indexOf("/stories/", ignoreCase = true)
        if (ix >= 0) {
            return u.substring(ix + 1).trimEnd('/')
        }
        if (u.startsWith("stories/", ignoreCase = true)) {
            return u.trimEnd('/')
        }
        return null
    }

    fun rewriteSegmentAudioStorageLanguage(
        graph: JsonNode,
        storyId: Long,
        fromLang: String,
        toLang: String,
        storageKeyExists: (String) -> Boolean = { false },
    ): JsonNode {
        val from = fromLang.trim().lowercase().take(10)
        val to = toLang.trim().lowercase().take(10)
        if (from == to) return graph
        val segments = graph.path("segments")
        if (!segments.isObject) return graph
        val copy: JsonNode = graph.deepCopy()
        val segsObj = copy.get("segments") as? ObjectNode ?: return copy
        val fieldIter = segsObj.fields()
        while (fieldIter.hasNext()) {
            val (_, seg) = fieldIter.next()
            if (!seg.isObject) continue
            val on = seg as ObjectNode
            if (!on.has("audioUrl")) continue
            val url = on.path("audioUrl").asText("").trim()
            if (url.isBlank()) continue
            val rewritten = rewriteStoriesPathLanguageInUrl(url, storyId, from, to)
            if (rewritten == url) continue
            val newKey = extractStoriesStorageKey(rewritten) ?: continue
            if (storageKeyExists(newKey)) {
                on.put("audioUrl", rewritten)
            }
        }
        return copy
    }

    fun rewriteStoriesPathLanguageInUrl(url: String, storyId: Long, from: String, to: String): String {
        val fromNorm = from.trim().lowercase().take(10)
        val toNorm = to.trim().lowercase().take(10)
        if (fromNorm == toNorm) return url
        val sid = storyId.toString()
        val needle = "/stories/$sid/$fromNorm/"
        val regex = Regex(Regex.escape(needle), RegexOption.IGNORE_CASE)
        return if (regex.containsMatchIn(url)) regex.replace(url, "/stories/$sid/$toNorm/") else url
    }
}
