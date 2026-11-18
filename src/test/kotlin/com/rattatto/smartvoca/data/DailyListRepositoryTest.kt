// 2026-11-18 - Smart Voca - 일일 목록 저장, 고정, 학습 완료 후 재노출 흐름 테스트 (메모리 DB, Robolectric) (FR-010~015, FR-020, FR-021)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/data/DailyListRepositoryTest.kt

package com.rattatto.smartvoca.data

import android.content.Context
import androidx.room.Room
import com.rattatto.smartvoca.data.csv.Level
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
class DailyListRepositoryTest {

    private val header = "word,meaning_ko,freq_rank,level"
    private val fixedNow: Instant = Instant.parse("2026-11-11T00:00:00Z")
    private val day1 = LocalDate.of(2026, 11, 11)
    private val day2 = LocalDate.of(2026, 11, 12)

    private lateinit var database: SmartVocaDatabase
    private lateinit var wordRepository: WordRepository
    private lateinit var dailyRepository: DailyListRepository
    private lateinit var studyRepository: StudyRepository

    @Before
    fun setUp() {
        // 실제 기기 파일 대신 메모리 DB 를 써서 테스트마다 깨끗한 상태로 시작한다
        val context = RuntimeEnvironment.getApplication() as Context
        database = Room.inMemoryDatabaseBuilder(context, SmartVocaDatabase::class.java).build()
        wordRepository = WordRepository(database)
        dailyRepository = DailyListRepository(database)
        studyRepository = StudyRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun importRows(vararg rows: String) = runBlocking {
        wordRepository.importCsv((listOf(header) + rows.toList()).joinToString("\n"))
    }

    private suspend fun list(date: LocalDate, count: Int = 10, level: Level? = null) =
        dailyRepository.getOrCreate(date, dailyCount = count, levelFilter = level, now = fixedNow)

    /** 영문자 단어 20개 (파서는 영문자만 받는다). 빈도 순서는 나열 순서와 같다. */
    private fun importTwentyWords() = importRows(
        *listOf(
            "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa",
            "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon",
        ).mapIndexed { index, word -> "$word,뜻$word,${index + 1},basic" }.toTypedArray(),
    )

    @Test
    fun `같은 날짜에는 새 단어가 들어와도 목록이 바뀌지 않는다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,basic", "gamma,뜻3,3,basic")
        val first = list(day1)

        importRows("delta,뜻4,4,basic", "epsilon,뜻5,5,basic")
        val second = list(day1)

        assertEquals(listOf("alpha", "beta", "gamma"), first.map { it.word.word })
        assertEquals(first.map { it.word.id }, second.map { it.word.id })
    }

    @Test
    fun `날짜가 바뀌면 새 목록을 만든다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,basic")
        list(day1)

        importRows("gamma,뜻3,3,basic")
        val nextDay = list(day2)

        assertEquals(listOf("alpha", "beta", "gamma"), nextDay.map { it.word.word })
    }

    @Test
    fun `설정 개수와 난이도 필터를 적용한다`() = runBlocking {
        importRows(
            "alpha,뜻1,1,basic",
            "beta,뜻2,2,intermediate",
            "gamma,뜻3,3,intermediate",
            "delta,뜻4,4,advanced",
        )

        val selected = list(day1, count = 1, level = Level.INTERMEDIATE)

        assertEquals(listOf("beta"), selected.map { it.word.word })
    }

    @Test
    fun `노출 시각과 설정 스냅샷을 함께 저장하고 순서를 유지한다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,basic", "gamma,뜻3,3,basic")
        list(day1, count = 2)

        val rows = dailyRepository.findRows(day1)

        assertEquals(listOf(1, 2), rows.map { it.position })
        assertTrue(rows.all { it.shownAt == fixedNow.toString() })
        assertTrue(rows.all { it.source == "new" && it.dailyCount == 2 && it.levelFilter == "all" })
    }

    @Test
    fun `학습 완료한 단어는 다음 날 재노출되고 신규 목록에서는 빠진다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,basic")
        val first = list(day1)
        studyRepository.markStudied(first[0].word.id, day1, fixedNow)

        val nextDay = list(day2)

        assertEquals(listOf("alpha", "beta"), nextDay.map { it.word.word })
        assertEquals(listOf("review", "new"), nextDay.map { it.source })
    }

    @Test
    fun `같은 날 두 번 완료해도 재노출 일정은 한 번만 올라간다`() = runBlocking {
        importRows("alpha,뜻1,1,basic")
        val first = list(day1)
        studyRepository.markStudied(first[0].word.id, day1, fixedNow)
        studyRepository.markStudied(first[0].word.id, day1, fixedNow)

        // 단계 0 이므로 다음 날 재노출된다. 두 번 올랐다면 3일 뒤라서 다음 날에는 안 나온다
        val nextDay = list(day2, count = 5)

        assertEquals(listOf("alpha"), nextDay.map { it.word.word })
        assertEquals("review", nextDay[0].source)
    }

    @Test
    fun `재노출은 설정 개수의 30 퍼센트까지만 들어간다`() = runBlocking {
        importTwentyWords()
        val first = list(day1, count = 10)
        first.take(5).forEach { studyRepository.markStudied(it.word.id, day1, fixedNow) }

        // 재노출 대상 5개, 상한은 floor(10 × 0.3) = 3. 나머지 7칸은 신규 단어
        val nextDay = list(day2, count = 10)

        assertEquals(3, nextDay.count { it.source == "review" })
        assertEquals(7, nextDay.count { it.source == "new" })
    }

    @Test
    fun `단어가 없으면 빈 목록을 돌려주고 목록을 저장하지 않는다`() = runBlocking {
        val selected = list(day1)

        assertTrue(selected.isEmpty())
        assertTrue(dailyRepository.findRows(day1).isEmpty())
    }

    @Test
    fun `난이도 필터는 재노출 단어에도 적용된다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,advanced")
        val first = list(day1, count = 1)
        studyRepository.markStudied(first[0].word.id, day1, fixedNow)

        val advancedOnly = list(day2, count = 5, level = Level.ADVANCED)

        assertEquals(listOf("beta"), advancedOnly.map { it.word.word })
        assertEquals(listOf("new"), advancedOnly.map { it.source })
    }

    @Test
    fun `단어 2000개 기준 목록 생성이 1초 안에 끝난다`() = runBlocking {
        // 파서는 영문자 단어만 받으므로 숫자를 a~j 문자로 바꿔 단어를 만든다
        val lettersFor = { number: Int -> "w" + number.toString().map { 'a' + (it - '0') }.joinToString("") }
        importRows(*(1..2000).map { "${lettersFor(it)},뜻$it,$it,basic" }.toTypedArray())

        val startedAt = System.nanoTime()
        val selected = list(day1)
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals(10, selected.size)
        assertTrue(elapsedMillis < 1000, "목록 생성에 ${elapsedMillis}ms 걸림 (기준 1000ms)")
    }
}
