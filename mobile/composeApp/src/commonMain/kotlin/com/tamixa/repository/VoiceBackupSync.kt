package com.tamixa.repository

import com.tamixa.domain.VoiceDeleteResult
import com.tamixa.domain.VoiceLimitReachedException
import com.tamixa.domain.VoiceProfile
import com.tamixa.domain.uploadedFileName
import com.tamixa.util.TamixaLog
import kotlinx.coroutines.CancellationException

/** Result of backing up one local recording. */
sealed interface VoiceUploadOutcome {
    /** Stored on the server as voice profile [profileId] (also when the server already had this recording). */
    data class Uploaded(val profileId: Long) : VoiceUploadOutcome
    /** The parent already has the maximum number of voices; retrying won't help until one is deleted. */
    data object LimitReached : VoiceUploadOutcome
    /** Offline, timed out, or a server error; safe to retry (the server answers a repeat with the same profile). */
    data class Failed(val cause: Throwable) : VoiceUploadOutcome
}

/** Queued server deletes after a run: entries to retry later, and entries the server kept because a story uses them. */
data class PendingVoiceDeletes(val stillPending: List<String>, val inUse: Set<String>)

/**
 * Server side of the local-first voice backup: uploads recordings and deletes their server copies.
 * Server copies are found by upload file name (the local file name, e.g. "my_voice_1791546447445.m4a") in the
 * caller's own voice list. Ids saved on the device are not trusted for deletes: uploads before this fix saved
 * the cloning job id instead of the voice id, and deleting by it removed the wrong voice or nothing.
 */
class VoiceBackupSync(private val voiceRepository: VoiceRepository) {

    suspend fun upload(bytes: ByteArray, fileName: String): VoiceUploadOutcome =
        voiceRepository.upload(bytes, fileName).fold(
            onSuccess = { VoiceUploadOutcome.Uploaded(it.id) },
            onFailure = {
                if (it is CancellationException) throw it
                if (it is VoiceLimitReachedException) VoiceUploadOutcome.LimitReached else VoiceUploadOutcome.Failed(it)
            }
        )

    /** Upload file names of the parent's voices on the server, or null when the list can't be read (offline). */
    suspend fun serverFileNames(): Set<String>? =
        voiceRepository.listOrThrow()
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()
            ?.mapNotNull { it.uploadedFileName() }
            ?.toSet()

    /**
     * Deletes the server copies for queued [pending] entries ("file:<name>"). Every server voice uploaded under
     * that name is removed (a recording may have been uploaded more than once). A voice that is already gone
     * counts as deleted; network or server errors keep the entry queued. Legacy "id:<n>" entries can't be
     * matched to a recording and are dropped without deleting anything.
     */
    suspend fun deleteServerCopies(pending: List<String>): PendingVoiceDeletes {
        if (pending.isEmpty()) return PendingVoiceDeletes(emptyList(), emptySet())
        val stillPending = mutableListOf<String>()
        val inUse = mutableSetOf<String>()
        var serverVoices: List<VoiceProfile>? = null
        for (entry in pending) {
            val fileName = entry.removePrefix(FILE_PREFIX).takeIf { entry.startsWith(FILE_PREFIX) && it.isNotBlank() }
            if (fileName == null) {
                TamixaLog.w(TAG, "Dropping queued voice delete '$entry': it names no recording, so it can't be verified")
                continue
            }
            val voices = serverVoices ?: voiceRepository.listOrThrow().getOrNull()?.also { serverVoices = it }
            if (voices == null) {
                stillPending += entry
                continue
            }
            val ids = voices.filter { it.uploadedFileName() == fileName }.map { it.id }
            var retry = false
            for (id in ids) {
                voiceRepository.delete(id)
                    .onSuccess { result ->
                        if (result == VoiceDeleteResult.IN_USE) inUse += entry
                        TamixaLog.i(TAG, "Server voice delete id=$id ($fileName): $result")
                    }
                    .onFailure {
                        if (it is CancellationException) throw it
                        retry = true
                        TamixaLog.w(TAG, "Server voice delete id=$id ($fileName) failed; will retry", it)
                    }
            }
            if (retry) stillPending += entry
        }
        return PendingVoiceDeletes(stillPending, inUse)
    }

    companion object {
        private const val TAG = "VoiceBackupSync"
        private const val FILE_PREFIX = "file:"

        /** Queue entry for deleting the server copies of the local recording [fileName]. */
        fun deleteEntry(fileName: String): String = "$FILE_PREFIX$fileName"
    }
}
