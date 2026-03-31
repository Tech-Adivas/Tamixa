package com.tamixa.infrastructure.adapter

import com.tamixa.application.port.TeacherRepositoryPort
import com.tamixa.domain.Classroom
import com.tamixa.domain.ClassroomStudent
import com.tamixa.domain.Teacher
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.ClassroomEntity
import com.tamixa.infrastructure.persistence.ClassroomJpaRepository
import com.tamixa.infrastructure.persistence.ClassroomStudentEntity
import com.tamixa.infrastructure.persistence.ClassroomStudentJpaRepository
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import com.tamixa.infrastructure.persistence.TeacherEntity
import com.tamixa.infrastructure.persistence.TeacherJpaRepository
import org.springframework.stereotype.Repository

@Repository
class TeacherRepositoryAdapter(
    private val teacherJpaRepository: TeacherJpaRepository,
    private val classroomJpaRepository: ClassroomJpaRepository,
    private val classroomStudentJpaRepository: ClassroomStudentJpaRepository,
    private val parentJpaRepository: ParentJpaRepository,
    private val childJpaRepository: ChildJpaRepository
) : TeacherRepositoryPort {

    override fun saveTeacher(teacher: Teacher): Teacher {
        val parent = parentJpaRepository.getReferenceById(teacher.parentId)
        return teacherJpaRepository.save(
            TeacherEntity(
                id = teacher.id,
                parent = parent,
                schoolName = teacher.schoolName,
                verifiedAt = teacher.verifiedAt,
                createdAt = teacher.createdAt
            )
        ).toDomain()
    }

    override fun findTeacherById(teacherId: Long): Teacher? =
        teacherJpaRepository.findById(teacherId).orElse(null)?.toDomain()

    override fun findTeacherByParentId(parentId: Long): Teacher? =
        teacherJpaRepository.findByParentId(parentId)?.toDomain()

    override fun findAllTeachers(): List<Teacher> =
        teacherJpaRepository.findAll().map { it.toDomain() }

    override fun saveClassroom(classroom: Classroom): Classroom {
        val teacher = teacherJpaRepository.findById(classroom.teacherId)
            .orElseThrow { IllegalArgumentException("Teacher not found: ${classroom.teacherId}") }
        return classroomJpaRepository.save(
            ClassroomEntity(
                id = classroom.id,
                teacher = teacher,
                name = classroom.name,
                code = classroom.code,
                subject = classroom.subject,
                gradeLevel = classroom.gradeLevel,
                createdAt = classroom.createdAt
            )
        ).toDomain()
    }

    override fun findClassroomById(classroomId: Long): Classroom? =
        classroomJpaRepository.findById(classroomId).orElse(null)?.toDomain()

    override fun findClassroomByCode(code: String): Classroom? =
        classroomJpaRepository.findByCode(code)?.toDomain()

    override fun findClassroomsByTeacherId(teacherId: Long): List<Classroom> =
        classroomJpaRepository.findByTeacherId(teacherId).map { it.toDomain() }

    override fun saveClassroomStudent(classroomStudent: ClassroomStudent): ClassroomStudent {
        val classroom = classroomJpaRepository.findById(classroomStudent.classroomId)
            .orElseThrow { IllegalArgumentException("Classroom not found: ${classroomStudent.classroomId}") }
        val child = childJpaRepository.getReferenceById(classroomStudent.childId)
        return classroomStudentJpaRepository.save(
            ClassroomStudentEntity(
                id = classroomStudent.id,
                classroom = classroom,
                child = child,
                joinedAt = classroomStudent.joinedAt
            )
        ).toDomain()
    }

    override fun findClassroomStudents(classroomId: Long): List<ClassroomStudent> =
        classroomStudentJpaRepository.findByClassroomId(classroomId).map { it.toDomain() }

    override fun findClassroomStudent(classroomId: Long, childId: Long): ClassroomStudent? =
        classroomStudentJpaRepository.findByClassroomIdAndChildId(classroomId, childId)?.toDomain()

    override fun deleteClassroomStudent(classroomId: Long, childId: Long) =
        classroomStudentJpaRepository.deleteByClassroomIdAndChildId(classroomId, childId)

    override fun findClassroomsByChildId(childId: Long): List<Classroom> =
        classroomStudentJpaRepository.findByChildId(childId).map { it.classroom.toDomain() }

    override fun findAllTeacherIds(): List<Long> =
        teacherJpaRepository.findAll().map { it.id }
}
