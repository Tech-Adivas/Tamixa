package com.tamixa.application.voice

import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.VoiceCloningJobRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import com.tamixa.domain.Parent
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceCloningStatus
import com.tamixa.domain.VoiceProfile
import com.tamixa.infrastructure.config.AppProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Upload → profile → cloning job, with in-memory ports. The ElevenLabs port is a mock: these tests do not call
 * the real ElevenLabs API.
 */
class VoiceCloningServiceUploadTest {

    private val parentId = 8L
    private val email = "parent@test.com"

    private lateinit var profiles: FakeVoiceRepository
    private lateinit var jobs: FakeJobRepository
    private lateinit var storage: FakeReferenceStorage
    private lateinit var elevenLabs: ElevenLabsVoiceCloningPort
    private lateinit var service: VoiceCloningService

    @BeforeEach
    fun setUp() {
        profiles = FakeVoiceRepository()
        jobs = FakeJobRepository()
        storage = FakeReferenceStorage()
        elevenLabs = mock()
        val parent = mock<Parent> { on { id } doReturn parentId }
        val parents = mock<ParentRepositoryPort> { on { findByEmail(email) } doReturn parent }
        val props = AppProperties(
            voiceCloning = AppProperties.VoiceCloningProperties(enabled = true, provider = "elevenlabs", elevenLabsApiKey = "test-key")
        )
        service = VoiceCloningService(
            voiceCloningJobRepository = jobs,
            parentRepository = parents,
            voiceRepository = profiles,
            voiceReferenceStorage = storage,
            elevenLabsAdapter = elevenLabs,
            fishAudioAdapter = null,
            googleCloudAdapter = null,
            appProperties = props,
            voiceCloneAnalytics = null
        )
    }

    @Test
    fun `upload returns the persistent profile, not the cloning job`() {
        jobs.nextId = 100 // job ids and profile ids come from different sequences
        val upload = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")

        assertThat(upload.profile.id).isEqualTo(profiles.findByParentId(parentId).single().id)
        assertThat(upload.profile.id).isNotEqualTo(upload.job!!.id)
        assertThat(upload.profile.referenceAudioPath).isEqualTo(upload.job!!.audioStoragePath)
        assertThat(upload.job!!.status).isEqualTo(VoiceCloningStatus.PENDING)
        assertThat(originalReferenceFileName(upload.profile.referenceAudioPath)).isEqualTo("my_voice_1.m4a")
    }

    @Test
    fun `cloning job attaches the ElevenLabs voice to the uploaded profile instead of creating another`() {
        whenever(elevenLabs.addVoice(any(), any(), any())).thenReturn("el-voice-1")
        whenever(elevenLabs.synthesize(any(), any(), any())).thenReturn(bytes("mp3"))
        val upload = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")

        service.processVoiceCloningJob(upload.job!!.id)

        val all = profiles.findByParentId(parentId)
        assertThat(all).hasSize(1)
        assertThat(all.single().id).isEqualTo(upload.profile.id)
        assertThat(all.single().elevenlabsVoiceId).isEqualTo("el-voice-1")
        assertThat(jobs.findById(upload.job!!.id)!!.status).isEqualTo(VoiceCloningStatus.READY)
    }

    @Test
    fun `failed cloning keeps the uploaded profile with its recording`() {
        whenever(elevenLabs.addVoice(any(), any(), any())).thenReturn(null)
        val upload = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")

        service.processVoiceCloningJob(upload.job!!.id)

        assertThat(jobs.findById(upload.job!!.id)!!.status).isEqualTo(VoiceCloningStatus.FAILED)
        val kept = profiles.findByParentId(parentId).single()
        assertThat(kept.id).isEqualTo(upload.profile.id)
        assertThat(kept.elevenlabsVoiceId).isNull()
        assertThat(kept.referenceAudioPath).isEqualTo(upload.profile.referenceAudioPath)
    }

    @Test
    fun `cloning sends the recording to ElevenLabs with its real file type`() {
        whenever(elevenLabs.addVoice(any(), any(), any())).thenReturn("el-voice-1")
        whenever(elevenLabs.synthesize(any(), any(), any())).thenReturn(bytes("mp3"))
        val upload = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")

        service.processVoiceCloningJob(upload.job!!.id)

        verify(elevenLabs).addVoice(any(), eq("voice_my_voice_1.m4a"), eq("my_voice_1"))
    }

