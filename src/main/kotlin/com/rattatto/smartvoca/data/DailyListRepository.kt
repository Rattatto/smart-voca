// 2026-11-18 - Smart Voca - 날짜별 일일 목록 조회와 생성 (FR-010, FR-011, FR-013, FR-014, FR-015)
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

/** 목록 한 칸. [source] 는 `new` 또는 `review`. */
data class DailyItem(val word: WordEntity, val source: String)

/** [excludedWords] 는 소문자 기능어 집합이다. 일일 목록에 들어가지 않는다. */
class DailyListRepository(
    private val database: SmartVocaDatabase,
    private val excludedWords: Set<String> = emptySet(),
) {

    private val dailyDao = database.dailyWordDao()
    private val wordDao = database.wordDao()
    private val studyDao = database.studyDao()
    private val reviewDao = database.reviewDao()

    /**
     * [date] 의 일일 목록을 돌려준다.
     * 목록이 이미 있으면 저장된 그대로 돌려준다 (FR-013). 없으면 재노출 후보와 신규 단어로 새로 만들어 저장한다.
     * 난이도 필터와 기능어 제외는 재노출 후보와 신규 단어 모두에 적용한다 (FR-011).
     * 단어가 한 개도 없으면 목록을 저장하지 않고 빈 리스트를 돌려준다.
     */
    suspend fun getOrCreate(
        date: LocalDate,
        dailyCount: Int,
        levelFilter: Level?,
        now: Instant = Instant.now(),
    ): List<DailyItem> = withContext(Dispatchers.IO) {
        val dateText = date.toString()
        database.withTransaction {
            val saved = dailyDao.findRowsByDate(dateText)
            if (saved.isNotEmpty()) {
                val wordsById = wordDao.findByIds(saved.map { it.wordId }).associateBy { it.id }
                return@withTransaction saved.mapNotNull { row ->
                    wordsById[row.wordId]?.let { DailyItem(it, row.source) }
                }
            }

            val isUsable: (WordEntity) -> Boolean = { word ->
                (levelFilter == null || word.level == levelFilter.csvValue) &&
                    word.word.lowercase() !in excludedWords
            }
            val reviewCandidates = reviewDao.dueWords(dateText).filter(isUsable)
            val studiedIds = studyDao.studiedWordIds().toSet()
            val newCandidates = wordDao.findAllOrdered().filter { it.id !in studiedIds && isUsable(it) }

            val picked = DailyWordSelector.compose(dailyCount, reviewCandidates, newCandidates)
            if (picked.isEmpty()) return@withTransaction emptyList()

            val rows = picked.mapIndexed { index, item ->
                DailyWordEntity(
                    date = dateText,
                    wordId = item.word.id,
                    position = index + 1,
                    source = item.source,
                    dailyCount = dailyCount,
                    levelFilter = levelFilter?.csvValue ?: LEVEL_FILTER_ALL,
                    shownAt = now.toString(),
                )
            }
            dailyDao.insertAll(rows)
            picked.map { DailyItem(it.word, it.source) }
        }
    }

    /** 날짜의 목록 행을 그대로 돌려준다. 노출 시각 확인용 (FR-015). */
    suspend fun findRows(date: LocalDate): List<DailyWordEntity> = withContext(Dispatchers.IO) {
        dailyDao.findRowsByDate(date.toString())
    }

    private companion object {
        const val LEVEL_FILTER_ALL = "all"
    }
}
