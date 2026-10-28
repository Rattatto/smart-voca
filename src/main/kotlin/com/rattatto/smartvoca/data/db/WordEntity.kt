// 2026-10-28 - Smart Voca - Room 의 word 테이블 행 정의 (데이터 설계 §2.1)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/WordEntity.kt

package com.rattatto.smartvoca.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rattatto.smartvoca.data.csv.Level
import com.rattatto.smartvoca.data.csv.WordItem

/**
 * `id` 는 내부 식별자다. 학습 기록은 이 `id` 로 단어를 가리키므로,
 * 재가져오기 때 행을 지우고 다시 넣지 않고 같은 `id` 를 유지한 채 값만 갱신한다.
 * `freq_rank` 가 같은 행은 같은 단어로 본다 (1차 중복 키).
 */
@Entity(
    tableName = "word",
    indices = [
        Index(value = ["freq_rank"], unique = true),
        Index(value = ["level"]),
    ],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val pos: String?,
    @ColumnInfo(name = "meaning_ko") val meaningKo: String,
    @ColumnInfo(name = "example_en") val exampleEn: String?,
    @ColumnInfo(name = "example_ko") val exampleKo: String?,
    @ColumnInfo(name = "freq_rank") val freqRank: Int,
    val level: String,
    val rating: String?,
)

/** CSV 에서 읽은 항목을 저장용 행으로 바꾼다. `id` 는 저장소가 정한다. */
fun WordItem.toEntity(existingId: Long = 0): WordEntity = WordEntity(
    id = existingId,
    word = word,
    pos = pos,
    meaningKo = meaningKo,
    exampleEn = exampleEn,
    exampleKo = exampleKo,
    freqRank = freqRank,
    level = level.csvValue,
    rating = rating,
)

/** 저장된 행을 화면과 로직에서 쓰는 항목으로 바꾼다. */
fun WordEntity.toItem(): WordItem = WordItem(
    word = word,
    pos = pos,
    meaningKo = meaningKo,
    exampleEn = exampleEn,
    exampleKo = exampleKo,
    freqRank = freqRank,
    level = Level.fromCsv(level) ?: error("DB 에 허용되지 않은 level 값: $level"),
    rating = rating,
)
