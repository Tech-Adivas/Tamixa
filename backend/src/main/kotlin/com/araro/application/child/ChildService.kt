package com.araro.application.child

import com.araro.application.consent.ConsentService
import com.araro.application.port.ChildRepositoryPort
import com.araro.application.port.ParentRepositoryPort
import com.araro.application.subscription.SubscriptionService
import com.araro.domain.Child
import com.araro.domain.SubscriptionPlan
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class ChildService(
    private val childRepository: ChildRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val subscriptionService: SubscriptionService,
    private val consentService: ConsentService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Server-side enforcement: FAMILY plan allows max 5 children; block if limit exceeded. */
    fun create(
        parentEmail: String,
        name: String,
        dateOfBirth: LocalDate,
        languagePreference: String?,
        interests: String? = null,
        favoriteColor: String? = null,
        favoriteAnimal: String? = null,
        characterTraits: String? = null,
        avatarChoice: String? = null,
        childProfileConsent: Boolean = false
    ): Child {
        if (!childProfileConsent) {
            throw com.araro.application.auth.ConsentRequiredException("Parental consent is required to create a child profile")
        }
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw ParentNotFoundException(parentEmail)
        validateAge(dateOfBirth)
        val sub = subscriptionService.getOrCreateSubscription(parent.id)
        if (sub.plan == SubscriptionPlan.FAMILY) {
            val existingCount = childRepository.findByParentId(parent.id).size
            if (existingCount >= sub.maxChildren) {
                throw FamilyChildLimitReachedException(sub.maxChildren)
            }
        }
        val child = Child(
            id = 0,
            parentId = parent.id,
            name = name,
            dateOfBirth = dateOfBirth,
            languagePreference = languagePreference,
            interests = interests?.takeIf { it.isNotBlank() },
            createdAt = Instant.now(),
            favoriteColor = favoriteColor?.takeIf { it.isNotBlank() },
            favoriteAnimal = favoriteAnimal?.takeIf { it.isNotBlank() },
            characterTraits = characterTraits?.takeIf { it.isNotBlank() },
            avatarChoice = avatarChoice?.takeIf { it.isNotBlank() }
        )
        val saved = childRepository.save(child)
        consentService.record(parentEmail, "child_profile_consent", 1)
        log.info("Child created parentId={} childId={}", parent.id, saved.id)
        return saved
    }

    fun update(
        parentEmail: String,
        childId: Long,
        languagePreference: String?,
        interests: String?,
        favoriteColor: String?,
        favoriteAnimal: String?,
        characterTraits: String?,
        avatarChoice: String?
    ): Child {
        val existing = findByIdForParent(childId, parentEmail)
        val updated = existing.copy(
            languagePreference = if (languagePreference != null) languagePreference.ifBlank { null } else existing.languagePreference,
            interests = if (interests != null) interests.ifBlank { null } else existing.interests,
            favoriteColor = if (favoriteColor != null) favoriteColor.ifBlank { null } else existing.favoriteColor,
            favoriteAnimal = if (favoriteAnimal != null) favoriteAnimal.ifBlank { null } else existing.favoriteAnimal,
            characterTraits = if (characterTraits != null) characterTraits.ifBlank { null } else existing.characterTraits,
            avatarChoice = if (avatarChoice != null) avatarChoice.ifBlank { null } else existing.avatarChoice
        )
        val saved = childRepository.save(updated)
        log.info("Child updated parentId={} childId={}", existing.parentId, childId)
        return saved
    }

    fun findAllByParent(parentEmail: String): List<Child> {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw ParentNotFoundException(parentEmail)
        return childRepository.findByParentId(parent.id)
    }

    fun findByIdForParent(childId: Long, parentEmail: String): Child {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw ParentNotFoundException(parentEmail)
        val child = childRepository.findById(childId)
            ?: throw ChildNotFoundException(childId)
        if (child.parentId != parent.id) {
            throw ChildAccessDeniedException(childId)
        }
        return child
    }

    private fun validateAge(dateOfBirth: LocalDate) {
        val age = java.time.temporal.ChronoUnit.YEARS.between(dateOfBirth, LocalDate.now()).toInt()
        if (age < 1 || age > 12) {
            throw InvalidChildAgeException(age)
        }
    }
}

class ChildNotFoundException(id: Long) : RuntimeException("Child not found: $id")

class ChildAccessDeniedException(childId: Long) : RuntimeException("Access denied to child: $childId")

class InvalidChildAgeException(age: Int) : RuntimeException("Child age must be between 1 and 12 years, got: $age")

class ParentNotFoundException(email: String) : RuntimeException("Parent not found: $email")

class FamilyChildLimitReachedException(maxChildren: Int) : RuntimeException("Family plan allows up to $maxChildren child profiles. Limit reached.")
