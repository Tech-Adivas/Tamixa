package com.araro.infrastructure.webhook

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Optional encryption for webhook raw payloads at rest. When key is blank, returns null (no storage).
 * Uses AES-256-GCM; key must be 32 bytes base64.
 */
object WebhookPayloadEncryptor {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    fun encryptIfConfigured(base64Key: String, plaintext: String): String? {
        if (base64Key.isBlank()) return null
        val key = try {
            Base64.getDecoder().decode(base64Key).also { require(it.size == 32) { "Key must be 32 bytes" } }
        } catch (_: Exception) {
            return null
        }
        val iv = java.security.SecureRandom().generateSeed(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }
}
