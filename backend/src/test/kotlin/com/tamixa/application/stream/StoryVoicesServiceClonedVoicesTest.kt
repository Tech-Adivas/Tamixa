package com.tamixa.application.stream

import com.tamixa.application.port.StoryFamilyVoiceRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.VoiceCloningJobRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceCloningStatus
import com.tamixa.domain.VoiceProfile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.Instant

/** The narration voice list offers a parent's own voices, except ones whose cloud voice is still being created. */
class StoryVoicesServiceClonedVoicesTest {

    private val parentId = 8L

    @Test
    fun `voice whose cloning job is running is not offered yet`() {
        val service = service(
            profiles = listOf(profile(13, "voices/8/cloning_1_ref_my_voice_13.m4a", elevenLabsId = null)),
            jobs = listOf(job("voices/8/cloning_1_ref_my_voice_13.m4a", VoiceCloningStatus.PROCESSING, Instant.now()))
        )

        assertThat(service.getAvailableVoices(71, "ta", parentId).map { it.voiceProfile }).doesNotContain("cloned:13")
    }

    @Test
    fun `cloned, failed and stuck voices are offered`() {
        val service = service(
            profiles = listOf(
                profile(13, "voices/8/cloning_1_ref_ready.m4a", elevenLabsId = "el-13"),
                // Cloning failed: the recording is kept; narration can create the cloud voice from it on first use.
                profile(14, "voices/8/cloning_2_ref_failed.m4a", elevenLabsId = null),
                // Job never finished (e.g. backend restarted): not hidden forever.
                profile(15, "voices/8/cloning_3_ref_stuck.m4a", elevenLabsId = null)
            ),
            jobs = listOf(
                job("voices/8/cloning_1_ref_ready.m4a", VoiceCloningStatus.READY, Instant.now()),
                job("voices/8/cloning_2_ref_failed.m4a", VoiceCloningStatus.FAILED, Instant.now()),
                job("voices/8/cloning_3_ref_stuck.m4a", VoiceCloningStatus.PENDING, Instant.now().minus(Duration.ofHours(1)))
            )
        )

        assertThat(service.getAvailableVoices(71, "ta", parentId).map { it.voiceProfile })
            .containsExactly("cloned:13", "cloned:14", "cloned:15")
    }

    private fun service(profiles: List<VoiceProfile>, jobs: List<VoiceCloningJob>) = StoryVoicesService(
        translationRepository = mock<StoryTranslationRepositoryPort>(),
        narrationAudioRepository = mock<StoryNarrationAudioRepositoryPort>(),
        voiceCatalog = mock<NarrationVoiceCatalogRepositoryPort>(),
        familyVoiceRepository = mock<StoryFamilyVoiceRepositoryPort>(),
        voiceRepository = mock<VoiceRepositoryPort> { on { findByParentId(parentId) } doReturn profiles },
        voiceCloningJobRepository = mock<VoiceCloningJobRepositoryPort> { on { findByParentId(parentId) } doReturn jobs }
    )

    private fun profile(id: Long, path: String, elevenLabsId: String?) = VoiceProfile(
        id = id,
        parentId = parentId,
        encryptedEmbedding = ByteArray(0),
        createdAt = Instant.now(),
        elevenlabsVoiceId = elevenLabsId,
        referenceAudioPath = path
    )

    private fun job(path: String, status: VoiceCloningStatus, createdAt: Instant) = VoiceCloningJob(
        id = 0,
        parentId = parentId,
        audioStoragePath = path,
        audioFileSizeBytes = 1,
        voiceName = "v",
        elevenLabsVoiceId = null,
        status = status,
        errorMessage = null,
        createdAt = createdAt,
        completedAt = null
    )
}
