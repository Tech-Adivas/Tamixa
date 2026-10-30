package com.tamixa.application.narration

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.ProxyStoragePort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.AlignedWord
import com.tamixa.application.port.narration.SpeechAlignmentPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationScriptRepositoryPort
import com.tamixa.application.stream.StreamLanguageUtils
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationAudio
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Word-level timings for read-along sync of library narration.
 *
 * Timings come from forced alignment of the finished MP3 against the text it was spoken from (narration script,
 * else translation content) and are stored next to the audio: stories/75/ta/v1.mp3 → stories/75/ta/v1.timings.json.
 * The stored file records the audio's byte size; when the MP3 is regenerated under the same key the size changes,
 * the timings are treated as stale and aligned again.
 *
 * Missing or stale timings are aligned in the background (deduplicated per audio key) and the caller gets null,
 * so the app falls back to its estimate for that one play. Disable with NARRATION_WORD_TIMINGS_ENABLED=false.
 */
@Service
class NarrationWordTimingService(
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val narrationScriptRepository: StoryNarrationScriptRepositoryPort,
    private val objectMapper: ObjectMapper,
    @Autowired(required = false) private val alignment: SpeechAlignmentPort?,
    @Autowired(required = false) private val audioStorage: AudioStorageService?,
    @Autowired(required = false) private val proxyStorage: ProxyStoragePort?,
    @Value("\${app.narration.word-timings-enabled:true}") private val enabled: Boolean = true
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Stored JSON shape; [audioBytes] ties the timings to one exact version of the MP3. */
    data class StoredTimings(
        val audioKey: String = "",
        val audioBytes: Long = 0,
        val words: List<AlignedWord> = emptyList()
    )

    private val cache = ConcurrentHashMap<String, StoredTimings>()
    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val executor = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "narration-word-timings").apply { isDaemon = true }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    private val available: Boolean
        get() = enabled && alignment != null && audioStorage != null && proxyStorage != null

    /**
     * Timings for the library narration actually served at [servedUrl] (the voice's own audio, or the default
     * narration when a cloned voice fell back). Null when there are none yet; alignment is then started.
     */
    fun getLibraryWordTimings(storyId: Long, language: String, voiceProfile: String, servedUrl: String): List<AlignedWord>? {
        if (!available) return null
        return try {
            val audio = resolveServedAudio(storyId, language, voiceProfile, servedUrl) ?: return null
            val audioKey = audio.audioUrl
            val audioBytes = proxyStorage!!.getContentLength(audioKey) ?: return null
            val timingsKey = timingsKeyFor(audioKey)
            cache[timingsKey]?.takeIf { it.audioBytes == audioBytes }?.let { return it.words }
            val stored = readStored(timingsKey)
            if (stored != null && stored.audioBytes == audioBytes && stored.words.isNotEmpty()) {
                cache[timingsKey] = stored
                return stored.words
            }
            log.info(
                "Word timings {} for storyId={} lang={} key={}; aligning in background",
                if (stored == null) "missing" else "stale", storyId, language, audioKey
            )
            scheduleAlignment(audio.translationId, audioKey)
            null
        } catch (e: Exception) {
            log.warn("Word timings lookup failed storyId={} lang={} voice={}: {}", storyId, language, voiceProfile, e.message)
            null
        }
    }

    /** Align [audioKey] in the background unless an alignment for it is already running. */
    fun scheduleAlignment(translationId: Long, audioKey: String) {
        if (!available || !isNarrationMp3Key(audioKey)) return
        if (!inFlight.add(audioKey)) return
        try {
            executor.execute {
                try {
                    alignAndStore(translationId, audioKey)
                } catch (e: Exception) {
                    log.warn("Word timings alignment failed translationId={} key={}: {}", translationId, audioKey, e.message)
                } finally {
                    inFlight.remove(audioKey)
                }
            }
        } catch (e: Exception) {
            inFlight.remove(audioKey)
            log.warn("Word timings alignment not scheduled key={}: {}", audioKey, e.message)
        }
    }

    private fun alignAndStore(translationId: Long, audioKey: String) {
        val translation = translationRepository.findById(translationId) ?: return
        val spokenText = narrationScriptRepository.findByTranslationId(translationId)?.scriptText ?: translation.content
        val transcript = NarrationTextUtils.stripRemainingMarkers(spokenText).trim()
        if (transcript.isBlank()) {
            log.warn("Word timings skipped: no transcript translationId={} key={}", translationId, audioKey)
            return
        }
        val audio = proxyStorage!!.getObject(audioKey) ?: run {
            log.warn("Word timings skipped: audio not found key={}", audioKey)
            return
        }
        val words = alignment!!.alignWords(audio, transcript)?.takeIf { it.isNotEmpty() } ?: return
        val stored = StoredTimings(audioKey, audio.size.toLong(), words)
        val timingsKey = timingsKeyFor(audioKey)
        if (audioStorage!!.putNarrationObject(timingsKey, objectMapper.writeValueAsBytes(stored), "application/json")) {
            if (cache.size >= MAX_CACHE_ENTRIES) cache.clear()
            cache[timingsKey] = stored
            log.info("Word timings stored key={} words={} lastWordEnd={}s", timingsKey, words.size, words.last().endSec)
        }
    }

    private fun resolveServedAudio(storyId: Long, language: String, voiceProfile: String, servedUrl: String): StoryNarrationAudio? {
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, StreamLanguageUtils.normalize(language))
            ?: return null
        return listOf(voiceProfile.ifBlank { DEFAULT_VOICE }, DEFAULT_VOICE).distinct().firstNotNullOfOrNull { profile ->
            narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, profile)
                ?.takeIf {
                    it.status == NarrationAudioStatus.READY && isNarrationMp3Key(it.audioUrl) &&
                        // Only the file that is actually being played (guards against family/generated audio).
                        servedUrl.contains(it.audioUrl)
                }
        }
    }

    private fun readStored(timingsKey: String): StoredTimings? {
        val bytes = proxyStorage!!.getObject(timingsKey) ?: return null
        return try {
            objectMapper.readValue(bytes, StoredTimings::class.java)
        } catch (e: Exception) {
            log.warn("Word timings file unreadable key={}: {}", timingsKey, e.message)
            null
        }
    }

    companion object {
        private const val DEFAULT_VOICE = "default"
        private const val MAX_CACHE_ENTRIES = 500

        fun timingsKeyFor(audioKey: String): String = audioKey.removeSuffix(".mp3") + ".timings.json"

        private fun isNarrationMp3Key(key: String) = key.startsWith("stories/") && key.endsWith(".mp3")
    }
}
