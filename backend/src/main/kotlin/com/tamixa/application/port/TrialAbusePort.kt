package com.tamixa.application.port

/**
 * Trial abuse prevention: block multiple trial claims.
 * Financial safety: one trial per device+email combination; one per account.
 */
interface TrialAbusePort {
    /** Returns true if this device+email has already claimed a trial (abuse). */
    fun hasTrialBeenUsed(deviceHash: String, emailHash: String): Boolean
    /** Returns true if this parent has ever used a trial. */
    fun hasParentUsedTrial(parentId: Long): Boolean
    /** Record trial usage for abuse detection. */
    fun recordTrialUsage(parentId: Long, deviceHash: String, emailHash: String)
}
