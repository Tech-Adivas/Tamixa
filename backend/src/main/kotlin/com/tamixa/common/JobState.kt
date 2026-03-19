package com.tamixa.common

/**
 * Canonical job states for processing_job.
 * Maps to DB status values.
 */
enum class JobState(val value: String) {
    QUEUED("QUEUED"),
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    override fun toString(): String = value

    companion object {
        fun fromString(s: String?): JobState? = entries.find { it.value.equals(s, ignoreCase = true) }
    }
}
