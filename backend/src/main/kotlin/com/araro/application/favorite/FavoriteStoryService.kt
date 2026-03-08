package com.araro.application.favorite

import com.araro.application.port.FavoriteStoryRepositoryPort
import com.araro.application.port.ParentRepositoryPort
import com.araro.domain.FavoriteStory
import org.springframework.stereotype.Service

@Service
class FavoriteStoryService(
    private val repository: FavoriteStoryRepositoryPort,
    private val parentRepository: ParentRepositoryPort
) {
    fun listByParent(parentEmail: String): List<FavoriteStory> {
        val parent = parentRepository.findByEmail(parentEmail) ?: throw com.araro.application.child.ParentNotFoundException(parentEmail)
        return repository.findByParentId(parent.id)
    }

    fun add(parentEmail: String, storyId: Long, storySource: String = "generated"): FavoriteStory {
        val parent = parentRepository.findByEmail(parentEmail) ?: throw com.araro.application.child.ParentNotFoundException(parentEmail)
        return repository.add(parent.id, storyId, storySource)
    }

    fun remove(parentEmail: String, storyId: Long) {
        val parent = parentRepository.findByEmail(parentEmail) ?: throw com.araro.application.child.ParentNotFoundException(parentEmail)
        repository.remove(parent.id, storyId)
    }

    fun isFavorite(parentEmail: String, storyId: Long): Boolean {
        val parent = parentRepository.findByEmail(parentEmail) ?: return false
        return repository.isFavorite(parent.id, storyId)
    }
}
