package com.tamixa.application.stream

import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Signs /audio proxy links to cloned-voice audio (file names starting with "cloned_") so they can't be fetched by
 * guessing the path. The signed link is handed out only by authenticated stream-url calls that already checked the
 * parent owns the voice; the proxy then serves the file only with a valid, unexpired signature.
 * Signature = base64url(HMAC-SHA256(secret, "{key}|{exp}")), exp in epoch seconds.
 */
@Component
class AudioLinkSigner(private val appProperties: AppProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val secret: ByteArray = appProperties.audio.linkSigningSecret.trim().takeIf { it.isNotEmpty() }
        ?.toByteArray(Charsets.UTF_8)
        ?: ByteArray(32).also {
            SecureRandom().nextBytes(it)
            log.warn(
                "AUDIO_LINK_SIGNING_SECRET is not set: using a random per-process secret. Cloned-voice audio links " +
                    "stop working after a restart and across instances. Set it in production."
            )
        }

    /** True for audio that must only be served with a signature: cloned-voice narration files. */
    fun requiresSignature(key: String): Boolean = key.substringAfterLast('/').startsWith(CLONED_FILE_PREFIX)

    /** Query string ("exp=…&sig=…") that makes [key] playable until now + link expiry. */
    fun signatureQuery(key: String, now: Instant = Instant.now()): String {
        val exp = now.epochSecond + appProperties.audio.linkExpiryMinutes.coerceAtLeast(1) * 60
        return "exp=$exp&sig=${sign(key, exp)}"
    }

    fun isValid(key: String, exp: String?, sig: String?, now: Instant = Instant.now()): Boolean {
        val expSeconds = exp?.toLongOrNull() ?: return false
        if (sig.isNullOrBlank() || expSeconds < now.epochSecond) return false
        return MessageDigest.isEqual(sign(key, expSeconds).toByteArray(), sig.toByteArray())
    }

    private fun sign(key: String, exp: Long): String {
        val mac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(secret, ALGORITHM)) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal("$key|$exp".toByteArray(Charsets.UTF_8)))
    }

    companion object {
        const val CLONED_FILE_PREFIX = "cloned_"
        private const val ALGORITHM = "HmacSHA256"
    }
}
