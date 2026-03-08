package com.araro.network

import com.araro.util.AraroConstants
import com.araro.util.AraroLog
import com.araro.domain.CuratedStoryResponse
import com.araro.domain.toStory
import com.araro.domain.GenerateStoryRequest
import com.araro.domain.StoriesPageResponse
import com.araro.domain.Story
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

/** Extended timeout for cloned voice (on-demand TTS can take 60–90s). */
private const val CLONED_VOICE_REQUEST_TIMEOUT_MS = 120_000L

/** Voice option from GET /stories/{id}/voices */
@kotlinx.serialization.Serializable
data class VoiceOptionDto(val voiceProfile: String, val isPremium: Boolean)

@kotlinx.serialization.Serializable
internal data class VoicesResponseDto(val voices: List<VoiceOptionDto>)

class StoryApi(private val client: HttpClient) {
    suspend fun generate(request: GenerateStoryRequest): Story =
        client.post("${ApiConfig.API_VERSION}/stories/generate") {
            setBody(request)
        }.body()

    /** Fetch single generated story by id (parent's own story). */
    suspend fun getGeneratedStoryById(id: Long): com.araro.domain.Story? =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$id")
            if (resp.status.value in 200..299) resp.body<com.araro.domain.StoryApiResponse>().toStory()
            else null
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getGeneratedStoryById(id=$id) failed", e)
            null
        }

    /** Fetch parent's AI-generated stories from server (paginated). */
    suspend fun getMyStories(page: Int = 0, size: Int = AraroConstants.MY_STORIES_PAGE_SIZE): StoriesPageResponse =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories") {
                parameter("page", page)
                parameter("size", size)
            }
            if (resp.status.value in 200..299) resp.body()
            else StoriesPageResponse(content = emptyList(), totalElements = 0, totalPages = 0, first = true, last = true)
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getMyStories failed", e)
            StoriesPageResponse(content = emptyList(), totalElements = 0, totalPages = 0, first = true, last = true)
        }

    suspend fun getCuratedStories(language: String = AraroConstants.DEFAULT_LANGUAGE): List<CuratedStoryResponse> =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/curated") {
                parameter("language", language)
                parameter("size", AraroConstants.CURATED_PAGE_SIZE)
            }
            val body = if (resp.status.value in 200..299) resp.body<List<CuratedStoryResponse>>()
            else {
                AraroLog.w("StoryApi", "getCuratedStories status=${resp.status.value}")
                emptyList()
            }
            body
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getCuratedStories failed", e)
            emptyList()
        }

    suspend fun getCuratedStoryById(id: Long, language: String = AraroConstants.DEFAULT_LANGUAGE): CuratedStoryResponse? {
        return try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/curated/$id") {
                parameter("language", language)
            }
            if (resp.status.value !in 200..299) null else resp.body()
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getCuratedStoryById(id=$id) failed", e)
            null
        }
    }

    suspend fun getFavorites(): List<FavoriteStoryDto> =
        client.get("${ApiConfig.API_VERSION}/favorites").body()

    suspend fun addFavorite(storyId: Long, storySource: String = AraroConstants.STORY_SOURCE_GENERATED) {
        client.post("${ApiConfig.API_VERSION}/favorites/$storyId?storySource=$storySource") { }
    }

    suspend fun removeFavorite(storyId: Long) {
        client.delete("${ApiConfig.API_VERSION}/favorites/$storyId")
    }

    suspend fun isFavorite(storyId: Long): Boolean =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/favorites/$storyId/check")
            val body = resp.body<FavoriteCheckDto>()
            body.isFavorite
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "isFavorite(storyId=$storyId) failed", e)
            false
        }

    /**
     * Upload family voice recording for a story.
     * @param audioBytes MP3 or AAC audio bytes
     * @param isAac true if AAC/MP4 from in-app recorder
     * @return true on success, false on failure
     */
    suspend fun uploadFamilyVoice(storyId: Long, language: String = AraroConstants.DEFAULT_LANGUAGE, audioBytes: ByteArray, isAac: Boolean = false): Boolean =
        try {
            val (contentType, filename) = if (isAac) "audio/mp4" to "voice.m4a" else "audio/mpeg" to "voice.mp3"
            val resp = client.post("${ApiConfig.API_VERSION}/stories/$storyId/upload-family-voice") {
                parameter("language", language)
                setBody(MultiPartFormDataContent(formData {
                    append("file", audioBytes, Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }))
            }
            resp.status.value in 200..299
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "uploadFamilyVoice(storyId=$storyId) failed", e)
            false
        }

    /**
     * Delete family voice recording for a story.
     * @return true on success, false on failure
     */
    suspend fun deleteFamilyVoice(storyId: Long, language: String = AraroConstants.DEFAULT_LANGUAGE): Boolean =
        try {
            val resp = client.delete("${ApiConfig.API_VERSION}/stories/$storyId/delete-family-voice") {
                parameter("language", language)
            }
            resp.status.value in 200..299
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "deleteFamilyVoice(storyId=$storyId) failed", e)
            false
        }

    /** Fetch available voices for a story (default, calm, etc.) with premium flags. */
    suspend fun getAvailableVoices(storyId: Long, language: String = AraroConstants.DEFAULT_LANGUAGE): List<VoiceOptionDto> =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$storyId/voices") {
                parameter("language", language)
            }
            if (resp.status.value in 200..299) resp.body<VoicesResponseDto>().voices
            else {
                AraroLog.w("StoryApi", "getAvailableVoices(storyId=$storyId) status=${resp.status.value}")
                emptyList()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            emptyList()
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getAvailableVoices(storyId=$storyId) failed", e)
            emptyList()
        }

    /**
     * Fetch short-lived signed URL for audio streaming (CDN mode).
     * @param voiceProfile Optional; e.g. "calm" for premium, "cloned:123" for parent's voice. null = default.
     * @return StreamUrlResult.Url on success, StreamUrlResult.UpgradeRequired on 402, null on 404
     */
    suspend fun getStreamUrl(storyId: Long, language: String = AraroConstants.DEFAULT_LANGUAGE, voiceProfile: String? = null, storySource: String? = null): StreamUrlResult =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$storyId/stream-url") {
                parameter("language", language)
                voiceProfile?.takeIf { it != AraroConstants.VOICE_PROFILE_DEFAULT }?.let { parameter("voiceProfile", it) }
                storySource?.takeIf { it in listOf("curated", "generated") }?.let { parameter("storySource", it) }
                if (voiceProfile?.startsWith("cloned:") == true) {
                    timeout { requestTimeoutMillis = CLONED_VOICE_REQUEST_TIMEOUT_MS }
                }
            }
            when (resp.status.value) {
                402 -> StreamUrlResult.UpgradeRequired
                in 200..299 -> {
                    val body = resp.body<StreamUrlResponse>()
                    StreamUrlResult.Url(body.streamUrl, body.avatarUrl, body.avatarVideoUrl, body.wordTimings)
                }
                else -> StreamUrlResult.NotFound
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            StreamUrlResult.NotFound
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getStreamUrl(storyId=$storyId, voice=$voiceProfile) failed", e)
            StreamUrlResult.NotFound
        }

    sealed class StreamUrlResult {
        data class Url(
            val url: String,
            val avatarUrl: String? = null,
            val avatarVideoUrl: String? = null,
            val wordTimings: List<WordTiming>? = null
        ) : StreamUrlResult()
        data object UpgradeRequired : StreamUrlResult()
        data object NotFound : StreamUrlResult()
    }

    /**
     * Fetch conversational (rewritten) narration script for TTS fallback.
     * When backend audio stream fails, mobile uses device TTS—this script sounds warm instead of dry.
     * @return Script text or null on failure
     */
    suspend fun getNarrationScript(storyId: Long, language: String = AraroConstants.DEFAULT_LANGUAGE): String? =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$storyId/narration-script") {
                parameter("language", language)
            }
            if (resp.status.value in 200..299) resp.body<NarrationScriptResponse>().script
            else null
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getNarrationScript(storyId=$storyId) failed", e)
            null
        }

    /** Generate AI cover illustration for a story. */
    suspend fun generateCover(storyId: Long): Story =
        client.post("${ApiConfig.API_VERSION}/stories/$storyId/generate-cover").body()

    /** Remix a story with a tweak (e.g. "make the dragon friendly"). */
    suspend fun remix(storyId: Long, remixInstruction: String): Story =
        client.post("${ApiConfig.API_VERSION}/stories/$storyId/remix") {
            setBody(RemixRequest(remixInstruction))
        }.body()

    suspend fun getSoundscapes(): List<SoundscapeDto> =
        client.get("${ApiConfig.API_VERSION}/soundscapes").body()

    suspend fun getRecommended(childId: Long? = null, language: String = AraroConstants.DEFAULT_LANGUAGE, limit: Int = AraroConstants.RECOMMENDED_LIMIT): List<RecommendedStoryDto> =
        client.get("${ApiConfig.API_VERSION}/stories/recommended") {
            parameter("language", language)
            parameter("limit", limit)
            childId?.let { parameter("childId", it) }
        }.body()

    suspend fun savePlaybackPosition(storyId: Long, storySource: String, positionSeconds: Int, childId: Long? = null) {
        client.post("${ApiConfig.API_VERSION}/playback/position") {
            setBody(buildMap<String, Any> {
                put("storyId", storyId)
                put("storySource", storySource)
                put("positionSeconds", positionSeconds)
                childId?.let { put("childId", it) }
            })
        }
    }

    suspend fun getPlaybackPosition(storyId: Long, storySource: String = AraroConstants.STORY_SOURCE_GENERATED): Int =
        try {
            client.get("${ApiConfig.API_VERSION}/playback/position") {
                parameter("storyId", storyId)
                parameter("storySource", storySource)
            }.body<PlaybackPositionResponse>().positionSeconds
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "getPlaybackPosition(storyId=$storyId) failed", e)
            0
        }

    suspend fun getRecentPlayback(limit: Int = AraroConstants.RECENT_PLAYBACK_LIMIT): List<PlaybackPositionDto> =
        client.get("${ApiConfig.API_VERSION}/playback/recent") { parameter("limit", limit) }.body()

    suspend fun searchStories(q: String, language: String = AraroConstants.DEFAULT_LANGUAGE, page: Int = 0, size: Int = AraroConstants.SEARCH_PAGE_SIZE): SearchStoriesResponse =
        try {
            client.get("${ApiConfig.API_VERSION}/stories/search") {
                parameter("q", q)
                parameter("language", language)
                parameter("page", page)
                parameter("size", size)
            }.body()
        } catch (e: Exception) {
            AraroLog.w("StoryApi", "searchStories(q=$q) failed", e)
            SearchStoriesResponse(content = emptyList(), totalElements = 0)
        }

    /** Report streaming metrics (latency, buffering, completion) for observability. */
    suspend fun reportStreamAnalytics(
        storyId: Long,
        streamStartLatencyMs: Double? = null,
        bufferingEvent: Boolean? = null,
        completed: Boolean? = null
    ) {
        runCatching {
            client.post("${ApiConfig.API_VERSION}/stories/stream/analytics") {
                setBody(
                    StreamAnalyticsRequest(
                        storyId = storyId,
                        streamStartLatencyMs = streamStartLatencyMs,
                        bufferingEvent = bufferingEvent,
                        completed = completed
                    )
                )
            }
        }.onFailure { e ->
            AraroLog.w("StoryApi", "reportStreamAnalytics(storyId=$storyId) failed", e)
        }
    }
}

