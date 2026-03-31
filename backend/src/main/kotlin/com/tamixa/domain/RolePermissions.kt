package com.tamixa.domain

/**
 * Maps admin roles to permissions. SUPER_ADMIN and legacy ADMIN have full access.
 */
object RolePermissions {

    fun hasPermission(role: Role, permission: AdminPermission): Boolean =
        permission in permissionsFor(role)

    fun permissionsFor(role: Role): Set<AdminPermission> =
        when (role) {
            Role.SUPER_ADMIN, Role.ADMIN -> AdminPermission.entries.toSet()
            Role.REVENUE_ANALYST -> setOf(
                AdminPermission.VIEW_REVENUE,
                AdminPermission.VIEW_SUBSCRIPTIONS,
                AdminPermission.MANAGE_INVOICES,
                AdminPermission.VIEW_AI_METRICS,
            )
            Role.CONTENT_MANAGER -> setOf(
                AdminPermission.MANAGE_STORIES,
                AdminPermission.MODERATE_STORIES,
                AdminPermission.VIEW_VOICE_LOGS,
                AdminPermission.VIEW_STORIES,
                AdminPermission.VIEW_AI_CONTROL_PLANE,
            )
            Role.SUPPORT -> setOf(
                AdminPermission.VIEW_PARENTS,
                AdminPermission.VIEW_CHILDREN,
                AdminPermission.VIEW_STORIES,
                AdminPermission.VIEW_HEALTH,
            )
            Role.PARENT -> emptySet()
        }

    /** True if role can access admin dashboard (any admin permission). */
    fun isAdminRole(role: Role): Boolean =
        role == Role.SUPER_ADMIN || role == Role.ADMIN ||
            role == Role.REVENUE_ANALYST || role == Role.CONTENT_MANAGER || role == Role.SUPPORT
}
