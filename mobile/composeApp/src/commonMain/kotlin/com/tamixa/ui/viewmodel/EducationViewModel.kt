package com.tamixa.ui.viewmodel

import com.tamixa.network.ChildVocabularyDto
import com.tamixa.network.ClassroomDto
import com.tamixa.network.QuizDto
import com.tamixa.network.QuizResultDto
import com.tamixa.network.ReadingLevelDto
import com.tamixa.network.ReadingStreakDto
import com.tamixa.network.VocabularyProgressDto
import com.tamixa.network.VocabularyWordDto
import com.tamixa.repository.EducationRepository
import com.tamixa.ui.AppMessageNotifier
import com.tamixa.ui.errorMessageForUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EducationViewModel(
    private val repository: EducationRepository,
    private val scope: CoroutineScope,
    private val appMessageNotifier: AppMessageNotifier? = null
) {
    // Reading level
    private val _readingLevel = MutableStateFlow<ReadingLevelDto?>(null)
    val readingLevel: StateFlow<ReadingLevelDto?> = _readingLevel.asStateFlow()

    private val _readingLevelLoading = MutableStateFlow(false)
    val readingLevelLoading: StateFlow<Boolean> = _readingLevelLoading.asStateFlow()

    private val _readingLevelError = MutableStateFlow<String?>(null)
    val readingLevelError: StateFlow<String?> = _readingLevelError.asStateFlow()

    // Streak
    private val _streak = MutableStateFlow<ReadingStreakDto?>(null)
    val streak: StateFlow<ReadingStreakDto?> = _streak.asStateFlow()

    private val _streakLoading = MutableStateFlow(false)
    val streakLoading: StateFlow<Boolean> = _streakLoading.asStateFlow()

    private val _streakError = MutableStateFlow<String?>(null)
    val streakError: StateFlow<String?> = _streakError.asStateFlow()

    // Vocabulary
    private val _vocabProgress = MutableStateFlow<VocabularyProgressDto?>(null)
    val vocabProgress: StateFlow<VocabularyProgressDto?> = _vocabProgress.asStateFlow()

    private val _learnedWords = MutableStateFlow<List<ChildVocabularyDto>>(emptyList())
    val learnedWords: StateFlow<List<ChildVocabularyDto>> = _learnedWords.asStateFlow()

    private val _suggestions = MutableStateFlow<List<VocabularyWordDto>>(emptyList())
    val suggestions: StateFlow<List<VocabularyWordDto>> = _suggestions.asStateFlow()

    private val _vocabLoading = MutableStateFlow(false)
    val vocabLoading: StateFlow<Boolean> = _vocabLoading.asStateFlow()

    private val _vocabError = MutableStateFlow<String?>(null)
    val vocabError: StateFlow<String?> = _vocabError.asStateFlow()

    // Classrooms
    private val _classrooms = MutableStateFlow<List<ClassroomDto>>(emptyList())
    val classrooms: StateFlow<List<ClassroomDto>> = _classrooms.asStateFlow()

    private val _classroomsLoading = MutableStateFlow(false)
    val classroomsLoading: StateFlow<Boolean> = _classroomsLoading.asStateFlow()

    private val _classroomsError = MutableStateFlow<String?>(null)
    val classroomsError: StateFlow<String?> = _classroomsError.asStateFlow()

    private val _joinResult = MutableStateFlow<ClassroomDto?>(null)
    val joinResult: StateFlow<ClassroomDto?> = _joinResult.asStateFlow()

    private val _joinLoading = MutableStateFlow(false)
    val joinLoading: StateFlow<Boolean> = _joinLoading.asStateFlow()

    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError.asStateFlow()

    // Quiz
    private val _quiz = MutableStateFlow<QuizDto?>(null)
    val quiz: StateFlow<QuizDto?> = _quiz.asStateFlow()

    private val _quizResult = MutableStateFlow<QuizResultDto?>(null)
    val quizResult: StateFlow<QuizResultDto?> = _quizResult.asStateFlow()

    private val _quizLoading = MutableStateFlow(false)
    val quizLoading: StateFlow<Boolean> = _quizLoading.asStateFlow()

    private val _quizError = MutableStateFlow<String?>(null)
    val quizError: StateFlow<String?> = _quizError.asStateFlow()

    // ── Actions ───────────────────────────────────────────────────────────────

    fun loadReadingLevel(childId: Long) {
        scope.launch {
            _readingLevelLoading.value = true
            _readingLevelError.value = null
            repository.getReadingLevel(childId).fold(
                onSuccess = { _readingLevel.value = it },
                onFailure = { _readingLevelError.value = errorMessageForUser(it); appMessageNotifier?.showError() }
            )
            _readingLevelLoading.value = false
        }
    }

    fun loadStreak(childId: Long) {
        scope.launch {
            _streakLoading.value = true
            _streakError.value = null
            repository.getStreak(childId).fold(
                onSuccess = { _streak.value = it },
                onFailure = { _streakError.value = errorMessageForUser(it); appMessageNotifier?.showError() }
            )
            _streakLoading.value = false
        }
    }

    fun recordRead(childId: Long) {
        scope.launch {
            repository.recordRead(childId).fold(
                onSuccess = { _streak.value = it },
                onFailure = { appMessageNotifier?.showError() }
            )
        }
    }

    fun loadVocabulary(childId: Long, language: String? = null) {
        scope.launch {
            _vocabLoading.value = true
            _vocabError.value = null
            repository.getVocabularyProgress(childId).fold(
                onSuccess = { _vocabProgress.value = it },
                onFailure = { _vocabError.value = errorMessageForUser(it) }
            )
            repository.getChildVocabulary(childId).fold(
                onSuccess = { _learnedWords.value = it },
                onFailure = { if (_vocabError.value == null) _vocabError.value = errorMessageForUser(it) }
            )
            repository.getWordSuggestions(childId, language).fold(
                onSuccess = { _suggestions.value = it },
                onFailure = { /* non-critical */ }
            )
            _vocabLoading.value = false
        }
    }

    fun markWordLearned(childId: Long, wordId: Long, masteryLevel: Int = 1) {
        scope.launch {
            repository.markWordLearned(childId, wordId, masteryLevel).fold(
                onSuccess = { loadVocabulary(childId) },
                onFailure = { appMessageNotifier?.showError() }
            )
        }
    }

    fun loadClassrooms(childId: Long) {
        scope.launch {
            _classroomsLoading.value = true
            _classroomsError.value = null
            repository.getChildClassrooms(childId).fold(
                onSuccess = { _classrooms.value = it },
                onFailure = { _classroomsError.value = errorMessageForUser(it); appMessageNotifier?.showError() }
            )
            _classroomsLoading.value = false
        }
    }

    fun joinClassroom(code: String, childId: Long) {
        scope.launch {
            _joinLoading.value = true
            _joinError.value = null
            repository.joinClassroom(code, childId).fold(
                onSuccess = {
                    _joinResult.value = it
                    loadClassrooms(childId)
                },
                onFailure = { _joinError.value = errorMessageForUser(it) }
            )
            _joinLoading.value = false
        }
    }

    fun clearJoinResult() {
        _joinResult.value = null
        _joinError.value = null
    }

    fun loadQuiz(storyId: Long) {
        scope.launch {
            _quizLoading.value = true
            _quizError.value = null
            _quizResult.value = null
            _quiz.value = null
            repository.getQuizForStory(storyId).fold(
                onSuccess = { _quiz.value = it },
                onFailure = { _quizError.value = errorMessageForUser(it) }
            )
            _quizLoading.value = false
        }
    }

    fun submitQuiz(quizId: Long, childId: Long, answers: Map<String, String>) {
        scope.launch {
            _quizLoading.value = true
            repository.submitQuiz(quizId, childId, answers).fold(
                onSuccess = { _quizResult.value = it },
                onFailure = { _quizError.value = errorMessageForUser(it) }
            )
            _quizLoading.value = false
        }
    }

    fun clearQuiz() {
        _quiz.value = null
        _quizResult.value = null
        _quizError.value = null
    }
}
