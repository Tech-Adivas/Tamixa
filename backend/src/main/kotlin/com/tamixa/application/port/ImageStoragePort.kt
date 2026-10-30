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

    /**
     * Upload the picture shown with an interactive story's question. Returns path
     * (e.g. interactive_images/{storyId}/{segmentId}.png), or null if disabled.
     */
    fun storeInteractiveSegmentImage(libraryStoryId: Long, segmentId: String, imageBytes: ByteArray): String?

    /**
     * Upload a story slideshow picture (JPEG). Returns path (e.g. curated_covers/visuals/{storyId}/{name}.jpg), or
     * null if disabled. [name] should change on regeneration so apps don't show a cached older picture.
     */
    fun storeStoryVisualImage(libraryStoryId: Long, name: String, jpegBytes: ByteArray): String? = null
}
