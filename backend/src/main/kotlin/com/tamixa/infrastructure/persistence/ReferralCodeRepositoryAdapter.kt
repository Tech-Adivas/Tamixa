package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.ReferralCodeRepositoryPort
import com.tamixa.domain.ReferralCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReferralCodeRepositoryAdapter(
    private val jpaRepository: ReferralCodeJpaRepository
) : ReferralCodeRepositoryPort {

    private fun toDomain(e: ReferralCodeEntity): ReferralCode = ReferralCode(
        id = e.id,
        shortcode = e.shortcode,
        shopName = e.shopName,
        offerPercent = e.offerPercent,
        expiresAt = e.expiresAt,
        active = e.active,
        stripeCouponId = e.stripeCouponId,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt
    )

    @Transactional
    override fun save(referralCode: ReferralCode): ReferralCode {
        val entity = ReferralCodeEntity(
            id = referralCode.id,
            shortcode = referralCode.shortcode.uppercase().trim(),
            shopName = referralCode.shopName.trim(),
            offerPercent = referralCode.offerPercent,
            expiresAt = referralCode.expiresAt,
            active = referralCode.active,
            stripeCouponId = referralCode.stripeCouponId,
            createdAt = referralCode.createdAt,
            updatedAt = java.time.Instant.now()
        )
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): ReferralCode? =
        jpaRepository.findById(id).map { toDomain(it) }.orElse(null)

    override fun findByShortcode(shortcode: String): ReferralCode? =
        jpaRepository.findByShortcodeIgnoreCase(shortcode.trim().uppercase())?.let { toDomain(it) }

    override fun findAll(pageable: Pageable): Page<ReferralCode> =
        jpaRepository.findAllByOrderByCreatedAtDesc(pageable).map { toDomain(it) }

    @Transactional
    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
