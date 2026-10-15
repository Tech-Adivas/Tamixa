package com.tamixa.ui.viewmodel

import com.tamixa.network.VoiceApi
import com.tamixa.repository.VoiceRepository
import com.tamixa.repository.VoiceTestServer
import com.tamixa.repository.json
import com.tamixa.ui.AppMessageNotifier
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Voice cloning consent: read from GET /consent, recorded with POST /consent before the first recording. */
class VoiceViewModelConsentTest {

    @Test
    fun earlierConsentIsLoaded() = runTest {
        val server = VoiceTestServer {
            json("""[{"consentType":"terms","version":1,"grantedAt":"x"},{"consentType":"voice_cloning","version":1,"grantedAt":"x"}]""")
        }
        val vm = VoiceViewModel(VoiceRepository(VoiceApi(server.client)), AppMessageNotifier(), backgroundScope)

        vm.loadVoiceConsent()

        assertTrue(vm.hasVoiceConsent.first { it })
    }

    @Test
    fun givingConsentPostsItAndUpdatesTheState() = runTest {
        val server = VoiceTestServer { respond("", HttpStatusCode.OK) }
        val vm = VoiceViewModel(VoiceRepository(VoiceApi(server.client)), AppMessageNotifier(), backgroundScope)

        assertTrue(vm.giveVoiceConsent())

        assertTrue(vm.hasVoiceConsent.value)
        val body = (server.requestsTo("POST", "/consent").single().body as TextContent).text
        assertEquals("""{"consentType":"voice_cloning","version":1}""", body)
    }

    @Test
    fun failedConsentKeepsAskingAndShowsTheError() = runTest {
        val server = VoiceTestServer { json("""{"message":"bad"}""", HttpStatusCode.BadRequest) }
        val notifier = AppMessageNotifier()
        val vm = VoiceViewModel(VoiceRepository(VoiceApi(server.client)), notifier, backgroundScope)

        assertFalse(vm.giveVoiceConsent())

        assertFalse(vm.hasVoiceConsent.value)
        assertTrue(notifier.showErrorDialog.value)
    }
}
