package com.tamixa.application.storylibrary

/**
 * Thrown when admin tries to submit for review without changing content.
 * Prevents redundant pipeline runs.
 */
class ContentUnchangedException(message: String = "Content is the same, cannot submit for review") :
    IllegalArgumentException(message)

/**
 * Thrown when admin tries to submit for review while pipeline is already running for that story.
 */
class PipelineRunningException(
    message: String,
    val processingLanguage: String? = null
) : IllegalArgumentException(message)
