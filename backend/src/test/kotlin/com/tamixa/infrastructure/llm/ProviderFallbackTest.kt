package com.tamixa.infrastructure.llm

import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.StructuredStoryResult
import com.tamixa.application.port.narration.TtsClientPort
import com.tamixa.application.story.ModerationResult
import com.tamixa.infrastructure.narration.FallbackTtsClient
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.env.MockEnvironment
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException

class ProviderFallbackPolicyTest {

    private fun http(status: HttpStatus) =
        HttpClientErrorException.create(status, status.reasonPhrase, HttpHeaders.EMPTY, ByteArray(0), null)

    @Test
    fun `quota, billing, bad key and timeouts all fall back`() {
        assertTrue(ProviderFallbackPolicy.shouldFallBack(http(HttpStatus.TOO_MANY_REQUESTS)))
        assertTrue(ProviderFallbackPolicy.shouldFallBack(http(HttpStatus.FORBIDDEN)))
        assertTrue(ProviderFallbackPolicy.shouldFallBack(http(HttpStatus.UNAUTHORIZED)))
        assertTrue(ProviderFallbackPolicy.shouldFallBack(http(HttpStatus.NOT_FOUND)))
        assertTrue(ProviderFallbackPolicy.shouldFallBack(ResourceAccessException("Read timed out")))
        assertTrue(ProviderFallbackPolicy.shouldFallBack(IllegalStateException("BILLING_DISABLED")))
    }

    @Test
    fun `caller input errors do not fall back, even when wrapped`() {
        assertFalse(ProviderFallbackPolicy.shouldFallBack(IllegalArgumentException("bad input")))
        assertFalse(ProviderFallbackPolicy.shouldFallBack(RuntimeException("wrap", IllegalArgumentException("bad"))))
    }

    @Test
    fun `reason hides api keys`() {
        val r = ProviderFallbackPolicy.reason(IllegalStateException("GET https://x/y?key=AQ.secret123&a=1 sk-proj-abcdEFGHIJK"))
        assertFalse(r.contains("secret123"))
        assertFalse(r.contains("EFGHIJK"))
    }
}

class ProviderFallbackRunnerTest {

    private val registry = SimpleMeterRegistry()
    private val runner = ProviderFallbackRunner("test", "gemini", "openai", registry)

    private fun counter(outcome: String) =
        registry.find("tamixa.ai.provider.fallback").tag("outcome", outcome).counter()?.count() ?: 0.0

    @Test
    fun `primary success never calls backup`() {
        var backupCalls = 0
        val r = runner.run("op", primary = { "a" }, fallback = { backupCalls++; "b" })
        assertEquals("a", r)
        assertEquals(0, backupCalls)
    }

    @Test
    fun `primary failure uses backup and records metric`() {
        val r = runner.run<String>("op", primary = { throw IllegalStateException("403 billing") }, fallback = { "b" })
        assertEquals("b", r)
        assertEquals(1.0, counter("recovered"))
    }

    @Test
    fun `unusable primary result uses backup`() {
        val r = runner.run("op", isUsable = { it.isNotBlank() }, primary = { "  " }, fallback = { "b" })
        assertEquals("b", r)
    }

    @Test
    fun `input error is rethrown without backup`() {
        var backupCalls = 0
        val e = IllegalArgumentException("bad")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            runner.run<String>("op", primary = { throw e }, fallback = { backupCalls++; "b" })
        }
        assertSame(e, thrown)
        assertEquals(0, backupCalls)
    }

    @Test
    fun `both failing throws AllProvidersFailedException with both reasons`() {
        val thrown = assertThrows(AllProvidersFailedException::class.java) {
            runner.run<String>(
                "op",
                primary = { throw IllegalStateException("gemini down") },
                fallback = { throw IllegalStateException("openai down") },
            )
        }
        assertTrue(thrown.message!!.contains("gemini down"))
        assertTrue(thrown.message!!.contains("openai down"))
        assertEquals(1, thrown.suppressed.size)
        assertEquals(1.0, counter("failed"))
    }
}

class FallbackStoryLlmTest {

    private open class FakeLlm(private val text: String?, private val flagged: Boolean = false) : OpenAIPort {
        var calls = 0
        private fun answer(): String { calls++; return text ?: throw IllegalStateException("provider down") }
        override fun generateStory(prompt: String, maxTokens: Int) = answer()
        override fun completeChat(systemPrompt: String, userMessage: String, maxTokens: Int) = answer()
        override fun generateStructuredStory(
            systemPrompt: String, userPrompt: String, fallbackUserPrompt: String?, maxTokens: Int,
        ): StructuredStoryResult = throw UnsupportedOperationException()
        override fun isContentSafe(text: String): Boolean { answer(); return !flagged }
        override fun getModerationResult(text: String): ModerationResult = throw UnsupportedOperationException()
    }

    private fun llm(primary: FakeLlm, backup: FakeLlm) =
        FallbackStoryLlm(primary, backup, ProviderFallbackRunner("story-llm", "gemini", "openai"))

