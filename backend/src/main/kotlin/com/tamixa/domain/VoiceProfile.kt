package com.tamixa.domain

import java.time.Instant

data class VoiceProfile(
    val id: Long,
    val parentId: Long,
    val encryptedEmbedding: ByteArray,
    val createdAt: Instant,
    val elevenlabsVoiceId: String? = null,
    val googleVoiceCloningKey: String? = null,
    val referenceAudioPath: String? = null,
    val heygenVoiceId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceProfile
        if (id != other.id) return false
        if (parentId != other.parentId) return false
        if (!encryptedEmbedding.contentEquals(other.encryptedEmbedding)) return false
        if (createdAt != other.createdAt) return false
        if (elevenlabsVoiceId != other.elevenlabsVoiceId) return false
        if (googleVoiceCloningKey != other.googleVoiceCloningKey) return false
        if (referenceAudioPath != other.referenceAudioPath) return false
        if (heygenVoiceId != other.heygenVoiceId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + parentId.hashCode()
        result = 31 * result + encryptedEmbedding.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (elevenlabsVoiceId?.hashCode() ?: 0)
        result = 31 * result + (googleVoiceCloningKey?.hashCode() ?: 0)
        result = 31 * result + (referenceAudioPath?.hashCode() ?: 0)
        result = 31 * result + (heygenVoiceId?.hashCode() ?: 0)
        return result
    }
}
