// 2026-11-18 - Smart Voca - 학습 완료 기록 행 정의 (데이터 설계 §2.3, FR-020)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/StudyRecordEntity.kt

package com.rattatto.smartvoca.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 학습 완료 한 번이 행 하나다. 같은 단어를 여러 날 학습하면 행이 여러 개 생긴다. */
@Entity(
    tableName = "study_record",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
        ),
    ],
    indices = [Index(value = ["word_id"])],
)
data class StudyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    /** 학습 완료 시각 (ISO 8601) */
    @ColumnInfo(name = "studied_at") val studiedAt: String,
    /** 학습 완료 날짜 (기기 로컬, YYYY-MM-DD) */
    @ColumnInfo(name = "study_date") val studyDate: String,
)
