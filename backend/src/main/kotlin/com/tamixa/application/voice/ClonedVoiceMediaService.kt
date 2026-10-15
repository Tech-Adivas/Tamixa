package com.tamixa.application.voice

import com.tamixa.application.narration.AudioStorageService
import com.tamixa.application.port.StoryVoicePreferenceRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Story audio narrated in a parent's cloned voice ("cloned:{profileId}", stored as stories/.../cloned_{profileId}.mp3)
 * is personal data: it must go when the voice goes.
 *
 * - [detachMedia]: in the voice-delete transaction, removes the narration rows and "play in my voice" settings and
 *   returns the files to delete once it commits ([deleteFiles], retried).
 * - [reconcileOrphans]: sweeps rows and stored files of voices that no longer exist (row-less files, failed deletes,
 *   anything left from before this cleanup existed). Runs at startup, daily, and after each voice delete.
 */
@Service
class ClonedVoiceMediaService(
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val voicePreferenceRepository: StoryVoicePreferenceRepositoryPort,
    private val voiceRepository: VoiceRepositoryPort,
    @Autowired(required = false) private val audioStorage: AudioStorageService?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Removes narration rows and voice settings of [profileIds]; returns their stored files for [deleteFiles]. */
    @Transactional
    fun detachMedia(profileIds: Collection<Long>): List<String> {
        if (profileIds.isEmpty()) return emptyList()
        val voiceProfiles = profileIds.map { "$CLONED_PREFIX$it" }
        val rows = narrationAudioRepository.findAllByVoiceProfileIn(voiceProfiles)
        narrationAudioRepository.deleteAllById(rows.map { it.id })
        voicePreferenceRepository.deleteByVoiceProfileIn(voiceProfiles)
        log.info("Cloned voice media detached profileIds={} narrationRows={}", profileIds, rows.size)
        return rows.map { it.audioUrl }.filter { it.startsWith("stories/") }.distinct()
    }

    /** Deletes stored files, retrying each a few times. Returns how many could not be deleted (the sweep retries those). */
    fun deleteFiles(keys: Collection<String>): Int {
        val storage = audioStorage ?: return 0
        var failed = 0
        keys.forEach { key ->
            val deleted = (1..DELETE_ATTEMPTS).any { attempt ->
                storage.deleteNarrationAudio(key).also { ok ->
                    if (!ok && attempt < DELETE_ATTEMPTS) Thread.sleep(RETRY_DELAY_MS * attempt)
                }
            }
            if (!deleted) {
                failed++
                log.error("Cloned voice audio not deleted after {} attempts key={} (next cleanup sweep retries)", DELETE_ATTEMPTS, key)
            }
        }
        return failed
    }

    /** Runs the sweep in the background, e.g. right after a voice was deleted, to catch files without a narration row. */
    @Async
    fun reconcileOrphansAsync() = runSweep("after voice delete")

    @EventListener(ApplicationReadyEvent::class)
    @Async
    fun reconcileOnStartup() = runSweep("startup")

    @Scheduled(cron = "\${app.voice-cloning.media-cleanup-cron:0 30 3 * * *}")
    @SchedulerLock(name = "ClonedVoiceMediaService.reconcileOrphans", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    fun reconcileDaily() = runSweep("daily")

    private fun runSweep(trigger: String) {
        try {
            reconcileOrphans()
        } catch (e: Exception) {
            log.error("Cloned voice media cleanup failed trigger={}", trigger, e)
        }
    }

    /**
     * Deletes narration rows, "play in my voice" settings and stored cloned_{id} files whose voice profile no longer exists.
     * @return number of rows + files removed
     */
    fun reconcileOrphans(): Int {
        val exists = HashMap<Long, Boolean>()
        fun isOrphan(profileId: Long) = !exists.getOrPut(profileId) { voiceRepository.findById(profileId) != null }

        fun orphanId(voiceProfile: String) =
            voiceProfile.removePrefix(CLONED_PREFIX).trim().toLongOrNull()?.takeIf(::isOrphan)

        val orphanRows = narrationAudioRepository.findAllByVoiceProfileStartingWith(CLONED_PREFIX)
            .filter { orphanId(it.voiceProfile) != null }
        val orphanIds = (orphanRows.map { it.voiceProfile } +
            voicePreferenceRepository.findDistinctVoiceProfilesStartingWith(CLONED_PREFIX))
            .mapNotNull(::orphanId).toSet()
        val rowFiles = detachMedia(orphanIds)
        val orphanFiles = audioStorage?.listNarrationKeys()
            ?.filter { key -> CLONED_FILE.matchEntire(key.substringAfterLast('/'))?.groupValues?.get(1)?.toLongOrNull()?.let(::isOrphan) ?: false }
            ?.toList()
            .orEmpty()
        val failed = deleteFiles((rowFiles + orphanFiles).distinct())
        log.info(
            "Cloned voice media cleanup: orphanVoices={} orphanRows={} orphanFiles={} failedFiles={}",
            orphanIds.size, orphanRows.size, orphanFiles.size, failed
        )
        return orphanRows.size + orphanFiles.size - failed
    }

    companion object {
        private const val CLONED_PREFIX = "cloned:"
        /**
         * File name of cloned-voice narration: cloned_{profileId}.mp3 (see S3NarrationAudioStorageAdapter's slug), or
         * an interactive segment in that voice: cloned_{profileId}_seg_{segment}_{hash}.mp3 (InteractiveSegmentVoiceService).
         */
        private val CLONED_FILE = Regex("""cloned_(\d+)(?:_seg_[A-Za-z0-9_-]+)?\.[a-z0-9]+""")
        private const val DELETE_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 500L
    }
}
