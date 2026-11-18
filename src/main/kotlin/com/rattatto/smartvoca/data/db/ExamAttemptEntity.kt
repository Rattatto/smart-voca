// 2026-11-18 - Smart Voca - 시험 이력 행 정의 (데이터 설계 §2.5, FR-036, FR-037)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/ExamAttemptEntity.kt

package com.rattatto.smartvoca.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 시험 답 한 번이 행 하나다. 재노출 일정(`review_schedule`)은 바꾸지 않는다 (FR-037).
 */
@Entity(
    tableName = "exam_attempt",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
        ),
    ],
    indices = [Index(value = ["word_id"])],
)
data class ExamAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    /** 시험 날짜 (기기 로컬, YYYY-MM-DD) */
    @ColumnInfo(name = "attempt_date") val attemptDate: String,
    /** 답 제출 시각 (ISO 8601) */
    @ColumnInfo(name = "attempted_at") val attemptedAt: String,
    /** 문제로 보여 준 뜻 (스냅샷) */
    @ColumnInfo(name = "shown_meaning") val shownMeaning: String,
    /** 사용자가 입력한 값 (원문) */
    @ColumnInfo(name = "user_input") val userInput: String,
    /** 채점 결과. 정답 1, 오답 0 */
    @ColumnInfo(name = "is_correct") val isCorrect: Int,
)
