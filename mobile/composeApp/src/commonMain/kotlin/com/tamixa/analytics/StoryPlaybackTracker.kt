package com.tamixa.analytics

import com.tamixa.domain.Story
import com.tamixa.network.AnalyticsApi
import com.tamixa.network.StoryApi
import com.tamixa.platform.currentTimeMillis
import com.tamixa.util.TamixaConstants
import com.tamixa.util.TamixaLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MAX_POSITION_SECONDS = 86_400

/**
 * Tracks story retention events and persists playback position to the API so
 * "Continue listening" and resume work (POST /v1/playback/position).
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
    private var lastProgressSaveWallMs = 0L
    private var lastSavedPositionSec = -1

    fun reset() {
        milestones25 = false
        milestones50 = false
        milestones75 = false
        started = false
        completed = false
        lastProgressSaveWallMs = 0L
        lastSavedPositionSec = -1
    }

    private fun storySource(story: Story): String =
        if (story.parentId == 0L) TamixaConstants.STORY_SOURCE_LIBRARY else TamixaConstants.STORY_SOURCE_GENERATED

    private fun savePlaybackToServer(story: Story, positionSeconds: Int) {
        val clamped = positionSeconds.coerceIn(0, MAX_POSITION_SECONDS)
        scope.launch {
            runCatching {
                storyApi.savePlaybackPosition(
                    storyId = story.id,
                    storySource = storySource(story),
                    positionSeconds = clamped
                )
            }.onFailure { e ->
                TamixaLog.w("StoryPlaybackTracker", "savePlaybackPosition failed storyId=${story.id}", e)
            }
        }
    }

    fun onStoryStarted(story: Story, positionSeconds: Int = 0) {
        if (started) return
        started = true
        scope.launch {
            runCatching {
                analyticsApi.trackStoryEvent(
                    storyId = story.id,
                    storySource = storySource(story),
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
        maybeSaveProgressWhilePlaying(story, positionSeconds)
    }

    /** Throttled saves while listening so brief exits still get a row in Continue listening. */
    private fun maybeSaveProgressWhilePlaying(story: Story, positionSeconds: Int) {
        if (positionSeconds < 3) return
        val now = currentTimeMillis()
        val intervalOk = now - lastProgressSaveWallMs >= 15_000L
        val jumpOk = lastSavedPositionSec >= 0 && kotlin.math.abs(positionSeconds - lastSavedPositionSec) >= 25
        if (!intervalOk && !jumpOk) return
        lastProgressSaveWallMs = now
        lastSavedPositionSec = positionSeconds
        savePlaybackToServer(story, positionSeconds)
    }

    fun onCompleted(story: Story, positionSeconds: Int) {
        if (completed) return
        completed = true
        emit(story, "story_completed", positionSeconds)
        savePlaybackToServer(story, positionSeconds)
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
        savePlaybackToServer(story, positionSeconds)
    }

    private fun emit(story: Story, eventType: String, positionSeconds: Int) {
        scope.launch {
            runCatching {
                analyticsApi.trackStoryEvent(
                    storyId = story.id,
                    storySource = storySource(story),
                    language = story.language,
                    eventType = eventType,
                    playbackPositionSeconds = positionSeconds
                )
            }
        }
    }
}
