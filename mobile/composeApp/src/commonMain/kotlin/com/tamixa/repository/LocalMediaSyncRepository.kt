package com.tamixa.repository

import com.tamixa.application.port.PreferencesPort
import com.tamixa.domain.VoiceDeleteResult
import com.tamixa.domain.uploadedFileName
import com.tamixa.platform.LocalVoiceRecording
import com.tamixa.platform.deleteLocalVoiceRecording
import com.tamixa.platform.getActiveLocalAvatarImageUri
import com.tamixa.platform.listLocalAvatarImages
import com.tamixa.platform.listLocalVoiceRecordings
import com.tamixa.platform.markLocalMediaBackedUp
import com.tamixa.platform.readLocalAvatarForUpload
import com.tamixa.platform.readLocalMediaBytes
import com.tamixa.platform.takeDeletedBackedUpVoiceRecordings
import com.tamixa.util.TamixaLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What happened when a recording was deleted. */
enum class RecordingDeleteOutcome {
    /** Deleted on the device and on the server (or it was never uploaded). */
    DELETED_EVERYWHERE,
    /** Deleted on the device; it was never backed up, and a possible server copy is deleted when back online. */
    CLOUD_DELETE_PENDING,
    /** Nothing was deleted: a generated story uses this voice, so the server refused and the device copy is kept. */
    IN_USE,
    /** Nothing was deleted: the recording is backed up and the server can't be reached to check whether it's in use. */
    OFFLINE,
    /** Nothing was deleted (the file couldn't be removed). */
    FAILED
}

/** What [LocalMediaSyncRepository.syncPending] could not finish. */
data class LocalMediaSyncResult(
    /** A recording stayed on the device only because the parent already has the maximum number of voices. */
    val voiceLimitReached: Boolean = false,
    /** Recordings uploaded in this run; when > 0 the server's voice list changed (e.g. the player should reload it). */
    val voicesUploaded: Int = 0
)

/**
 * Local-first backup of voice recordings and the profile photo: they are saved on the device first
 * (see PlatformApi saveLocalVoiceRecording / saveLocalAvatarImage), then uploaded here to the backend,
 * which stores them in S3. Anything that fails stays on the device and is retried on the next [syncPending]
 * (a repeat upload of a recording the server already has returns the same voice, so retries don't duplicate it).
 * Deleting a recording removes it on the device and on the server (queued while offline); see [VoiceBackupSync].
 */
