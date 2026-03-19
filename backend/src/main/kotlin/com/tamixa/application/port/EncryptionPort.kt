package com.tamixa.application.port

interface EncryptionPort {

    fun encrypt(plaintext: ByteArray): ByteArray

    fun decrypt(ciphertext: ByteArray): ByteArray
}
