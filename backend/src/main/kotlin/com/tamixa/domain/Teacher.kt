package com.tamixa.domain

import java.time.Instant

data class Teacher(
    val id: Long,
    val parentId: Long,
    val schoolName: String?,
    val verifiedAt: Instant?,
    val createdAt: Instant
) {
    val isVerified: Boolean
        get() = verifiedAt != null
}

data class Classroom(
    val id: Long,
    val teacherId: Long,
    val name: String,
    val code: String,
    val subject: String?,
    val gradeLevel: String?,
    val createdAt: Instant
)

data class ClassroomStudent(
    val id: Long,
    val classroomId: Long,
    val childId: Long,
    val joinedAt: Instant
)
