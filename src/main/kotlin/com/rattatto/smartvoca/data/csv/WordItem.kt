// 2026-10-28 - Smart Voca - CSV 한 행에 해당하는 단어 항목과 난이도 값을 정의
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/csv/WordItem.kt

package com.rattatto.smartvoca.data.csv

/** 난이도. CSV 의 `level` 열 값과 1:1 로 대응한다 (OI-11, 빈도 구간 기준). */
enum class Level(val csvValue: String) {
    BASIC("basic"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced");

    companion object {
        /** CSV 값을 난이도로 바꾼다. 허용되지 않은 값이면 null 을 돌려준다. */
        fun fromCsv(value: String): Level? = entries.firstOrNull { it.csvValue == value }
    }
}

/**
 * 가져온 단어 한 항목.
 * 품사, 예문, 별점은 확보 전까지 비어 있을 수 있어 null 을 허용한다 (FR-002, OI-12, OI-13).
 */
data class WordItem(
    val word: String,
    val pos: String?,
    val meaningKo: String,
    val exampleEn: String?,
    val exampleKo: String?,
    val freqRank: Int,
    val level: Level,
    val rating: String?,
)
