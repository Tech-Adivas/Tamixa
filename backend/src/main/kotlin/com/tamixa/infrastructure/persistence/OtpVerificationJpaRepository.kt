package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface OtpVerificationJpaRepository : JpaRepository<OtpVerificationEntity, Long> {
    fun findFirstByPhoneAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(
        phone: String,
        code: String
    ): OtpVerificationEntity?
}
