package com.tamixa.application.profile

import com.tamixa.application.port.ParentRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
    private val parentRepository: ParentRepositoryPort
) {

    fun getProfile(email: String): ProfileResponse? {
        val parent = parentRepository.findByEmail(email) ?: return null
        return ProfileResponse(
            parent = ProfileParentDto(
                id = parent.id,
                email = parent.email,
                nickname = parent.nickname,
                displayName = parent.displayName
            )
        )
    }

    @Transactional
    fun updateProfile(email: String, nickname: String?, displayName: String?): Boolean {
        val parent = parentRepository.findByEmail(email) ?: return false
        val newNickname = when {
            nickname == null -> parent.nickname
            nickname.trim().isBlank() -> null
            else -> nickname.trim()
        }
        val newDisplayName = when {
            displayName == null -> parent.displayName
            displayName.trim().isBlank() -> null
            else -> displayName.trim()
        }
        val updated = parent.copy(nickname = newNickname, displayName = newDisplayName)
        parentRepository.save(updated)
        return true
    }
}
