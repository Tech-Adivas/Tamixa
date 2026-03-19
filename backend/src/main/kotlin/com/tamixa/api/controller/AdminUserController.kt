package com.tamixa.api.controller

import com.tamixa.api.ApiVersion
import com.tamixa.api.admin.dto.AddAdminUserRequest
import com.tamixa.api.admin.dto.AdminUserDto
import com.tamixa.api.admin.dto.AssignAdminRoleRequest
import com.tamixa.api.admin.dto.PagedResponse
import com.tamixa.application.admin.AdminParentNotFoundException
import com.tamixa.application.admin.AdminService
import com.tamixa.domain.Role
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
class AdminUserController(
    private val adminService: AdminService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/users")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_USERS')")
    fun getAdminUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<AdminUserDto>> =
        ResponseEntity.ok(adminService.getAdminUsers(page, size))

    @PostMapping("/users")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_USERS')")
    fun addAdminUser(@Valid @RequestBody request: AddAdminUserRequest): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity(null, HttpStatus.UNAUTHORIZED)
        return try {
            val dto = adminService.addAdminUser(adminEmail, request)
            log.info("Admin user added parentId={} role={} by {}", request.parentId, request.role, adminEmail)
            ResponseEntity.status(HttpStatus.CREATED).body(dto)
        } catch (e: AdminParentNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build<AdminUserDto>()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Invalid request")))
        }
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_USERS')")
    fun updateAdminRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignAdminRoleRequest
    ): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity(null, HttpStatus.UNAUTHORIZED)
        val role = runCatching { Role.valueOf(request.role) }.getOrNull()
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "Invalid role: ${request.role}"))
        return try {
            val dto = adminService.updateAdminRole(adminEmail, id, role)
            log.info("Admin role updated userId={} role={} by {}", id, role, adminEmail)
            ResponseEntity.ok(dto)
        } catch (e: AdminParentNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build<AdminUserDto>()
        }
    }
}
