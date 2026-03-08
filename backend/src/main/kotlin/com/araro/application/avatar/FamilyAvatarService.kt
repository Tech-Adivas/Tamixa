package com.araro.application.avatar

import com.araro.application.port.FamilyAvatarStoragePort
import com.araro.application.port.ParentAvatarRepositoryPort
import com.araro.application.port.ParentRepositoryPort
import com.araro.application.subscription.SubscriptionService
import com.araro.domain.ParentAvatar
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Max avatar image size: 5MB */
private const val MAX_AVATAR_BYTES = 5 * 1024 * 1024L

/** Allowed image content types */
private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/jpg")

@Service
class FamilyAvatarService(
    private val parentAvatarRepository: ParentAvatarRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val subscriptionService: SubscriptionService,
    familyAvatarStorageProvider: ObjectProvider<FamilyAvatarStoragePort>,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val s3SignedUrlGenerator: com.araro.infrastructure.cdn.S3SignedUrlGenerator?,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val familyAvatarStorage: FamilyAvatarStoragePort = familyAvatarStorageProvider.getIfAvailable()
        ?: object : FamilyAvatarStoragePort {
            override fun upload(parentId: Long, imageBytes: ByteArray, contentType: String): String {
                throw UnsupportedOperationException("Avatar storage requires S3 (app.storage.type=s3)")
            }
            override fun delete(storagePath: String) {}
        }

    private val expiryMinutes get() = appProperties.cdn.signedUrlExpiryMinutes.coerceIn(5L, 60L)

    @Transactional
    fun uploadAvatar(parentEmail: String, imageBytes: ByteArray, contentType: String): ParentAvatar {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw AvatarAccessDeniedException("Parent not found")
        enforcePremium(parent.id)
        if (imageBytes.size > MAX_AVATAR_BYTES) {
            throw AvatarFileTooLargeException("Image exceeds ${MAX_AVATAR_BYTES / 1024 / 1024}MB limit")
        }
        val normalizedContentType = contentType.trim().lowercase()
        if (normalizedContentType !in ALLOWED_CONTENT_TYPES) {
            throw InvalidAvatarFileException("Invalid image format (JPEG or PNG required)")
        }

        val existing = parentAvatarRepository.findByParentId(parent.id)
        if (existing != null) {
            familyAvatarStorage.delete(existing.storagePath)
        }

        val storagePath = familyAvatarStorage.upload(parent.id, imageBytes, normalizedContentType)
        val now = Instant.now()
        val avatar = ParentAvatar(
            id = existing?.id ?: 0L,
            parentId = parent.id,
            storagePath = storagePath,
            contentType = normalizedContentType,
            fileSizeBytes = imageBytes.size.toLong(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val saved = parentAvatarRepository.save(avatar)
        log.info("Avatar uploaded parentId={} path={}", parent.id, storagePath)
        return saved
    }

    fun getAvatarUrl(parentId: Long): String? {
        val avatar = parentAvatarRepository.findByParentId(parentId) ?: return null
        return buildSignedUrl(avatar.storagePath)
    }

    @Transactional
    fun deleteAvatar(parentEmail: String) {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw AvatarAccessDeniedException("Parent not found")
        val existing = parentAvatarRepository.findByParentId(parent.id)
            ?: throw AvatarNotFoundException("No avatar to delete")
        familyAvatarStorage.delete(existing.storagePath)
        parentAvatarRepository.deleteByParentId(parent.id)
        log.info("Avatar deleted parentId={}", parent.id)
    }

    fun hasAvatar(parentId: Long): Boolean =
        parentAvatarRepository.existsByParentId(parentId)

    private fun enforcePremium(parentId: Long) {
        val sub = subscriptionService.getOrCreateSubscription(parentId)
        if (!sub.isEntitledToUnlimitedStories()) {
            throw AvatarPremiumRequiredException("Premium subscription required for storytelling avatar")
        }
    }

    private fun buildSignedUrl(storagePath: String): String? {
        return when {
            appProperties.storage.type == "s3" && s3SignedUrlGenerator != null ->
                s3SignedUrlGenerator.signUrl(storagePath, expiryMinutes)?.toString()
            else -> null
        }
    }
}

class AvatarAccessDeniedException(message: String) : RuntimeException(message)
class AvatarFileTooLargeException(message: String) : RuntimeException(message)
class InvalidAvatarFileException(message: String) : RuntimeException(message)
class AvatarNotFoundException(message: String) : RuntimeException(message)
class AvatarPremiumRequiredException(message: String) : RuntimeException(message)
