// 2026-11-18 - Smart Voca - 시험 출제 구성, 시험 이력과 단계 저장 (FR-031, FR-036, FR-037, FR-038)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/ExamRepository.kt

package com.rattatto.smartvoca.data

import androidx.room.withTransaction
import com.rattatto.smartvoca.data.db.ExamAttemptEntity
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.data.db.WordEntity
import com.rattatto.smartvoca.data.db.WordStageEntity
import com.rattatto.smartvoca.domain.ExamPlanner
import com.rattatto.smartvoca.domain.ExamQuestion
import com.rattatto.smartvoca.domain.QuestionType
import com.rattatto.smartvoca.domain.StageRules
import com.rattatto.smartvoca.domain.StageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

class ExamRepository(private val database: SmartVocaDatabase) {

    private val examDao = database.examDao()
    private val reviewDao = database.reviewDao()
    private val wordDao = database.wordDao()
    private val stageDao = database.stageDao()

    /**
     * 오늘 시험 문제를 만든다 (P4). 후보는 [todayWords] 와 오늘 재노출 대상이다.
     * 단계 (객관식/주관식)에 따라 문제 형식을 정하고, 객관식 보기는 전체 단어에서 뽑는다.
     */
    suspend fun buildQuestions(
        date: LocalDate,
        examSize: Int,
        todayWords: List<WordEntity>,
        random: Random = Random.Default,
    ): List<ExamQuestion> = withContext(Dispatchers.IO) {
        val reviewWords = reviewDao.dueWords(date.toString())
        val candidates = (todayWords + reviewWords).distinctBy { it.id }
        val typedIds = stageDao.findAll()
            .filter { QuestionType.fromDb(it.type) == QuestionType.TYPED }
            .map { it.wordId }
            .toSet()
        ExamPlanner.plan(
            candidates = candidates,
            examSize = examSize,
            typedStage = { it.id in typedIds },
            pool = wordDao.findAllOrdered(),
            random = random,
        )
    }

    /**
     * 답 한 번을 저장한다 (FR-036). 같은 트랜잭션에서 단어의 시험 단계를 갱신하고 새 단계를 돌려준다 (FR-038).
     * 재노출 일정은 바꾸지 않는다 (FR-037).
     */
    suspend fun recordAttempt(
        wordId: Long,
        date: LocalDate,
        shownMeaning: String,
        userInput: String,
        isCorrect: Boolean,
        type: QuestionType,
        now: Instant = Instant.now(),
    ): StageState = withContext(Dispatchers.IO) {
        database.withTransaction {
            examDao.insert(
                ExamAttemptEntity(
                    wordId = wordId,
                    attemptDate = date.toString(),
                    attemptedAt = now.toString(),
                    shownMeaning = shownMeaning,
                    userInput = userInput,
                    isCorrect = if (isCorrect) 1 else 0,
                    questionType = type.dbValue,
                ),
            )
            val before = stageDao.find(wordId)?.toState() ?: StageState.DEFAULT
            val after = StageRules.afterAnswer(before, type, isCorrect, date, now)
            stageDao.save(after.toEntity(wordId))
            after
        }
    }

    /** 단어의 현재 시험 단계. 기록이 없으면 객관식. */
    suspend fun stageOf(wordId: Long): StageState = withContext(Dispatchers.IO) {
        stageDao.find(wordId)?.toState() ?: StageState.DEFAULT
    }

    /** 날짜의 시험 이력 (입력 순서). */
    suspend fun attemptsOn(date: LocalDate): List<ExamAttemptEntity> = withContext(Dispatchers.IO) {
        examDao.findByDate(date.toString())
    }

    private fun WordStageEntity.toState(): StageState = StageState(
        type = QuestionType.fromDb(type),
        streakDays = streakDays,
        lastCountedDate = lastCountedDate?.let { LocalDate.parse(it) },
        promotedAt = promotedAt?.let { Instant.parse(it) },
    )

    private fun StageState.toEntity(wordId: Long): WordStageEntity = WordStageEntity(
        wordId = wordId,
        type = type.dbValue,
        streakDays = streakDays,
        lastCountedDate = lastCountedDate?.toString(),
        promotedAt = promotedAt?.toString(),
    )
}
