package com.tamixa.application.port

/**
 * Stores image bytes and returns a persistent path/URL for retrieval.
 */
interface ImageStoragePort {

    /**
     * Upload image bytes. Returns GCS path (e.g. covers/{storyId}.png) for signed URL resolution, or null if disabled.
     */
    fun storeCoverImage(storyId: Long, imageBytes: ByteArray): String?

    /**
     * Upload curated story cover. Returns path (e.g. curated_covers/{id}.png), or null if disabled.
     */
    fun storeCuratedCoverImage(curatedStoryId: Long, imageBytes: ByteArray): String?

    /**
     * Delete curated cover image at the given storage key (e.g. curated_covers/33.png). No-op if key is null/blank.
     */
    fun deleteCuratedCoverImage(storageKey: String?)

    /**
     * Delete generated story cover image at the given storage key (e.g. covers/42.png). No-op if key is null/blank.
     */
    fun deleteCoverImage(storageKey: String?)
}
