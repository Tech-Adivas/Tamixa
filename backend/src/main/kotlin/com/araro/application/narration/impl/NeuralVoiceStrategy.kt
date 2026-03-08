package com.araro.application.narration.impl

import com.araro.application.narration.VoiceSynthesisStrategy
import com.araro.application.port.narration.TtsClientPort
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Default strategy: neural TTS (Azure/Google). Uses TtsClientPort.
 * Primary AI voice strategy for TTS synthesis.
 */
@Component
class NeuralVoiceStrategy(
    private val ttsClient: TtsClientPort
) : VoiceSynthesisStrategy {

    override fun synthesize(ssml: String, language: String, voiceProfile: String): ByteArray? =
        ttsClient.synthesizeToMp3(ssml, language, voiceProfile)
}