    @Test
    fun `voice that can't speak after cloning is deleted at ElevenLabs`() {
        whenever(elevenLabs.addVoice(any(), any(), any())).thenReturn("el-voice-1")
        whenever(elevenLabs.synthesize(any(), any(), any())).thenReturn(null)
        val upload = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")

        service.processVoiceCloningJob(upload.job!!.id)

        assertThat(jobs.findById(upload.job!!.id)!!.status).isEqualTo(VoiceCloningStatus.FAILED)
        verify(elevenLabs).deleteVoice("el-voice-1")
    }

    @Test
    fun `deleting a voice removes its cloning jobs and consent recording, not other voices' jobs`() {
        val upload = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a", consentBytes = bytes("consent"))
        val other = service.uploadVoice(email, bytes("b"), "my_voice_2.m4a")
        val consentPath = upload.job!!.consentAudioStoragePath!!
        assertThat(storage.getReferenceAudio(consentPath)).isNotNull()

        service.deleteReferenceVoiceProfileByParentId(parentId, upload.profile.id)

        assertThat(jobs.findByParentId(parentId).map { it.id }).containsExactly(other.job!!.id)
        assertThat(storage.getReferenceAudio(consentPath)).isNull()
        assertThat(storage.getReferenceAudio(upload.profile.referenceAudioPath!!)).isNull()
        assertThat(storage.getReferenceAudio(other.profile.referenceAudioPath!!)).isNotNull()
    }

    @Test
    fun `voice deleted while it is being cloned stays deleted and its new ElevenLabs voice is removed`() {
        val upload = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")
        whenever(elevenLabs.addVoice(any(), any(), any())).thenAnswer {
            // The parent deletes the voice while ElevenLabs is still creating the clone.
            service.deleteReferenceVoiceProfileByParentId(parentId, upload.profile.id)
            "el-voice-1"
        }
        whenever(elevenLabs.synthesize(any(), any(), any())).thenReturn(bytes("mp3"))

        service.processVoiceCloningJob(upload.job!!.id)

        assertThat(profiles.findByParentId(parentId)).isEmpty()
        verify(elevenLabs).deleteVoice("el-voice-1")
    }

