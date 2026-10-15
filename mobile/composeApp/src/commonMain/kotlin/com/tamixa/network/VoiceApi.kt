package com.tamixa.network

import com.tamixa.domain.VoiceLimitReachedException
import com.tamixa.domain.VoiceProfile
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.retry
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

/** Upload stores the file in S3 before responding; give it longer than the default socket timeout. */
internal const val VOICE_UPLOAD_TIMEOUT_MS = 120_000L

class VoiceApi(private val client: HttpClient) {
    /**
     * Uploads a recording and returns its server voice profile (the id GET/DELETE /voice/{id} take).
     * Not retried automatically: a retry after the server already stored the file is answered with the same
     * profile, but callers decide when to send it again. Throws [VoiceLimitReachedException] on 409.
     */
    suspend fun upload(fileBytes: ByteArray, fileName: String): VoiceProfile {
        val response = client.submitFormWithBinaryData(
            url = "${ApiConfig.API_VERSION}/voice/upload",
            formData = formData {
                // Ktor already writes `form-data; name="file"`; only add the filename (a full header here
                // gets merged into an invalid Content-Disposition and the server rejects the upload).
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        ) {
            retry { noRetry() }
            timeout {
                requestTimeoutMillis = VOICE_UPLOAD_TIMEOUT_MS
                socketTimeoutMillis = VOICE_UPLOAD_TIMEOUT_MS
            }
        }
        if (response.status == HttpStatusCode.Conflict) {
            val message = runCatching { response.body<ApiErrorResponse>().message }.getOrNull()
            throw VoiceLimitReachedException(message?.takeIf { it.isNotBlank() } ?: "Voice limit reached")
        }
        return response.requireBodyOrThrow()
    }

    suspend fun list(): List<VoiceProfile> =
        client.get("${ApiConfig.API_VERSION}/voice").bodyIfSuccess() ?: emptyList()

    /** Like [list] but throws on any non-2xx, so callers can tell "no voices" from "couldn't ask". */
    suspend fun listOrThrow(): List<VoiceProfile> =
        client.get("${ApiConfig.API_VERSION}/voice").requireBodyOrThrow()

    /** Deletes one of the caller's voices on the server (stored recording + profile). Throws on network / 5xx errors. */
    suspend fun delete(id: Long): com.tamixa.domain.VoiceDeleteResult {
        val response = client.delete("${ApiConfig.API_VERSION}/voice/$id")
        return when (response.status.value) {
            in 200..299 -> com.tamixa.domain.VoiceDeleteResult.DELETED
            404 -> com.tamixa.domain.VoiceDeleteResult.NOT_FOUND
            409 -> com.tamixa.domain.VoiceDeleteResult.IN_USE
            else -> throw IllegalStateException("Voice delete failed (${response.status})")
        }
    }

    suspend fun getById(id: Long): VoiceProfile =
        client.get("${ApiConfig.API_VERSION}/voice/$id").requireBodyOrThrow()

    /** Raw bytes of the caller's saved reference recording for [id] (for playback after save). */
    suspend fun getReferenceAudio(id: Long): ByteArray =
        client.get("${ApiConfig.API_VERSION}/voice/$id/reference-audio") {
            // Default request headers ask for JSON only; audio responses would get 406 without this.
            accept(ContentType.Audio.Any)
            accept(ContentType.Any)
        }.requireBodyOrThrow()

    /** True when the parent has agreed to voice cloning before (the server refuses voice uploads without it). */
    suspend fun hasVoiceCloningConsent(): Boolean =
        client.get("${ApiConfig.API_VERSION}/consent").requireBodyOrThrow<List<ConsentRecordDto>>()
            .any { it.consentType == VOICE_CLONING_CONSENT }

    /** Records the parent's voice cloning consent. Throws on any non-2xx. */
    suspend fun recordVoiceCloningConsent() {
        val response = client.post("${ApiConfig.API_VERSION}/consent") {
            contentType(ContentType.Application.Json)
            setBody(RecordConsentRequestDto(consentType = VOICE_CLONING_CONSENT, version = 1))
        }
        if (!response.status.isSuccess()) throw IllegalStateException("Recording voice consent failed (${response.status})")
    }

    companion object {
        /** Consent type the backend checks on POST /voice/upload. */
        const val VOICE_CLONING_CONSENT = "voice_cloning"
    }
}

@kotlinx.serialization.Serializable
data class RecordConsentRequestDto(val consentType: String, val version: Int)
