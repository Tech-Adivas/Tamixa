package com.tamixa.domain

/**
 * Fine-grained permissions for admin dashboard areas.
 * Used for RBAC: each admin role grants a subset of these permissions.
 */
enum class AdminPermission {
    VIEW_REVENUE,
    VIEW_SUBSCRIPTIONS,
    MANAGE_INVOICES,
    MANAGE_STORIES,
    MODERATE_STORIES,
    VIEW_VOICE_LOGS,
    VIEW_PARENTS,
    VIEW_CHILDREN,
    VIEW_STORIES,
    MANAGE_USERS,
    VIEW_HEALTH,
    VIEW_AI_METRICS,
    VIEW_AUDIT,
    VIEW_KAFKA,
    VIEW_MONITORING,
}
