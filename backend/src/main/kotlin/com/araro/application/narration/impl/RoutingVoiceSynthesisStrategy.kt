package com.araro.application.narration.impl

import com.araro.application.narration.VoiceSynthesisStrategy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Routes synthesis to ClonedVoiceStrategy when voiceProfile is "cloned:{id}",
 * otherwise to NeuralVoiceStrategy (Google/OpenAI TTS).
 */
@Component
@Primary
class RoutingVoiceSynthesisStrategy(
    private val clonedStrategy: ClonedVoiceStrategy,
    private val neuralStrategy: NeuralVoiceStrategy
) : VoiceSynthesisStrategy {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun synthesize(ssml: String, language: String, voiceProfile: String): ByteArray? {
        return if (voiceProfile.startsWith("cloned:")) {
            clonedStrategy.synthesize(ssml, language, voiceProfile)
        } else {
            neuralStrategy.synthesize(ssml, language, voiceProfile)
        }
    }
}
