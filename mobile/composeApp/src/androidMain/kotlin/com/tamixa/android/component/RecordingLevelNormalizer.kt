package com.tamixa.android.component

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Evens out the level of a finished voice recording (MediaRecorder has no gain control):
 * loud takes are brought down to just under full scale so playback never clips, and quiet takes
 * are boosted (up to +12 dB) so every recording plays at a similar volume.
 *
 * Decodes the AAC/M4A file to 16-bit PCM, scales it, and re-encodes it with the recorder's settings.
 */
internal object RecordingLevelNormalizer {
    private const val TAG = "RecordingNormalizer"

    /** Target peak ≈ -1 dBFS: headroom so the AAC encoder's small overshoots stay below full scale. */
    private const val TARGET_PEAK = 0.89f

    /** Largest boost for quiet recordings (+12 dB); more would mostly amplify room noise. */
    private const val MAX_GAIN = 4f

    private const val BIT_RATE = 128_000
    private const val TIMEOUT_US = 10_000L

    private class Pcm(val samples: ShortArray, val sampleRate: Int, val channels: Int)

    /**
     * Returns the normalized recording as M4A bytes, or null when no change is needed or anything fails
     * (the caller then keeps the original recording, so a recording is never lost).
     */
    fun normalize(input: File, workDir: File): ByteArray? {
        val pcm = decodeToPcm16(input) ?: return null
        var peak = 0
        for (s in pcm.samples) {
            val a = abs(s.toInt())
            if (a > peak) peak = a
        }
        // Digital silence: nothing to scale.
        if (peak == 0) return null
        val gain = (TARGET_PEAK * Short.MAX_VALUE / peak).coerceAtMost(MAX_GAIN)
        // Already within ~0.25 dB of the target: re-encoding would only cost quality.
        if (gain in 0.97f..1.03f) return null
        for (i in pcm.samples.indices) {
            pcm.samples[i] = (pcm.samples[i] * gain).roundToInt().coerceIn(-32768, 32767).toShort()
        }
        val out = File(workDir, "normalized_${System.currentTimeMillis()}.m4a")
        return try {
            encodeAac(pcm, out)
            val bytes = out.readBytes().takeIf { it.isNotEmpty() }
            Log.i(TAG, "Recording level adjusted by %+.1f dB (peak was %.1f dBFS)".format(
                20 * log10(gain), 20 * log10(peak / Short.MAX_VALUE.toFloat())))
            bytes
        } catch (e: Exception) {
            Log.w(TAG, "Re-encoding normalized recording failed; keeping original", e)
            null
        } finally {
            out.delete()
        }
    }

    private fun decodeToPcm16(input: File): Pcm? {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(input.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            decoder = codec
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmBytes = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outFormat = codec.outputFormat
                    sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        outFormat.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) != AudioFormat.ENCODING_PCM_16BIT
                    ) {
                        Log.w(TAG, "Decoder output is not 16-bit PCM; skipping normalization")
                        return null
                    }
                } else if (index >= 0) {
                    val buffer = codec.getOutputBuffer(index)!!
                    if (info.size > 0) {
                        val chunk = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        buffer.get(chunk)
                        pcmBytes.write(chunk)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
            val raw = pcmBytes.toByteArray()
            val samples = ShortArray(raw.size / 2)
            ByteBuffer.wrap(raw).order(ByteOrder.nativeOrder()).asShortBuffer().get(samples)
            return Pcm(samples, sampleRate, channels)
        } catch (e: Exception) {
            Log.w(TAG, "Decoding recording failed; keeping original", e)
            return null
        } finally {
            try {
                decoder?.stop()
            } catch (_: Exception) {
            }
            decoder?.release()
            extractor.release()
        }
    }

    private fun encodeAac(pcm: Pcm, out: File) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, pcm.sampleRate, pcm.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        }
        val bytes = ByteBuffer.allocate(pcm.samples.size * 2).order(ByteOrder.nativeOrder())
        bytes.asShortBuffer().put(pcm.samples)
        val data = bytes.array()
        val bytesPerFrame = 2 * pcm.channels

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            val info = MediaCodec.BufferInfo()
            var track = -1
            var offset = 0
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val index = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val buffer = encoder.getInputBuffer(index)!!
                        buffer.clear()
                        // Whole frames only, so channels never get out of step.
                        val size = minOf(buffer.remaining(), data.size - offset) / bytesPerFrame * bytesPerFrame
                        val presentationUs = offset.toLong() / bytesPerFrame * 1_000_000L / pcm.sampleRate
                        if (size <= 0) {
                            encoder.queueInputBuffer(index, 0, 0, presentationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            buffer.put(data, offset, size)
                            encoder.queueInputBuffer(index, 0, size, presentationUs, 0)
                            offset += size
                        }
                    }
                }
                val index = encoder.dequeueOutputBuffer(info, TIMEOUT_US)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (index >= 0) {
                    val buffer = encoder.getOutputBuffer(index)!!
                    // Codec config (AAC header) is carried by the track format, not written as a sample.
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        muxer.writeSampleData(track, buffer, info)
                    }
                    encoder.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        } finally {
            try {
                encoder.stop()
            } catch (_: Exception) {
            }
            encoder.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
        }
    }
}
