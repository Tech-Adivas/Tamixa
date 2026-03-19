package com.tamixa.application.port

import com.tamixa.domain.VoiceProfile

interface VoiceRepositoryPort {

    fun save(voiceProfile: VoiceProfile): VoiceProfile

    fun findById(id: Long): VoiceProfile?

    fun findByParentId(parentId: Long): List<VoiceProfile>

    fun findByIdAndParentId(id: Long, parentId: Long): VoiceProfile?

    /** Remove voice profile by id (caller must verify ownership first). */
    fun deleteById(id: Long)
}
