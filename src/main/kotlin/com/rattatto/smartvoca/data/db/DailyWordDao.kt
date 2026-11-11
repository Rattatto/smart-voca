// 2026-11-11 - Smart Voca - daily_list 테이블 접근 쿼리 (날짜별 조회, 저장, 개수)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/DailyWordDao.kt

package com.rattatto.smartvoca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DailyWordDao {

    /** 날짜의 목록을 순서대로 단어 행과 함께 돌려준다. 목록이 없으면 빈 리스트. */
    @Query(
        "SELECT word.* FROM daily_list " +
            "INNER JOIN word ON word.id = daily_list.word_id " +
            "WHERE daily_list.date = :date " +
            "ORDER BY daily_list.position",
    )
    suspend fun findWordsByDate(date: String): List<WordEntity>

    /** 날짜의 목록 행을 그대로 돌려준다. 노출 시각 확인용. */
    @Query("SELECT * FROM daily_list WHERE date = :date ORDER BY position")
    suspend fun findRowsByDate(date: String): List<DailyWordEntity>

    @Insert
    suspend fun insertAll(rows: List<DailyWordEntity>)
}
