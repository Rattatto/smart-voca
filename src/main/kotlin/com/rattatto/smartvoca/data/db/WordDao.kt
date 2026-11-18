// 2026-10-28 - Smart Voca - word 테이블 접근 쿼리 (조회, 추가, 갱신, 개수)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/WordDao.kt

package com.rattatto.smartvoca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface WordDao {

    /** 같은 빈도 순위의 행이 있으면 그 `id` 를 돌려준다. 없으면 null. */
    @Query("SELECT id FROM word WHERE freq_rank = :freqRank")
    suspend fun findIdByFreqRank(freqRank: Int): Long?

    @Insert
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity): Int

    @Query("SELECT COUNT(*) FROM word")
    suspend fun countAll(): Int

    @Query("SELECT * FROM word ORDER BY freq_rank")
    suspend fun findAllOrdered(): List<WordEntity>

    @Query("SELECT * FROM word WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<WordEntity>
}
