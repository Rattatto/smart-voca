// 2026-11-11 - Smart Voca - 날짜별 일일 목록 행 정의 (데이터 설계 §2.2, FR-010, FR-013, FR-015)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/DailyWordEntity.kt

package com.rattatto.smartvoca.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 날짜별 일일 목록의 한 행. 같은 날짜의 행은 한 번 만들면 바뀌지 않는다 (FR-013).
 * `shown_at` 은 목록이 처음 만들어져 화면에 보인 시각이다 (FR-015).
 * 목록은 화면을 열 때 만들어지므로 만든 시각이 곧 노출 시각이다.
 */
@Entity(
    tableName = "daily_list",
    primaryKeys = ["date", "word_id"],
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
        ),
    ],
    indices = [Index(value = ["word_id"])],
)
data class DailyWordEntity(
    /** 기기 로컬 날짜 (YYYY-MM-DD) */
    val date: String,
    @ColumnInfo(name = "word_id") val wordId: Long,
    /** 목록 안의 순서. 1부터 */
    val position: Int,
    /** 신규 단어인지 재노출인지. 이번 단계는 `new` 만 쓴다 */
    val source: String,
    /** 생성 시점의 일일 개수 설정 스냅샷 (FR-045) */
    @ColumnInfo(name = "daily_count") val dailyCount: Int,
    /** 생성 시점의 난이도 스냅샷. 전체는 `all` */
    @ColumnInfo(name = "level_filter") val levelFilter: String,
    /** 노출 시각 (ISO 8601) */
    @ColumnInfo(name = "shown_at") val shownAt: String,
)
