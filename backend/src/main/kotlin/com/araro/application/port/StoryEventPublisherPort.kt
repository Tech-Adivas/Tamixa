package com.araro.application.port

interface StoryEventPublisherPort {

    fun publishStoryCreated(storyId: Long, content: String)
}
