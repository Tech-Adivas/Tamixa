package com.tamixa.application.narration.impl

import com.tamixa.application.narration.EmotionTaggingService
import com.tamixa.domain.narration.EmotionSegment
import com.tamixa.domain.narration.EmotionTag
import com.tamixa.domain.narration.EmotionTaggedScript
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

    /**
     * Detect dialogue spans across common quote styles used in multilingual content.
     * Supports ASCII quotes, curly quotes, and guillemets.
     */
    private val dialogueRegex = Regex("""(?:"[^"]+"|“[^”]+”|‘[^’]+’|«[^»]+»)""")
    private val markerRegex = Regex("""[\[\［]\s*([^\]\］]+)\s*[\]\］]""")

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
        val markerMatches = markerRegex.findAll(para)
        val parts = mutableListOf<EmotionSegment>()
        var forcedEmotion: EmotionTag? = null
        var lastEnd = 0
        for (m in markerMatches) {
            val before = para.substring(lastEnd, m.range.first).trim()
            if (before.isNotBlank()) {
                parts.addAll(splitDialogueAndTag(before, language, forcedEmotion))
            }
            val markerLabel = m.groupValues.getOrNull(1)?.trim().orEmpty()
            val mappedEmotion = markerToEmotion(markerLabel)
            if (mappedEmotion != null) {
                forcedEmotion = mappedEmotion
            }
            lastEnd = m.range.last + 1
        }
        val remainder = para.substring(lastEnd).trim()
        if (remainder.isNotBlank()) {
            parts.addAll(splitDialogueAndTag(remainder, language, forcedEmotion))
        }
        return parts.ifEmpty { listOf(EmotionSegment(para, detectEmotion(para, language))) }
    }

    private fun splitDialogueAndTag(text: String, language: String, forcedEmotion: EmotionTag?): List<EmotionSegment> {
        val matches = dialogueRegex.findAll(text)
        val segments = mutableListOf<EmotionSegment>()
        var start = 0
        for (m in matches) {
            val before = text.substring(start, m.range.first).trim()
            if (before.isNotBlank()) {
                segments.add(EmotionSegment(before, forcedEmotion ?: detectEmotion(before, language)))
            }
            // Keep quoted text explicitly as dialogue unless an emotion marker forces a different style.
            segments.add(EmotionSegment(m.value, forcedEmotion ?: EmotionTag.DIALOGUE))
            start = m.range.last + 1
        }
        val rest = text.substring(start).trim()
        if (rest.isNotBlank()) {
            segments.add(EmotionSegment(rest, forcedEmotion ?: detectEmotion(rest, language)))
        }
        return segments.ifEmpty {
            listOf(EmotionSegment(text, forcedEmotion ?: detectEmotion(text, language)))
        }
    }

    private fun markerToEmotion(markerLabel: String): EmotionTag? {
        if (markerLabel.isBlank()) return null
        val normalized = markerLabel.trim().lowercase()
        if (normalized.matches(Regex("""pause\s*\d+(?:ms|s)"""))) return null
        return when {
            normalized.contains("soft emotional") -> EmotionTag.WHISPER
            normalized.contains("soft voice") || normalized.contains("whisper") -> EmotionTag.WHISPER
            normalized.contains("excited") || normalized.contains("celebration") || normalized.contains("joyful") -> EmotionTag.EXCITED
            normalized.contains("playful") || normalized.contains("curious") || normalized.contains("wonder") ||
                normalized.contains("reassuring") || normalized.contains("thoughtful") || normalized.contains("storyteller") -> EmotionTag.CONVERSATIONAL
            normalized.contains("warm") || normalized.contains("gentle") || normalized.contains("calm") ||
                normalized.contains("scene") || normalized.contains("closing") || normalized.contains("pacing") ||
                normalized.contains("audio imagination") || normalized.contains("clear tone") -> EmotionTag.CALM
            else -> null
        }
    }

    private fun detectEmotion(text: String, language: String = "en"): EmotionTag {
        val lower = text.lowercase()
        val hasExclamation = text.contains('!')
        val hasQuestion = text.contains('?')
        val hasDialoguePunctuation = text.contains('“') || text.contains('”') ||
            text.contains('‘') || text.contains('’') || text.contains('«') || text.contains('»')
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
        if (hasQuestion || lower.contains("imagine") || lower.contains("guess what") ||
            lower.contains("you know what") || lower.contains("can you believe") ||
            lower.contains("what do you think") || lower.contains("can you guess") ||
            lower.contains("can you see") || lower.contains("would you like") ||
            lower.startsWith("so,") || (lower.startsWith("and ") && lower.contains("what")) ||
            lower.contains("oh!") || lower.contains("ah!") || lower.contains("hmm")
        ) {
            return EmotionTag.CONVERSATIONAL
        }
        if (hasDialoguePunctuation) return EmotionTag.DIALOGUE
        // Native language conversational cues (script-specific)—more phrases for natural warmth
        when (language) {
            "ta" -> if (text.contains("அடடா") || text.contains("ஆஹா") || text.contains("ஓ") || text.contains("என்ன") ||
                text.contains("சரி") || text.contains("பாருங்க") || text.contains("சொல்லுங்க") || text.contains("தெரியுமா") ||
                text.contains("அப்படியா") || text.contains("சரி,") || text.contains("ஆமாம்") || text.contains("நல்லா") ||
                text.contains("பாருங்கள்") || text.contains("கேளுங்க")
            ) return EmotionTag.CONVERSATIONAL
            "hi" -> if (text.contains("अरे") || text.contains("वाह") || text.contains("अच्छा") || text.contains("ओहो") ||
                (text.contains("क्या") && text.contains("बात")) || text.contains("सुनो") || text.contains("देखो") ||
                text.contains("समझे") || text.contains("जानते हो") || text.contains("कल्पना करो") || text.contains("पता है")
            ) return EmotionTag.CONVERSATIONAL
            "te" -> if (text.contains("అయ్యో") || text.contains("ఓహో") || text.contains("చూడండి") || text.contains("వినండి") ||
                text.contains("ఏమిటి") || text.contains("తెలుసా") || text.contains("అలాగే") || text.contains("చూడు")
            ) return EmotionTag.CONVERSATIONAL
            "kn" -> if (text.contains("ಅಯ್ಯೋ") || text.contains("ಓಹೋ") || text.contains("ನೋಡಿ") || text.contains("ಕೇಳಿ") ||
                text.contains("ಏನು") || text.contains("ತಿಳಿಯದೆ") || text.contains("ನೋಡು") || text.contains("ಅರಿತೀರಾ")
            ) return EmotionTag.CONVERSATIONAL
            "ml" -> if (text.contains("അയ്യോ") || text.contains("ഓ") || text.contains("കേൾക്കൂ") || text.contains("നോക്കൂ") ||
                text.contains("എന്ത്") || text.contains("അറിയാമോ") || text.contains("കേട്ടോ") || text.contains("നോക്കുക")
            ) return EmotionTag.CONVERSATIONAL
        }
        if (hasExclamation) return EmotionTag.EXCITED
        return EmotionTag.CALM
    }
}
