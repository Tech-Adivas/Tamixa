package com.tamixa.application.port

interface StoryEventPublisherPort {

    fun publishStoryCreated(storyId: Long, content: String)
}
