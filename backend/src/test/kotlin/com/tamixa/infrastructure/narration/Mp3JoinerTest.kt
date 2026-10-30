package com.tamixa.infrastructure.narration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Mp3JoinerTest {

    /** MPEG-1 Layer III, 128 kbps, 44.1 kHz, no padding, mono: 417-byte frames of 1152 samples (~26 ms). */
    private val frameHeader = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0xC4.toByte())
    private val frameSize = 417

    private fun audioFrame(fill: Int) = ByteArray(frameSize) { fill.toByte() }.also { frameHeader.copyInto(it) }

    /** Header frame like the one TTS services put first: "Info" after the mono side info (offset 4 + 17). */
    private fun infoFrame() = ByteArray(frameSize).also {
        frameHeader.copyInto(it)
        "Info".encodeToByteArray().copyInto(it, 21)
    }

    /** ID3v2.4 tag with a 20-byte body. */
    private fun id3Tag() = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 0, 20) +
        ByteArray(20)

    /** One TTS part as an MP3 file: ID3 tag, Info frame, then [frames] audio frames. */
    private fun ttsPart(frames: Int, fill: Int): ByteArray {
        var bytes = id3Tag() + infoFrame()
        repeat(frames) { bytes += audioFrame(fill) }
        return bytes
    }

    @Test
    fun `frame length of 128 kbps 44_1 kHz MPEG-1 Layer III`() {
        assertThat(Mp3Joiner.frameLength(audioFrame(0), 0)).isEqualTo(frameSize)
        assertThat(Mp3Joiner.frameLength(byteArrayOf(1, 2, 3, 4), 0)).isNull()
    }

    @Test
    fun `audioFrames drops ID3 tag and Info frame and keeps audio frames`() {
        val frames = Mp3Joiner.audioFrames(ttsPart(frames = 3, fill = 1))

        assertThat(frames.size).isEqualTo(3 * frameSize)
        assertThat(frames.copyOfRange(0, 4)).isEqualTo(frameHeader)
        assertThat(String(frames, Charsets.ISO_8859_1)).doesNotContain("Info").doesNotContain("ID3")
    }

    @Test
    fun `audioFrames drops a trailing ID3v1 tag`() {
        val tag = "TAG".encodeToByteArray() + ByteArray(125)
        assertThat(Mp3Joiner.audioFrames(audioFrame(1) + tag).size).isEqualTo(frameSize)
    }

    @Test
    fun `audioFrames leaves unrecognized bytes unchanged`() {
        val bytes = ByteArray(1000) { 7 }
        assertThat(Mp3Joiner.audioFrames(bytes)).isEqualTo(bytes)
    }

    @Test
    fun `single part is returned as is`() {
        val part = ttsPart(frames = 2, fill = 1)
        assertThat(Mp3Joiner.join(listOf(part))).isSameAs(part)
    }

    @Test
    fun `joined parts contain only audio frames so the measured duration covers every part`() {
        val part1 = ttsPart(frames = 300, fill = 1)
        val part2 = ttsPart(frames = 200, fill = 2)

        val joined = Mp3Joiner.join(listOf(part1, part2))

        assertThat(joined.size).isEqualTo(500 * frameSize)
        assertThat(String(joined, Charsets.ISO_8859_1)).doesNotContain("Info").doesNotContain("ID3")
        // 500 frames x 1152 samples / 44100 Hz = 13.06 s; the old append reported part 1's Info length instead.
        assertThat(Mp3DurationReader.durationSecondsOrNull(joined)).isEqualTo(13)
    }
}
