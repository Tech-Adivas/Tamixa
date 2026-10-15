package com.tamixa.ui.viewmodel

import com.tamixa.network.VoiceApi
import com.tamixa.repository.VoiceRepository
import com.tamixa.repository.VoiceTestServer
import com.tamixa.repository.json
import com.tamixa.repository.voiceJson
import com.tamixa.ui.AppMessageNotifier
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/** Premium Voice Upload screen actions: one upload per tap sequence, limit message, safe delete. */
class VoiceViewModelPremiumTest {

    @Test
    fun secondTapWhileUploadingSendsNothing() = runTest {
        val release = CompletableDeferred<Unit>()
        val server = VoiceTestServer {
            release.await()
            json(voiceJson(13, "recorded_voice_1.m4a"))
        }
        val vm = VoiceViewModel(VoiceRepository(VoiceApi(server.client)), AppMessageNotifier(), backgroundScope)

        vm.uploadVoice(byteArrayOf(1), "recorded_voice_1.m4a")
        vm.uploadVoice(byteArrayOf(1), "recorded_voice_1.m4a")
        release.complete(Unit)
        val done = vm.uploadState.first { it !is UiState.Loading }

        assertEquals(13L, assertIs<UiState.Success<*>>(done).let { (it.data as com.tamixa.domain.VoiceProfile).id })
        assertEquals(1, server.requestsTo("POST", "/voice/upload").size)
    }

    @Test
    fun uploadAtTheLimitShowsTheLimitMessageInsteadOfTheErrorDialog() = runTest {
        val server = VoiceTestServer {
            json("""{"message":"Maximum 5 cloned voices per profile.","status":409}""", HttpStatusCode.Conflict)
        }
        val notifier = AppMessageNotifier()
        val vm = VoiceViewModel(VoiceRepository(VoiceApi(server.client)), notifier, backgroundScope)

        vm.uploadVoice(byteArrayOf(1), "recorded_voice_1.m4a")
        val done = vm.uploadState.first { it !is UiState.Loading }

        assertEquals(Strings.voiceLimitReached(), assertIs<UiState.Error>(done).message)
        assertFalse(notifier.showErrorDialog.value)
    }

    @Test
    fun deletingAVoiceUsedByAStoryKeepsItAndSaysWhy() = runTest {
        val server = VoiceTestServer { request ->
            if (request.method.value == "DELETE") json("""{"message":"in use","voiceProfileId":13}""", HttpStatusCode.Conflict)
            else json("[${voiceJson(13, "recorded_voice_1.m4a")}]")
        }
        val notifier = AppMessageNotifier()
        val vm = VoiceViewModel(VoiceRepository(VoiceApi(server.client)), notifier, backgroundScope)

        vm.deleteProfile(13)
        val profiles = vm.profiles.first { it is UiState.Success && it.data.isNotEmpty() }

        assertEquals(Strings.voiceInUseCannotDelete(), notifier.message.value?.text)
        assertEquals(listOf(13L), assertIs<UiState.Success<List<com.tamixa.domain.VoiceProfile>>>(profiles).data.map { it.id })
    }

    @Test
    fun deleteDoubleTapSendsOneRequestAndReloadsTheList() = runTest {
        val release = CompletableDeferred<Unit>()
        var listed = 0
        val server = VoiceTestServer { request ->
            if (request.method.value == "DELETE") {
                release.await()
                respond("", HttpStatusCode.NoContent)
            } else {
                listed++
                json("[]")
            }
        }
        val notifier = AppMessageNotifier()
        val vm = VoiceViewModel(VoiceRepository(VoiceApi(server.client)), notifier, backgroundScope)

        vm.deleteProfile(13)
        vm.deleteProfile(13)
        assertEquals(setOf(13L), vm.deletingProfileIds.value)
        release.complete(Unit)
        vm.deletingProfileIds.first { it.isEmpty() }
        vm.profiles.first { it is UiState.Success && listed > 0 }

        assertEquals(1, server.requestsTo("DELETE", "/voice/13").size)
        assertEquals(Strings.voiceDeleted(), notifier.message.value?.text)
    }
}
