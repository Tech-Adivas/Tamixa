package com.tamixa.network

import io.ktor.client.*
import io.ktor.client.request.*

class AnalyticsApi(private val client: HttpClient) {

    /** Lightweight app events: screen views, search, favorites. No PII. */
    suspend fun trackAppEvent(
        eventType: String,
        screenName: String? = null,
        searchQueryLength: Int? = null,
        storyId: Long? = null,
        storySource: String? = null
    ) {
        client.post("${ApiConfig.API_VERSION}/analytics/app-events") {
            setBody(
                buildMap {
                    put("eventType", eventType)
                    screenName?.let { put("screenName", it) }
                    searchQueryLength?.let { put("searchQueryLength", it) }
                    storyId?.let { put("storyId", it) }
                    storySource?.let { put("storySource", it) }
                }
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
            setBody(
                buildMap {
                    put("storyId", storyId)
                    put("storySource", storySource)
                    put("language", language)
                    put("eventType", eventType)
                    put("playbackPositionSeconds", playbackPositionSeconds)
                    childId?.let { put("childId", it) }
                }
            )
        }
    }
}
