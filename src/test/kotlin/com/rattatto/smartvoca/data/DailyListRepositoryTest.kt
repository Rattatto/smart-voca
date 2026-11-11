// 2026-11-11 - Smart Voca - 일일 목록 저장과 고정 테스트 (메모리 DB, Robolectric) (FR-010, FR-013, FR-015)
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

    @Before
    fun setUp() {
        // 실제 기기 파일 대신 메모리 DB 를 써서 테스트마다 깨끗한 상태로 시작한다
        val context = RuntimeEnvironment.getApplication() as Context
        database = Room.inMemoryDatabaseBuilder(context, SmartVocaDatabase::class.java).build()
        wordRepository = WordRepository(database)
        dailyRepository = DailyListRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun importRows(vararg rows: String) = runBlocking {
        wordRepository.importCsv((listOf(header) + rows.toList()).joinToString("\n"))
    }

    @Test
    fun `같은 날짜에는 새 단어가 들어와도 목록이 바뀌지 않는다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,basic", "gamma,뜻3,3,basic")
        val first = dailyRepository.getOrCreate(day1, dailyCount = 10, levelFilter = null, now = fixedNow)

        importRows("delta,뜻4,4,basic", "epsilon,뜻5,5,basic")
        val second = dailyRepository.getOrCreate(day1, dailyCount = 10, levelFilter = null, now = fixedNow)

        assertEquals(first.map { it.word }, listOf("alpha", "beta", "gamma"))
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun `날짜가 바뀌면 새 목록을 만든다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,basic")
        dailyRepository.getOrCreate(day1, dailyCount = 10, levelFilter = null, now = fixedNow)

        importRows("gamma,뜻3,3,basic")
        val nextDay = dailyRepository.getOrCreate(day2, dailyCount = 10, levelFilter = null, now = fixedNow)

        assertEquals(listOf("alpha", "beta", "gamma"), nextDay.map { it.word })
    }

    @Test
    fun `설정 개수와 난이도 필터를 적용한다`() = runBlocking {
        importRows(
            "alpha,뜻1,1,basic",
            "beta,뜻2,2,intermediate",
            "gamma,뜻3,3,intermediate",
            "delta,뜻4,4,advanced",
        )

        val selected = dailyRepository.getOrCreate(day1, dailyCount = 1, levelFilter = Level.INTERMEDIATE, now = fixedNow)

        assertEquals(listOf("beta"), selected.map { it.word })
    }

    @Test
    fun `노출 시각과 설정 스냅샷을 함께 저장하고 순서를 유지한다`() = runBlocking {
        importRows("alpha,뜻1,1,basic", "beta,뜻2,2,basic", "gamma,뜻3,3,basic")
        dailyRepository.getOrCreate(day1, dailyCount = 2, levelFilter = null, now = fixedNow)

        val rows = dailyRepository.findRows(day1)

        assertEquals(listOf(1, 2), rows.map { it.position })
        assertTrue(rows.all { it.shownAt == fixedNow.toString() })
        assertTrue(rows.all { it.source == "new" && it.dailyCount == 2 && it.levelFilter == "all" })
    }

    @Test
    fun `단어 2000개 기준 목록 생성이 1초 안에 끝난다`() = runBlocking {
        // 파서는 영문자 단어만 받으므로 숫자를 a~j 문자로 바꿔 단어를 만든다
        val lettersFor = { number: Int -> "w" + number.toString().map { 'a' + (it - '0') }.joinToString("") }
        importRows(*(1..2000).map { "${lettersFor(it)},뜻$it,$it,basic" }.toTypedArray())

        val startedAt = System.nanoTime()
        val selected = dailyRepository.getOrCreate(day1, dailyCount = 10, levelFilter = null, now = fixedNow)
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals(10, selected.size)
        assertTrue(elapsedMillis < 1000, "목록 생성에 ${elapsedMillis}ms 걸림 (기준 1000ms)")
    }

    @Test
    fun `단어가 없으면 빈 목록을 돌려주고 목록을 저장하지 않는다`() = runBlocking {
        val selected = dailyRepository.getOrCreate(day1, dailyCount = 10, levelFilter = null, now = fixedNow)

        assertTrue(selected.isEmpty())
        assertTrue(dailyRepository.findRows(day1).isEmpty())
    }
}
