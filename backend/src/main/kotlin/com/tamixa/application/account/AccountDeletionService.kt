package com.tamixa.application.account

import com.tamixa.application.port.AccountDeletionPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.domain.Role
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Orchestrates account deletion (GDPR/right to erasure).
 * Only PARENT accounts can be deleted via self-service.
 */
@Service
class AccountDeletionService(
    private val accountDeletionPort: AccountDeletionPort,
    private val parentRepository: ParentRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Permanently deletes the account for the given email.
     * @throws AccountDeletionException if account not found, is suspended, or is not a PARENT (e.g. admin)
     */
    fun deleteAccount(email: String) {
        val parent = parentRepository.findByEmail(email)
            ?: throw AccountDeletionException("Account not found")
        if (parent.suspendedAt != null) {
            throw AccountDeletionException("Cannot delete suspended account")
        }
        if (parent.role != Role.PARENT) {
            log.warn("Account deletion refused: non-PARENT role")
            throw AccountDeletionException("Only parent accounts can be deleted via this action")
        }
        val deleted = accountDeletionPort.deleteAccount(email)
        if (!deleted) {
            throw AccountDeletionException("Account could not be deleted")
        }
        log.info("Account deleted successfully")
    }
}

class AccountDeletionException(message: String) : RuntimeException(message)
