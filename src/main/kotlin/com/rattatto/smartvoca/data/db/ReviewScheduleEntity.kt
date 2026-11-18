// 2026-11-18 - Smart Voca - 재노출 일정 행 정의 (데이터 설계 §2.4, FR-020~023)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/ReviewScheduleEntity.kt

package com.rattatto.smartvoca.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 단어 하나당 일정 행 하나. [step] 은 재노출 단계, [nextReviewDate] 가 비어 있으면 재노출이 끝난 단어다 (FR-023).
 */
@Entity(
    tableName = "review_schedule",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
        ),
    ],
    indices = [Index(value = ["next_review_date"])],
)
data class ReviewScheduleEntity(
    @PrimaryKey @ColumnInfo(name = "word_id") val wordId: Long,
    val step: Int,
    /** 다음 재노출 날짜 (YYYY-MM-DD). 끝났으면 null */
    @ColumnInfo(name = "next_review_date") val nextReviewDate: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)
