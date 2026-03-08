package com.araro.infrastructure.storage

import com.araro.application.port.FamilyVoiceStoragePort
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Fallback when S3 is not configured.
 * FamilyVoiceStorageConfig also provides a @Bean; both use ConditionalOnMissingBean
 * so only one is active. This component is the backup if config load order defers to it.
 */
@Component
@Primary
@ConditionalOnMissingBean(FamilyVoiceStoragePort::class)
class NoOpFamilyVoiceStorageAdapter : FamilyVoiceStoragePort {

    override fun upload(storyId: Long, parentId: Long, language: String, audioBytes: ByteArray, contentType: String, fileExt: String): String {
        throw UnsupportedOperationException("Family voice storage requires S3 (app.storage.type=s3)")
    }

    override fun delete(storagePath: String) {}
    override fun exists(storagePath: String): Boolean = false
}
