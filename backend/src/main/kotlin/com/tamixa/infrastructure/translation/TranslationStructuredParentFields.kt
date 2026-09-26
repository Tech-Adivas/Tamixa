package com.tamixa.infrastructure.translation

import com.fasterxml.jackson.databind.JsonNode

/**
 * Parses optional parent-facing fields from translation model JSON (OpenAI / Gemini structured translate).
 */
internal data class ParsedParentFacingFields(
    val parentContentNote: String?,
    val parentDiscussionPrompts: List<String>?,
    val speakAlongPrompt: String?,
)

internal fun hasParentSourceForTranslation(
    parentContentNote: String?,
    parentDiscussionPrompts: List<String>?,
    speakAlongPrompt: String?,
): Boolean {
    if (!parentContentNote.isNullOrBlank()) return true
    if (!speakAlongPrompt.isNullOrBlank()) return true
    return !parentDiscussionPrompts.isNullOrEmpty() &&
        parentDiscussionPrompts.any { !it.isNullOrBlank() }
}

internal fun parentFacingKeysSystemPromptBlock(tgtName: String): String = """
    When the user message includes PARENT_CONTENT_NOTE, PARENT_DISCUSSION_PROMPTS, and/or SPEAK_ALONG, also return JSON keys:
      parent_content_note (string or null),
      parent_discussion_prompts (JSON array of strings, or null),
      speak_along_prompt (string or null).
    Translate those fields from the source language into native $tgtName for caregivers (parents). Keep the same intent: gentle transparency, short speak-along invitation, and brief dinner-table prompts. Do not add new facts. If a source field was omitted or empty, return null for that key.
""".trimIndent()

internal fun buildParentFacingUserAppendix(
    parentContentNote: String?,
    parentDiscussionPrompts: List<String>?,
    speakAlongPrompt: String?,
): String? {
    if (!hasParentSourceForTranslation(parentContentNote, parentDiscussionPrompts, speakAlongPrompt)) return null
    val parts = mutableListOf<String>()
    parentContentNote?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add("PARENT_CONTENT_NOTE:\n$it") }
    if (!parentDiscussionPrompts.isNullOrEmpty()) {
        val lines = parentDiscussionPrompts.mapIndexed { i, p -> "${i + 1}. ${p.trim()}" }.joinToString("\n")
        if (lines.isNotBlank()) parts.add("PARENT_DISCUSSION_PROMPTS:\n$lines")
    }
    speakAlongPrompt?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add("SPEAK_ALONG:\n$it") }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
}

internal fun jsonTextFieldInsensitive(node: JsonNode, vararg keys: String): String? {
    if (!node.isObject) return null
    val wanted = keys.map { it.lowercase() }.toSet()
    val it = node.fields()
    while (it.hasNext()) {
        val e = it.next()
        if (e.key.lowercase() in wanted) {
            val v = e.value ?: continue
            if (v.isNull) return null
            return v.asText()?.trim()?.takeIf { it.isNotBlank() }
        }
    }
    return null
}

internal fun jsonStringArrayFieldInsensitive(node: JsonNode, vararg keys: String): List<String>? {
    if (!node.isObject) return null
    val wanted = keys.map { it.lowercase() }.toSet()
    val it = node.fields()
    while (it.hasNext()) {
        val e = it.next()
        if (e.key.lowercase() !in wanted) continue
        val v = e.value ?: continue
        if (v.isNull) return null
        if (!v.isArray) return null
        val out = v.mapNotNull { el ->
            if (el.isNull || !el.isTextual) null else el.asText().trim().takeIf { it.isNotBlank() }
        }
        return out.takeIf { it.isNotEmpty() }
    }
    return null
}

internal fun parseParentFacingFromTranslationJson(node: JsonNode): ParsedParentFacingFields {
    val note = sanitizeParentFacingText(
        jsonTextFieldInsensitive(node, "parent_content_note", "parentContentNote")
    )
    val speak = sanitizeParentFacingText(
        jsonTextFieldInsensitive(node, "speak_along_prompt", "speakAlongPrompt", "speak_along")
    )
    val prompts = jsonStringArrayFieldInsensitive(node, "parent_discussion_prompts", "parentDiscussionPrompts")
    return ParsedParentFacingFields(
        parentContentNote = note,
        parentDiscussionPrompts = prompts,
        speakAlongPrompt = speak,
    )
}

internal fun mergeParentFacingWithSourceTarget(
    parsed: ParsedParentFacingFields,
    sourceLang: String,
    targetLang: String,
    originalNote: String?,
    originalPrompts: List<String>?,
    originalSpeak: String?,
): ParsedParentFacingFields {
    val src = sourceLang.trim().lowercase()
    val tgt = targetLang.trim().lowercase()
    if (src != tgt) return parsed
    return ParsedParentFacingFields(
        parentContentNote = parsed.parentContentNote ?: originalNote?.trim()?.takeIf { it.isNotBlank() },
        parentDiscussionPrompts = parsed.parentDiscussionPrompts
            ?: originalPrompts?.map { it.trim() }?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
        speakAlongPrompt = parsed.speakAlongPrompt ?: originalSpeak?.trim()?.takeIf { it.isNotBlank() },
    )
}

private fun sanitizeParentFacingText(text: String?): String? {
    if (text.isNullOrBlank()) return null
    var t = text.trim()
    t = Regex(
        "^\\s*\"?(parent_content_note|parentContentNote|speak_along_prompt|speakAlongPrompt)\"?:?\\s*[\"']?",
        RegexOption.IGNORE_CASE
    ).replace(t, "")
    t = Regex("^[\"',;:\\s]+|[\"',;:\\s]+$").replace(t, "")
    return t.trim().takeIf { it.isNotBlank() }
}
