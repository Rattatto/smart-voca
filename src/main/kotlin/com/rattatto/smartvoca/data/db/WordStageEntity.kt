// 2026-11-18 - Smart Voca - 단어별 시험 단계 행 정의 (객관식/주관식, 연속 일수) (FR-038, ADR-012)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/WordStageEntity.kt

package com.rattatto.smartvoca.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 단어 하나당 단계 행 하나. 행이 없는 단어는 객관식 단계다.
 * [type] 은 `mc` 또는 `typed`.
 */
@Entity(
    tableName = "word_stage",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
        ),
    ],
)
data class WordStageEntity(
    @PrimaryKey @ColumnInfo(name = "word_id") val wordId: Long,
    val type: String,
    @ColumnInfo(name = "streak_days") val streakDays: Int,
    @ColumnInfo(name = "last_counted_date") val lastCountedDate: String?,
    @ColumnInfo(name = "promoted_at") val promotedAt: String?,
)
