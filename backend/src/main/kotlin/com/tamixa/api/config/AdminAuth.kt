package com.tamixa.api.config

import com.tamixa.domain.AdminPermission
import com.tamixa.domain.Role
import com.tamixa.domain.RolePermissions
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Bean for @PreAuthorize SpEL: @adminAuth.hasPermission('VIEW_REVENUE').
 * Checks if the authenticated user's role grants the given permission.
 */
@Component("adminAuth")
class AdminAuth {

    fun hasPermission(permission: String): Boolean {
        val auth = SecurityContextHolder.getContext().authentication ?: return false
        val role = resolveRole(auth) ?: return false
        val perm = runCatching { AdminPermission.valueOf(permission) }.getOrNull() ?: return false
        return RolePermissions.hasPermission(role, perm)
    }

    private fun resolveRole(auth: Authentication): Role? {
        val authority = auth.authorities.firstOrNull()?.authority ?: return null
        val roleName = authority.removePrefix("ROLE_")
        return runCatching { Role.valueOf(roleName) }.getOrNull()
    }
}
