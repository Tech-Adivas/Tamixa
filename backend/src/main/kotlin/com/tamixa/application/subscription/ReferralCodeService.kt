package com.tamixa.application.subscription

import com.tamixa.application.port.ReferralCodeRepositoryPort
import com.tamixa.domain.ReferralCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Validates referral codes and returns discount info. Enforces expiration and active flag.
 */
@Service
class ReferralCodeService(
    private val referralCodeRepository: ReferralCodeRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Validates a referral code. Returns referral info if code is active and not expired, null otherwise.
     */
    fun validate(shortcode: String): ReferralCodeInfo? {
        val code = shortcode.takeIf { it.isNotBlank() }?.trim()?.uppercase() ?: return null
        val referral = referralCodeRepository.findByShortcode(code) ?: run {
            log.debug("Referral code not found: {}", code)
            return null
        }
        if (!referral.active) {
            log.debug("Referral code inactive: {}", code)
            return null
        }
        if (Instant.now().isAfter(referral.expiresAt)) {
            log.debug("Referral code expired: {} expiresAt={}", code, referral.expiresAt)
            return null
        }
        return ReferralCodeInfo(
            shortcode = referral.shortcode,
            shopName = referral.shopName,
            offerPercent = referral.offerPercent,
            stripeCouponId = referral.stripeCouponId
        )
    }

    fun findById(id: Long): ReferralCode? = referralCodeRepository.findById(id)
    fun findByShortcode(shortcode: String): ReferralCode? = referralCodeRepository.findByShortcode(shortcode)
    fun findAll(): List<ReferralCode> = referralCodeRepository.findAll()

    @Transactional
    fun create(shortcode: String, shopName: String, offerPercent: Int, expiresAt: java.time.Instant, active: Boolean): ReferralCode {
        val existing = referralCodeRepository.findByShortcode(shortcode.trim().uppercase())
        if (existing != null) throw ReferralCodeDuplicateException(shortcode)
        val code = ReferralCode(
            id = 0,
            shortcode = shortcode.trim().uppercase(),
            shopName = shopName.trim(),
            offerPercent = offerPercent.coerceIn(0, 100),
            expiresAt = expiresAt,
            active = active,
            stripeCouponId = null,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
        return referralCodeRepository.save(code)
    }

    @Transactional
    fun update(id: Long, shortcode: String?, shopName: String?, offerPercent: Int?, expiresAt: java.time.Instant?, active: Boolean?): ReferralCode {
        val current = referralCodeRepository.findById(id) ?: throw ReferralCodeNotFoundException(id)
        val updated = current.copy(
            shortcode = shortcode?.trim()?.uppercase() ?: current.shortcode,
            shopName = shopName?.trim() ?: current.shopName,
            offerPercent = offerPercent?.coerceIn(0, 100) ?: current.offerPercent,
            expiresAt = expiresAt ?: current.expiresAt,
            active = active ?: current.active,
            updatedAt = java.time.Instant.now()
        )
        return referralCodeRepository.save(updated)
    }

    @Transactional
    fun delete(id: Long) {
        if (referralCodeRepository.findById(id) == null) throw ReferralCodeNotFoundException(id)
        referralCodeRepository.deleteById(id)
    }
}

class ReferralCodeNotFoundException(id: Long) : RuntimeException("Referral code not found: $id")
class ReferralCodeDuplicateException(shortcode: String) : RuntimeException("Referral code already exists: $shortcode")

data class ReferralCodeInfo(
    val shortcode: String,
    val shopName: String,
    val offerPercent: Int,
    val stripeCouponId: String?
)
