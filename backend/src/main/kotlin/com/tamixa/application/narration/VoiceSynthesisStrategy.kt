package com.tamixa.application.narration

/**
 * Strategy for voice synthesis. Enables swapping neural TTS with cloned voices later.
 *
 * Architecture: Why cloning is future extension only? Cloned voice needs consent,
 * COPPA compliance, and provider integration (ElevenLabs). Implementing now would
 * add risk and delay. The strategy pattern lets us ship cloning incrementally
 * without changing TTSService or the orchestration flow.
 *
 * - NeuralVoiceStrategy: AI TTS (Calm default, Expressive premium)
 */
interface VoiceSynthesisStrategy {

    /**
     * Synthesize SSML to MP3 bytes.
     * @param ssml Valid SSML
     * @param language BCP-47
     * @param voiceProfile Voice key (default, calm, or cloned:id)
     * @return MP3 bytes or null on failure
     */
    fun synthesize(ssml: String, language: String, voiceProfile: String): ByteArray?
}
