// 2026-11-11 - Smart Voca - 날짜별 일일 목록 조회와 생성 (FR-010, FR-013, FR-015)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/DailyListRepository.kt

package com.rattatto.smartvoca.data

import androidx.room.withTransaction
import com.rattatto.smartvoca.data.csv.Level
import com.rattatto.smartvoca.data.db.DailyWordEntity
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.data.db.WordEntity
import com.rattatto.smartvoca.domain.DailyWordSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

class DailyListRepository(private val database: SmartVocaDatabase) {

    private val dailyDao = database.dailyWordDao()
    private val wordDao = database.wordDao()

    /**
     * [date] 의 일일 목록을 돌려준다.
     * 목록이 이미 있으면 저장된 그대로 돌려주고, 없으면 새로 골라 저장한다 (FR-013).
     * 조회와 저장을 한 트랜잭션으로 묶어 같은 날짜의 목록이 두 번 만들어지지 않게 한다.
     * 단어가 한 개도 없으면 목록을 저장하지 않고 빈 리스트를 돌려준다.
     */
    suspend fun getOrCreate(
        date: LocalDate,
        dailyCount: Int,
        levelFilter: Level?,
        now: Instant = Instant.now(),
    ): List<WordEntity> = withContext(Dispatchers.IO) {
        val dateText = date.toString()
        database.withTransaction {
            val saved = dailyDao.findWordsByDate(dateText)
            if (saved.isNotEmpty()) return@withTransaction saved

            val selected = DailyWordSelector.select(wordDao.findAllOrdered(), dailyCount, levelFilter)
            if (selected.isEmpty()) return@withTransaction selected

            val rows = selected.mapIndexed { index, word ->
                DailyWordEntity(
                    date = dateText,
                    wordId = word.id,
                    position = index + 1,
                    source = SOURCE_NEW,
                    dailyCount = dailyCount,
                    levelFilter = levelFilter?.csvValue ?: LEVEL_FILTER_ALL,
                    shownAt = now.toString(),
                )
            }
            dailyDao.insertAll(rows)
            selected
        }
    }

    /** 날짜의 목록 행을 그대로 돌려준다. 노출 시각 확인용 (FR-015). */
    suspend fun findRows(date: LocalDate): List<DailyWordEntity> = withContext(Dispatchers.IO) {
        dailyDao.findRowsByDate(date.toString())
    }

    private companion object {
        const val SOURCE_NEW = "new"
        const val LEVEL_FILTER_ALL = "all"
    }
}
