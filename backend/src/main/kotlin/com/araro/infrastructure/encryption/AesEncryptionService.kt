package com.araro.infrastructure.encryption

import com.araro.application.port.EncryptionPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val ALGORITHM = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH = 128
private const val GCM_IV_LENGTH = 12

@Service
class AesEncryptionService(
    @Value("\${app.voice.encryption-key:}") private val base64Key: String
) : EncryptionPort {

    private val key: ByteArray by lazy {
        if (base64Key.isBlank()) {
            throw IllegalStateException("Voice encryption key is not configured. Set VOICE_ENCRYPTION_KEY environment variable.")
        }
        Base64.getDecoder().decode(base64Key).also { decoded ->
            require(decoded.size == 32) { "VOICE_ENCRYPTION_KEY must be 32 bytes (256 bits) for AES-256. Got ${decoded.size} bytes." }
        }
    }

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val iv = java.security.SecureRandom().generateSeed(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        require(ciphertext.size > GCM_IV_LENGTH) { "Invalid ciphertext: too short" }
        val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }
}
