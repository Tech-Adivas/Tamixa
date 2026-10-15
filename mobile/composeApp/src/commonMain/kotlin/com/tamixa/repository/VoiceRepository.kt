package com.tamixa.repository

import com.tamixa.domain.VoiceProfile
import com.tamixa.network.VoiceApi

class VoiceRepository(private val api: VoiceApi) {
    suspend fun upload(fileBytes: ByteArray, fileName: String): Result<VoiceProfile> =
        runCatching { api.upload(fileBytes, fileName) }

    suspend fun list(): Result<List<VoiceProfile>> =
        runCatching { api.list() }

    suspend fun listOrThrow(): Result<List<VoiceProfile>> =
        runCatching { api.listOrThrow() }

    suspend fun delete(id: Long): Result<com.tamixa.domain.VoiceDeleteResult> =
        runCatching { api.delete(id) }

    suspend fun getById(id: Long): Result<VoiceProfile> =
        runCatching { api.getById(id) }

    suspend fun getReferenceAudio(id: Long): Result<ByteArray> =
        runCatching { api.getReferenceAudio(id) }

    suspend fun hasVoiceCloningConsent(): Result<Boolean> =
        runCatching { api.hasVoiceCloningConsent() }

    suspend fun recordVoiceCloningConsent(): Result<Unit> =
        runCatching { api.recordVoiceCloningConsent() }
}
