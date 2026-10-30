package com.tamixa.infrastructure.narration

/**
 * Joins MP3 files made in parts (long stories are synthesized in chunks) into one playable stream.
 *
 * Each TTS part is a complete MP3 file: an ID3v2 tag, then a Xing/Info (or VBRI) header frame that states the
 * frame count and length of *that part only*, then the audio frames. Appending the files as-is left part 2's tag
 * and header in the middle of the story, so readers that trust the first header (mp3agic, ExoPlayer) reported only
 * part 1's length: a 119 s story was stored as 74 s and the read-along highlight ran 45 s ahead of the voice, and
 * decoders hit an invalid frame at the join. Keeping only the audio frames of every part gives one CBR stream whose
 * length is counted from its frames.
 */
object Mp3Joiner {

    fun join(parts: List<ByteArray>): ByteArray {
        if (parts.isEmpty()) return ByteArray(0)
        if (parts.size == 1) return parts[0]
        val audio = parts.map { audioFrames(it) }
        val out = ByteArray(audio.sumOf { it.size })
        var offset = 0
        for (a in audio) {
            a.copyInto(out, offset)
            offset += a.size
        }
        return out
    }

    /** [mp3] without its ID3v2 tag, ID3v1 tag and Xing/Info/VBRI header frame; unchanged if it is not recognized. */
    internal fun audioFrames(mp3: ByteArray): ByteArray {
        var start = id3v2Size(mp3)
        var end = mp3.size
        if (end - start >= ID3V1_SIZE && mp3[end - ID3V1_SIZE] == 'T'.code.toByte() &&
            mp3[end - ID3V1_SIZE + 1] == 'A'.code.toByte() && mp3[end - ID3V1_SIZE + 2] == 'G'.code.toByte()
        ) {
            end -= ID3V1_SIZE
        }
        val frameLength = frameLength(mp3, start)
        if (frameLength != null && start + frameLength <= end && isInfoFrame(mp3, start, frameLength)) {
            start += frameLength
        }
        return mp3.copyOfRange(start, end)
    }

    /** Size of a leading ID3v2 tag (header + body + optional footer), or 0. */
    private fun id3v2Size(b: ByteArray): Int {
        if (b.size < 10 || b[0] != 'I'.code.toByte() || b[1] != 'D'.code.toByte() || b[2] != '3'.code.toByte()) return 0
        // Tag size is "syncsafe": 4 bytes of 7 bits each.
        val body = ((b[6].toInt() and 0x7F) shl 21) or ((b[7].toInt() and 0x7F) shl 14) or
            ((b[8].toInt() and 0x7F) shl 7) or (b[9].toInt() and 0x7F)
        val footer = if ((b[5].toInt() and 0x10) != 0) 10 else 0
        return (10 + body + footer).coerceAtMost(b.size)
    }

    /** Length in bytes of the MPEG Layer III frame starting at [pos], or null if there is no valid frame header. */
    internal fun frameLength(b: ByteArray, pos: Int): Int? {
        if (pos + 4 > b.size) return null
        val b1 = b[pos + 1].toInt() and 0xFF
        val b2 = b[pos + 2].toInt() and 0xFF
        if ((b[pos].toInt() and 0xFF) != 0xFF || (b1 and 0xE0) != 0xE0) return null
        val version = (b1 shr 3) and 0x03 // 3 = MPEG-1, 2 = MPEG-2, 0 = MPEG-2.5, 1 = reserved
        val layer = (b1 shr 1) and 0x03 // 1 = Layer III
        if (version == 1 || layer != 1) return null
        val bitrateIndex = b2 shr 4
        val sampleRateIndex = (b2 shr 2) and 0x03
        if (bitrateIndex == 0 || bitrateIndex == 15 || sampleRateIndex == 3) return null
        val mpeg1 = version == 3
        val bitrateKbps = (if (mpeg1) MPEG1_L3_BITRATES else MPEG2_L3_BITRATES)[bitrateIndex]
        val sampleRate = MPEG1_SAMPLE_RATES[sampleRateIndex] / (if (mpeg1) 1 else if (version == 2) 2 else 4)
        val padding = (b2 shr 1) and 0x01
        return (if (mpeg1) 144_000 else 72_000) * bitrateKbps / sampleRate + padding
    }

    /** Whether the frame holds a Xing/Info (after the side info) or VBRI (fixed offset 36) header instead of audio. */
    private fun isInfoFrame(b: ByteArray, pos: Int, length: Int): Boolean {
        val searchEnd = (pos + minOf(length, INFO_TAG_SEARCH_BYTES)) - 4
        for (i in pos + 4..searchEnd) {
            if (matches(b, i, "Xing") || matches(b, i, "Info") || matches(b, i, "VBRI")) return true
        }
        return false
    }

    private fun matches(b: ByteArray, at: Int, tag: String): Boolean =
        tag.indices.all { b[at + it] == tag[it].code.toByte() }

    private const val ID3V1_SIZE = 128

    /** Xing/Info sits right after the 4-byte header and 9–32 bytes of side info; VBRI at byte 36. */
    private const val INFO_TAG_SEARCH_BYTES = 48

    private val MPEG1_L3_BITRATES = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
    private val MPEG2_L3_BITRATES = intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
    private val MPEG1_SAMPLE_RATES = intArrayOf(44_100, 48_000, 32_000)
}
