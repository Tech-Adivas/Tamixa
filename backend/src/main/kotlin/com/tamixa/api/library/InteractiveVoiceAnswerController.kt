package com.tamixa.api.library

import com.tamixa.api.ApiVersion
import com.tamixa.application.library.InteractiveVoiceAnswerService
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.domain.LibraryStoryStatus
import com.tamixa.infrastructure.config.AppProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Voice answers for interactive library stories: the app sends a short recording of the child answering the
 * question at the end of [segmentId]; the response says which answer they chose (or none — try again / tap).
 * The recording is processed in memory and not stored.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/stories/library")
@PreAuthorize("hasRole('PARENT')")
class InteractiveVoiceAnswerController(
    private val storyLibraryService: StoryLibraryService,
    private val voiceAnswerService: InteractiveVoiceAnswerService,
    @Value("\${app.voice-answer.enabled:true}") private val enabled: Boolean,
    @Value("\${app.voice-answer.max-audio-bytes:1048576}") private val maxAudioBytes: Long,
    @Value("\${app.voice-answer.per-parent-per-hour:60}") private val perParentPerHour: Long,
    private val appProperties: AppProperties,
    private val redisTemplate: ObjectProvider<RedisTemplate<String, String>>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Per-account limit; bounds transcription cost if a client misbehaves. Shared across instances through Redis when
     * app.rate-limit.use-redis=true, otherwise (or if Redis fails) per instance in memory.
     */
    private val buckets = ConcurrentHashMap<String, Bucket>()

    data class VoiceAnswerResponse(val choiceId: String?, val heardText: String, val method: String)

    @PostMapping("/{id}/interactive-segments/{segmentId}/voice-answer", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun voiceAnswer(
        @PathVariable id: Long,
        @PathVariable segmentId: String,
        @RequestParam("audio") audio: MultipartFile,
        @RequestParam(defaultValue = "ta") language: String,
    ): ResponseEntity<Any> {
        if (!enabled) return error(HttpStatus.SERVICE_UNAVAILABLE, "Voice answers are turned off")
        val principal = SecurityContextHolder.getContext().authentication?.name
            ?: return error(HttpStatus.UNAUTHORIZED, "Not signed in")
        if (audio.isEmpty) return error(HttpStatus.BAD_REQUEST, "Recording is empty")
        if (audio.size > maxAudioBytes) return error(HttpStatus.PAYLOAD_TOO_LARGE, "Recording is too long")
        val type = audio.contentType.orEmpty().lowercase()
        if (type.isNotBlank() && !type.startsWith("audio/") && type != MediaType.APPLICATION_OCTET_STREAM_VALUE) {
            return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Recording must be audio")
        }
        if (!tryConsume(principal)) {
            log.warn("Voice answer rate limit exceeded storyId={} limit={}/hr", id, perParentPerHour)
            return error(HttpStatus.TOO_MANY_REQUESTS, "Too many voice answers. Please tap an answer.")
        }

        val lang = language.trim().lowercase().take(10)
        val story = storyLibraryService.findByIdAndLanguage(id, lang)
        if (story == null || story.narrationApprovedAt == null || story.status != LibraryStoryStatus.PUBLISHED.name) {
            return error(HttpStatus.NOT_FOUND, "Story not found")
        }
        val segment = story.interactiveGraph?.path("segments")?.path(segmentId.trim())
            ?.takeIf { it.isObject }
            ?: return error(HttpStatus.NOT_FOUND, "Question not found")

        return try {
            val result = voiceAnswerService.answer(
                storyId = id,
                segmentId = segmentId.trim(),
                segment = segment,
                language = lang,
                audio = audio.bytes,
                fileName = audio.originalFilename?.takeIf { it.isNotBlank() } ?: "answer.m4a",
            )
            ResponseEntity.ok(VoiceAnswerResponse(result.choiceId, result.heardText, result.method))
        } catch (e: IllegalArgumentException) {
            error(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request")
        }
    }

    private fun tryConsume(principal: String): Boolean {
        val redis = if (appProperties.rateLimit.useRedis) redisTemplate.getIfAvailable() else null
        if (redis != null) {
            try {
                val hourWindow = System.currentTimeMillis() / 3_600_000L
                val key = "$REDIS_KEY_PREFIX${principal.replace(":", "_").replace(" ", "_").take(120)}:$hourWindow"
                val count = redis.opsForValue().increment(key) ?: 1L
                if (count == 1L) redis.expire(key, Duration.ofSeconds(3900))
                return count <= perParentPerHour
            } catch (e: Exception) {
                // Not fail-open: every answer is a paid transcription, so keep the per-instance limit instead.
                log.warn("Redis voice answer rate limit failed, using in-memory limit: {}", e.message)
            }
        }
        val bucket = buckets.computeIfAbsent(principal) {
            Bucket.builder()
                .addLimit(Bandwidth.classic(perParentPerHour, Refill.greedy(perParentPerHour, Duration.ofHours(1))))
                .build()
        }
        return bucket.tryConsume(1)
    }

    private fun error(status: HttpStatus, message: String): ResponseEntity<Any> =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("message" to message, "status" to status.value()))

    private companion object {
        const val REDIS_KEY_PREFIX = "rl:voice-answer:"
    }
}
