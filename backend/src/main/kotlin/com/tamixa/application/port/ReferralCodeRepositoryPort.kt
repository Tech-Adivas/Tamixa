package com.tamixa.application.port

import com.tamixa.domain.ReferralCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReferralCodeRepositoryPort {
    fun save(referralCode: ReferralCode): ReferralCode
    fun findById(id: Long): ReferralCode?
    fun findByShortcode(shortcode: String): ReferralCode?
    fun findAll(pageable: Pageable): Page<ReferralCode>
    fun deleteById(id: Long)
}
