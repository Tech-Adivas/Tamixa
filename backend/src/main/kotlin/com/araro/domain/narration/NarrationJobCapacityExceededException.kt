package com.araro.domain.narration

/**
 * Thrown when max concurrent narration jobs limit is reached.
 * Kafka consumer can retry; API returns 503.
 */
class NarrationJobCapacityExceededException(message: String) : RuntimeException(message)
