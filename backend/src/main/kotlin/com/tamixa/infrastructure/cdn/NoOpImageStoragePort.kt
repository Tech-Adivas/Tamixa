package com.tamixa.infrastructure.cdn

import com.tamixa.application.port.ImageStoragePort
import org.springframework.stereotype.Component

/** Fallback when S3 is not configured; used in dev/local. */
@Component
class NoOpImageStoragePort : ImageStoragePort {

    override fun storeCoverImage(storyId: Long, imageBytes: ByteArray): String? = null

    override fun storeCuratedCoverImage(curatedStoryId: Long, imageBytes: ByteArray): String? = null

    override fun deleteCuratedCoverImage(storageKey: String?) {}

    override fun deleteCoverImage(storageKey: String?) {}
}
