// 2026-10-28 - Smart Voca - CSV 가져오기 결과를 DB 에 한 트랜잭션으로 저장 (FR-001, FR-005, NFR-003)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/WordRepository.kt

package com.rattatto.smartvoca.data

import androidx.room.withTransaction
import com.rattatto.smartvoca.data.csv.SkippedRow
import com.rattatto.smartvoca.data.csv.WordCsvParser
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.data.db.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 가져오기 한 번의 결과. 저장 개수와 건너뛴 행을 함께 보여 준다 (FR-003). */
data class ImportSummary(
    val inserted: Int,
    val updated: Int,
    val skipped: List<SkippedRow>,
)

class WordRepository(private val database: SmartVocaDatabase) {

    private val wordDao = database.wordDao()

    /**
     * CSV 텍스트를 읽어 저장한다. 헤더가 잘못되면 파서가 예외를 던지고 아무것도 저장하지 않는다.
     * 같은 `freq_rank` 가 이미 있으면 내용만 갱신하고 `id` 는 그대로 둔다 (데이터 설계 §4).
     * `id` 를 유지해야 나중에 붙을 학습 기록이 끊기지 않는다.
     */
    suspend fun importCsv(csvText: String): ImportSummary = withContext(Dispatchers.IO) {
        val parsed = WordCsvParser().parse(csvText)
        var inserted = 0
        var updated = 0

        // 중간에 실패하면 전부 되돌린다 (프로세스 설계 P1, 저장 실패 시 전체 롤백)
        database.withTransaction {
            for (item in parsed.words) {
                val existingId = wordDao.findIdByFreqRank(item.freqRank)
                if (existingId == null) {
                    wordDao.insert(item.toEntity())
                    inserted++
                } else {
                    wordDao.update(item.toEntity(existingId))
                    updated++
                }
            }
        }
        ImportSummary(inserted, updated, parsed.skipped)
    }

    suspend fun countWords(): Int = withContext(Dispatchers.IO) { wordDao.countAll() }
}