@kotlinx.serialization.Serializable
data class RecommendedStoryDto(
    val storyId: Long,
    val storySource: String,
    val title: String,
    val theme: String,
    val age: Int,
    val reason: String
)

@kotlinx.serialization.Serializable
data class PlaybackPositionDto(
    val storyId: Long,
    val storySource: String,
    val positionSeconds: Int,
    val updatedAt: String
)

@kotlinx.serialization.Serializable
internal data class PlaybackPositionResponse(val positionSeconds: Int)

@kotlinx.serialization.Serializable
internal data class RemixRequest(val remixInstruction: String)

@kotlinx.serialization.Serializable
data class SoundscapeDto(
    val id: String,
    val name: String,
    val description: String,
    val url: String
)

/** Word-level timing for transcript sync (from TTS or voice transcription). */
@kotlinx.serialization.Serializable
data class WordTiming(val word: String, val startSec: Double, val endSec: Double)

@kotlinx.serialization.Serializable
internal data class StreamUrlResponse(
    val streamUrl: String,
    val avatarUrl: String? = null,
    val avatarVideoUrl: String? = null,
    val wordTimings: List<WordTiming>? = null
)

@kotlinx.serialization.Serializable
internal data class NarrationScriptResponse(val script: String)

@kotlinx.serialization.Serializable
internal data class StreamAnalyticsRequest(
    val storyId: Long,
    val streamStartLatencyMs: Double? = null,
    val bufferingEvent: Boolean? = null,
    val completed: Boolean? = null
)

@kotlinx.serialization.Serializable
data class FavoriteStoryDto(val storyId: Long, val storySource: String)

@kotlinx.serialization.Serializable
internal data class FavoriteCheckDto(val storyId: Long, val isFavorite: Boolean)

@kotlinx.serialization.Serializable
data class SearchStoryItemDto(
    val storyId: Long,
    val storySource: String,
    val title: String? = null,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val coverImageUrl: String? = null,
    val coverVideoUrl: String? = null,
    val status: String
)

@kotlinx.serialization.Serializable
data class SearchStoriesResponse(
    val content: List<SearchStoryItemDto>,
    val totalElements: Long
)
