package com.tamixa.application.consent

import com.tamixa.infrastructure.persistence.ParentConsentEntity
import com.tamixa.infrastructure.persistence.ParentConsentJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant

data class ConsentRecord(val consentType: String, val version: Int, val grantedAt: Instant)

@Service
class ConsentService(
    private val parentJpaRepository: com.tamixa.infrastructure.persistence.ParentJpaRepository,
    private val consentJpaRepository: ParentConsentJpaRepository
) {
    fun record(parentEmail: String, consentType: String, version: Int = 1) {
        val parent = parentJpaRepository.findByEmail(parentEmail) ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        val entity = ParentConsentEntity(
            parent = parent,
            consentType = consentType,
            version = version,
            grantedAt = Instant.now()
        )
        consentJpaRepository.save(entity)
    }

    /** True when the parent has ever granted [consentType] (e.g. "voice_cloning"). */
    fun hasConsent(parentEmail: String, consentType: String): Boolean {
        val parent = parentJpaRepository.findByEmail(parentEmail) ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        return consentJpaRepository.existsByParent_IdAndConsentType(parent.id, consentType)
    }

    fun listByParent(parentEmail: String, limit: Int = 50): List<ConsentRecord> {
        val parent = parentJpaRepository.findByEmail(parentEmail) ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        return consentJpaRepository.findByParent_IdOrderByGrantedAtDesc(parent.id, PageRequest.of(0, limit))
            .content.map { ConsentRecord(it.consentType, it.version, it.grantedAt) }
    }
}
