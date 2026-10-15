package com.tamixa.repository

import com.tamixa.domain.VoiceProfile
import com.tamixa.domain.uploadedFileName
import com.tamixa.network.VoiceApi
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VoiceBackupSyncTest {

    private fun sync(server: VoiceTestServer) = VoiceBackupSync(VoiceRepository(VoiceApi(server.client)))

    // --- uploads ---

    @Test
    fun uploadReturnsTheVoiceProfileIdFromTheServer() = runTest {
        val server = VoiceTestServer { json(voiceJson(13, "my_voice_1.m4a")) }

        val outcome = sync(server).upload(byteArrayOf(1, 2), "my_voice_1.m4a")

        assertEquals(VoiceUploadOutcome.Uploaded(13), outcome)
    }

    @Test
    fun uploadAtTheVoiceLimitIsReportedAsLimitReached() = runTest {
        val server = VoiceTestServer {
            json("""{"message":"Maximum 5 cloned voices per profile. Delete one to add another.","status":409}""", HttpStatusCode.Conflict)
        }

        assertEquals(VoiceUploadOutcome.LimitReached, sync(server).upload(byteArrayOf(1), "my_voice_6.m4a"))
        assertEquals(1, server.requests.size)
    }

    @Test
    fun uploadIsNotRetriedAutomaticallyOnServerErrors() = runTest {
        val server = VoiceTestServer { respond("", HttpStatusCode.InternalServerError) }

        val outcome = sync(server).upload(byteArrayOf(1), "my_voice_1.m4a")

        assertIs<VoiceUploadOutcome.Failed>(outcome)
        assertEquals(1, server.requestsTo("POST", "/voice/upload").size)
        // Same client, other request: still retried, so only uploads opt out.
        VoiceRepository(VoiceApi(server.client)).listOrThrow()
        assertEquals(4, server.requestsTo("GET", "/voice").size)
    }

    @Test
    fun timedOutUploadFailsOnceAndTheRetryGetsTheSameVoice() = runTest {
        var attempt = 0
        val server = VoiceTestServer { request ->
            attempt++
            // The server stored the first upload, but the app stopped waiting; the repeat is answered with that voice.
            if (attempt == 1) throw HttpRequestTimeoutException(request.url.toString(), 120_000)
            json(voiceJson(13, "my_voice_1.m4a"))
        }
        val sync = sync(server)

        assertIs<VoiceUploadOutcome.Failed>(sync.upload(byteArrayOf(1), "my_voice_1.m4a"))
        assertEquals(1, server.requests.size)
        assertEquals(VoiceUploadOutcome.Uploaded(13), sync.upload(byteArrayOf(1), "my_voice_1.m4a"))
    }

    // --- server check for recordings marked backed up ---

    @Test
    fun serverFileNamesListsTheUploadNamesOfTheParentsVoices() = runTest {
        val server = VoiceTestServer {
            json(
                "[" + listOf(
                    voiceJson(9, "my_voice_1.m4a"),
                    // Older servers send the stored name with its prefix.
                    voiceJson(10, "cloning_1791637796207_ref_my_voice_2.m4a"),
                ).joinToString(",") + "]"
            )
        }

        assertEquals(setOf("my_voice_1.m4a", "my_voice_2.m4a"), sync(server).serverFileNames())
    }

    @Test
    fun serverFileNamesIsNullWhenTheListCantBeRead() = runTest {
        val server = VoiceTestServer { respond("", HttpStatusCode.InternalServerError) }

        // Unknown, not empty: an empty set would re-upload every recording.
        assertEquals(null, sync(server).serverFileNames())
    }

    // --- deletes ---

    @Test
    fun deleteRemovesOnlyServerVoicesUploadedUnderThatFileName() = runTest {
        val server = VoiceTestServer { request ->
            when (request.method.value) {
                "GET" -> json(
                    "[" + listOf(
                        voiceJson(9, "my_voice_1791624645592.m4a"),
                        // Older servers send the stored name with its prefix.
                        voiceJson(10, "cloning_1791637796207_ref_my_voice_1791637793404.m4a"),
                        voiceJson(11, "cloning_1791637860651_ref_my_voice_1791637793404.m4a"),
                        voiceJson(13, "my_voice_1791798008062.m4a")
                    ).joinToString(",") + "]"
                )
                else -> respond("", HttpStatusCode.NoContent)
            }
        }

        val result = sync(server).deleteServerCopies(listOf(VoiceBackupSync.deleteEntry("my_voice_1791637793404.m4a")))

        assertEquals(listOf("/api/v1/voice/10", "/api/v1/voice/11"), server.requestsTo("DELETE", "").map { it.url.encodedPath })
        assertEquals(emptyList(), result.stillPending)
    }

    @Test
    fun legacyIdEntriesAreDroppedWithoutDeletingAnything() = runTest {
        // "id:5" was saved from an upload response that carried the cloning job id, not the voice id.
        val server = VoiceTestServer { json("[${voiceJson(5, "my_voice_other.m4a")}]") }

        val result = sync(server).deleteServerCopies(listOf("id:5"))

        assertTrue(server.requestsTo("DELETE", "").isEmpty())
        assertEquals(emptyList(), result.stillPending)
    }

    @Test
    fun voiceAlreadyGoneOnTheServerCountsAsDeleted() = runTest {
        val server = VoiceTestServer { request ->
            if (request.method.value == "GET") json("[${voiceJson(13, "my_voice_1.m4a")}]")
            else respond("", HttpStatusCode.NotFound)
        }

        val result = sync(server).deleteServerCopies(listOf(VoiceBackupSync.deleteEntry("my_voice_1.m4a")))

        assertEquals(emptyList(), result.stillPending)
    }

    @Test
    fun failedDeleteStaysQueued() = runTest {
        val server = VoiceTestServer { request ->
            if (request.method.value == "GET") json("[${voiceJson(13, "my_voice_1.m4a")}]")
            else respond("", HttpStatusCode.ServiceUnavailable)
        }
        val entry = VoiceBackupSync.deleteEntry("my_voice_1.m4a")

        val result = sync(server).deleteServerCopies(listOf(entry))

        assertEquals(listOf(entry), result.stillPending)
    }

    @Test
    fun deleteWhileTheVoiceListCannotBeLoadedStaysQueued() = runTest {
        val server = VoiceTestServer { respond("", HttpStatusCode.ServiceUnavailable) }
        val entry = VoiceBackupSync.deleteEntry("my_voice_1.m4a")

        val result = sync(server).deleteServerCopies(listOf(entry))

        assertEquals(listOf(entry), result.stillPending)
        assertTrue(server.requestsTo("DELETE", "").isEmpty())
    }

    @Test
    fun voiceUsedByAStoryIsReportedInUse() = runTest {
        val server = VoiceTestServer { request ->
            if (request.method.value == "GET") json("[${voiceJson(13, "my_voice_1.m4a")}]")
            else json("""{"message":"This voice is used by a story and can't be deleted","voiceProfileId":13}""", HttpStatusCode.Conflict)
        }
        val entry = VoiceBackupSync.deleteEntry("my_voice_1.m4a")

        val result = sync(server).deleteServerCopies(listOf(entry))

        assertEquals(setOf(entry), result.inUse)
        assertEquals(emptyList(), result.stillPending)
    }

    @Test
    fun uploadedFileNameDropsTheServerPrefix() {
        fun voice(name: String?) = VoiceProfile(id = 1, parentId = 8, createdAt = "", referenceFileName = name)
        assertEquals("my_voice_1.m4a", voice("cloning_1791637796207_ref_my_voice_1.m4a").uploadedFileName())
        assertEquals("my_voice_1.m4a", voice("my_voice_1.m4a").uploadedFileName())
        assertEquals(null, voice(null).uploadedFileName())
    }
}
