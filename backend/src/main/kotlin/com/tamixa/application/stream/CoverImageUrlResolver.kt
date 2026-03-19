package com.tamixa.application.stream

import com.tamixa.api.ApiVersion
import com.tamixa.domain.Story
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.cdn.S3SignedUrlGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI

/**
 * Resolves story cover image URL.
 * S3: returns proxy path (/api/v1/covers/{path}) to avoid presigned URL issues (CORS, 400 from access points).
 */
@Service
class CoverImageUrlResolver(
    private val appProperties: AppProperties,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun resolveCoverUrl(story: Story): String? {
        val path = story.coverImageUrl ?: return null
        return resolveCoverPath(path)
    }

    /** Resolves cover video URL for a generated story. */
    fun resolveCoverVideoUrl(story: Story): String? {
        val path = story.coverVideoUrl ?: return null
        return resolveCoverVideoPath(path)
    }

    /** Normalizes cover URL for storage. Proxy paths (/api/v1/covers/...) and full URLs containing /covers/ are converted to S3 keys. */
    fun normalizeForStorage(coverImageUrl: String?): String? {
        if (coverImageUrl.isNullOrBlank()) return null
        val prefix = "${ApiVersion.V1}/covers/"
        if (coverImageUrl.startsWith(prefix)) return coverImageUrl.removePrefix(prefix)
        val coversSegment = "/covers/"
        val idx = coverImageUrl.indexOf(coversSegment)
        if (idx >= 0) return coverImageUrl.substring(idx + coversSegment.length)
        val s3Key = extractS3KeyFromUrl(coverImageUrl)
        return s3Key ?: coverImageUrl
    }

    /**
     * Resolves cover video path. Curated cover videos are stored as GIF only;
     * legacy DB may have .mp4 — rewrite to .gif so clients get the correct URL.
     */
    fun resolveCoverVideoPath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val normalized = when {
            path.startsWith("curated_cover_videos/") && path.lowercase().endsWith(".mp4") ->
                path.substring(0, path.length - 4) + ".gif"
            path.startsWith("generated_cover_videos/") && path.lowercase().endsWith(".mp4") ->
                path.substring(0, path.length - 4) + ".gif"
            else -> path
        }
        return resolveCoverPath(normalized)
    }

    /** Resolves a cover path (covers/ or curated_covers/) to a loadable URL. */
    fun resolveCoverPath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) {
            val s3Key = extractS3KeyFromUrl(path)
            if (s3Key != null && (s3Key.startsWith("covers/") || s3Key.startsWith("curated_covers/") || s3Key.startsWith("curated_cover_videos/") || s3Key.startsWith("generated_cover_videos/"))) {
                return "${ApiVersion.V1}/covers/$s3Key"
            }
            return path
        }
        if (!path.startsWith("covers/") && !path.startsWith("curated_covers/") && !path.startsWith("curated_cover_videos/") && !path.startsWith("generated_cover_videos/")) return null
        // S3: use proxy URL instead of presigned (avoids CORS/access-point issues)
        return if (s3SignedUrlGenerator != null) "${ApiVersion.V1}/covers/$path" else null
    }

    /** Extracts S3 object key from presigned S3 URL. Returns null if not an S3 URL. */
    private fun extractS3KeyFromUrl(url: String): String? {
        if (!url.contains("amazonaws.com")) return null
        return try {
            val uri = URI.create(url)
            val path = uri.path?.removePrefix("/")?.takeIf { it.isNotBlank() }
            path?.takeIf { it.startsWith("covers/") || it.startsWith("curated_covers/") || it.startsWith("curated_cover_videos/") || it.startsWith("generated_cover_videos/") }
        } catch (e: Exception) {
            log.debug("extractS3KeyFromUrl failed for url: {}", e.message)
            null
        }
    }
}
