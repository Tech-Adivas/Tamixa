package com.araro.application.port

import com.araro.domain.Child

interface ChildRepositoryPort {

    fun save(child: Child): Child

    fun findById(id: Long): Child?

    fun findByParentId(parentId: Long): List<Child>

    fun existsByIdAndParentId(id: Long, parentId: Long): Boolean
}
