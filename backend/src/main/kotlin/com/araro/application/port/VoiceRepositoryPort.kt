package com.araro.application.port

import com.araro.domain.VoiceProfile

interface VoiceRepositoryPort {

    fun save(voiceProfile: VoiceProfile): VoiceProfile

    fun findById(id: Long): VoiceProfile?

    fun findByParentId(parentId: Long): List<VoiceProfile>

    fun findByIdAndParentId(id: Long, parentId: Long): VoiceProfile?
}
