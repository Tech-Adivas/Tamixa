package com.araro.repository

import com.araro.domain.VoiceProfile
import com.araro.network.VoiceApi

class VoiceRepository(private val api: VoiceApi) {
    suspend fun upload(fileBytes: ByteArray, fileName: String): Result<VoiceProfile> =
        runCatching { api.upload(fileBytes, fileName) }

    suspend fun list(): Result<List<VoiceProfile>> =
        runCatching { api.list() }

    suspend fun getById(id: Long): Result<VoiceProfile> =
        runCatching { api.getById(id) }
}
