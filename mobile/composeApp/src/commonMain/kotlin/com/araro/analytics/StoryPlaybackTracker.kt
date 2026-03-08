package com.araro.analytics

import com.araro.domain.Story
import com.araro.network.AnalyticsApi
import com.araro.network.StoryApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Tracks story retention events and streaming metrics.
 * One instance per playback session; call reset() when starting a new story.
 */
class StoryPlaybackTracker(
    private val analyticsApi: AnalyticsApi,
    private val storyApi: StoryApi,
    private val scope: CoroutineScope
) {
    private var milestones25 = false
    private var milestones50 = false
    private var milestones75 = false
    private var started = false
    private var completed = false

    fun reset() {
        milestones25 = false
        milestones50 = false
        milestones75 = false
        started = false
        completed = false
    }

    fun onStoryStarted(story: Story, positionSeconds: Int = 0) {
        if (started) return
        started = true
        scope.launch {
            runCatching {
                analyticsApi.trackStoryEvent(
                    storyId = story.id,
                    storySource = if (story.parentId == 0L) "curated" else "generated",
                    language = story.language,
                    eventType = "story_started",
                    playbackPositionSeconds = positionSeconds
                )
            }
        }
    }

    fun onProgress(story: Story, progress01: Float, positionSeconds: Int) {
        if (!started) return
        if (progress01 >= 0.25f && !milestones25) {
            milestones25 = true
            emit(story, "story_25_percent", positionSeconds)
        }
        if (progress01 >= 0.50f && !milestones50) {
            milestones50 = true
            emit(story, "story_50_percent", positionSeconds)
        }
        if (progress01 >= 0.75f && !milestones75) {
            milestones75 = true
            emit(story, "story_75_percent", positionSeconds)
        }
    }

    fun onCompleted(story: Story, positionSeconds: Int) {
        if (completed) return
        completed = true
        emit(story, "story_completed", positionSeconds)
        scope.launch {
            runCatching {
                storyApi.reportStreamAnalytics(storyId = story.id, completed = true)
            }
        }
    }

    fun onStoppedEarly(story: Story, positionSeconds: Int) {
        if (completed) return
        completed = true
        emit(story, "story_stopped_early", positionSeconds)
    }

    private fun emit(story: Story, eventType: String, positionSeconds: Int) {
        scope.launch {
            runCatching {
                analyticsApi.trackStoryEvent(
                    storyId = story.id,
                    storySource = if (story.parentId == 0L) "curated" else "generated",
                    language = story.language,
                    eventType = eventType,
                    playbackPositionSeconds = positionSeconds
                )
            }
        }
    }
}
