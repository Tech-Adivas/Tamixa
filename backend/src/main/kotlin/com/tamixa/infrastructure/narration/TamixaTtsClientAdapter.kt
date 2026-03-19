package com.tamixa.infrastructure.narration

import com.tamixa.application.port.narration.TtsClientPort
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.io.InputStream

/**
 * Uses Tamixa sample MP3 files per language instead of cloud TTS APIs.
 * Does NOT narrate story content: the same pre-recorded sample is returned for every story (same "original voice").
 * For story-specific narration (audio that speaks the actual story text), use NARRATION_TTS_PROVIDER=google (or openai).
 *
 * Enable by setting NARRATION_TTS_PROVIDER=tamixa (or sample).
 * Place files: resources/tts-samples/ta.mp3, hi.mp3, en.mp3, etc.
 * Or set NARRATION_TAMIXA_TTS_DIR to a file path (e.g. /path/to/samples).
 * To use one file for all languages (Tamixa voice): set NARRATION_TAMIXA_TTS_DEFAULT_FILE (e.g. classpath:tts-samples/tamixa.mp3 or file:/path/to/tamixa.mp3).
 */
@Component
@ConditionalOnExpression("#{'\${app.narration.tts-provider:tamixa}' == 'tamixa' or '\${app.narration.tts-provider:tamixa}' == 'sample'}")
class TamixaTtsClientAdapter(
    private val resourceLoader: ResourceLoader,
    @param:Value("\${app.narration.tamixa-tts-dir:classpath:tts-samples}") private val sampleDir: String,
    @param:Value("\${app.narration.tamixa-tts-default-file:}") private val defaultSamplePath: String,
    @param:Value("\${app.narration.tamixa-tts-fallback:en}") private val fallbackLang: String
) : TtsClientPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = mutableMapOf<String, ByteArray>()

    /** Pipeline uses "kn" for Kannada; some use "ka" for filenames. */
    private val languageFileAliases = mapOf("kn" to listOf("kn", "ka"))

    @PostConstruct
    fun logSampleAvailability() {
        log.warn(
            "Tamixa TTS is sample-only: the same pre-recorded audio is used for every story (no story narration). " +
            "For story-specific narration set NARRATION_TTS_PROVIDER=google and GOOGLE_CLOUD_TTS_API_KEY in .env, then restart."
        )
        if (defaultSamplePath.isNotBlank()) {
            val r = resourceLoader.getResource(defaultSamplePath)
            if (r.exists()) log.info("Tamixa TTS: default file set: {} (used for all languages)", defaultSamplePath)
            else log.warn("Tamixa TTS: NARRATION_TAMIXA_TTS_DEFAULT_FILE={} not found", defaultSamplePath)
            return
        }
        val langCodes = listOf("ta", "hi", "en", "te", "kn", "ml")
        val found = langCodes.mapNotNull { lang ->
            val fileNames = languageFileAliases[lang] ?: listOf(lang)
            for (fn in fileNames) {
                val r = resourceLoader.getResource(if (sampleDir.startsWith("classpath:")) "$sampleDir/$fn.mp3" else "file:${sampleDir.trimEnd('/')}/$fn.mp3")
                if (r.exists()) return@mapNotNull lang to fn
            }
            null
        }
        log.info("Tamixa TTS: loaded {} samples from {}: {}", found.size, sampleDir, found.joinToString { "${it.first}(${it.second}.mp3)" })
        if (found.isEmpty()) log.warn("Tamixa TTS: no sample files found. Add ta.mp3, hi.mp3, en.mp3 etc. or set NARRATION_TAMIXA_TTS_DEFAULT_FILE")
    }

    override fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray? {
        val normalized = language.trim().lowercase().take(10).ifEmpty { "ta" }
        val bytes = loadDefaultSample() ?: loadSample(normalized) ?: loadSample(fallbackLang)
        if (bytes != null) {
            log.info("Tamixa TTS: using {} bytes for lang={}", bytes.size, normalized)
            return bytes
        }
        log.warn("Tamixa TTS: no sample for lang={} or fallback={}, using placeholder", normalized, fallbackLang)
        return MINIMAL_MP3_PLACEHOLDER.copyOf()
    }

    /** When NARRATION_TAMIXA_TTS_DEFAULT_FILE is set, use that single file for all languages. */
    private fun loadDefaultSample(): ByteArray? {
        val path = defaultSamplePath.trim()
        if (path.isBlank()) return null
        cache["__default"]?.let { return it }
        return try {
            val resource = resourceLoader.getResource(path)
            if (!resource.exists()) {
                log.debug("Tamixa TTS default file not found: {}", path)
                return null
            }
            resource.inputStream.use { it.readBytes() }.takeIf { it.isNotEmpty() }?.also {
                cache["__default"] = it
                log.info("Tamixa TTS: loaded default sample from {} ({} bytes)", path, it.size)
            }
        } catch (e: Exception) {
            log.debug("Tamixa TTS: could not load default file {}: {}", path, e.message)
            null
        }
    }

    private fun loadSample(language: String): ByteArray? {
        cache[language]?.let { return it }
        val fileNames = languageFileAliases[language] ?: listOf(language)
        for (fileName in fileNames) {
            val bytes = tryLoad("$fileName.mp3", language)
            if (bytes != null) return bytes
        }
        return null
    }

    private fun tryLoad(fileName: String, cacheKey: String): ByteArray? {
        val path = if (sampleDir.startsWith("classpath:")) {
            "$sampleDir/$fileName"
        } else {
            "file:${sampleDir.trimEnd('/')}/$fileName"
        }
        return try {
            val resource: Resource = resourceLoader.getResource(path)
            resource.inputStream.use { stream: InputStream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) {
                    cache[cacheKey] = bytes
                    bytes
                } else null
            }
        } catch (e: Exception) {
            log.debug("Tamixa TTS: could not load {}: {}", path, e.message)
            null
        }
    }

    companion object {
        /** Minimal valid MP3 frame when no sample file exists. Same as SimulatedTtsClientAdapter. */
        private val MINIMAL_MP3_PLACEHOLDER = byteArrayOf(
            -1, -7, -112, 0, 67, -128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
    }
}
