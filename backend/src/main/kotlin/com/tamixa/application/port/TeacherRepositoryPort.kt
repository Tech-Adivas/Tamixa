package com.tamixa.application.port

import com.tamixa.domain.Teacher
import com.tamixa.domain.Classroom
import com.tamixa.domain.ClassroomStudent

interface TeacherRepositoryPort {
    fun saveTeacher(teacher: Teacher): Teacher
    fun findTeacherById(teacherId: Long): Teacher?
    fun findTeacherByParentId(parentId: Long): Teacher?
    fun findAllTeachers(): List<Teacher>
    
    fun saveClassroom(classroom: Classroom): Classroom
    fun findClassroomById(classroomId: Long): Classroom?
    fun findClassroomByCode(code: String): Classroom?
    fun findClassroomsByTeacherId(teacherId: Long): List<Classroom>
    
    fun saveClassroomStudent(classroomStudent: ClassroomStudent): ClassroomStudent
    fun findClassroomStudents(classroomId: Long): List<ClassroomStudent>
    fun findClassroomStudent(classroomId: Long, childId: Long): ClassroomStudent?
    fun deleteClassroomStudent(classroomId: Long, childId: Long)
    fun findClassroomsByChildId(childId: Long): List<Classroom>
    fun findAllTeacherIds(): List<Long>
}