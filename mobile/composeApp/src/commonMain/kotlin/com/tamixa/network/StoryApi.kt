package com.tamixa.network

import com.tamixa.util.TamixaConstants
import com.tamixa.util.TamixaLog
import com.tamixa.domain.LibraryStoriesPageResponse
import com.tamixa.domain.LibraryStoryResponse
import com.tamixa.domain.toStory
import com.tamixa.domain.GenerateStoryRequest
import com.tamixa.domain.StoriesPageResponse
import com.tamixa.domain.Story
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

/** Extended timeout for cloned voice (on-demand TTS can take 60–90s). */
private const val CLONED_VOICE_REQUEST_TIMEOUT_MS = 120_000L

/** Voice option from GET /stories/{id}/voices */
@kotlinx.serialization.Serializable
data class VoiceOptionDto(val voiceProfile: String, val isPremium: Boolean, val displayLabel: String? = null)

@kotlinx.serialization.Serializable
internal data class VoicesResponseDto(val voices: List<VoiceOptionDto>)

@kotlinx.serialization.Serializable
internal data class VoicePreferenceResponseDto(
    val voiceProfile: String,
    val playbackMode: String = "default"
)

@kotlinx.serialization.Serializable
internal data class VoicePreferenceRequestDto(
    val voiceProfile: String,
    val playbackMode: String = "default"
)

@kotlinx.serialization.Serializable
internal data class LimitReachedResponseDto(
    val status: String = "LIMIT_REACHED",
    val remaining: Int = 0,
    val upgradeRequired: Boolean = true,
    val recommendedPlan: String = "PREMIUM"
)

class StoryApi(private val client: HttpClient) {
    suspend fun generate(request: GenerateStoryRequest): Story {
        return try {
            client.post("${ApiConfig.API_VERSION}/stories/generate") {
                setBody(request)
            }.body()
        } catch (e: ResponseException) {
            if (e.response.status.value == 402) {
                val body = try {
                    e.response.body<LimitReachedResponseDto>()
                } catch (_: Exception) {
                    LimitReachedResponseDto()
                }
                throw LimitReachedException(
                    message = "Story limit reached. Upgrade for unlimited stories.",
                    recommendedPlan = body.recommendedPlan
                )
            }
            throw e
        }
    }

