// 2026-11-18 - Smart Voca - exam_attempt 테이블 접근 쿼리 (시험 이력 저장, 날짜별 개수)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/ExamDao.kt

package com.rattatto.smartvoca.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExamDao {

    @Insert
    suspend fun insert(attempt: ExamAttemptEntity): Long

    /** 날짜의 시험 답 목록 (입력 순서). */
    @Query("SELECT * FROM exam_attempt WHERE attempt_date = :date ORDER BY id")
    suspend fun findByDate(date: String): List<ExamAttemptEntity>
}
