package com.tamixa.domain

/**
 * User roles. PARENT for consumer app; admin roles for dashboard access.
 * ADMIN is deprecated alias for SUPER_ADMIN (backward compatibility).
 */
enum class Role {
    PARENT,
    /** @deprecated Use SUPER_ADMIN */
    ADMIN,
    SUPER_ADMIN,
    REVENUE_ANALYST,
    CONTENT_MANAGER,
    SUPPORT,
}
