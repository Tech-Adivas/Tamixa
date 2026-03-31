package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QuizJpaRepository : JpaRepository<QuizEntity, Long> {
    fun findByStoryId(storyId: Long): QuizEntity?
}

interface QuizResponseJpaRepository : JpaRepository<QuizResponseEntity, Long> {
    @Query("SELECT r FROM QuizResponseEntity r WHERE r.child.id = :childId ORDER BY r.completedAt DESC")
    fun findByChildIdOrderByCompletedAtDesc(@Param("childId") childId: Long): List<QuizResponseEntity>

    @Query("SELECT r FROM QuizResponseEntity r WHERE r.quiz.id = :quizId ORDER BY r.completedAt DESC")
    fun findByQuizIdOrderByCompletedAtDesc(@Param("quizId") quizId: Long): List<QuizResponseEntity>
}
