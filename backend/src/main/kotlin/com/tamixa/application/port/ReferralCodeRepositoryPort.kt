package com.tamixa.application.port

import com.tamixa.domain.ReferralCode

interface ReferralCodeRepositoryPort {
    fun save(referralCode: ReferralCode): ReferralCode
    fun findById(id: Long): ReferralCode?
    fun findByShortcode(shortcode: String): ReferralCode?
    fun findAll(): List<ReferralCode>
    fun deleteById(id: Long)
}
