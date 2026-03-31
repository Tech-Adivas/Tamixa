package com.tamixa.api.education.dto

import com.tamixa.domain.Classroom
import com.tamixa.domain.ClassroomStudent
import com.tamixa.domain.Teacher
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.Instant

data class TeacherResponse(
    val id: Long,
    val parentId: Long,
    val schoolName: String?,
    val isVerified: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(domain: Teacher) = TeacherResponse(
            id = domain.id,
            parentId = domain.parentId,
            schoolName = domain.schoolName,
            isVerified = domain.isVerified,
            createdAt = domain.createdAt
        )
    }
}

data class ClassroomResponse(
    val id: Long,
    val teacherId: Long,
    val name: String,
    val code: String,
    val subject: String?,
    val gradeLevel: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(domain: Classroom) = ClassroomResponse(
            id = domain.id,
            teacherId = domain.teacherId,
            name = domain.name,
            code = domain.code,
            subject = domain.subject,
            gradeLevel = domain.gradeLevel,
            createdAt = domain.createdAt
        )
    }
}

data class ClassroomStudentResponse(
    val classroomId: Long,
    val childId: Long,
    val joinedAt: Instant
) {
    companion object {
        fun from(domain: ClassroomStudent) = ClassroomStudentResponse(
            classroomId = domain.classroomId,
            childId = domain.childId,
            joinedAt = domain.joinedAt
        )
    }
}

data class CreateTeacherRequest(
    @field:NotNull(message = "Parent ID is required")
    @field:Positive(message = "Parent ID must be positive")
    val parentId: Long,
    
    val schoolName: String? = null
)

data class CreateClassroomRequest(
    @field:NotBlank(message = "Classroom name is required")
    val name: String,
    
    val subject: String? = null,
    val gradeLevel: String? = null
)

data class JoinClassroomRequest(
    @field:NotBlank(message = "Classroom code is required")
    val code: String,
    
    @field:NotNull(message = "Child ID is required")
    @field:Positive(message = "Child ID must be positive")
    val childId: Long
)

data class ClassroomWithStudentsResponse(
    val classroom: ClassroomResponse,
    val students: List<ClassroomStudentResponse>
)

data class TeacherClassroomsResponse(
    val teacher: TeacherResponse,
    val classrooms: List<ClassroomResponse>
)

data class ChildClassroomsResponse(
    val childId: Long,
    val classrooms: List<ClassroomResponse>
)