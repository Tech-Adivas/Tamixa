package com.tamixa.application.port

import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceCloningStatus

interface VoiceCloningJobRepositoryPort {
    fun save(job: VoiceCloningJob): VoiceCloningJob
    fun findById(id: Long): VoiceCloningJob?
    fun findByParentId(parentId: Long): List<VoiceCloningJob>
    fun findByStatus(status: VoiceCloningStatus): List<VoiceCloningJob>
    fun updateStatus(
        id: Long,
        status: VoiceCloningStatus,
        errorMessage: String? = null,
        elevenLabsVoiceId: String? = null,
        googleVoiceCloningKey: String? = null
    )
}
