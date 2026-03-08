package com.araro.infrastructure.persistence

import com.araro.application.port.InvoicePort
import com.araro.domain.Invoice
import com.araro.domain.InvoiceStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class InvoiceRepositoryAdapter(
    private val invoiceJpaRepository: InvoiceJpaRepository,
    private val parentJpaRepository: ParentJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository
) : InvoicePort {

    @Transactional
    override fun save(invoice: Invoice): Invoice {
        val existing = invoiceJpaRepository.findByProviderInvoiceId(invoice.providerInvoiceId)
        val parent = parentJpaRepository.findById(invoice.parentId).orElseThrow { IllegalArgumentException("Parent not found: ${invoice.parentId}") }
        val subscription = invoice.subscriptionId?.let { subscriptionJpaRepository.findById(it).orElse(null) }
        val saved = if (existing != null) {
            invoiceJpaRepository.save(
                InvoiceEntity(
                    id = existing.id,
                    providerInvoiceId = invoice.providerInvoiceId,
                    parent = parent,
                    subscription = subscription,
                    amountMinor = invoice.amountMinor,
                    currency = invoice.currency,
                    status = invoice.status,
                    paidAt = invoice.paidAt,
                    createdAt = existing.createdAt
                )
            )
        } else {
            invoiceJpaRepository.save(
                InvoiceEntity(
                    providerInvoiceId = invoice.providerInvoiceId,
                    parent = parent,
                    subscription = subscription,
                    amountMinor = invoice.amountMinor,
                    currency = invoice.currency,
                    status = invoice.status,
                    paidAt = invoice.paidAt,
                    createdAt = invoice.createdAt
                )
            )
        }
        return saved.toDomain()
    }

    override fun findByProviderInvoiceId(providerInvoiceId: String): Invoice? {
        return invoiceJpaRepository.findByProviderInvoiceId(providerInvoiceId)?.toDomain()
    }
}

private fun InvoiceEntity.toDomain(): Invoice = Invoice(
    id = id,
    providerInvoiceId = providerInvoiceId,
    parentId = parent.id,
    subscriptionId = subscription?.id,
    amountMinor = amountMinor,
    currency = currency,
    status = status,
    paidAt = paidAt,
    createdAt = createdAt
)
