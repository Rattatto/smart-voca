// 2026-11-18 - Smart Voca - review_schedule 테이블 접근 쿼리 (일정 조회, 저장, 오늘 재노출 대상)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/ReviewDao.kt

package com.rattatto.smartvoca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReviewDao {

    @Query("SELECT * FROM review_schedule WHERE word_id = :wordId")
    suspend fun find(wordId: Long): ReviewScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(schedule: ReviewScheduleEntity)

    /**
     * 날짜가 되었거나 지난 재노출 대상 단어 (FR-021).
     * 다음 재노출 날짜가 빠른 순, 같으면 빈도 순.
     */
    @Query(
        "SELECT word.* FROM review_schedule " +
            "INNER JOIN word ON word.id = review_schedule.word_id " +
            "WHERE review_schedule.next_review_date <= :date " +
            "ORDER BY review_schedule.next_review_date, word.freq_rank",
    )
    suspend fun dueWords(date: String): List<WordEntity>
}