    /** Fetch single generated story by id (parent's own story). */
    suspend fun getGeneratedStoryById(id: Long): com.tamixa.domain.Story? =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$id")
            if (resp.status.value in 200..299) resp.body<com.tamixa.domain.StoryApiResponse>().toStory()
            else null
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getGeneratedStoryById(id=$id) failed", e)
            null
        }

    /** Fetch parent's AI-generated stories from server (paginated). */
    suspend fun getMyStories(page: Int = 0, size: Int = TamixaConstants.MY_STORIES_PAGE_SIZE): StoriesPageResponse =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories") {
                parameter("page", page)
                parameter("size", size)
            }
            if (resp.status.value in 200..299) resp.body()
            else StoriesPageResponse(content = emptyList(), totalElements = 0, totalPages = 0, first = true, last = true)
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getMyStories failed", e)
            StoriesPageResponse(content = emptyList(), totalElements = 0, totalPages = 0, first = true, last = true)
        }

    /** Fetches curated stories approved for delivery (admin "Story for review" → Approve). Only approved stories appear. */
    suspend fun getLibraryStories(
        language: String = TamixaConstants.DEFAULT_LANGUAGE,
        page: Int = 0,
        theme: String? = null
    ): List<LibraryStoryResponse> =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/library") {
                parameter("language", language)
                parameter("page", page)
                parameter("size", TamixaConstants.LIBRARY_PAGE_SIZE)
                theme?.takeIf { it.isNotBlank() }?.let { parameter("theme", it) }
            }
            if (resp.status.value in 200..299) {
                val paged = resp.body<LibraryStoriesPageResponse>()
                paged.content
            } else {
                TamixaLog.w("StoryApi", "getLibraryStories status=${resp.status.value}")
                emptyList()
            }
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getLibraryStories failed", e)
            emptyList()
        }

    suspend fun getLibraryStoryById(id: Long, language: String = TamixaConstants.DEFAULT_LANGUAGE): LibraryStoryResponse? {
        return try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/library/$id") {
                parameter("language", language)
            }
            if (resp.status.value !in 200..299) null else resp.body()
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getLibraryStoryById(id=$id) failed", e)
            null
        }
    }

    suspend fun getFavorites(): List<FavoriteStoryDto> =
        client.get("${ApiConfig.API_VERSION}/favorites").body()

    suspend fun addFavorite(storyId: Long, storySource: String = TamixaConstants.STORY_SOURCE_GENERATED) {
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
            TamixaLog.w("StoryApi", "isFavorite(storyId=$storyId) failed", e)
            false
        }

    /**
     * Upload family voice recording for a story.
     * @param audioBytes MP3 or AAC audio bytes
     * @param isAac true if AAC/MP4 from in-app recorder
     * @return true on success, false on failure
     */
    suspend fun uploadFamilyVoice(storyId: Long, language: String = TamixaConstants.DEFAULT_LANGUAGE, audioBytes: ByteArray, isAac: Boolean = false): Boolean =
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
            TamixaLog.w("StoryApi", "uploadFamilyVoice(storyId=$storyId) failed", e)
            false
        }

    /**
     * Delete family voice recording for a story.
     * @return true on success, false on failure
     */
    suspend fun deleteFamilyVoice(storyId: Long, language: String = TamixaConstants.DEFAULT_LANGUAGE): Boolean =
        try {
            val resp = client.delete("${ApiConfig.API_VERSION}/stories/$storyId/delete-family-voice") {
                parameter("language", language)
            }
            resp.status.value in 200..299
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "deleteFamilyVoice(storyId=$storyId) failed", e)
            false
        }

    /** Fetch available voices for a story (default, calm, etc.) with premium flags. */
    suspend fun getAvailableVoices(storyId: Long, language: String = TamixaConstants.DEFAULT_LANGUAGE): List<VoiceOptionDto> =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$storyId/voices") {
                parameter("language", language)
            }
            if (resp.status.value in 200..299) resp.body<VoicesResponseDto>().voices
            else {
                TamixaLog.w("StoryApi", "getAvailableVoices(storyId=$storyId) status=${resp.status.value}")
                emptyList()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            emptyList()
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getAvailableVoices(storyId=$storyId) failed", e)
            emptyList()
        }

    /** Fetch saved voice and playback mode preference. Returns Pair(voiceProfile, playbackMode). */
    suspend fun getVoicePreference(storyId: Long, storySource: String = "library"): Pair<String, String> =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$storyId/voice-preference") {
                parameter("storySource", storySource.take(20).ifEmpty { "library" })
            }
            if (resp.status.value in 200..299) {
                val b = resp.body<VoicePreferenceResponseDto>()
                Pair(b.voiceProfile, b.playbackMode.takeIf { it in setOf("default", "my_voice", "avatar") } ?: "default")
            } else Pair(TamixaConstants.VOICE_PROFILE_DEFAULT, "default")
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getVoicePreference(storyId=$storyId) failed", e)
            Pair(TamixaConstants.VOICE_PROFILE_DEFAULT, "default")
        }

    /** Save voice and playback mode preference for this story. */
    suspend fun setVoicePreference(storyId: Long, storySource: String, voiceProfile: String, playbackMode: String = "default") {
        try {
            val mode = playbackMode.takeIf { it in setOf("default", "my_voice", "avatar") } ?: "default"
            client.put("${ApiConfig.API_VERSION}/stories/$storyId/voice-preference") {
                parameter("storySource", storySource.take(20).ifEmpty { "library" })
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(VoicePreferenceRequestDto(voiceProfile.trim().ifEmpty { TamixaConstants.VOICE_PROFILE_DEFAULT }, mode))
            }
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "setVoicePreference(storyId=$storyId) failed", e)
        }
    }

    /**
     * Fetch short-lived signed URL for audio streaming (CDN mode).
     * @param voiceProfile Optional; e.g. "calm" for premium, "cloned:123" for parent's voice. null = default.
     * @param playbackMode default | my_voice | avatar. Only "avatar" triggers avatar video generation. "my_voice" = audio only.
     * @return StreamUrlResult.Url on success, StreamUrlResult.UpgradeRequired on 402, null on 404
     */
    suspend fun getStreamUrl(
        storyId: Long,
        language: String = TamixaConstants.DEFAULT_LANGUAGE,
        voiceProfile: String? = null,
        storySource: String? = null,
        playbackMode: String? = null
    ): StreamUrlResult =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$storyId/stream-url") {
                parameter("language", language)
                voiceProfile?.takeIf { it != TamixaConstants.VOICE_PROFILE_DEFAULT }?.let { parameter("voiceProfile", it) }
                storySource?.takeIf { it in listOf("library", "generated") }?.let { parameter("storySource", it) }
                playbackMode?.takeIf { it in setOf("default", "my_voice", "avatar") }?.let { parameter("playbackMode", it) }
                if (voiceProfile?.startsWith("cloned:") == true) {
                    timeout { requestTimeoutMillis = CLONED_VOICE_REQUEST_TIMEOUT_MS }
                }
            }
            when (resp.status.value) {
                402 -> StreamUrlResult.UpgradeRequired
                in 200..299 -> {
                    val body = resp.body<StreamUrlResponse>()
                    StreamUrlResult.Url(body.streamUrl, body.avatarUrl, body.avatarVideoUrl, body.avatarStatus, body.voiceFallback, body.wordTimings, body.durationSeconds)
                }
                else -> StreamUrlResult.NotFound
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            StreamUrlResult.NotFound
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getStreamUrl(storyId=$storyId, voice=$voiceProfile) failed", e)
            StreamUrlResult.NotFound
        }

    sealed class StreamUrlResult {
        data class Url(
            val url: String,
            val avatarUrl: String? = null,
            val avatarVideoUrl: String? = null,
            val avatarStatus: String? = null,
            val voiceFallback: Boolean = false,
            val wordTimings: List<WordTiming>? = null,
            val durationSeconds: Int? = null
        ) : StreamUrlResult()
        data object UpgradeRequired : StreamUrlResult()
        data object NotFound : StreamUrlResult()
    }

    /**
     * Fetch conversational (rewritten) narration script for TTS fallback.
     * When backend audio stream fails, mobile uses device TTS—this script sounds warm instead of dry.
     * @return Script text or null on failure
     */
    suspend fun getNarrationScript(storyId: Long, language: String = TamixaConstants.DEFAULT_LANGUAGE): String? =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/stories/$storyId/narration-script") {
                parameter("language", language)
            }
            if (resp.status.value in 200..299) resp.body<NarrationScriptResponse>().script
            else null
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getNarrationScript(storyId=$storyId) failed", e)
            null
        }

    /** Regenerate AI cover for a story. */
    suspend fun regenerateCover(storyId: Long): Story =
        client.post("${ApiConfig.API_VERSION}/stories/$storyId/regenerate-cover").body()

    /** Remix a story with a tweak (e.g. "make the dragon friendly"). */
    suspend fun remix(storyId: Long, remixInstruction: String): Story =
        client.post("${ApiConfig.API_VERSION}/stories/$storyId/remix") {
            setBody(RemixRequest(remixInstruction))
        }.body()

    suspend fun getRecommended(childId: Long? = null, language: String = TamixaConstants.DEFAULT_LANGUAGE, limit: Int = TamixaConstants.RECOMMENDED_LIMIT): List<RecommendedStoryDto> =
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

    suspend fun getPlaybackPosition(storyId: Long, storySource: String = TamixaConstants.STORY_SOURCE_GENERATED): Int =
        try {
            client.get("${ApiConfig.API_VERSION}/playback/position") {
                parameter("storyId", storyId)
                parameter("storySource", storySource)
            }.body<PlaybackPositionResponse>().positionSeconds
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "getPlaybackPosition(storyId=$storyId) failed", e)
            0
        }

    suspend fun getRecentPlayback(limit: Int = TamixaConstants.RECENT_PLAYBACK_LIMIT): List<PlaybackPositionDto> =
        client.get("${ApiConfig.API_VERSION}/playback/recent") { parameter("limit", limit) }.body()

    suspend fun searchStories(q: String, language: String = TamixaConstants.DEFAULT_LANGUAGE, page: Int = 0, size: Int = TamixaConstants.SEARCH_PAGE_SIZE): SearchStoriesResponse =
        try {
            client.get("${ApiConfig.API_VERSION}/stories/search") {
                parameter("q", q)
                parameter("language", language)
                parameter("page", page)
                parameter("size", size)
            }.body()
        } catch (e: Exception) {
            TamixaLog.w("StoryApi", "searchStories(q=$q) failed", e)
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
            TamixaLog.w("StoryApi", "reportStreamAnalytics(storyId=$storyId) failed", e)
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

/** Word-level timing for transcript sync (from TTS or voice transcription). */
@kotlinx.serialization.Serializable
data class WordTiming(val word: String, val startSec: Double, val endSec: Double)

@kotlinx.serialization.Serializable
internal data class StreamUrlResponse(
    val streamUrl: String,
    val avatarUrl: String? = null,
    val avatarVideoUrl: String? = null,
    val avatarStatus: String? = null,
    val voiceFallback: Boolean = false,
    val wordTimings: List<WordTiming>? = null,
    val durationSeconds: Int? = null
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
