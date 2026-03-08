package com.araro.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "voice_profiles")
class VoiceProfileEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @Column(name = "encrypted_embedding", nullable = false, columnDefinition = "bytea")
    val encryptedEmbedding: ByteArray,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "elevenlabs_voice_id", length = 64)
    val elevenlabsVoiceId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceProfileEntity
        if (id != other.id) return false
        if (!encryptedEmbedding.contentEquals(other.encryptedEmbedding)) return false
        return true
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode() + encryptedEmbedding.contentHashCode()
    }
}
