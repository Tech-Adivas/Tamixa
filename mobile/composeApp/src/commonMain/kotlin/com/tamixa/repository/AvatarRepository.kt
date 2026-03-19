package com.tamixa.repository

import com.tamixa.network.AvatarApi

class AvatarRepository(private val api: AvatarApi) {

    suspend fun uploadAvatar(imageBytes: ByteArray, contentType: String = "image/jpeg"): Result<String> =
        runCatching {
            api.uploadAvatar(imageBytes, contentType)?.avatarUrl
                ?: throw IllegalStateException("Avatar upload failed")
        }

    suspend fun getAvatarUrl(): Result<String?> =
        runCatching { api.getAvatarUrl() }

    suspend fun deleteAvatar(): Result<Unit> =
        runCatching {
            if (!api.deleteAvatar()) throw IllegalStateException("Avatar delete failed")
        }
}
