package com.tamixa.application.voice

/**
 * Recognizes supported audio containers from a file's first bytes (not its name or declared content type),
 * so non-audio files are never stored as voice recordings or sent to a cloning provider.
 * Supported: MP3, WAV, M4A/MP4/AAC-in-MP4, raw AAC (ADTS), OGG, FLAC, WebM.
 */
object AudioFileSignature {

    fun isSupportedAudio(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        return when {
            startsWith(bytes, 0, "ID3") -> true                                   // MP3 with ID3 tag
            startsWith(bytes, 0, "RIFF") && startsWith(bytes, 8, "WAVE") -> true  // WAV
            startsWith(bytes, 4, "ftyp") -> true                                  // M4A / MP4 / 3GP (phone recordings)
            startsWith(bytes, 0, "OggS") -> true                                  // OGG (Vorbis / Opus)
            startsWith(bytes, 0, "fLaC") -> true                                  // FLAC
            bytes[0] == 0x1A.toByte() && bytes[1] == 0x45.toByte() &&
                bytes[2] == 0xDF.toByte() && bytes[3] == 0xA3.toByte() -> true    // WebM / Matroska
            isMpegAudioFrameSync(bytes) -> true                                   // MP3 without tag, or raw AAC (ADTS)
            else -> false
        }
    }

    /** 11 set sync bits, then a valid MPEG version / layer: MP3 frames (layer 1-3) and ADTS AAC (layer 0). */
    private fun isMpegAudioFrameSync(bytes: ByteArray): Boolean {
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        if (b0 != 0xFF || (b1 and 0xE0) != 0xE0) return false
        val version = (b1 shr 3) and 0x03
        return version != 0x01 // 01 is reserved
    }

    private fun startsWith(bytes: ByteArray, offset: Int, ascii: String): Boolean =
        ascii.indices.all { bytes[offset + it] == ascii[it].code.toByte() }
}
