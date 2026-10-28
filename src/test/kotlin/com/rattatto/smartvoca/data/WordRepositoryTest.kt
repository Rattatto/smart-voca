// 2026-10-28 - Smart Voca - CSV 가져오기 저장 로직 테스트 (메모리 DB, Robolectric) (FR-001~003)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/data/WordRepositoryTest.kt

package com.rattatto.smartvoca.data

import android.content.Context
import androidx.room.Room
import com.rattatto.smartvoca.data.csv.MissingColumnsException
import com.rattatto.smartvoca.data.csv.SkippedRow
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class WordRepositoryTest {

    private val header = "word,meaning_ko,freq_rank,level"

    private lateinit var database: SmartVocaDatabase
    private lateinit var repository: WordRepository

    @Before
    fun setUp() {
        // 실제 기기 파일 대신 메모리 DB 를 써서 테스트마다 깨끗한 상태로 시작한다
        val context = RuntimeEnvironment.getApplication() as Context
        database = Room.inMemoryDatabaseBuilder(context, SmartVocaDatabase::class.java).build()
        repository = WordRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `새 단어는 모두 저장되고 개수가 맞다`() = runBlocking {
        val csv = "$header\nbe,있다,1,basic\ngo,가다,2,basic\n"

        val summary = repository.importCsv(csv)

        assertEquals(2, summary.inserted)
        assertEquals(0, summary.updated)
        assertEquals(2, repository.countWords())
    }

    @Test
    fun `같은 빈도 순위를 다시 가져오면 내용만 갱신하고 id 는 유지한다`() = runBlocking {
        repository.importCsv("$header\nbe,있다,1,basic\n")
        val idBefore = database.wordDao().findIdByFreqRank(1)

        val summary = repository.importCsv("$header\nbe,존재하다,1,basic\n")

        assertEquals(0, summary.inserted)
        assertEquals(1, summary.updated)
        assertEquals(1, repository.countWords())
        assertEquals(idBefore, database.wordDao().findIdByFreqRank(1))
        assertEquals("존재하다", database.wordDao().findAllOrdered().single().meaningKo)
    }

    @Test
    fun `잘못된 행은 저장하지 않고 건너뛴 행을 돌려준다`() = runBlocking {
        val csv = "$header\nbe,있다,1,basic\nhave,가지다,,basic\n"

        val summary = repository.importCsv(csv)

        assertEquals(1, summary.inserted)
        assertEquals(listOf(SkippedRow(3, "freq_rank 누락")), summary.skipped)
        assertEquals(1, repository.countWords())
    }

    @Test
    fun `필수 열이 없으면 예외를 던지고 아무것도 저장하지 않는다`() = runBlocking {
        val csv = "word,meaning_ko,level\nbe,있다,basic\n"

        assertFailsWith<MissingColumnsException> { repository.importCsv(csv) }

        assertEquals(0, repository.countWords())
    }
}