class LocalMediaSyncRepository(
    private val voiceRepository: VoiceRepository,
    private val avatarRepository: AvatarRepository,
    private val preferences: PreferencesPort
) {
    private val voiceBackup = VoiceBackupSync(voiceRepository)

    // One sync at a time, so a recording is never uploaded twice by overlapping calls.
    private val mutex = Mutex()

    suspend fun syncPending(): LocalMediaSyncResult = mutex.withLock {
        // Recordings deleted on the device by an older version (which kept the cloud copy) are deleted in the cloud too.
        val deletedEarlier = takeDeletedBackedUpVoiceRecordings()
        if (deletedEarlier.isNotEmpty()) {
            TamixaLog.i("LocalMediaSync", "Queueing cloud delete for ${deletedEarlier.size} recording(s) already deleted on the device")
            savePendingVoiceDeletes(readPendingVoiceDeletes() + deletedEarlier.map { (name, _) -> VoiceBackupSync.deleteEntry(name) })
        }
        // Deletes first, so a freed slot (max voices per parent) is available to pending uploads.
        processPendingVoiceDeletes()
        val voices = syncVoiceRecordings()
        syncActiveAvatar()
        LocalMediaSyncResult(voiceLimitReached = voices.limitReached, voicesUploaded = voices.uploaded)
    }

    /**
     * Deletes [recording]'s copies on the server, then the recording on the device, so both always match: when the
     * server refuses because a generated story uses the voice, nothing is deleted. A backed-up recording is only
     * deleted while the server is reachable (offline it can't be checked). One that was never backed up is deleted
     * right away and its server copy, if an upload finished after the app stopped waiting, is queued for deletion.
     * Server copies are looked up by file name.
     */
    suspend fun deleteVoiceRecording(recording: LocalVoiceRecording): RecordingDeleteOutcome = mutex.withLock {
        val entry = VoiceBackupSync.deleteEntry(recording.uri.substringAfterLast('/'))
        savePendingVoiceDeletes(readPendingVoiceDeletes() + entry)
        val inUse = processPendingVoiceDeletes()
        val serverDeletePending = entry in readPendingVoiceDeletes()
        when {
            entry in inUse -> {
                TamixaLog.i("LocalMediaSync", "Recording not deleted: a story uses this voice")
                RecordingDeleteOutcome.IN_USE
            }
            serverDeletePending && recording.backedUp -> {
                // Not deleted anywhere; drop the queued delete so it can't remove the server copy later on its own.
                savePendingVoiceDeletes(readPendingVoiceDeletes() - entry)
                TamixaLog.i("LocalMediaSync", "Recording not deleted: server unreachable")
                RecordingDeleteOutcome.OFFLINE
            }
            !deleteLocalVoiceRecording(recording.uri) -> {
                // The server copy may already be gone; the next sync uploads the recording again, so they match.
                TamixaLog.w("LocalMediaSync", "Deleting recording on the device failed")
                RecordingDeleteOutcome.FAILED
            }
            serverDeletePending -> RecordingDeleteOutcome.CLOUD_DELETE_PENDING
            else -> RecordingDeleteOutcome.DELETED_EVERYWHERE
        }
    }

    /**
     * Deletes server voice [profileId] (the voices list), then the device recording it was uploaded from, if any:
     * otherwise the next sync would find that recording backed up but missing on the server and upload it again.
     * The recording is matched by upload file name, read from the server first; if that can't be read, nothing is
     * deleted. Runs between syncs, so a sync can't re-upload the recording between the two deletes.
     */
    suspend fun deleteServerVoice(profileId: Long): Result<VoiceDeleteResult> = mutex.withLock {
        val voices = voiceRepository.listOrThrow().getOrElse { return@withLock Result.failure(it) }
        val fileName = voices.firstOrNull { it.id == profileId }?.uploadedFileName()
        voiceRepository.delete(profileId).onSuccess { result ->
            if (result == VoiceDeleteResult.IN_USE || fileName == null) return@onSuccess
            listLocalVoiceRecordings()
                .filter { it.uri.substringAfterLast('/') == fileName }
                .forEach { recording ->
                    if (deleteLocalVoiceRecording(recording.uri)) {
                        TamixaLog.i("LocalMediaSync", "Deleted device recording of server voice $profileId")
                    } else {
                        TamixaLog.w("LocalMediaSync", "Deleting device recording of server voice $profileId failed; the next sync uploads it again")
                    }
                }
        }
    }

    /** Sends queued server deletes; returns entries the server kept because a story uses the voice. */
    private suspend fun processPendingVoiceDeletes(): Set<String> {
        val pending = readPendingVoiceDeletes()
        if (pending.isEmpty()) return emptySet()
        val result = voiceBackup.deleteServerCopies(pending)
        savePendingVoiceDeletes(result.stillPending)
        return result.inUse
    }

    private suspend fun readPendingVoiceDeletes(): List<String> =
        runCatching { preferences.getPendingVoiceDeletes() }.getOrDefault("")
            .lines().map { it.trim() }.filter { it.isNotEmpty() }

    private suspend fun savePendingVoiceDeletes(entries: List<String>) {
        runCatching { preferences.setPendingVoiceDeletes(entries.distinct().joinToString("\n")) }
            .onFailure { TamixaLog.w("LocalMediaSync", "Saving pending voice deletes failed", it) }
    }

    private data class VoiceSyncRun(val uploaded: Int, val limitReached: Boolean)

    /**
     * Uploads recordings that aren't backed up yet, and recordings marked backed up whose server voice is gone
     * (removed on the server, e.g. by support or cleanup): otherwise the phone lists a recording the story player
     * can't offer. The server check is skipped when its voice list can't be read (offline).
     */
    private suspend fun syncVoiceRecordings(): VoiceSyncRun {
        // Oldest first, so server-side voice profiles keep the recording order.
        val local = listLocalVoiceRecordings().sortedBy { it.createdAtMillis }
        val onServer = if (local.any { it.backedUp }) voiceBackup.serverFileNames() else null
        val pending = local.filter { recording ->
            !recording.backedUp || (onServer != null && recording.uri.substringAfterLast('/') !in onServer)
        }
        val missing = pending.count { it.backedUp }
        if (missing > 0) TamixaLog.w("LocalMediaSync", "$missing backed-up recording(s) missing on the server; uploading again")
        var uploaded = 0
        for (recording in pending) {
            val bytes = readLocalMediaBytes(recording.uri) ?: continue
            when (val outcome = voiceBackup.upload(bytes, "my_voice_${recording.createdAtMillis}.m4a")) {
                is VoiceUploadOutcome.Uploaded -> {
                    // The server voice id, kept for reference; deletes match the server copy by file name.
                    markLocalMediaBackedUp(recording.uri, outcome.profileId)
                    uploaded++
                    TamixaLog.i("LocalMediaSync", "Voice recording backed up (profileId=${outcome.profileId})")
                }
                VoiceUploadOutcome.LimitReached -> {
                    TamixaLog.w("LocalMediaSync", "Voice recording not backed up: voice limit reached; it stays on the device")
                    return VoiceSyncRun(uploaded, limitReached = true)
                }
                is VoiceUploadOutcome.Failed -> {
                    // e.g. offline or timed out; the recording stays on the device and is retried next sync.
                    TamixaLog.w("LocalMediaSync", "Voice recording backup failed; will retry", outcome.cause)
                    return VoiceSyncRun(uploaded, limitReached = false)
                }
            }
        }
        return VoiceSyncRun(uploaded, limitReached = false)
    }

    private suspend fun syncActiveAvatar() {
        val activeUri = getActiveLocalAvatarImageUri() ?: return
        val active = listLocalAvatarImages().firstOrNull { it.uri == activeUri } ?: return
        if (active.backedUp) return
        val (bytes, contentType) = readLocalAvatarForUpload(active.uri) ?: return
        avatarRepository.uploadAvatar(bytes, contentType)
            .onSuccess {
                markLocalMediaBackedUp(active.uri)
                TamixaLog.i("LocalMediaSync", "Profile photo backed up")
            }
            .onFailure { TamixaLog.w("LocalMediaSync", "Profile photo backup failed; will retry", it) }
    }
}
