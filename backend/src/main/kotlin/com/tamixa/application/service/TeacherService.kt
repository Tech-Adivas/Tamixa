package com.tamixa.application.service

import com.tamixa.application.port.TeacherRepositoryPort
import com.tamixa.domain.Classroom
import com.tamixa.domain.ClassroomStudent
import com.tamixa.domain.Teacher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class TeacherService(
    private val teacherRepository: TeacherRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    fun createTeacher(parentId: Long, schoolName: String? = null): Teacher {
        val teacher = Teacher(
            id = 0,
            parentId = parentId,
            schoolName = schoolName,
            verifiedAt = null,
            createdAt = Instant.now()
        )
        return teacherRepository.saveTeacher(teacher)
    }
    
    fun getTeacherByParentId(parentId: Long): Teacher? {
        return teacherRepository.findTeacherByParentId(parentId)
    }
    
    fun getTeacherById(teacherId: Long): Teacher? {
        return teacherRepository.findTeacherById(teacherId)
    }
    
    fun createClassroom(teacherId: Long, name: String, subject: String? = null, gradeLevel: String? = null): Classroom {
        val classroom = Classroom(
            id = 0,
            teacherId = teacherId,
            name = name,
            code = generateClassroomCode(),
            subject = subject,
            gradeLevel = gradeLevel,
            createdAt = Instant.now()
        )
        return teacherRepository.saveClassroom(classroom)
    }
    
    fun getClassroomByCode(code: String): Classroom? {
        return teacherRepository.findClassroomByCode(code)
    }
    
    fun getClassroomById(classroomId: Long): Classroom? {
        return teacherRepository.findClassroomById(classroomId)
    }
    
    fun getClassroomsByTeacher(teacherId: Long): List<Classroom> {
        return teacherRepository.findClassroomsByTeacherId(teacherId)
    }
    
    fun addStudentToClassroom(classroomId: Long, childId: Long) {
        val existing = teacherRepository.findClassroomStudent(classroomId, childId)
        if (existing != null) {
            return // Already enrolled
        }
        
        val classroomStudent = ClassroomStudent(
            id = 0,
            classroomId = classroomId,
            childId = childId,
            joinedAt = Instant.now()
        )
        teacherRepository.saveClassroomStudent(classroomStudent)
    }
    
    fun removeStudentFromClassroom(classroomId: Long, childId: Long) {
        teacherRepository.deleteClassroomStudent(classroomId, childId)
    }
    
    fun getClassroomStudents(classroomId: Long): List<ClassroomStudent> {
        return teacherRepository.findClassroomStudents(classroomId)
    }
    
    fun getClassroomsForChild(childId: Long): List<Classroom> {
        return teacherRepository.findClassroomsByChildId(childId)
    }
    
    fun getClassroomWithStudents(classroomId: Long): ClassroomWithStudents? {
        val classroom = teacherRepository.findClassroomById(classroomId) ?: return null
        val students = teacherRepository.findClassroomStudents(classroomId)
        return ClassroomWithStudents(classroom, students)
    }
    
    fun getClassroomStudent(classroomId: Long, childId: Long): ClassroomStudent? {
        return teacherRepository.findClassroomStudent(classroomId, childId)
    }
    
    private fun generateClassroomCode(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
    
    data class ClassroomWithStudents(
        val classroom: Classroom,
        val students: List<ClassroomStudent>
    )
}