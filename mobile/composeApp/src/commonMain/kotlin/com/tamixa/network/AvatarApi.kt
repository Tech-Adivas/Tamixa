package com.tamixa.network

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.*

/** Response from POST /parents/me/avatar */
@kotlinx.serialization.Serializable
data class AvatarUploadResponseDto(val avatarUrl: String)

/** Response from GET /parents/me/avatar */
@kotlinx.serialization.Serializable
data class AvatarResponseDto(val avatarUrl: String?)

/**
 * API for premium parent avatar (storytelling avatar).
 * Premium users upload an image; it becomes the storyteller in generated videos.
 */
class AvatarApi(private val client: HttpClient) {

    /**
     * Upload avatar image. Premium only.
     * @param imageBytes JPEG or PNG bytes
     * @param contentType image/jpeg or image/png
     * @return AvatarUploadResponseDto on success, null on failure (402, 403, 404)
     */
    suspend fun uploadAvatar(imageBytes: ByteArray, contentType: String = "image/jpeg"): AvatarUploadResponseDto? =
        try {
            val ext = if (contentType.contains("png")) "png" else "jpg"
            val resp = client.post("${ApiConfig.API_VERSION}/parents/me/avatar") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                imageBytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"avatar.$ext\"")
                                }
                            )
                        }
                    )
                )
            }
            if (resp.status.value in 200..299) resp.body<AvatarUploadResponseDto>()
            else null
        } catch (e: Exception) {
            com.tamixa.util.TamixaLog.w("AvatarApi", "uploadAvatar failed", e)
            null
        }

    /**
     * Get current avatar signed URL. Returns null if no avatar or not premium.
     */
    suspend fun getAvatarUrl(): String? =
        try {
            val resp = client.get("${ApiConfig.API_VERSION}/parents/me/avatar")
            if (resp.status.value in 200..299) resp.body<AvatarResponseDto>().avatarUrl
            else null
        } catch (e: Exception) {
            com.tamixa.util.TamixaLog.w("AvatarApi", "getAvatarUrl failed", e)
            null
        }

    /**
     * Delete avatar. Premium only.
     */
    suspend fun deleteAvatar(): Boolean =
        try {
            val resp = client.delete("${ApiConfig.API_VERSION}/parents/me/avatar")
            resp.status.value in 200..299
        } catch (e: Exception) {
            com.tamixa.util.TamixaLog.w("AvatarApi", "deleteAvatar failed", e)
            false
        }
}
