package com.araro.infrastructure.narration

import com.araro.application.port.narration.TtsClientPort
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.io.InputStream

/**
 * Uses sample MP3 files per language instead of cloud TTS APIs.
 * No API costs; same sample plays for all stories in that language.
 *
 * Place files: resources/tts-samples/ta.mp3, hi.mp3, en.mp3, etc.
 * Or set NARRATION_SAMPLE_TTS_DIR to a file path (e.g. /path/to/samples).
 */
@Component
@ConditionalOnProperty(name = ["app.narration.tts-provider"], havingValue = "sample")
class SampleTtsClientAdapter(
    private val resourceLoader: ResourceLoader,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.sample-tts-dir:classpath:tts-samples}") private val sampleDir: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.sample-tts-fallback:en}") private val fallbackLang: String
) : TtsClientPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = mutableMapOf<String, ByteArray>()

    /** Pipeline uses "kn" for Kannada; some use "ka" for filenames. */
    private val languageFileAliases = mapOf("kn" to listOf("kn", "ka"))

    @PostConstruct
    fun logSampleAvailability() {
        val langCodes = listOf("ta", "hi", "en", "te", "kn", "ml")
        val found = langCodes.mapNotNull { lang ->
            val fileNames = languageFileAliases[lang] ?: listOf(lang)
            for (fn in fileNames) {
                val r = resourceLoader.getResource(if (sampleDir.startsWith("classpath:")) "$sampleDir/$fn.mp3" else "file:${sampleDir.trimEnd('/')}/$fn.mp3")
                if (r.exists()) return@mapNotNull lang to fn
            }
            null
        }
        log.info("Sample TTS: loaded {} samples from {}: {}", found.size, sampleDir, found.joinToString { "${it.first}(${it.second}.mp3)" })
        if (found.isEmpty()) log.warn("Sample TTS: no sample files found. Add ta.mp3, hi.mp3, en.mp3 etc. and run ./gradlew processResources")
    }

    override fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray? {
        val normalized = language.trim().lowercase().take(10).ifEmpty { "ta" }
        val bytes = loadSample(normalized) ?: loadSample(fallbackLang)
        if (bytes != null) {
            log.info("Sample TTS: using {} bytes for lang={}", bytes.size, normalized)
            return bytes
        }
        log.warn("Sample TTS: no sample for lang={} or fallback={}, using placeholder", normalized, fallbackLang)
        return MINIMAL_MP3_PLACEHOLDER.copyOf()
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
            log.debug("Sample TTS: could not load {}: {}", path, e.message)
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
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
    }
}
