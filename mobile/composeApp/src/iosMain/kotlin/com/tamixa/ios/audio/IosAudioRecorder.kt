package com.tamixa.ios.audio

import com.tamixa.ui.edu.VoiceAnswerRecorder
import com.tamixa.util.TamixaLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.math.pow

/**
 * Microphone recording on iOS with AVAudioRecorder: AAC in an .m4a file in the temp directory, mono, at
 * [sampleRate] / [bitRate] (same settings as Android: 48 kHz / 128 kbps for the parent's voice sample, 16 kHz /
 * 32 kbps for a child's spoken answer). Metering gives [level] for the listening animation and silence detection.
 *
 * The audio session is switched to play-and-record while recording and back to playback afterwards, so story
 * playback keeps working. Implements [VoiceAnswerRecorder]; the parent recording dialog also uses [stopToFile]
 * to keep the file for preview.
 *
 * NOT YET BUILT ON A MAC (written on Windows, 2026-10-17): compile and test on a device before release.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioRecorder(
    private val sampleRate: Double,
    private val bitRate: Int,
    private val filePrefix: String,
) : VoiceAnswerRecorder {

    private var recorder: AVAudioRecorder? = null
    private var path: String? = null

    override fun hasPermission(): Boolean =
        AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        if (hasPermission()) return onResult(true)
        // The system asks once; after a denial only the Settings app can grant it (the callback then says false).
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            dispatch_async(dispatch_get_main_queue()) {
                if (!granted) TamixaLog.i(TAG, "Microphone permission denied")
                onResult(granted)
            }
        }
    }

    override fun start(): Boolean {
        cancel()
        if (!hasPermission()) return false
        return try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, AVAudioSessionCategoryOptionDefaultToSpeaker, null)
            val millis = (NSDate().timeIntervalSince1970 * 1000).toLong()
            val dir = (NSTemporaryDirectory()).trimEnd('/')
            val filePath = "$dir/${filePrefix}_$millis.m4a"
            val settings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC.toInt(),
                AVSampleRateKey to sampleRate,
                AVNumberOfChannelsKey to 1,
                AVEncoderBitRateKey to bitRate,
                AVEncoderAudioQualityKey to AVAudioQualityHigh,
            )
            val rec = AVAudioRecorder(uRL = NSURL.fileURLWithPath(filePath), settings = settings, error = null)
            rec.meteringEnabled = true
            if (!rec.prepareToRecord() || !rec.record()) {
                TamixaLog.w(TAG, "Could not start recording")
                restorePlaybackSession()
                false
            } else {
                recorder = rec
                path = filePath
                true
            }
        } catch (e: Throwable) {
            TamixaLog.w(TAG, "Could not start recording: ${e.message}", e)
            cancel()
            false
        }
    }

    /** Loudness 0..1 (average power in dBFS converted to linear amplitude). */
    override fun level(): Float {
        val rec = recorder ?: return 0f
        rec.updateMeters()
        val db = rec.averagePowerForChannel(0uL).toDouble().coerceIn(-160.0, 0.0)
        return 10.0.pow(db / 20.0).toFloat().coerceIn(0f, 1f)
    }

    /** Stops and returns the recording; the temporary file is deleted. Null when nothing usable was recorded. */
    override fun stop(): ByteArray? {
        val filePath = stopToFile() ?: return null
        return try {
            readFile(filePath)
        } finally {
            deleteFile(filePath)
        }
    }

    /** Stops and keeps the file (for preview); the caller deletes it with [deleteFile]. Null when nothing was recorded. */
    fun stopToFile(): String? {
        val rec = recorder ?: return null
        val filePath = path
        recorder = null
        path = null
        rec.stop()
        restorePlaybackSession()
        val size = filePath?.let { fileSize(it) } ?: 0L
        if (filePath == null || size <= 0L) {
            TamixaLog.w(TAG, "Recording produced no audio")
            filePath?.let { deleteFile(it) }
            return null
        }
        return filePath
    }

    override fun cancel() {
        val rec = recorder
        val filePath = path
        recorder = null
        path = null
        if (rec != null) {
            rec.stop()
            rec.deleteRecording()
            restorePlaybackSession()
        }
        filePath?.let { deleteFile(it) }
    }

    private fun restorePlaybackSession() {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
    }

    companion object {
        private const val TAG = "IosAudioRecorder"

        fun readFile(filePath: String): ByteArray? {
            val data = NSFileManager.defaultManager.contentsAtPath(filePath) ?: return null
            val length = data.length.toInt()
            if (length <= 0) return null
            val bytes = ByteArray(length)
            bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
            return bytes
        }

        fun deleteFile(filePath: String) {
            NSFileManager.defaultManager.removeItemAtPath(filePath, null)
        }

        private fun fileSize(filePath: String): Long =
            NSFileManager.defaultManager.contentsAtPath(filePath)?.length?.toLong() ?: 0L
    }
}
