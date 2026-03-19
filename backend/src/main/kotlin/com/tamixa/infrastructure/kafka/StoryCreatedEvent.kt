package com.tamixa.infrastructure.kafka

data class StoryCreatedEvent(
    val storyId: Long,
    val content: String
)
