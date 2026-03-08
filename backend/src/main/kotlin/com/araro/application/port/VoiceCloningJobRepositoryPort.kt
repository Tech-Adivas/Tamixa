package com.araro.application.port

import com.araro.domain.VoiceCloningJob
import com.araro.domain.VoiceCloningStatus

interface VoiceCloningJobRepositoryPort {
    fun save(job: VoiceCloningJob): VoiceCloningJob
    fun findById(id: Long): VoiceCloningJob?
    fun findByParentId(parentId: Long): List<VoiceCloningJob>
    fun findByStatus(status: VoiceCloningStatus): List<VoiceCloningJob>
    fun updateStatus(id: Long, status: VoiceCloningStatus, errorMessage: String? = null, elevenLabsVoiceId: String? = null)
}
