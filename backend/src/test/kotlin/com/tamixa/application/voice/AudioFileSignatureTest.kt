package com.tamixa.application.voice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Audio is recognized from its first bytes, whatever the file is called. */
class AudioFileSignatureTest {

    @Test
    fun `supported audio containers are accepted`() {
        assertThat(accepted(header(0, 0, 0, 0x1C) + ascii("ftypM4A "))).isTrue // phone m4a
        assertThat(accepted(ascii("ID3") + header(4, 0, 0))).isTrue
        assertThat(accepted(header(0xFF, 0xFB, 0x90, 0x64))).isTrue // MP3 frame
        assertThat(accepted(header(0xFF, 0xF1, 0x50, 0x80))).isTrue // AAC ADTS
        assertThat(accepted(ascii("RIFF") + header(0, 0, 0, 0) + ascii("WAVE"))).isTrue
        assertThat(accepted(ascii("OggS"))).isTrue
        assertThat(accepted(ascii("fLaC"))).isTrue
        assertThat(accepted(header(0x1A, 0x45, 0xDF, 0xA3))).isTrue // WebM
    }

    @Test
    fun `other files are refused`() {
        assertThat(accepted(ascii("%PDF-1.7"))).isFalse
        assertThat(accepted(header(0x89) + ascii("PNG"))).isFalse
        assertThat(accepted(ascii("PK") + header(3, 4))).isFalse // zip / apk
        assertThat(accepted(ascii("RIFF") + header(0, 0, 0, 0) + ascii("AVI "))).isFalse
        assertThat(accepted(header(0xFF, 0xE8))).isFalse // reserved MPEG version
        assertThat(AudioFileSignature.isSupportedAudio(byteArrayOf(1, 2, 3))).isFalse // too short
    }

    /** Pads [bytes] to 12 (the minimum checked) after the signature, so short headers test the signature only. */
    private fun accepted(bytes: ByteArray) =
        AudioFileSignature.isSupportedAudio(if (bytes.size >= 12) bytes else bytes + ByteArray(12 - bytes.size))
    private fun ascii(s: String) = s.toByteArray(Charsets.US_ASCII)
    private fun header(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }
}
