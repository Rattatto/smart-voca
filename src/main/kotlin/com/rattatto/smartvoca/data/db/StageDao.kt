// 2026-11-18 - Smart Voca - word_stage 테이블 접근 쿼리 (단계 조회, 저장)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/StageDao.kt

package com.rattatto.smartvoca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StageDao {

    @Query("SELECT * FROM word_stage WHERE word_id = :wordId")
    suspend fun find(wordId: Long): WordStageEntity?

    @Query("SELECT * FROM word_stage")
    suspend fun findAll(): List<WordStageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(stage: WordStageEntity)
}
