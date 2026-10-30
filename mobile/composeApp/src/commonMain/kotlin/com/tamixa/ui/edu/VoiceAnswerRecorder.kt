package com.tamixa.ui.edu

import androidx.compose.runtime.Composable

/** Records a child's short spoken answer to an interactive story question. Nothing is kept on the device. */
interface VoiceAnswerRecorder {
    fun hasPermission(): Boolean

    /** Asks for the microphone; [onResult] gets whether it was granted. */
    fun requestPermission(onResult: (Boolean) -> Unit)

    /** Starts recording; false when the microphone could not be opened. */
    fun start(): Boolean

    /** Loudness 0..1 of the input since the last call (listening animation and silence detection). */
    fun level(): Float

    /** Stops and returns the recording (AAC in .m4a); the temporary file is already deleted. Null on failure. */
    fun stop(): ByteArray?

    /** Stops without returning anything and deletes the temporary file. */
    fun cancel()
}

/** The platform recorder, or null where voice answers are not supported yet. */
@Composable
expect fun rememberVoiceAnswerRecorder(): VoiceAnswerRecorder?
