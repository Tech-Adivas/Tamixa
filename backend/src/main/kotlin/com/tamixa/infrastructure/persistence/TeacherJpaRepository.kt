package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface TeacherJpaRepository : JpaRepository<TeacherEntity, Long> {
    @Query("SELECT t FROM TeacherEntity t WHERE t.parent.id = :parentId")
    fun findByParentId(@Param("parentId") parentId: Long): TeacherEntity?
}

interface ClassroomJpaRepository : JpaRepository<ClassroomEntity, Long> {
    fun findByTeacherId(teacherId: Long): List<ClassroomEntity>
    fun findByCode(code: String): ClassroomEntity?
    fun findByTeacherIdAndNameContainingIgnoreCase(teacherId: Long, name: String): List<ClassroomEntity>
    
    @Query("SELECT c FROM ClassroomEntity c WHERE c.teacher.id = :teacherId AND c.name LIKE %:searchTerm%")
    fun searchByTeacherAndName(@Param("teacherId") teacherId: Long, @Param("searchTerm") searchTerm: String): List<ClassroomEntity>
}

interface ClassroomStudentJpaRepository : JpaRepository<ClassroomStudentEntity, Long> {
    fun findByClassroomId(classroomId: Long): List<ClassroomStudentEntity>
    fun findByChildId(childId: Long): List<ClassroomStudentEntity>
    fun findByClassroomIdAndChildId(classroomId: Long, childId: Long): ClassroomStudentEntity?
    fun existsByClassroomIdAndChildId(classroomId: Long, childId: Long): Boolean
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ClassroomStudentEntity cs WHERE cs.classroom.id = :classroomId AND cs.child.id = :childId")
    fun deleteByClassroomIdAndChildId(@Param("classroomId") classroomId: Long, @Param("childId") childId: Long)
    
    @Query("SELECT cs FROM ClassroomStudentEntity cs WHERE cs.classroom.teacher.id = :teacherId")
    fun findByTeacherId(@Param("teacherId") teacherId: Long): List<ClassroomStudentEntity>
}