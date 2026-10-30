package com.tamixa.application.port

/** Turns a short spoken recording into text. Implementations must not store the audio. */
interface SpeechToTextPort {

    /**
     * Transcribes [audio] (e.g. m4a/AAC from the app) in [language] (ISO code such as "ta", "en").
     * [hint] lists words the speaker is likely to say (improves accuracy for names and answers).
     * Returns null when the provider is not configured or fails.
     */
    fun transcribe(audio: ByteArray, fileName: String, language: String, hint: String?): String?
}
