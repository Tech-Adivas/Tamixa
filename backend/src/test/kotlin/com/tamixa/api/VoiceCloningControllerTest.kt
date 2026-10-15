package com.tamixa.api

import com.tamixa.api.exception.GlobalExceptionHandler
import com.tamixa.application.narration.NarrationPremiumVoiceValidator
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.subscription.SubscriptionTierService
import com.tamixa.application.voice.VoiceAlreadyClonedException
import com.tamixa.application.voice.VoiceCloningService
import com.tamixa.domain.Parent
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceCloningStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

/** POST /voice-cloning only clones the caller's own recordings (voices/{parentId}/), once each. */
class VoiceCloningControllerTest {

    private val email = "parent@test.com"
    private val voiceCloningService: VoiceCloningService = mock()
    private val premiumVoiceValidator: NarrationPremiumVoiceValidator = mock()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val parent = mock<Parent> { on { id } doReturn 8L }
        val parents = mock<ParentRepositoryPort> { on { findByEmail(email) } doReturn parent }
        whenever(premiumVoiceValidator.canCloneVoice(8L)).thenReturn(true)
        val controller = VoiceCloningController(voiceCloningService, mock<SubscriptionTierService>(), parents, premiumVoiceValidator)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(email, null, emptyList())
    }

    @AfterEach
    fun tearDown() = SecurityContextHolder.clearContext()

    @Test
    fun `own recording is cloned`() {
        whenever(voiceCloningService.requestCloneOfRecording(eq(8L), eq("voices/8/a.m4a"), anyOrNull(), any(), any()))
            .thenReturn(job())

        mockMvc.perform(request("voices/8/a.m4a")).andExpect(status().isOk)

        verify(voiceCloningService).processVoiceCloningJob(6)
    }

    @Test
    fun `another parent's recording is refused`() {
        mockMvc.perform(request("voices/9/a.m4a")).andExpect(status().isForbidden)
        mockMvc.perform(request("voices/8/../9/a.m4a")).andExpect(status().isForbidden)
        mockMvc.perform(request("voices/8/a.m4a", consentPath = "voices/9/consent.m4a")).andExpect(status().isForbidden)

        verify(voiceCloningService, never()).requestCloneOfRecording(any(), any(), anyOrNull(), any(), any())
    }

    @Test
    fun `recording that is already cloned is 409 and starts nothing`() {
        whenever(voiceCloningService.requestCloneOfRecording(eq(8L), eq("voices/8/a.m4a"), anyOrNull(), any(), any()))
            .thenThrow(VoiceAlreadyClonedException("This voice is already cloned"))

        mockMvc.perform(request("voices/8/a.m4a")).andExpect(status().isConflict)

        verify(voiceCloningService, never()).processVoiceCloningJob(any())
    }

    @Test
    fun `plan without voice cloning is refused`() {
        whenever(premiumVoiceValidator.canCloneVoice(8L)).thenReturn(false)

        mockMvc.perform(request("voices/8/a.m4a")).andExpect(status().isPaymentRequired)

        verify(voiceCloningService, never()).requestCloneOfRecording(any(), any(), anyOrNull(), any(), any())
    }

    private fun request(path: String, consentPath: String? = null) =
        post("${ApiVersion.V1}/voice-cloning")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """{"audioStoragePath":"$path",""" +
                    (consentPath?.let { """"consentAudioStoragePath":"$it",""" } ?: "") +
                    """"audioFileSizeBytes":3,"voiceName":"my voice"}"""
            )

    private fun job() = VoiceCloningJob(
        id = 6,
        parentId = 8,
        audioStoragePath = "voices/8/a.m4a",
        audioFileSizeBytes = 3,
        voiceName = "my voice",
        elevenLabsVoiceId = null,
        status = VoiceCloningStatus.PENDING,
        errorMessage = null,
        createdAt = Instant.now(),
        completedAt = null
    )
}