    @Test
    fun `deleting a profile deletes its ElevenLabs voice and recording`() {
        val profile = profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = "el-voice-1"))
        storage.storeReferenceAudio(parentId, "a.m4a", bytes("a"))

        service.deleteReferenceVoiceProfileByParentId(parentId, profile.id)

        verify(elevenLabs).deleteVoice("el-voice-1")
        assertThat(storage.getReferenceAudio("voices/8/a.m4a")).isNull()
        assertThat(profiles.findByParentId(parentId)).isEmpty()
    }

    @Test
    fun `deleting a voice also removes the story narrations made with it`() {
        val media = mock<ClonedVoiceMediaService>()
        whenever(media.detachMedia(listOf(13L))).thenReturn(listOf("stories/71/ta/cloned_13.mp3"))
        val withMedia = VoiceCloningService(
            voiceCloningJobRepository = jobs, parentRepository = mock(), voiceRepository = profiles,
            voiceReferenceStorage = storage, elevenLabsAdapter = elevenLabs, fishAudioAdapter = null, googleCloudAdapter = null,
            appProperties = AppProperties(), voiceCloneAnalytics = null, clonedVoiceMedia = media
        )
        profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = null)) // id 9
        repeat(3) { profiles.save(clonedProfile(path = "voices/8/x$it.m4a", voiceId = null)) } // ids 10-12
        val profile = profiles.save(clonedProfile(path = "voices/8/b.m4a", voiceId = null))
        assertThat(profile.id).isEqualTo(13L)

        withMedia.deleteReferenceVoiceProfileByParentId(parentId, profile.id)

        verify(media).detachMedia(listOf(13L))
        verify(media).deleteFiles(listOf("stories/71/ta/cloned_13.mp3"))
        verify(media).reconcileOrphansAsync()
    }

    @Test
    fun `ElevenLabs voice still used by another profile is kept`() {
        val profile = profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = "el-voice-1"))
        profiles.save(clonedProfile(path = "voices/8/b.m4a", voiceId = "el-voice-1"))

        service.deleteReferenceVoiceProfileByParentId(parentId, profile.id)

        verify(elevenLabs, never()).deleteVoice(any())
    }

    @Test
    fun `clone request for an uncloned recording creates one job`() {
        profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = null))

        val job = service.requestCloneOfRecording(parentId, "voices/8/a.m4a", null, 3, "a")

        assertThat(job.status).isEqualTo(VoiceCloningStatus.PENDING)
        assertThat(jobs.findByParentId(parentId)).hasSize(1)
    }

    @Test
    fun `clone request for an already cloned recording is refused`() {
        profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = "el-voice-1"))

        assertThatThrownBy { service.requestCloneOfRecording(parentId, "voices/8/a.m4a", null, 3, "a") }
            .isInstanceOf(VoiceAlreadyClonedException::class.java)
        assertThat(jobs.findByParentId(parentId)).isEmpty()
    }

    @Test
    fun `second clone request while the first is in progress is refused`() {
        profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = null))
        service.requestCloneOfRecording(parentId, "voices/8/a.m4a", null, 3, "a")

        assertThatThrownBy { service.requestCloneOfRecording(parentId, "voices/8/a.m4a", null, 3, "a") }
            .isInstanceOf(VoiceAlreadyClonedException::class.java)
        assertThat(jobs.findByParentId(parentId)).hasSize(1)
    }

    @Test
    fun `clone request for a file that is not one of the parent's voices is refused`() {
        assertThatThrownBy { service.requestCloneOfRecording(parentId, "voices/8/consent.m4a", null, 3, "a") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(jobs.findByParentId(parentId)).isEmpty()
    }

    @Test
    fun `re-cloning a profile deletes the voice it replaces at ElevenLabs`() {
        val profile = profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = "el-old"))
        storage.storeReferenceAudio(parentId, "a.m4a", bytes("a"))
        val job = service.createVoiceCloningJob(parentId, "voices/8/a.m4a", null, 1, "a")
        whenever(elevenLabs.addVoice(any(), any(), any())).thenReturn("el-new")
        whenever(elevenLabs.synthesize(any(), any(), any())).thenReturn(bytes("mp3"))

        service.processVoiceCloningJob(job.id)

        assertThat(profiles.findById(profile.id)!!.elevenlabsVoiceId).isEqualTo("el-new")
        verify(elevenLabs).deleteVoice("el-old")
        verify(elevenLabs, never()).deleteVoice("el-new")
    }

    @Test
    fun `account deletion purges every recording and provider voice of the parent`() {
        profiles.save(clonedProfile(path = "voices/8/a.m4a", voiceId = "el-1"))
        storage.storeReferenceAudio(parentId, "a.m4a", bytes("a"))
        storage.storeReferenceAudio(parentId, "consent.m4a", bytes("c"))
        jobs.save(
            VoiceCloningJob(
                id = 0, parentId = parentId, audioStoragePath = "voices/8/old.m4a",
                consentAudioStoragePath = "voices/8/consent.m4a", audioFileSizeBytes = 1, voiceName = "old",
                elevenLabsVoiceId = "el-orphan", status = VoiceCloningStatus.READY, errorMessage = null,
                createdAt = Instant.now(), completedAt = null
            )
        )
        whenever(elevenLabs.deleteVoice(any())).thenReturn(true)

        val data = service.collectVoiceData(parentId)
        service.purgeVoiceData(parentId, data)

        assertThat(data.storagePaths).containsExactlyInAnyOrder("voices/8/a.m4a", "voices/8/old.m4a", "voices/8/consent.m4a")
        assertThat(storage.getReferenceAudio("voices/8/a.m4a")).isNull()
        assertThat(storage.getReferenceAudio("voices/8/consent.m4a")).isNull()
        verify(elevenLabs).deleteVoice("el-1")
        verify(elevenLabs).deleteVoice("el-orphan")
    }

    @Test
    fun `reference audio extension falls back to mp3`() {
        assertThat(referenceAudioExtension("voices/8/cloning_1_ref_my_voice.M4A")).isEqualTo("m4a")
        assertThat(referenceAudioExtension("voices/8/voice_audio")).isEqualTo("mp3")
        assertThat(referenceAudioExtension(null)).isEqualTo("mp3")
    }

    @Test
    fun `retried upload of the same recording returns the same profile and creates nothing`() {
        val first = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")
        val retry = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")

        assertThat(retry.profile.id).isEqualTo(first.profile.id)
        assertThat(retry.job).isNull()
        assertThat(profiles.findByParentId(parentId)).hasSize(1)
        assertThat(jobs.findByParentId(parentId)).hasSize(1)
        assertThat(storage.storeCount).isEqualTo(1)
    }

    @Test
    fun `retry after a client timeout returns the profile even when the parent is at the voice limit`() {
        // The first request finished on the server, but the client gave up waiting and will send it again.
        val first = service.uploadVoice(email, bytes("timed-out"), "my_voice_5.m4a")
        repeat(4) { service.uploadVoice(email, bytes("other-$it"), "my_voice_other_$it.m4a") }
        assertThat(profiles.findByParentId(parentId)).hasSize(VoiceCloningService.MAX_CLONED_VOICES_PER_PARENT)

        val retry = service.uploadVoice(email, bytes("timed-out"), "my_voice_5.m4a")

        assertThat(retry.profile.id).isEqualTo(first.profile.id)
        assertThat(retry.job).isNull()
        assertThat(profiles.findByParentId(parentId)).hasSize(VoiceCloningService.MAX_CLONED_VOICES_PER_PARENT)
    }

    @Test
    fun `overlapping uploads of the same recording create one profile`() {
        storage.storeDelayMillis = 200 // like a slow S3 upload: the retry arrives while the first is still storing
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val calls = (1..2).map {
                pool.submit<VoiceUploadResult> {
                    start.await()
                    service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")
                }
            }
            start.countDown()
            val results = calls.map { it.get(5, TimeUnit.SECONDS) }

            assertThat(results.map { it.profile.id }.toSet()).hasSize(1)
            assertThat(results.count { it.job != null }).isEqualTo(1)
            assertThat(profiles.findByParentId(parentId)).hasSize(1)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `same file name with different audio is a new recording`() {
        val first = service.uploadVoice(email, bytes("a"), "voice_audio")
        val second = service.uploadVoice(email, bytes("b"), "voice_audio")

        assertThat(second.profile.id).isNotEqualTo(first.profile.id)
        assertThat(second.job).isNotNull()
        assertThat(profiles.findByParentId(parentId)).hasSize(2)
    }

    @Test
    fun `duplicate check falls back to the job's file size when the stored file can't be read`() {
        val first = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")
        storage.unreadable = true

        val retry = service.uploadVoice(email, bytes("a"), "my_voice_1.m4a")

        assertThat(retry.profile.id).isEqualTo(first.profile.id)
        assertThat(profiles.findByParentId(parentId)).hasSize(1)
    }

    @Test
    fun `new recording at the voice limit is refused without storing anything`() {
        repeat(VoiceCloningService.MAX_CLONED_VOICES_PER_PARENT) { service.uploadVoice(email, bytes("v$it"), "my_voice_$it.m4a") }
        val storedBefore = storage.storeCount

        assertThatThrownBy { service.uploadVoice(email, bytes("new"), "my_voice_new.m4a") }
            .isInstanceOf(MaxClonedVoicesReachedException::class.java)
        assertThat(storage.storeCount).isEqualTo(storedBefore)
        assertThat(profiles.findByParentId(parentId)).hasSize(VoiceCloningService.MAX_CLONED_VOICES_PER_PARENT)
    }

    @Test
    fun `reference-only upload is also idempotent`() {
        val first = service.createReferenceVoiceProfile(email, bytes("a"), "recorded_voice_1.m4a")
        val retry = service.createReferenceVoiceProfile(email, bytes("a"), "recorded_voice_1.m4a")

        assertThat(retry.id).isEqualTo(first.id)
        assertThat(profiles.findByParentId(parentId)).hasSize(1)
        verify(elevenLabs, never()).addVoice(any(), any(), any())
    }

    @Test
    fun `original file name strips the storage prefix`() {
        assertThat(originalReferenceFileName("voices/8/cloning_1791637796207_ref_my_voice_1791637793404.m4a"))
            .isEqualTo("my_voice_1791637793404.m4a")
        assertThat(originalReferenceFileName("voices/18/dharshik-voice_final.mp3")).isEqualTo("dharshik-voice_final.mp3")
        assertThat(originalReferenceFileName(null)).isNull()
        assertThat(originalReferenceFileName("voices/8/")).isNull()
    }

    private fun bytes(s: String) = s.toByteArray()

    private fun clonedProfile(path: String, voiceId: String?) = VoiceProfile(
        id = 0,
        parentId = parentId,
        encryptedEmbedding = ByteArray(0),
        createdAt = Instant.now(),
        elevenlabsVoiceId = voiceId,
        referenceAudioPath = path
    )

    private class FakeVoiceRepository : VoiceRepositoryPort {
        private val rows = linkedMapOf<Long, VoiceProfile>()
        private var nextId = 9L
        @Synchronized override fun save(voiceProfile: VoiceProfile): VoiceProfile {
            val saved = if (voiceProfile.id == 0L) voiceProfile.copy(id = nextId++) else voiceProfile
            rows[saved.id] = saved
            return saved
        }
        @Synchronized override fun findById(id: Long) = rows[id]
        @Synchronized override fun findByParentId(parentId: Long) = rows.values.filter { it.parentId == parentId }
        @Synchronized override fun findByIdAndParentId(id: Long, parentId: Long) = rows[id]?.takeIf { it.parentId == parentId }
        @Synchronized override fun deleteById(id: Long) { rows.remove(id) }
    }

    private class FakeJobRepository : VoiceCloningJobRepositoryPort {
        private val rows = linkedMapOf<Long, VoiceCloningJob>()
        var nextId = 2L
        @Synchronized override fun save(job: VoiceCloningJob): VoiceCloningJob {
            val saved = if (job.id == 0L) job.copy(id = nextId++) else job
            rows[saved.id] = saved
            return saved
        }
        @Synchronized override fun findById(id: Long) = rows[id]
        @Synchronized override fun findByParentId(parentId: Long) = rows.values.filter { it.parentId == parentId }
        @Synchronized override fun findByStatus(status: VoiceCloningStatus) = rows.values.filter { it.status == status }
        @Synchronized override fun deleteAllById(ids: Collection<Long>) { ids.forEach { rows.remove(it) } }
        @Synchronized override fun updateStatus(
            id: Long,
            status: VoiceCloningStatus,
            errorMessage: String?,
            elevenLabsVoiceId: String?,
            googleVoiceCloningKey: String?,
            fishAudioModelId: String?
        ) {
            val job = rows[id] ?: return
            rows[id] = job.copy(
                status = status,
                errorMessage = errorMessage,
                elevenLabsVoiceId = elevenLabsVoiceId ?: job.elevenLabsVoiceId,
                completedAt = if (status == VoiceCloningStatus.READY || status == VoiceCloningStatus.FAILED) Instant.now() else null
            )
        }
    }

    private class FakeReferenceStorage : VoiceReferenceStoragePort {
        private val files = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
        private val counter = java.util.concurrent.atomic.AtomicInteger()
        val storeCount get() = counter.get()
        @Volatile var storeDelayMillis = 0L
        @Volatile var unreadable = false
        override fun storeReferenceAudio(parentId: Long, fileName: String, bytes: ByteArray): String {
            if (storeDelayMillis > 0) Thread.sleep(storeDelayMillis)
            val path = "voices/$parentId/${fileName.replace(Regex("^cloning_\\d+_"), "cloning_${System.nanoTime()}_")}"
            files[path] = bytes
            counter.incrementAndGet()
            return path
        }
        override fun getReferenceAudio(path: String): ByteArray? = if (unreadable) null else files[path]
        override fun deleteReferenceAudio(path: String?) { if (path != null) files.remove(path) }
    }
}
