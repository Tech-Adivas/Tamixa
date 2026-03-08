package com.araro.api.avatar

import com.araro.api.ApiVersion
import com.araro.api.avatar.dto.AvatarResponse
import com.araro.api.avatar.dto.AvatarUploadResponse
import com.araro.application.avatar.AvatarAccessDeniedException
import com.araro.application.avatar.AvatarFileTooLargeException
import com.araro.application.avatar.AvatarNotFoundException
import com.araro.application.avatar.AvatarPremiumRequiredException
import com.araro.application.avatar.FamilyAvatarService
import com.araro.application.avatar.InvalidAvatarFileException
import com.araro.application.port.ParentRepositoryPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Premium avatar: parent uploads image for storytelling.
 * GET /parents/me/avatar - get signed URL (returns 404 if no avatar)
 * POST /parents/me/avatar - upload (multipart file)
 * DELETE /parents/me/avatar - remove avatar
 */
@RestController
@RequestMapping("${ApiVersion.V1}/parents/me")
@PreAuthorize("hasRole('PARENT')")
class FamilyAvatarController(
    private val familyAvatarService: FamilyAvatarService,
    private val parentRepository: ParentRepositoryPort
) {

    @GetMapping("/avatar")
    fun getAvatar(): ResponseEntity<AvatarResponse> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val url = familyAvatarService.getAvatarUrl(parentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AvatarResponse(avatarUrl = url))
    }

    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(@RequestParam("file") file: MultipartFile): ResponseEntity<AvatarUploadResponse> {
        if (file.isEmpty) return ResponseEntity.badRequest().build()
        return try {
            val email = currentParentEmail()
            val avatar = familyAvatarService.uploadAvatar(email, file.bytes, file.contentType ?: "image/jpeg")
            val url = familyAvatarService.getAvatarUrl(avatar.parentId)
                ?: throw IllegalStateException("Avatar stored but signed URL failed")
            ResponseEntity.status(HttpStatus.CREATED).body(AvatarUploadResponse(avatarUrl = url))
        } catch (e: AvatarPremiumRequiredException) {
            ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build()
        } catch (e: AvatarFileTooLargeException) {
            ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build()
        } catch (e: InvalidAvatarFileException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/avatar")
    fun deleteAvatar(): ResponseEntity<Unit> {
        return try {
            val email = currentParentEmail()
            familyAvatarService.deleteAvatar(email)
            ResponseEntity.ok().build()
        } catch (e: AvatarNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: AvatarPremiumRequiredException) {
            ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build()
        }
    }

    private fun currentParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentRepository.findByEmail(email)?.id
    }

    private fun currentParentEmail(): String {
        return SecurityContextHolder.getContext().authentication?.name
            ?: throw AvatarAccessDeniedException("Not authenticated")
    }
}

