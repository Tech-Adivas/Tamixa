package com.tamixa.application.port

import com.tamixa.domain.Invoice
import com.tamixa.domain.InvoiceStatus

/**
 * Invoice records for financial audit; link payments to subscriptions.
 */
interface InvoicePort {

    fun save(invoice: Invoice): Invoice

    fun findByProviderInvoiceId(providerInvoiceId: String): Invoice?
}
