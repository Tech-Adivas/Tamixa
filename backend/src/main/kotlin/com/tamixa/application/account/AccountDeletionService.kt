package com.tamixa.application.account

import com.tamixa.application.port.AccountDeletionPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.TokenRevocationPort
import com.tamixa.domain.Role
import com.tamixa.infrastructure.logging.PiiMask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Orchestrates account deletion (GDPR/right to erasure).
 * Only PARENT accounts can be deleted via self-service.
 */
@Service
class AccountDeletionService(
    private val accountDeletionPort: AccountDeletionPort,
    private val parentRepository: ParentRepositoryPort,
    private val tokenRevocationPort: TokenRevocationPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Permanently deletes the account for the given email.
     * Revokes all active sessions before deletion.
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

        // Revoke all sessions before deletion (GDPR compliance)
        try {
            tokenRevocationPort.revokeAllForUser(email)
            log.info("Revoked all sessions for account email={}", PiiMask.maskEmail(email))
        } catch (e: Exception) {
            log.error("Failed to revoke sessions during account deletion email={}: {}", PiiMask.maskEmail(email), e.message, e)
            // Continue with deletion even if revocation fails
        }

        val deleted = accountDeletionPort.deleteAccount(email)
        if (!deleted) {
            throw AccountDeletionException("Account could not be deleted")
        }
        log.info("Account deleted successfully email={}", PiiMask.maskEmail(email))
    }
}

class AccountDeletionException(message: String) : RuntimeException(message)
