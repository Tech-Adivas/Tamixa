package com.tamixa.application.favorite

import com.tamixa.application.port.FavoriteStoryRepositoryPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.domain.FavoriteStory
import org.springframework.stereotype.Service

@Service
class FavoriteStoryService(
    private val repository: FavoriteStoryRepositoryPort,
    private val parentRepository: ParentRepositoryPort
) {
    fun listByParent(parentEmail: String): List<FavoriteStory> {
        val parent = parentRepository.findByEmail(parentEmail) ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        return repository.findByParentId(parent.id)
    }

    fun add(parentEmail: String, storyId: Long, storySource: String = "generated"): FavoriteStory {
        val parent = parentRepository.findByEmail(parentEmail) ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        return repository.add(parent.id, storyId, storySource)
    }

    fun remove(parentEmail: String, storyId: Long) {
        val parent = parentRepository.findByEmail(parentEmail) ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        repository.remove(parent.id, storyId)
    }

    fun isFavorite(parentEmail: String, storyId: Long): Boolean {
        val parent = parentRepository.findByEmail(parentEmail) ?: return false
        return repository.isFavorite(parent.id, storyId)
    }
}
