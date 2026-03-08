package com.araro.application.port

/**
 * Stores parent avatar image in private path.
 * Path: avatars/{parentId}/avatar.jpg or .png
 */
interface FamilyAvatarStoragePort {
    fun upload(parentId: Long, imageBytes: ByteArray, contentType: String): String
    fun delete(storagePath: String)
}
