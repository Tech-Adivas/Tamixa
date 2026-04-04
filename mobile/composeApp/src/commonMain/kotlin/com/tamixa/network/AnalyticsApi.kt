package com.tamixa.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class TrackAppEventRequestDto(
    val eventType: String,
    val screenName: String? = null,
    val searchQueryLength: Int? = null,
    val storyId: Long? = null,
    val storySource: String? = null,
    val hubKey: String? = null,
)

@Serializable
private data class TrackStoryEventRequestDto(
    val storyId: Long,
    val storySource: String,
    val language: String,
    val eventType: String,
    val playbackPositionSeconds: Int = 0,
    val childId: Long? = null,
)

class AnalyticsApi(private val client: HttpClient) {

    /** Lightweight app events: screen views, search, favorites. No PII. */
    suspend fun trackAppEvent(
        eventType: String,
        screenName: String? = null,
        searchQueryLength: Int? = null,
        storyId: Long? = null,
        storySource: String? = null,
        hubKey: String? = null,
    ) {
        client.post("${ApiConfig.API_VERSION}/analytics/app-events") {
            contentType(ContentType.Application.Json)
            setBody(
                TrackAppEventRequestDto(
                    eventType = eventType,
                    screenName = screenName,
                    searchQueryLength = searchQueryLength,
                    storyId = storyId,
                    storySource = storySource,
                    hubKey = hubKey,
                )
            )
        }
    }

    suspend fun trackStoryEvent(
        storyId: Long,
        storySource: String,
        language: String,
        eventType: String,
        playbackPositionSeconds: Int = 0,
        childId: Long? = null
    ) {
        client.post("${ApiConfig.API_VERSION}/analytics/story-events") {
            contentType(ContentType.Application.Json)
            setBody(
                TrackStoryEventRequestDto(
                    storyId = storyId,
                    storySource = storySource,
                    language = language,
                    eventType = eventType,
                    playbackPositionSeconds = playbackPositionSeconds,
                    childId = childId,
                )
            )
        }
    }
}
