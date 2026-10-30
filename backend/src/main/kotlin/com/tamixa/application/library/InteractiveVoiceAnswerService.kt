package com.tamixa.application.library

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.SpeechToTextPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Answers an interactive story question by voice: transcribe the child's recording, then pick the answer they meant —
 * by word match first, the LLM only when that is unclear (own words, "the first one", …).
 *
 * Privacy: the audio lives only in memory for the duration of the call and the transcript is returned to the caller,
 * never stored or logged (logs carry only lengths, the method and whether it matched).
 */
@Service
class InteractiveVoiceAnswerService(
    private val speechToText: SpeechToTextPort,
    private val llm: OpenAIPort,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class VoiceAnswerResult(
        /** Matched answer id, or null: ask the child to try again or tap. */
        val choiceId: String?,
        /** What was heard, shown back to the child. Blank when nothing was understood. */
        val heardText: String,
        /** "text" (word match), "ai" (LLM), or "none". */
        val method: String,
    )

    /**
     * [segment] is the question's node from the graph the app is playing (`text`, `choices`). Throws
     * [IllegalArgumentException] when it has no answers.
     */
    fun answer(storyId: Long, segmentId: String, segment: JsonNode, language: String, audio: ByteArray, fileName: String): VoiceAnswerResult {
        val choices = segment.path("choices").takeIf { it.isArray }?.mapNotNull { c ->
            val id = c.path("id").asText("").trim()
            val label = c.path("label").asText("").trim()
            if (id.isBlank() || label.isBlank()) null else VoiceAnswerMatcher.Choice(id, label)
        }.orEmpty()
        require(choices.isNotEmpty()) { "This part of the story has no question to answer" }

        // No answer labels as a transcription hint: the model copies hint text into the transcript, so the child
        // seems to have said every answer. The LLM match covers words the transcription gets slightly wrong.
        val question = segment.path("text").asText("").takeLast(QUESTION_CONTEXT_CHARS)
        val first = transcribeAndMatch(storyId, segmentId, audio, fileName, language, question, choices)
        if (first.choiceId != null || language.isBlank()) return first
        // Children mix languages: an English answer in a Tamil story comes out as wrong Tamil when the language is
        // forced, so a miss is transcribed once more with the language detected.
        val detected = transcribeAndMatch(storyId, segmentId, audio, fileName, "", question, choices)
        return if (detected.choiceId != null) detected else first.takeIf { it.heardText.isNotBlank() } ?: detected
    }

    private fun transcribeAndMatch(
        storyId: Long,
        segmentId: String,
        audio: ByteArray,
        fileName: String,
        language: String,
        question: String,
        choices: List<VoiceAnswerMatcher.Choice>,
    ): VoiceAnswerResult {
        val lang = language.ifBlank { "auto" }
        val heard = speechToText.transcribe(audio, fileName, language, hint = null)?.trim().orEmpty()
        if (heard.isBlank()) {
            log.info("Voice answer storyId={} segmentId={} lang={} matched=false method=none reason=empty_transcript", storyId, segmentId, lang)
            return VoiceAnswerResult(choiceId = null, heardText = "", method = "none")
        }
        VoiceAnswerMatcher.match(heard, choices)?.let { m ->
            log.info("Voice answer storyId={} segmentId={} lang={} matched=true method=text heardChars={}", storyId, segmentId, lang, heard.length)
            return VoiceAnswerResult(choiceId = m.id, heardText = heard, method = "text")
        }
        val viaLlm = matchWithLlm(heard, question, choices)
        log.info(
            "Voice answer storyId={} segmentId={} lang={} matched={} method={} heardChars={}",
            storyId, segmentId, lang, viaLlm != null, if (viaLlm != null) "ai" else "none", heard.length
        )
        return VoiceAnswerResult(choiceId = viaLlm, heardText = heard, method = if (viaLlm != null) "ai" else "none")
    }

    private fun matchWithLlm(heard: String, questionContext: String, choices: List<VoiceAnswerMatcher.Choice>): String? {
        val system = """
            You match a child's spoken answer to one option of a story question. The child may use their own words,
            a different language, describe the option, or say its position ("the first one", "முதல்", "दूसरा").
            Reply with STRICT JSON only: {"choiceId": "<id>"} for the option they clearly meant, or {"choiceId": null}
            when it is unclear, unrelated, or matches more than one option. Never guess.
        """.trimIndent()
        val user = buildString {
            append("Question (end of the narration): ").append(questionContext.trim()).append('\n')
            append("Options:\n")
            choices.forEachIndexed { i, c -> append("${i + 1}. id=").append(c.id).append(" : ").append(c.label).append('\n') }
            append("Child said: ").append(heard)
        }
        return try {
            val raw = llm.completeChat(system, user, LLM_MAX_TOKENS).trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val id = objectMapper.readTree(raw).path("choiceId").takeUnless { it.isNull || it.isMissingNode }?.asText("")?.trim()
            choices.firstOrNull { it.id == id }?.id
        } catch (e: Exception) {
            log.warn("Voice answer LLM match failed: {}", e.message?.take(200))
            null
        }
    }

    private companion object {
        const val QUESTION_CONTEXT_CHARS = 400
        const val LLM_MAX_TOKENS = 60
    }
}
