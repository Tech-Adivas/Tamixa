package com.tamixa.application.port

/**
 * Storage for avatar video files (mp4).
 * Path convention: avatar_videos/{storyId}_{parentId}_{language}_{voice}.mp4
 */
interface StoryAvatarVideoStoragePort {
    fun upload(storagePath: String, videoBytes: ByteArray): Unit
    fun delete(storagePath: String): Unit
}
