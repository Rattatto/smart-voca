// 2026-11-18 - Smart Voca - 시험 이력 저장과 재노출 일정 불변 테스트 (메모리 DB, Robolectric) (FR-036, FR-037)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/data/ExamRepositoryTest.kt

package com.rattatto.smartvoca.data

import android.content.Context
import androidx.room.Room
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
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

    @Test
    fun `시험 답은 날짜와 함께 저장되고 정답 여부는 0 또는 1 이다`() = runBlocking {
        wordRepository.importCsv("word,meaning_ko,freq_rank,level\ngo,가다,1,basic")
        val word = dailyRepository.getOrCreate(day1, 10, null, now).first().word

        examRepository.recordAttempt(word.id, day1, shownMeaning = "가다", userInput = "went", isCorrect = false, now = now)
        examRepository.recordAttempt(word.id, day1, shownMeaning = "가다", userInput = "Go", isCorrect = true, now = now)

        val attempts = examRepository.attemptsOn(day1)
        assertEquals(listOf("went", "Go"), attempts.map { it.userInput })
        assertEquals(listOf(0, 1), attempts.map { it.isCorrect })
        assertTrue(attempts.all { it.attemptDate == "2026-11-11" && it.shownMeaning == "가다" })
    }

    @Test
    fun `시험 결과는 재노출 일정을 바꾸지 않는다`() = runBlocking {
        wordRepository.importCsv("word,meaning_ko,freq_rank,level\ngo,가다,1,basic")
        val word = dailyRepository.getOrCreate(day1, 10, null, now).first().word
        studyRepository.markStudied(word.id, day1, now)

        // 틀린 답을 내도 일정은 그대로 다음 날 재노출이다
        examRepository.recordAttempt(word.id, day1, shownMeaning = "가다", userInput = "xx", isCorrect = false, now = now)

        val due = examRepository.reviewWordsDue(day2)
        assertEquals(listOf("go"), due.map { it.word })
    }

    @Test
    fun `재노출 대상이 아닌 날에는 재노출 단어가 없다`() = runBlocking {
        wordRepository.importCsv("word,meaning_ko,freq_rank,level\ngo,가다,1,basic")
        val word = dailyRepository.getOrCreate(day1, 10, null, now).first().word
        studyRepository.markStudied(word.id, day1, now)

        // 단계 0 이라 다음 날 (day2) 이 재노출 날이고, 같은 날 (day1) 에는 없다
        assertTrue(examRepository.reviewWordsDue(day1).isEmpty())
    }
}
