package com.araro.api.child

import com.araro.api.child.dto.ChildResponse
import com.araro.domain.Child

fun Child.toResponse(): ChildResponse = ChildResponse(
    id = id,
    parentId = parentId,
    name = name,
    dateOfBirth = dateOfBirth,
    age = ageInYears(),
    languagePreference = languagePreference,
    interests = interests,
    createdAt = createdAt,
    favoriteColor = favoriteColor,
    favoriteAnimal = favoriteAnimal,
    characterTraits = characterTraits,
    avatarChoice = avatarChoice
)
