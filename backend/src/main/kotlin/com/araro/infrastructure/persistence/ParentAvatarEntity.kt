package com.araro.infrastructure.persistence

import com.araro.domain.ParentAvatar
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "parent_avatar")
class ParentAvatarEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "parent_id", nullable = false, unique = true)
    val parentId: Long,

    @Column(name = "storage_path", nullable = false, length = 512)
    val storagePath: String,

    @Column(name = "content_type", nullable = false, length = 64)
    val contentType: String = "image/jpeg",

    @Column(name = "file_size_bytes", nullable = false)
    val fileSizeBytes: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = ParentAvatar(
        id = id,
        parentId = parentId,
        storagePath = storagePath,
        contentType = contentType,
        fileSizeBytes = fileSizeBytes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
