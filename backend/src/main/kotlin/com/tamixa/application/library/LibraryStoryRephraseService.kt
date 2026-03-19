package com.tamixa.application.library

import com.tamixa.application.port.OpenAIPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class RephraseSuggestion(val suggestedTitle: String?, val suggestedContent: String)

/**
 * Suggests rephrased Tamil title and content for library stories using AI.
 */
@Service
class LibraryStoryRephraseService(
    private val openAI: OpenAIPort
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun suggestRephrase(title: String?, content: String): RephraseSuggestion? {
        val effectiveTitle = title?.takeIf { it.isNotBlank() } ?: "Untitled"
        val contentForPrompt = content.take(4000)
        val systemPrompt = """You are a Tamil children's story editor. Suggest alternative phrasing that keeps the same meaning but uses different words. 
Output format exactly:
TITLE:
<rephrased title in Tamil, one line>
CONTENT:
<rephrased story content in Tamil, same structure as input>
Do not add any extra text before TITLE: or after the content. Preserve paragraph breaks."""

        val userPrompt = "Rephrase this Tamil children's story. Keep the same meaning and moral.\n\nTitle: $effectiveTitle\n\nContent:\n$contentForPrompt"
        val response = openAI.completeChat(systemPrompt, userPrompt, maxTokens = 2048)
        if (response.isBlank()) {
            log.warn("Rephrase suggestion failed: empty OpenAI response")
            return null
        }
        return parseRephraseResponse(response)
    }

    private fun parseRephraseResponse(response: String): RephraseSuggestion? {
        val lines = response.lines()
        var title: String? = null
        val contentBuilder = StringBuilder()
        var inContent = false
        for (line in lines) {
            when {
                line.startsWith("TITLE:") -> {
                    title = line.removePrefix("TITLE:").trim()
                }
                line.startsWith("CONTENT:") -> {
                    inContent = true
                    val afterLabel = line.removePrefix("CONTENT:").trim()
                    if (afterLabel.isNotBlank()) contentBuilder.appendLine(afterLabel)
                }
                inContent -> contentBuilder.appendLine(line)
            }
        }
        val suggestedContent = contentBuilder.toString().trim()
        if (suggestedContent.isBlank()) return null
        return RephraseSuggestion(
            suggestedTitle = title?.takeIf { it.isNotBlank() },
            suggestedContent = suggestedContent
        )
    }
}
