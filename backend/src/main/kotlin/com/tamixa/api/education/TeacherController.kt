package com.tamixa.api.education

import com.tamixa.api.education.dto.*
import com.tamixa.application.service.TeacherService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/teachers")
@PreAuthorize("hasRole('PARENT')")
class TeacherController(
    private val teacherService: TeacherService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun createTeacher(@Valid @RequestBody request: CreateTeacherRequest): ResponseEntity<TeacherResponse> {
        val teacher = teacherService.createTeacher(request.parentId, request.schoolName)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TeacherResponse.from(teacher))
    }

    @GetMapping("/{teacherId}")
    fun getTeacher(@PathVariable teacherId: Long): ResponseEntity<TeacherResponse> {
        val teacher = teacherService.getTeacherById(teacherId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(TeacherResponse.from(teacher))
    }

    @GetMapping("/parent/{parentId}")
    fun getTeacherByParent(@PathVariable parentId: Long): ResponseEntity<TeacherResponse> {
        val teacher = teacherService.getTeacherByParentId(parentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(TeacherResponse.from(teacher))
    }

    @PostMapping("/{teacherId}/classrooms")
    fun createClassroom(
        @PathVariable teacherId: Long,
        @Valid @RequestBody request: CreateClassroomRequest
    ): ResponseEntity<ClassroomResponse> {
        val classroom = teacherService.createClassroom(teacherId, request.name, request.subject, request.gradeLevel)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ClassroomResponse.from(classroom))
    }

    @GetMapping("/{teacherId}/classrooms")
    fun getTeacherClassrooms(@PathVariable teacherId: Long): ResponseEntity<List<ClassroomResponse>> {
        val classrooms = teacherService.getClassroomsByTeacher(teacherId)
        return ResponseEntity.ok(classrooms.map { ClassroomResponse.from(it) })
    }

    @GetMapping("/classrooms/{classroomId}")
    fun getClassroom(@PathVariable classroomId: Long): ResponseEntity<ClassroomResponse> {
        val classroom = teacherService.getClassroomById(classroomId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ClassroomResponse.from(classroom))
    }

    @GetMapping("/classrooms/code/{code}")
    fun getClassroomByCode(@PathVariable code: String): ResponseEntity<ClassroomResponse> {
        val classroom = teacherService.getClassroomByCode(code)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ClassroomResponse.from(classroom))
    }

    @PostMapping("/classrooms/join")
    fun joinClassroom(@Valid @RequestBody request: JoinClassroomRequest): ResponseEntity<ClassroomStudentResponse> {
        val classroom = teacherService.getClassroomByCode(request.code)
            ?: return ResponseEntity.notFound().build()
        
        teacherService.addStudentToClassroom(classroom.id, request.childId)
        val classroomStudent = teacherService.getClassroomStudent(classroom.id, request.childId)
            ?: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ClassroomStudentResponse.from(classroomStudent))
    }

    @GetMapping("/classrooms/{classroomId}/students")
    fun getClassroomStudents(@PathVariable classroomId: Long): ResponseEntity<List<ClassroomStudentResponse>> {
        val students = teacherService.getClassroomStudents(classroomId)
        return ResponseEntity.ok(students.map { ClassroomStudentResponse.from(it) })
    }

    @DeleteMapping("/classrooms/{classroomId}/students/{childId}")
    fun removeStudentFromClassroom(
        @PathVariable classroomId: Long,
        @PathVariable childId: Long
    ): ResponseEntity<Void> {
        teacherService.removeStudentFromClassroom(classroomId, childId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/child/{childId}/classrooms")
    fun getChildClassrooms(@PathVariable childId: Long): ResponseEntity<List<ClassroomResponse>> {
        val classrooms = teacherService.getClassroomsForChild(childId)
        return ResponseEntity.ok(classrooms.map { ClassroomResponse.from(it) })
    }

    @GetMapping("/classrooms/{classroomId}/with-students")
    fun getClassroomWithStudents(@PathVariable classroomId: Long): ResponseEntity<ClassroomWithStudentsResponse> {
        val classroomWithStudents = teacherService.getClassroomWithStudents(classroomId)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(
            ClassroomWithStudentsResponse(
                classroom = ClassroomResponse.from(classroomWithStudents.classroom),
                students = classroomWithStudents.students.map { ClassroomStudentResponse.from(it) }
            )
        )
    }
}