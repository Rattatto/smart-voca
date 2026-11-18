// 2026-11-18 - Smart Voca - 시험 이력, 단계 저장, 재노출 일정 불변 테스트 (메모리 DB, Robolectric) (FR-036, FR-037, FR-038)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/data/ExamRepositoryTest.kt

package com.rattatto.smartvoca.data

import android.content.Context
import androidx.room.Room
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.domain.QuestionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ExamRepositoryTest {

    private val day1 = LocalDate.of(2026, 11, 11)
    private val day2 = LocalDate.of(2026, 11, 12)
    private val day3 = LocalDate.of(2026, 11, 13)
    private val now = Instant.parse("2026-11-11T01:00:00Z")

    private lateinit var database: SmartVocaDatabase
    private lateinit var wordRepository: WordRepository
    private lateinit var studyRepository: StudyRepository
    private lateinit var dailyRepository: DailyListRepository
    private lateinit var examRepository: ExamRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication() as Context
        database = Room.inMemoryDatabaseBuilder(context, SmartVocaDatabase::class.java).build()
        wordRepository = WordRepository(database)
        studyRepository = StudyRepository(database)
        dailyRepository = DailyListRepository(database)
        examRepository = ExamRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun importGo(): Long {
        wordRepository.importCsv("word,meaning_ko,freq_rank,level\ngo,가다,1,basic")
        return dailyRepository.getOrCreate(day1, 10, null, now).first().word.id
    }

    @Test
    fun `시험 답은 날짜, 문제 형식과 함께 저장되고 정답 여부는 0 또는 1 이다`() = runBlocking {
        val wordId = importGo()

        examRepository.recordAttempt(wordId, day1, "가다", "went", isCorrect = false, type = QuestionType.TYPED, now = now)
        examRepository.recordAttempt(wordId, day1, "가다", "뜻 가다", isCorrect = true, type = QuestionType.MULTIPLE_CHOICE, now = now)

        val attempts = examRepository.attemptsOn(day1)
        assertEquals(listOf("went", "뜻 가다"), attempts.map { it.userInput })
        assertEquals(listOf(0, 1), attempts.map { it.isCorrect })
        assertEquals(listOf("typed", "mc"), attempts.map { it.questionType })
    }

    @Test
    fun `객관식 연속 3일 정답이면 단계가 주관식으로 바뀐다`() = runBlocking {
        val wordId = importGo()

        examRepository.recordAttempt(wordId, day1, "가다", "뜻 가다", true, QuestionType.MULTIPLE_CHOICE, now)
        examRepository.recordAttempt(wordId, day2, "가다", "뜻 가다", true, QuestionType.MULTIPLE_CHOICE, now)
        val after = examRepository.recordAttempt(wordId, day3, "가다", "뜻 가다", true, QuestionType.MULTIPLE_CHOICE, now)

        assertEquals(QuestionType.TYPED, after.type)
        assertEquals(QuestionType.TYPED, examRepository.stageOf(wordId).type)
    }

    @Test
    fun `주관식에서 틀리면 객관식으로 강등된다`() = runBlocking {
        val wordId = importGo()
        repeat(3) { offset ->
            examRepository.recordAttempt(wordId, day1.plusDays(offset.toLong()), "가다", "뜻 가다", true, QuestionType.MULTIPLE_CHOICE, now)
        }

        val after = examRepository.recordAttempt(wordId, day3.plusDays(1), "가다", "went", false, QuestionType.TYPED, now)

        assertEquals(QuestionType.MULTIPLE_CHOICE, after.type)
    }

    @Test
    fun `시험 결과는 재노출 일정을 바꾸지 않는다`() = runBlocking {
        val wordId = importGo()
        studyRepository.markStudied(wordId, day1, now)

        examRepository.recordAttempt(wordId, day1, "가다", "xx", isCorrect = false, type = QuestionType.MULTIPLE_CHOICE, now = now)

        val due = dailyRepository.getOrCreate(day2, 10, null, now).filter { it.source == "review" }
        assertEquals(listOf("go"), due.map { it.word.word })
    }

    @Test
    fun `시험 문제는 오늘 목록 단어로 만들고 객관식 보기는 정답을 포함한다`() = runBlocking {
        wordRepository.importCsv(
            "word,meaning_ko,freq_rank,level\ngo,가다,1,basic\ncome,오다,2,basic\nmake,만들다,3,basic\ntake,가지다,4,basic\nsee,보다,5,basic",
        )
        val today = dailyRepository.getOrCreate(day1, 10, null, now).map { it.word }

        val questions = examRepository.buildQuestions(day1, examSize = 5, todayWords = today)

        assertEquals(5, questions.size)
        assertTrue(questions.filter { it.type == QuestionType.MULTIPLE_CHOICE }.all { q ->
            q.choices!!.contains(q.word.meaningKo)
        })
    }
}
