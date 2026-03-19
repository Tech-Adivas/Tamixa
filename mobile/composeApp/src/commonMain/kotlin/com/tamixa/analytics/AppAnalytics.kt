package com.tamixa.analytics

import com.tamixa.network.AnalyticsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Lightweight analytics for screen views, search, and favorites. No PII. */
class AppAnalytics(
    private val analyticsApi: AnalyticsApi,
    private val scope: CoroutineScope
) {

    fun trackScreenView(screenName: String) {
        scope.launch {
            runCatching {
                analyticsApi.trackAppEvent(
                    eventType = "screen_view",
                    screenName = screenName
                )
            }
        }
    }

    fun trackSearch(queryLength: Int) {
        scope.launch {
            runCatching {
                analyticsApi.trackAppEvent(
                    eventType = "search",
                    searchQueryLength = queryLength
                )
            }
        }
    }

    fun trackFavoriteAdd(storyId: Long, storySource: String) {
        scope.launch {
            runCatching {
                analyticsApi.trackAppEvent(
                    eventType = "favorite_add",
                    storyId = storyId,
                    storySource = storySource
                )
            }
        }
    }

    fun trackFavoriteRemove(storyId: Long, storySource: String) {
        scope.launch {
            runCatching {
                analyticsApi.trackAppEvent(
                    eventType = "favorite_remove",
                    storyId = storyId,
                    storySource = storySource
                )
            }
        }
    }
}
