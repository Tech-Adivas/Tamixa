package com.tamixa.application.familyvoice

import com.tamixa.application.port.AuditLogPort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.FamilyVoiceStoragePort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.StoryFamilyVoiceRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.stream.StreamLanguageUtils
import com.tamixa.domain.StoryFamilyVoice
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Family recorded voice: parent uploads MP3 per story.
 * Stored in private path; audit logged; parent-only access.
 * Uses ObjectProvider so a no-op adapter can be used when GCS is not configured.
 */
@Service
class FamilyVoiceService(
    private val familyVoiceRepository: StoryFamilyVoiceRepositoryPort,
    familyVoiceStorageProvider: ObjectProvider<FamilyVoiceStoragePort>,
    private val parentRepository: ParentRepositoryPort,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val auditLog: AuditLogPort,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val familyVoiceStorage: FamilyVoiceStoragePort = familyVoiceStorageProvider.getIfAvailable()
        ?: object : FamilyVoiceStoragePort {
            override fun upload(storyId: Long, parentId: Long, language: String, audioBytes: ByteArray, contentType: String, fileExt: String): String {
                throw UnsupportedOperationException("Family voice storage requires S3 (app.storage.type=s3)")
            }
            override fun delete(storagePath: String) {}
            override fun exists(storagePath: String): Boolean = false
        }
    private val maxFileSizeBytes get() = appProperties.voice.maxFileSizeBytes

    @Transactional
    fun uploadFamilyVoice(
        parentEmail: String,
        storyId: Long,
        language: String,
        mp3Bytes: ByteArray
    ): StoryFamilyVoice {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw FamilyVoiceAccessDeniedException("Parent not found")
        validateStoryAccess(storyId, parent.id)
        if (mp3Bytes.size > maxFileSizeBytes) {
            throw FamilyVoiceFileTooLargeException("File exceeds ${maxFileSizeBytes / 1024}KB limit")
        }
        val (contentType, fileExt) = validateAudioFormat(mp3Bytes)

        val effectiveLang = StreamLanguageUtils.normalize(language)
        val existing = familyVoiceRepository.findByStoryIdAndParentIdAndLanguage(storyId, parent.id, effectiveLang)
        if (existing != null) {
            familyVoiceStorage.delete(existing.storagePath)
        }

        val storagePath = familyVoiceStorage.upload(storyId, parent.id, effectiveLang, mp3Bytes, contentType, fileExt)
        val voice = StoryFamilyVoice(
            id = 0,
            storyId = storyId,
            parentId = parent.id,
            language = effectiveLang,
            storagePath = storagePath,
            fileSizeBytes = mp3Bytes.size.toLong(),
            createdAt = Instant.now()
        )
        val saved = familyVoiceRepository.save(voice)
        auditLog.logVoiceUpload(parent.id, saved.id, traceId = "family:$storyId:$effectiveLang")
        log.info("Family voice uploaded storyId={} parentId={} lang={}", storyId, parent.id, effectiveLang)
        return saved
    }

    @Transactional
    fun deleteFamilyVoice(parentEmail: String, storyId: Long, language: String) {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw FamilyVoiceAccessDeniedException("Parent not found")
        val effectiveLang = StreamLanguageUtils.normalize(language)
        val existing = familyVoiceRepository.findByStoryIdAndParentIdAndLanguage(storyId, parent.id, effectiveLang)
            ?: throw FamilyVoiceNotFoundException(storyId, effectiveLang)

        familyVoiceStorage.delete(existing.storagePath)
        familyVoiceRepository.deleteByStoryIdAndParentIdAndLanguage(storyId, parent.id, effectiveLang)
        auditLog.logVoiceDelete(parent.id, existing.id, traceId = "family:$storyId:$effectiveLang")
        log.info("Family voice deleted storyId={} parentId={} lang={}", storyId, parent.id, effectiveLang)
    }

    fun getFamilyVoiceStreamPath(parentId: Long, storyId: Long, language: String): String? {
        val effectiveLang = StreamLanguageUtils.normalize(language)
        val voice = familyVoiceRepository.findByStoryIdAndParentIdAndLanguage(storyId, parentId, effectiveLang)
        return voice?.storagePath
    }

    /** Admin: delete all family voice recordings for a story (e.g. when deleting library story). */
    fun deleteAllVoicesForStory(storyId: Long) {
        val voices = familyVoiceRepository.findByStoryId(storyId)
        voices.forEach { v ->
            try {
                familyVoiceStorage.delete(v.storagePath)
            } catch (e: Exception) {
                log.warn("Failed to delete family voice storage storyId={} path={}: {}", storyId, v.storagePath, e.message)
            }
        }
        if (voices.isNotEmpty()) {
            familyVoiceRepository.deleteByStoryId(storyId)
            log.info("Deleted {} family voice(s) for story {}", voices.size, storyId)
        }
    }

    /** Parent-only: story must be curated OR parent-owned generated. */
    private fun validateStoryAccess(storyId: Long, parentId: Long) {
        val libraryStory = storyLibraryRepository.findById(storyId)
        if (libraryStory != null) return
        val generated = storyRepository.findById(storyId)
            ?: throw FamilyVoiceAccessDeniedException("Story not found: $storyId")
        if (generated.parentId != parentId) {
            throw FamilyVoiceAccessDeniedException("Access denied: family voice only for your own stories")
        }
    }

    /** Returns Pair(contentType, fileExtension) for storage. */
    private fun validateAudioFormat(bytes: ByteArray): Pair<String, String> {
        if (bytes.size < 4) throw InvalidVoiceFileException("File too small")
        // MP3: frame sync 0xFF 0xE?, ID3 0x49 0x44 0x33
        val isMp3 = (bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0) ||
            bytes.take(3).toByteArray().contentEquals(byteArrayOf(0x49, 0x44, 0x33))
        // AAC in MP4: ftyp box, or AAC_ADTS 0xFF 0xF?
        val isMp4 = bytes.size >= 8 && bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() &&
            bytes[6] == 0x79.toByte() && bytes[7] == 0x70.toByte() // "ftyp"
        val isAacAdts = bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xF0) == 0xF0
        val isAac = isMp4 || isAacAdts
        return when {
            isMp3 -> "audio/mpeg" to "mp3"
            isAac -> "audio/mp4" to "m4a"
            else -> throw InvalidVoiceFileException("Invalid audio format (MP3 or AAC required)")
        }
    }
}

class FamilyVoiceAccessDeniedException(message: String) : RuntimeException(message)
class FamilyVoiceNotFoundException(storyId: Long, language: String) : RuntimeException("Family voice not found for story=$storyId lang=$language")
class FamilyVoiceFileTooLargeException(message: String) : RuntimeException(message)
class InvalidVoiceFileException(message: String) : RuntimeException(message)
