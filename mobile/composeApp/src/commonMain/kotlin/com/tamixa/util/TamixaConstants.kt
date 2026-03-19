package com.tamixa.util

/**
 * Centralized constants to avoid hardcoded values throughout the application.
 * Language, story sources, and limits should be referenced from here.
 */
object TamixaConstants {

    /** Default language when none is set (English). */
    const val DEFAULT_LANGUAGE = "en"

    /** Story source: AI-generated (parent's own). */
    const val STORY_SOURCE_GENERATED = "generated"

    /** Story source: Library from catalog. */
    const val STORY_SOURCE_LIBRARY = "library"

    /** Default voice profile. */
    const val VOICE_PROFILE_DEFAULT = "default"

    /** Free plan identifier. */
    const val PLAN_FREE = "FREE"

    /** Sleep timer preset options (minutes). */
    val SLEEP_TIMER_PRESETS = listOf(5, 10, 15, 30)

    /** Number of recent playback items to load. */
    const val RECENT_PLAYBACK_LIMIT = 10

    /** Number of recommended stories to load. */
    const val RECOMMENDED_LIMIT = 15

    /** Library stories page size. */
    const val LIBRARY_PAGE_SIZE = 50

    /** My stories page size. */
    const val MY_STORIES_PAGE_SIZE = 20

    /** Search results page size. */
    const val SEARCH_PAGE_SIZE = 20

    /** Max stories in in-memory cache. */
    const val CACHE_MAX_STORIES = 50

    /** Seek step in milliseconds (10 seconds). */
    const val SEEK_MS = 10_000L

    /** Max conversation messages in story generation. */
    const val MAX_CONVERSATION_MESSAGES = 10

    /** Default child age when not specified. */
    const val DEFAULT_CHILD_AGE = 5

    /** Min phone number length for validation. */
    const val PHONE_NUMBER_MIN_LENGTH = 10

    /** Stories limit threshold to show "free plan" style copy (e.g. 5 for free tier). */
    const val FREE_PLAN_STORIES_LIMIT_THRESHOLD = 10

    /** Placeholder audio URL pattern (no real stream). */
    const val PLACEHOLDER_AUDIO_PREFIX = "https://example.com"
}
