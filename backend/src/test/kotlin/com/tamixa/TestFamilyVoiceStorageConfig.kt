package com.tamixa

import com.tamixa.application.port.FamilyVoiceStoragePort
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("test")
class TestFamilyVoiceStorageConfig {

    @Bean
    @Primary
    fun familyVoiceStoragePort(): FamilyVoiceStoragePort = object : FamilyVoiceStoragePort {
        override fun upload(storyId: Long, parentId: Long, language: String, audioBytes: ByteArray, contentType: String, fileExt: String): String =
            "families/$parentId/stories/$storyId/$language.$fileExt"
        override fun delete(storagePath: String) {}
        override fun exists(storagePath: String): Boolean = false
    }
}
