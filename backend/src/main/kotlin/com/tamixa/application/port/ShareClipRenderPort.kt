package com.tamixa.application.port

/**
 * Renders a shareable clip from audio + cover/avatar.
 * Returns MP4 bytes or null on failure.
 * Implementation may use FFmpeg to extract segment, composite with image, add watermark.
 */
interface ShareClipRenderPort {

    /**
     * @param audioUrl HTTP URL to full story audio
     * @param coverImageUrl HTTP URL to cover image (or avatar)
     * @param startSeconds Start offset in audio
     * @param durationSeconds Clip duration
     * @param format "9:16" (Reels) or "1:1" (IG feed)
     * @return MP4 bytes or null if render failed
     */
    fun renderClip(
        audioUrl: String,
        coverImageUrl: String,
        startSeconds: Int,
        durationSeconds: Int,
        format: String
    ): ByteArray?
}
