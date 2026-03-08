package com.araro.application.narration.impl

import com.araro.application.narration.EmotionTaggingService
import com.araro.domain.narration.EmotionSegment
import com.araro.domain.narration.EmotionTag
import com.araro.domain.narration.EmotionTaggedScript
import org.springframework.stereotype.Service

/**
 * Deterministic emotion tagging: rule-based detection only.
 * No AI; no hallucinated content. Safe for kids' content.
 *
 * Architecture: Why deterministic first? AI-generated labels risk hallucination
 * in kids' content. Rules are predictable, auditable, and zero-cost. Optional
 * AI assist can be added later with strict validation (word count tolerance).
 *
 * Rules:
 * - Quoted text ("...") → DIALOGUE
 * - Exclamation-heavy, "wow", "amazing", "gasp", "laughed", "shouted" → EXCITED
 * - "whisper", "softly", "quiet", "murmured", "mumbled" → WHISPER
 * - "suddenly", "then", "finally", "all at once" → SOFT_SUSPENSE
 * - Questions (?), "imagine", "guess what", "you know what", "can you" → CONVERSATIONAL
 * - Default → CALM
 */
@Service
class EmotionTaggingServiceImpl : EmotionTaggingService {

    override fun tagEmotions(scriptText: String, language: String): EmotionTaggedScript {
        val paragraphs = scriptText.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        val segments = mutableListOf<EmotionSegment>()
        val lang = language.trim().lowercase()
        for (para in paragraphs) {
            segments.addAll(splitAndTagParagraph(para, lang))
        }
        val wordCount = scriptText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        return EmotionTaggedScript(
            originalScript = scriptText,
            segments = segments.ifEmpty { listOf(EmotionSegment(scriptText, EmotionTag.CALM)) },
            wordCount = wordCount
        )
    }

    private fun splitAndTagParagraph(para: String, language: String): List<EmotionSegment> {
        val quoted = Regex(""""[^"]*"""")
        val matches = quoted.findAll(para)
        val parts = mutableListOf<EmotionSegment>()
        var lastEnd = 0
        for (m in matches) {
            val before = para.substring(lastEnd, m.range.first).trim()
            if (before.isNotBlank()) {
                parts.add(EmotionSegment(before, detectEmotion(before, language)))
            }
            parts.add(EmotionSegment(m.value, EmotionTag.DIALOGUE))
            lastEnd = m.range.last + 1
        }
        val remainder = para.substring(lastEnd).trim()
        if (remainder.isNotBlank()) {
            parts.add(EmotionSegment(remainder, detectEmotion(remainder, language)))
        }
        return parts.ifEmpty { listOf(EmotionSegment(para, detectEmotion(para, language))) }
    }

    private fun detectEmotion(text: String, language: String = "en"): EmotionTag {
        val lower = text.lowercase()
        if (lower.contains("whisper") || lower.contains("softly") || lower.contains("quietly") ||
            lower.contains("murmured") || lower.contains("mumbled")
        ) {
            return EmotionTag.WHISPER
        }
        if (lower.contains("suddenly") || lower.contains("finally") || lower.contains("then,") ||
            lower.contains("all at once")
        ) {
            return EmotionTag.SOFT_SUSPENSE
        }
        if (text.count { it == '!' } >= 2 || lower.contains("wow") || lower.contains("amazing") ||
            lower.contains("gasp") || lower.contains("laughed") || lower.contains("shouted") ||
            lower.contains("yelled") || lower.contains("cheered")
        ) {
            return EmotionTag.EXCITED
        }
        // Conversational: questions and engaging phrases → warm, inviting prosody
        // English + transliterated
        if (text.contains("?") || lower.contains("imagine") || lower.contains("guess what") ||
            lower.contains("you know what") || lower.contains("can you believe") ||
            lower.contains("what do you think") || lower.contains("can you guess") ||
            lower.contains("can you see") || lower.contains("would you like") ||
            lower.startsWith("so,") || (lower.startsWith("and ") && lower.contains("what")) ||
            lower.contains("oh!") || lower.contains("ah!") || lower.contains("hmm")
        ) {
            return EmotionTag.CONVERSATIONAL
        }
        // Native language conversational cues (script-specific)
        when (language) {
            "ta" -> if (text.contains("அடடா") || text.contains("ஆஹா") || text.contains("ஓ") || text.contains("என்ன") ||
                text.contains("சரி") || text.contains("பாருங்க") || text.contains("சொல்லுங்க") || text.contains("தெரியுமா")
            ) return EmotionTag.CONVERSATIONAL
            "hi" -> if (text.contains("अरे") || text.contains("वाह") || text.contains("अच्छा") || text.contains("ओहो") ||
                text.contains("सुनो") || text.contains("देखो") || text.contains("क्या") && text.contains("बात")
            ) return EmotionTag.CONVERSATIONAL
            "te" -> if (text.contains("అయ్యో") || text.contains("ఓహో") || text.contains("చూడండి") || text.contains("వినండి") ||
                text.contains("ఏమిటి") || text.contains("తెలుసా")
            ) return EmotionTag.CONVERSATIONAL
            "kn" -> if (text.contains("ಅಯ್ಯೋ") || text.contains("ಓಹೋ") || text.contains("ನೋಡಿ") || text.contains("ಕೇಳಿ") ||
                text.contains("ಏನು") || text.contains("ತಿಳಿಯದೆ")
            ) return EmotionTag.CONVERSATIONAL
            "ml" -> if (text.contains("അയ്യോ") || text.contains("ഓ") || text.contains("കേൾക്കൂ") || text.contains("നോക്കൂ") ||
                text.contains("എന്ത്") || text.contains("അറിയാമോ")
            ) return EmotionTag.CONVERSATIONAL
        }
        return EmotionTag.CALM
    }
}
