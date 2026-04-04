package com.tamixa.application.profile

import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class ProfileService(
    private val parentRepository: ParentRepositoryPort,
    private val childJpaRepository: ChildJpaRepository,
) {

    private val dobFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getProfile(email: String): ProfileResponse? {
        val parent = parentRepository.findByEmail(email) ?: return null
        val children = childJpaRepository.findByParent_Id(parent.id)
            .map { c ->
                ProfileChildDto(
                    id = c.id,
                    name = c.name,
                    dateOfBirth = c.dateOfBirth.format(dobFormat),
                    languagePreference = c.languagePreference,
                )
            }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
        return ProfileResponse(
            parent = ProfileParentDto(
                id = parent.id,
                email = parent.email,
                nickname = parent.nickname,
                displayName = parent.displayName
            ),
            children = children,
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

    @Transactional
    fun updateStoryArtPersonalizationOptIn(email: String, optIn: Boolean): Boolean {
        val parent = parentRepository.findByEmail(email) ?: return false
        parentRepository.save(parent.copy(storyArtPersonalizationOptIn = optIn))
        return true
    }
}
