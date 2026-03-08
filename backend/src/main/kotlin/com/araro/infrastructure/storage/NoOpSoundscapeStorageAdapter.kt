package com.araro.infrastructure.storage

import com.araro.application.port.SoundscapeStoragePort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * No-op storage adapter for soundscape (dev/testing).
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "noop", matchIfMissing = true)
class NoOpSoundscapeStorageAdapter : SoundscapeStoragePort {

    override fun upload(soundscapeId: Long, audioBytes: ByteArray, contentType: String): String {
        return "/soundscapes/${soundscapeId}.mp3"
    }

    override fun delete(storagePath: String) {
        // No-op
    }

    override fun exists(storagePath: String): Boolean {
        return true
    }
}
