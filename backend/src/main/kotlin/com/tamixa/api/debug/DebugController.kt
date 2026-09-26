package com.tamixa.api.debug

import com.tamixa.api.ApiVersion
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.infrastructure.cdn.S3SignedUrlGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Debug endpoint for testing cover image URL resolution.
 * Only available in dev profile. No auth required for debugging.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/debug")
@ConditionalOnProperty(name = ["spring.profiles.active"], havingValue = "dev", matchIfMissing = true)
class DebugController(
    private val coverImageUrlResolver: CoverImageUrlResolver,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    companion object {
        const val DEBUG_PATHS = "/api/v1/debug/**"
    }

    @GetMapping("/cover-url")
    fun testCoverUrl(@RequestParam path: String): ResponseEntity<Map<String, Any?>> {
        log.info("🔍 Testing cover URL resolution for path: {}", path)
        
        val resolvedUrl = coverImageUrlResolver.resolveCoverPath(path)
        val directS3Url = s3SignedUrlGenerator?.signUrl(path, 60)?.toString()
        
        val response = mapOf(
            "inputPath" to path,
            "resolvedUrl" to resolvedUrl,
            "directS3Url" to directS3Url,
            "s3SignedUrlGeneratorPresent" to (s3SignedUrlGenerator != null),
            "resolvedUrlLength" to resolvedUrl?.length,
            "directS3UrlLength" to directS3Url?.length
        )
        
        log.info("🔍 Test result: {}", response)
        return ResponseEntity.ok(response)
    }
}
