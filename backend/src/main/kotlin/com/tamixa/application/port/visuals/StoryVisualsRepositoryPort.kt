package com.tamixa.application.port.visuals

import com.tamixa.domain.visuals.StoryVisualImage
import com.tamixa.domain.visuals.StoryVisualSet
import com.tamixa.domain.visuals.StoryVisualShot

interface StoryVisualsRepositoryPort {

    fun findSetByStoryId(storyId: Long): StoryVisualSet?

    fun saveSet(set: StoryVisualSet): StoryVisualSet

    fun findImages(setId: Long): List<StoryVisualImage>

    fun saveImage(image: StoryVisualImage): StoryVisualImage

    fun findShots(setId: Long): List<StoryVisualShot>

    /** Replaces the whole plan (pictures and shots) of a set, e.g. after re-planning. */
    fun replacePlan(setId: Long, images: List<StoryVisualImage>, shots: List<StoryVisualShot>)

    fun deleteSetByStoryId(storyId: Long)
}
