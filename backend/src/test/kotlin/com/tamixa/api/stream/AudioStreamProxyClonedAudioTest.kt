package com.tamixa.api.stream

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.ProxyStoragePort
import com.tamixa.application.stream.AudioLinkSigner
import com.tamixa.infrastructure.config.AppProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/** GET /audio/{key} serves cloned-voice audio only with a valid signed link; other narration stays open. */
class AudioStreamProxyClonedAudioTest {

    private val storage = mock<ProxyStoragePort> { on { getObject(any()) } doReturn byteArrayOf(1, 2, 3) }
    private val signer = AudioLinkSigner(AppProperties(audio = AppProperties.AudioProperties(linkSigningSecret = "test-secret")))
    private val mockMvc = MockMvcBuilders.standaloneSetup(AudioStreamProxyController(storage, ObjectMapper(), signer)).build()
    private val clonedKey = "stories/71/ta/cloned_12.mp3"

    @Test
    fun `cloned-voice audio without a signature is refused`() {
        mockMvc.perform(get("/audio/$clonedKey")).andExpect(status().isForbidden)
        mockMvc.perform(get("/audio/$clonedKey?exp=9999999999&sig=forged")).andExpect(status().isForbidden)

        verify(storage, never()).getObject(any())
    }

    @Test
    fun `cloned-voice audio with a valid signed link is served`() {
        mockMvc.perform(get("/audio/$clonedKey?v=1&${signer.signatureQuery(clonedKey)}")).andExpect(status().isOk)
    }

    @Test
    fun `normal library narration needs no signature`() {
        mockMvc.perform(get("/audio/stories/71/ta/v1.mp3")).andExpect(status().isOk)
    }
}
