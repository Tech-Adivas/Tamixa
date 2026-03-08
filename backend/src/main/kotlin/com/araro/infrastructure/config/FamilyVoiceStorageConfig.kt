package com.araro.infrastructure.config

import com.araro.application.port.FamilyVoiceStoragePort
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.Ordered

/**
 * Provides NoOp FamilyVoiceStoragePort when S3 is not configured.
 * S3FamilyVoiceStorageAdapter is used when app.storage.type=s3 (default).
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class FamilyVoiceStorageConfig {

    @Bean
    @ConditionalOnMissingBean(FamilyVoiceStoragePort::class)
    fun noOpFamilyVoiceStorage(): FamilyVoiceStoragePort = object : FamilyVoiceStoragePort {
        override fun upload(storyId: Long, parentId: Long, language: String, audioBytes: ByteArray, contentType: String, fileExt: String): String {
            throw UnsupportedOperationException("Family voice storage requires S3 (app.storage.type=s3)")
        }

        override fun delete(storagePath: String) {}
        override fun exists(storagePath: String): Boolean = false
    }
}
