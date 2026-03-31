package com.tamixa.application.service

import com.tamixa.api.education.dto.*
import com.tamixa.application.port.VocabularyRepositoryPort
import com.tamixa.domain.ChildVocabulary
import com.tamixa.domain.VocabularyWord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class VocabularyService(
    private val vocabularyRepository: VocabularyRepositoryPort
) {
    private val log = LoggerFactory.getLogger(VocabularyService::class.java)

    fun getWordsForLanguageAndDifficulty(language: String?, difficulty: Int?): List<VocabularyWord> {
        return if (language != null) {
            if (difficulty != null) {
                vocabularyRepository.findWordsByLanguageAndDifficulty(language, difficulty)
            } else {
                vocabularyRepository.findWordsByLanguage(language)
            }
        } else {
            vocabularyRepository.findAllWords()
        }
    }

    fun getWordById(wordId: Long): VocabularyWord? {
        return vocabularyRepository.findWordById(wordId)
    }

    fun createWord(request: CreateWordRequest): VocabularyWord {
        val word = VocabularyWord(
            id = 0,
            word = request.word,
            definition = request.definition,
            language = request.language,
            difficultyLevel = request.difficultyLevel,
            exampleSentence = request.exampleSentence,
            createdAt = Instant.now()
        )
        return vocabularyRepository.saveWord(word)
    }

    fun markWordAsLearned(childId: Long, wordId: Long, masteryLevel: Int): ChildVocabulary {
        val existing = vocabularyRepository.findChildVocabularyByChildIdAndWordId(childId, wordId)
        return if (existing != null) {
            if (masteryLevel > existing.masteryLevel) {
                val updated = existing.copy(masteryLevel = masteryLevel)
                vocabularyRepository.saveChildVocabulary(updated)
            } else {
                existing
            }
        } else {
            val childVocab = ChildVocabulary(
                id = 0,
                childId = childId,
                wordId = wordId,
                masteryLevel = masteryLevel,
                learnedAt = Instant.now()
            )
            vocabularyRepository.saveChildVocabulary(childVocab)
        }
    }

    fun getChildProgress(childId: Long): VocabularyProgressResponse {
        val totalWords = vocabularyRepository.countAllWords()
        val learnedWords = vocabularyRepository.countLearnedWordsByChild(childId)
        val masteredWords = vocabularyRepository.countMasteredWordsByChild(childId, ChildVocabulary.MASTERY_PROFICIENT)
        return VocabularyProgressResponse(
            totalWords = totalWords,
            wordsLearned = learnedWords,
            wordsMastered = masteredWords,
            progressPercentage = if (totalWords > 0) (learnedWords.toDouble() / totalWords * 100).toInt() else 0
        )
    }

    fun getChildVocabulary(childId: Long): List<ChildVocabulary> {
        return vocabularyRepository.findChildVocabularyByChildId(childId)
    }

    fun getWordSuggestions(childId: Long, language: String?, limit: Int): List<VocabularyWord> {
        val learnedWords = vocabularyRepository.findChildVocabularyByChildId(childId)
            .map { it.wordId }.toSet()
        val allWords = if (language != null) {
            vocabularyRepository.findWordsByLanguage(language)
        } else {
            vocabularyRepository.findAllWords()
        }
        return allWords.filterNot { learnedWords.contains(it.id) }.take(limit)
    }

    fun getMasteryLevels(childId: Long): Map<Int, Int> {
        return vocabularyRepository.findChildVocabularyByChildId(childId)
            .groupBy { it.masteryLevel }
            .mapValues { (_, list) -> list.size }
    }

    fun getVocabularyStats(): VocabularyStatsResponse {
        val allWords = vocabularyRepository.findAllWords()
        val languages = allWords.groupBy { it.language }.mapValues { (_, words) -> words.size }
        val difficultyDistribution = (1..4).associateWith { level -> allWords.count { it.difficultyLevel == level } }
        return VocabularyStatsResponse(
            totalWords = allWords.size,
            languages = languages,
            difficultyDistribution = difficultyDistribution
        )
    }

    fun searchWords(query: String, page: Int, size: Int): VocabularyWordsPageResponse {
        val filtered = vocabularyRepository.findAllWords().filter {
            it.word.contains(query, ignoreCase = true) || it.definition.contains(query, ignoreCase = true)
        }
        val start = page * size
        val end = minOf(start + size, filtered.size)
        return VocabularyWordsPageResponse(
            content = filtered.subList(start, end).map { VocabularyWordResponse.from(it) },
            totalElements = filtered.size.toLong(),
            totalPages = (filtered.size + size - 1) / size,
            page = page,
            first = page == 0,
            last = end >= filtered.size
        )
    }

    fun getWordsByDifficulty(difficulty: Int, language: String?): List<VocabularyWord> {
        return if (language != null) {
            vocabularyRepository.findWordsByLanguageAndDifficulty(language, difficulty)
        } else {
            vocabularyRepository.findWordsByDifficulty(difficulty)
        }
    }

    fun getWordsByLanguage(language: String): List<VocabularyWord> {
        return vocabularyRepository.findWordsByLanguage(language)
    }

    fun getWordsByIds(wordIds: List<Long>): List<VocabularyWord> {
        return wordIds.mapNotNull { vocabularyRepository.findWordById(it) }
    }
}