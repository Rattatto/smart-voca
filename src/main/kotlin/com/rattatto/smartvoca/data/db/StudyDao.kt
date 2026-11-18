// 2026-11-18 - Smart Voca - study_record 테이블 접근 쿼리 (학습 기록, 날짜별 완료 여부)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/StudyDao.kt

package com.rattatto.smartvoca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StudyDao {

    @Insert
    suspend fun insert(record: StudyRecordEntity): Long

    /** 단어가 그날 이미 학습 완료됐는지 행 수로 확인한다. */
    @Query("SELECT COUNT(*) FROM study_record WHERE word_id = :wordId AND study_date = :date")
    suspend fun countOn(wordId: Long, date: String): Int

    /** 한 번이라도 학습한 단어 id (신규 단어 제외용). */
    @Query("SELECT DISTINCT word_id FROM study_record")
    suspend fun studiedWordIds(): List<Long>

    /** 그날 학습 완료한 단어 id (목록의 완료 표시용). */
    @Query("SELECT DISTINCT word_id FROM study_record WHERE study_date = :date")
    suspend fun studiedWordIdsOn(date: String): List<Long>
}
