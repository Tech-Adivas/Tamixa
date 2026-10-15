package com.tamixa.api.voice

import com.tamixa.api.ApiVersion
import com.tamixa.api.exception.GlobalExceptionHandler
import com.tamixa.application.consent.ConsentService
import com.tamixa.application.narration.NarrationPremiumVoiceValidator
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.voice.MaxClonedVoicesReachedException
import com.tamixa.application.voice.VoiceCloningService
import com.tamixa.application.voice.VoiceUploadResult
import com.tamixa.domain.Parent
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceCloningStatus
import com.tamixa.domain.VoiceProfile
import com.tamixa.infrastructure.config.AppProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

/**
 * POST /voice/upload responds with the voice profile (not the cloning job), maps the voice limit to 409, requires
 * voice cloning consent, and only starts a paid cloud clone for plans that include voice cloning.
 */
class VoiceUploadControllerTest {

    private val email = "parent@test.com"
    private val voiceCloningService: VoiceCloningService = mock()
    private val consentService: ConsentService = mock()
    private val premiumVoiceValidator: NarrationPremiumVoiceValidator = mock()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() = buildController(provider = "elevenlabs")

    private fun buildController(provider: String) {
        val props = AppProperties(
            voiceCloning = AppProperties.VoiceCloningProperties(
                enabled = true,
                provider = provider,
                elevenLabsApiKey = "test-key",
                fishAudioApiKey = "fish-key"
            )
        )
        val parent = mock<Parent> { on { id } doReturn 8L }
        val parents = mock<ParentRepositoryPort> { on { findByEmail(email) } doReturn parent }
        whenever(consentService.hasConsent(email, VoiceController.VOICE_CLONING_CONSENT)).thenReturn(true)
        whenever(premiumVoiceValidator.canCloneVoice(8L)).thenReturn(true)
        val controller = VoiceController(
            voiceCloningService,
            mock<VoiceRepositoryPort>(),
            parents,
            props,
            consentService,
            premiumVoiceValidator
        )
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(email, null, emptyList())
    }

    @AfterEach
    fun tearDown() = SecurityContextHolder.clearContext()

    @Test
    fun `new upload returns the profile id and starts its cloning job`() {
        whenever(voiceCloningService.uploadVoice(eq(email), any(), eq("my_voice_1.m4a"), anyOrNull()))
            .thenReturn(VoiceUploadResult(profile(id = 13), job(id = 6)))

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(file("my_voice_1.m4a")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(13))
            .andExpect(jsonPath("$.referenceFileName").value("my_voice_1.m4a"))

        verify(voiceCloningService, times(1)).processVoiceCloningJob(6)
    }

    @Test
    fun `repeat upload returns the existing profile and starts no job`() {
        whenever(voiceCloningService.uploadVoice(eq(email), any(), eq("my_voice_1.m4a"), anyOrNull()))
            .thenReturn(VoiceUploadResult(profile(id = 13), job = null))

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(file("my_voice_1.m4a")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(13))

        verify(voiceCloningService, never()).processVoiceCloningJob(any())
    }

    @Test
    fun `voice limit is 409 with a message`() {
        whenever(voiceCloningService.uploadVoice(eq(email), any(), any(), anyOrNull()))
            .thenThrow(MaxClonedVoicesReachedException("Maximum 5 cloned voices per profile. Delete one to add another."))

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(file("my_voice_6.m4a")))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Maximum 5 cloned voices per profile. Delete one to add another."))

        verify(voiceCloningService, never()).processVoiceCloningJob(any())
    }

    @Test
    fun `upload without any voice cloning consent is refused and stores nothing`() {
        whenever(consentService.hasConsent(email, VoiceController.VOICE_CLONING_CONSENT)).thenReturn(false)

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(file("my_voice_1.m4a")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VOICE_CONSENT_REQUIRED"))

        verify(voiceCloningService, never()).uploadVoice(any(), any(), any(), anyOrNull())
        verify(voiceCloningService, never()).createReferenceVoiceProfile(any(), any(), any())
    }

    @Test
    fun `consent sent with the upload is recorded before storing`() {
        whenever(consentService.hasConsent(email, VoiceController.VOICE_CLONING_CONSENT)).thenReturn(false)
        whenever(voiceCloningService.uploadVoice(eq(email), any(), eq("my_voice_1.m4a"), anyOrNull()))
            .thenReturn(VoiceUploadResult(profile(id = 13), job(id = 6)))

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(file("my_voice_1.m4a")).param("userConsent", "true"))
            .andExpect(status().isOk)

        verify(consentService).record(email, VoiceController.VOICE_CLONING_CONSENT, 1)
        verify(voiceCloningService).processVoiceCloningJob(6)
    }

    @Test
    fun `plan without voice cloning stores the recording but starts no paid clone`() {
        whenever(premiumVoiceValidator.canCloneVoice(8L)).thenReturn(false)
        whenever(voiceCloningService.createReferenceVoiceProfile(eq(email), any(), eq("my_voice_1.m4a")))
            .thenReturn(profile(id = 13))

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(file("my_voice_1.m4a")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(13))

        verify(voiceCloningService, never()).uploadVoice(any(), any(), any(), anyOrNull())
        verify(voiceCloningService, never()).processVoiceCloningJob(any())
    }

    @Test
    fun `fish audio provider starts a cloning job`() {
        buildController(provider = " FishAudio ")
        whenever(voiceCloningService.uploadVoice(eq(email), any(), eq("my_voice_1.m4a"), anyOrNull()))
            .thenReturn(VoiceUploadResult(profile(id = 13), job(id = 6)))

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(file("my_voice_1.m4a")))
            .andExpect(status().isOk)

        verify(voiceCloningService).processVoiceCloningJob(6)
    }

    @Test
    fun `non-audio file is refused before storing or recording consent`() {
        val pdf = MockMultipartFile("file", "my_voice_1.m4a", "audio/mp4", "%PDF-1.7 not audio at all".toByteArray())

        mockMvc.perform(multipart("${ApiVersion.V1}/voice/upload").file(pdf).param("userConsent", "true"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_AUDIO"))

        verify(voiceCloningService, never()).uploadVoice(any(), any(), any(), anyOrNull())
        verify(consentService, never()).record(any(), any(), any())
    }

    /** Starts like a phone recording: an MP4 container ("ftyp" at byte 4). */
    private fun file(name: String) =
        MockMultipartFile("file", name, "audio/mp4", byteArrayOf(0, 0, 0, 0x1C) + "ftypM4A ".toByteArray() + ByteArray(8))

    private fun profile(id: Long) = VoiceProfile(
        id = id,
        parentId = 8,
        encryptedEmbedding = ByteArray(0),
        createdAt = Instant.parse("2026-10-12T09:40:23Z"),
        referenceAudioPath = "voices/8/cloning_1791798009461_ref_my_voice_1.m4a"
    )

    private fun job(id: Long) = VoiceCloningJob(
        id = id,
        parentId = 8,
        audioStoragePath = "voices/8/cloning_1791798009461_ref_my_voice_1.m4a",
        audioFileSizeBytes = 3,
        voiceName = "my_voice_1",
        elevenLabsVoiceId = null,
        status = VoiceCloningStatus.PENDING,
        errorMessage = null,
        createdAt = Instant.now(),
        completedAt = null
    )
}
