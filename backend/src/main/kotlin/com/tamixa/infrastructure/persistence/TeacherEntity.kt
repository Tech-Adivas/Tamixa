package com.tamixa.infrastructure.persistence

import com.tamixa.domain.Classroom
import com.tamixa.domain.ClassroomStudent
import com.tamixa.domain.Teacher
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "teachers")
class TeacherEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @Column(name = "school_name", length = 255)
    val schoolName: String? = null,

    @Column(name = "verified_at")
    val verifiedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = Teacher(
        id = id,
        parentId = parent.id,
        schoolName = schoolName,
        verifiedAt = verifiedAt,
        createdAt = createdAt
    )
}

@Entity
@Table(name = "classrooms")
class ClassroomEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    val teacher: TeacherEntity,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(nullable = false, length = 10, unique = true)
    val code: String,

    @Column(length = 100)
    val subject: String? = null,

    @Column(name = "grade_level", length = 50)
    val gradeLevel: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = Classroom(
        id = id,
        teacherId = teacher.id,
        name = name,
        code = code,
        subject = subject,
        gradeLevel = gradeLevel,
        createdAt = createdAt
    )
}

@Entity
@Table(name = "classroom_students")
class ClassroomStudentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    val classroom: ClassroomEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    val child: ChildEntity,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: Instant = Instant.now()
) {
    fun toDomain() = ClassroomStudent(
        id = id,
        classroomId = classroom.id,
        childId = child.id,
        joinedAt = joinedAt
    )
}
