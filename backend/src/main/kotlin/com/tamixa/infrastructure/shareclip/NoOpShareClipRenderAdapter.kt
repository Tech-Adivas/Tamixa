package com.tamixa.infrastructure.shareclip

import com.tamixa.application.port.ShareClipRenderPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

/**
 * No-op adapter: returns null so share clip jobs fail with "Clip render returned empty".
 * Replace with FFmpegShareClipRenderAdapter when ffmpeg is available.
 */
@Component
@ConditionalOnMissingBean(ShareClipRenderPort::class)
class NoOpShareClipRenderAdapter : ShareClipRenderPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun renderClip(
        audioUrl: String,
        coverImageUrl: String,
        startSeconds: Int,
        durationSeconds: Int,
        format: String
    ): ByteArray? {
        log.warn("ShareClipRenderPort: NoOp adapter - clip rendering not configured. Install ffmpeg and add FFmpegShareClipRenderAdapter.")
        return null
    }
}
