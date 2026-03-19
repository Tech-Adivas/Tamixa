package com.tamixa.application.port

/**
 * Stores cover animation bytes (GIF preferred; path convention uses .gif).
 * Path conventions: curated_cover_videos/{curatedStoryId}.gif, generated_cover_videos/{storyId}.gif
 * (served via same /api/v1/covers/ proxy).
 */
interface CoverVideoStoragePort {

    /**
     * Upload curated story cover animation (GIF). Returns path (e.g. curated_cover_videos/{id}.gif), or null if disabled.
     */
    fun storeCuratedCoverVideo(curatedStoryId: Long, animationBytes: ByteArray): String?

    /**
     * Upload generated story cover animation (GIF). Returns path (e.g. generated_cover_videos/{id}.gif), or null if disabled.
     */
    fun storeGeneratedCoverVideo(storyId: Long, animationBytes: ByteArray): String?

    /**
     * Delete curated cover video at the given storage key (e.g. curated_cover_videos/33.gif or .mp4). No-op if key is null/blank.
     */
    fun deleteCuratedCoverVideo(storageKey: String?)

    /**
     * Delete generated cover video at the given storage key (e.g. generated_cover_videos/42.gif). No-op if key is null/blank.
     */
    fun deleteGeneratedCoverVideo(storageKey: String?)
}
