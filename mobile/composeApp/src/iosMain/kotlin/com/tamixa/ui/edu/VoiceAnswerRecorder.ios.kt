package com.tamixa.ui.edu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.tamixa.ios.audio.IosAudioRecorder

/**
 * Voice answers on iOS: AVAudioRecorder at 16 kHz / 32 kbps AAC (same as Android). NOT YET BUILT ON A MAC.
 */
@Composable
actual fun rememberVoiceAnswerRecorder(): VoiceAnswerRecorder? {
    val recorder = remember { IosAudioRecorder(sampleRate = 16_000.0, bitRate = 32_000, filePrefix = "voice_answer") }
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    return recorder
}
