package com.tamixa.infrastructure.narration

import com.tamixa.application.port.narration.TtsClientPort
import com.tamixa.infrastructure.llm.ProviderFallbackRunner

/**
 * Narration audio with a backup engine: Google Cloud TTS first, OpenAI TTS when Google fails
 * (billing disabled, bad key, quota, outage) or returns no audio.
 * Enable with `NARRATION_TTS_PROVIDER=google` + `NARRATION_TTS_FALLBACK=openai` (wired in
 * [com.tamixa.infrastructure.llm.ProviderFallbackConfig]).
 *
 * Note: OpenAI voices are not native Tamil voices — the backup keeps stories playable, Google stays preferred.
 */
class FallbackTtsClient(
    val primary: TtsClientPort,
    val fallback: TtsClientPort,
    private val runner: ProviderFallbackRunner,
) : TtsClientPort {

    override fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray? =
        runner.run(
            "synthesizeToMp3 lang=$language",
            isUsable = { it != null && it.isNotEmpty() },
            primary = { primary.synthesizeToMp3(ssml, language, voiceProfile) },
            fallback = { fallback.synthesizeToMp3(ssml, language, voiceProfile) },
        )
}
