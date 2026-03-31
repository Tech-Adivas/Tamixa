package com.tamixa.api.education

import com.tamixa.api.education.dto.*
import com.tamixa.application.service.VocabularyService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/vocabulary")
@PreAuthorize("hasRole('PARENT')")
class VocabularyController(
    private val vocabularyService: VocabularyService
) {
    private val log = LoggerFactory.getLogger(VocabularyController::class.java)

    @GetMapping("/words")
    fun getWords(
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) difficulty: Int?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<VocabularyWordsPageResponse> {
        val result = vocabularyService.searchWords("", page, size).let {
            val words = vocabularyService.getWordsForLanguageAndDifficulty(language, difficulty)
            val start = page * size
            val end = minOf(start + size, words.size)
            VocabularyWordsPageResponse(
                content = words.subList(start, minOf(end, words.size)).map { VocabularyWordResponse.from(it) },
                totalElements = words.size.toLong(),
                totalPages = (words.size + size - 1) / size,
                page = page,
                first = page == 0,
                last = end >= words.size
            )
        }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/words/{wordId}")
    fun getWordById(@PathVariable wordId: Long): ResponseEntity<VocabularyWordResponse> {
        val word = vocabularyService.getWordById(wordId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(VocabularyWordResponse.from(word))
    }

    @PostMapping("/words")
    @PreAuthorize("hasRole('ADMIN')")
    fun createWord(@Valid @RequestBody request: CreateWordRequest): ResponseEntity<VocabularyWordResponse> {
        val word = vocabularyService.createWord(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(VocabularyWordResponse.from(word))
    }

    @PostMapping("/learn")
    fun markWordAsLearned(@Valid @RequestBody request: LearnWordRequest): ResponseEntity<ChildVocabularyResponse> {
        val childVocabulary = vocabularyService.markWordAsLearned(request.childId, request.wordId, request.masteryLevel)
        val word = vocabularyService.getWordById(request.wordId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(HttpStatus.CREATED).body(ChildVocabularyResponse.from(childVocabulary, word))
    }

    @GetMapping("/child/{childId}/progress")
    fun getChildProgress(@PathVariable childId: Long): ResponseEntity<VocabularyProgressResponse> {
        return ResponseEntity.ok(vocabularyService.getChildProgress(childId))
    }

    @GetMapping("/child/{childId}/words")
    fun getChildVocabulary(@PathVariable childId: Long): ResponseEntity<List<ChildVocabularyResponse>> {
        val childVocab = vocabularyService.getChildVocabulary(childId)
        val wordMap = vocabularyService.getWordsByIds(childVocab.map { it.wordId }).associateBy { it.id }
        val responses = childVocab.mapNotNull { cv -> wordMap[cv.wordId]?.let { ChildVocabularyResponse.from(cv, it) } }
        return ResponseEntity.ok(responses)
    }

    @GetMapping("/child/{childId}/suggestions")
    fun getWordSuggestions(
        @PathVariable childId: Long,
        @RequestParam(required = false) language: String?,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<VocabularyWordResponse>> {
        return ResponseEntity.ok(vocabularyService.getWordSuggestions(childId, language, limit).map { VocabularyWordResponse.from(it) })
    }

    @GetMapping("/child/{childId}/mastery")
    fun getMasteryLevels(@PathVariable childId: Long): ResponseEntity<MasteryLevelsResponse> {
        return ResponseEntity.ok(MasteryLevelsResponse(vocabularyService.getMasteryLevels(childId)))
    }

    @GetMapping("/stats")
    fun getVocabularyStats(): ResponseEntity<VocabularyStatsResponse> {
        return ResponseEntity.ok(vocabularyService.getVocabularyStats())
    }

    @GetMapping("/search")
    fun searchWords(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<VocabularyWordsPageResponse> {
        return ResponseEntity.ok(vocabularyService.searchWords(query, page, size))
    }

    @GetMapping("/words/difficulty/{difficulty}")
    fun getWordsByDifficulty(
        @PathVariable difficulty: Int,
        @RequestParam(required = false) language: String?
    ): ResponseEntity<List<VocabularyWordResponse>> {
        return ResponseEntity.ok(vocabularyService.getWordsByDifficulty(difficulty, language).map { VocabularyWordResponse.from(it) })
    }
}
