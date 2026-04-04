package com.tamixa.network

import com.tamixa.domain.VoiceProfile
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class VoiceApi(private val client: HttpClient) {
    suspend fun upload(fileBytes: ByteArray, fileName: String): VoiceProfile =
        client.submitFormWithBinaryData(
            url = "${ApiConfig.API_VERSION}/voice/upload",
            formData = formData {
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                })
            }
        ).requireBodyOrThrow()

    suspend fun list(): List<VoiceProfile> =
        client.get("${ApiConfig.API_VERSION}/voice").bodyIfSuccess() ?: emptyList()

    suspend fun getById(id: Long): VoiceProfile =
        client.get("${ApiConfig.API_VERSION}/voice/$id").requireBodyOrThrow()
}
