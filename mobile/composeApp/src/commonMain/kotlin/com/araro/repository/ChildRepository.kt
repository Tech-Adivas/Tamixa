package com.araro.repository

import com.araro.domain.Child
import com.araro.domain.CreateChildRequest
import com.araro.network.ChildApi

class ChildRepository(private val api: ChildApi) {
    suspend fun create(request: CreateChildRequest): Result<Child> =
        runCatching { api.create(request) }

    suspend fun list(): Result<List<Child>> =
        runCatching { api.list() }

    suspend fun getById(id: Long): Result<Child> =
        runCatching { api.getById(id) }
}
