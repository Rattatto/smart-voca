// 2026-11-18 - Smart Voca - 시험 대상 조회와 시험 이력 저장 (FR-031, FR-036, FR-037)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/ExamRepository.kt

package com.rattatto.smartvoca.data

import com.rattatto.smartvoca.data.db.ExamAttemptEntity
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.data.db.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

class ExamRepository(private val database: SmartVocaDatabase) {

    private val examDao = database.examDao()
    private val reviewDao = database.reviewDao()

    /** 오늘 재노출 대상 단어. 오늘 목록 단어와 합쳐서 시험 문제를 고른다 (FR-031). */
    suspend fun reviewWordsDue(date: LocalDate): List<WordEntity> = withContext(Dispatchers.IO) {
        reviewDao.dueWords(date.toString())
    }

    /**
     * 답 한 번을 저장한다 (FR-036). 재노출 일정은 바꾸지 않는다 (FR-037).
     */
    suspend fun recordAttempt(
        wordId: Long,
        date: LocalDate,
        shownMeaning: String,
        userInput: String,
        isCorrect: Boolean,
        now: Instant = Instant.now(),
    ) = withContext(Dispatchers.IO) {
        examDao.insert(
            ExamAttemptEntity(
                wordId = wordId,
                attemptDate = date.toString(),
                attemptedAt = now.toString(),
                shownMeaning = shownMeaning,
                userInput = userInput,
                isCorrect = if (isCorrect) 1 else 0,
            ),
        )
    }

    /** 날짜의 시험 이력 (입력 순서). */
    suspend fun attemptsOn(date: LocalDate): List<ExamAttemptEntity> = withContext(Dispatchers.IO) {
        examDao.findByDate(date.toString())
    }
}
