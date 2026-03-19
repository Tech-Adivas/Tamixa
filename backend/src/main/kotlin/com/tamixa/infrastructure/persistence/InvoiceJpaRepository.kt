package com.tamixa.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface InvoiceJpaRepository : JpaRepository<InvoiceEntity, Long> {

    fun findByProviderInvoiceId(providerInvoiceId: String): InvoiceEntity?

    fun findByParent_Id(parentId: Long, pageable: Pageable): Page<InvoiceEntity>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<InvoiceEntity>
    fun deleteByParent_Id(parentId: Long)
}