    @Test
    fun `gemini failure is served by openai`() {
        val backup = FakeLlm("story from openai")
        assertEquals("story from openai", llm(FakeLlm(null), backup).generateStory("p", 100))
        assertEquals(1, backup.calls)
    }

    @Test
    fun `blank answer from primary falls back`() {
        assertEquals("ok", llm(FakeLlm(""), FakeLlm("ok")).completeChat("s", "u", 10))
    }

    @Test
    fun `moderation verdict is not retried on backup`() {
        val backup = FakeLlm("x")
        assertFalse(llm(FakeLlm("x", flagged = true), backup).isContentSafe("text"))
        assertEquals(0, backup.calls)
    }
}

class FallbackTtsClientTest {

    private class FakeTts(private val bytes: ByteArray?, private val fail: Boolean = false) : TtsClientPort {
        var calls = 0
        override fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray? {
            calls++
            if (fail) throw IllegalStateException("Google TTS failed: BILLING_DISABLED")
            return bytes
        }
    }

    private fun tts(primary: TtsClientPort, backup: TtsClientPort) =
        FallbackTtsClient(primary, backup, ProviderFallbackRunner("tts", "google", "openai"))

    @Test
    fun `google audio is used when it works`() {
        val backup = FakeTts(byteArrayOf(9))
        assertArrayEquals(byteArrayOf(1, 2), tts(FakeTts(byteArrayOf(1, 2)), backup).synthesizeToMp3("s", "ta", "default"))
        assertEquals(0, backup.calls)
    }

    @Test
    fun `google error falls back to openai`() {
        assertArrayEquals(byteArrayOf(9), tts(FakeTts(null, fail = true), FakeTts(byteArrayOf(9))).synthesizeToMp3("s", "ta", "default"))
    }

    @Test
    fun `google returning no audio falls back to openai`() {
        assertArrayEquals(byteArrayOf(9), tts(FakeTts(null), FakeTts(byteArrayOf(9))).synthesizeToMp3("s", "ta", "default"))
        assertArrayEquals(byteArrayOf(9), tts(FakeTts(ByteArray(0)), FakeTts(byteArrayOf(9))).synthesizeToMp3("s", "ta", "default"))
    }

    @Test
    fun `both engines failing raises a clear error`() {
        assertThrows(AllProvidersFailedException::class.java) {
            tts(FakeTts(null, fail = true), FakeTts(null)).synthesizeToMp3("s", "ta", "default")
        }
    }
}

class ProviderFallbackSettingsTest {

    private fun env(vararg kv: Pair<String, String>) = MockEnvironment().apply { kv.forEach { setProperty(it.first, it.second) } }

    @Test
    fun `llm backup is normalised and ignored when equal to primary or unknown`() {
        assertEquals("openai", ProviderFallbackSettings.llmFallbackProvider(env("app.llm.provider" to "gemini", "app.llm.fallback-provider" to " OpenAI ")))
        assertEquals("none", ProviderFallbackSettings.llmFallbackProvider(env("app.llm.provider" to "openai", "app.llm.fallback-provider" to "openai")))
        assertEquals("none", ProviderFallbackSettings.llmFallbackProvider(env("app.llm.fallback-provider" to "claude")))
        assertEquals("none", ProviderFallbackSettings.llmFallbackProvider(env()))
    }

    @Test
    fun `narration follows the llm backup switch and the legacy flag`() {
        assertTrue(ProviderFallbackSettings.narrationGeminiThenOpenAi(env("app.llm.provider" to "gemini", "app.llm.fallback-provider" to "openai")))
        assertTrue(ProviderFallbackSettings.narrationOpenAiThenGemini(env("app.llm.provider" to "openai", "app.llm.fallback-provider" to "gemini")))
        assertTrue(ProviderFallbackSettings.narrationOpenAiThenGemini(env("app.llm.provider" to "openai", "app.narration.openai-rewrite-fallback-to-gemini" to "true")))
        assertFalse(ProviderFallbackSettings.narrationGeminiThenOpenAi(env("app.llm.provider" to "gemini")))
    }

    @Test
    fun `translation fallback in both directions`() {
        assertTrue(ProviderFallbackSettings.translationOpenAiThenGemini(env("app.translation.provider" to "openai", "app.translation.openai-fallback-to-gemini" to "true")))
        assertTrue(ProviderFallbackSettings.translationGeminiThenOpenAi(env("app.translation.provider" to "gemini", "app.translation.gemini-fallback-to-openai" to "yes")))
        assertFalse(ProviderFallbackSettings.translationGeminiThenOpenAi(env("app.translation.provider" to "openai", "app.translation.gemini-fallback-to-openai" to "true")))
    }

    @Test
    fun `tts fallback only applies to google primary`() {
        assertEquals("openai", ProviderFallbackSettings.ttsFallback(env("app.narration.tts-provider" to "google", "app.narration.tts-fallback" to "openai")))
        assertEquals("none", ProviderFallbackSettings.ttsFallback(env("app.narration.tts-provider" to "openai", "app.narration.tts-fallback" to "openai")))
        assertEquals("none", ProviderFallbackSettings.ttsFallback(env("app.narration.tts-provider" to "google")))
    }
}
