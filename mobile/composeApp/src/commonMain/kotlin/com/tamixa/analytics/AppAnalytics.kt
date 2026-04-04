package com.tamixa.analytics

import com.tamixa.network.AnalyticsApi
import com.tamixa.util.TamixaConstants
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

    /** Fired once per player session when optional host story clip is shown (no PII). */
    fun trackHostStoryClipImpression(storyId: Long, storySource: String) {
        scope.launch {
            runCatching {
                analyticsApi.trackAppEvent(
                    eventType = "host_story_clip_impression",
                    storyId = storyId,
                    storySource = storySource
                )
            }
        }
    }

    /** Library lane selection (Browse, Fun, Learn, Practice) — no PII. */
    fun trackLibraryHub(hubKey: String) {
        scope.launch {
            runCatching {
                analyticsApi.trackAppEvent(
                    eventType = "library_hub",
                    hubKey = hubKey
                )
            }
        }
    }

    /** Interactive library story: user picked a branch (counts toward funnel; no choice text). */
    fun trackInteractiveBranch(storyId: Long, storySource: String, language: String) {
        scope.launch {
            runCatching {
                val src = if (storySource == TamixaConstants.STORY_SOURCE_LIBRARY) "library" else "generated"
                analyticsApi.trackStoryEvent(
                    storyId = storyId,
                    storySource = src,
                    language = language,
                    eventType = "interactive_branch",
                    playbackPositionSeconds = 0
                )
            }
        }
    }
}
