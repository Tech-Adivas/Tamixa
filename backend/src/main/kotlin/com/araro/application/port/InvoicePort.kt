package com.araro.application.port

import com.araro.domain.Invoice
import com.araro.domain.InvoiceStatus

/**
 * Invoice records for financial audit; link payments to subscriptions.
 */
interface InvoicePort {

    fun save(invoice: Invoice): Invoice

    fun findByProviderInvoiceId(providerInvoiceId: String): Invoice?
}
