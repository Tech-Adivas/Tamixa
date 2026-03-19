package com.tamixa.application.port

/**
 * Deletes all data for a parent account and the parent record (GDPR/right to erasure).
 * Implementations must delete in dependency order to satisfy FK constraints.
 */
interface AccountDeletionPort {

    /**
     * Permanently deletes the account and all associated data for the given email.
     * @return true if the account existed and was deleted, false if no such account
     */
    fun deleteAccount(email: String): Boolean
}
