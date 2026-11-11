// 2026-11-11 - Smart Voca - 일일 단어 선정 규칙 (FR-010, FR-011, 신규 단어만. 재노출은 #3)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/DailyWordSelector.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.csv.Level
import com.rattatto.smartvoca.data.db.WordEntity

/**
 * 일일 단어 목록을 고른다 (프로세스 설계서 P2 중 신규 단어 부분).
 * 빈도 순위가 높은 단어부터 채운다. 입력이 같으면 결과도 같다 (FR-013).
 * 입력을 받는 함수로 둬서 DB 없이 규칙만 테스트한다.
 */
object DailyWordSelector {

    /**
     * [levelFilter] 가 null 이면 전체 난이도에서 고른다 (FR-041 "전체").
     * 후보가 [dailyCount] 보다 적으면 있는 만큼만 돌려준다.
     */
    fun select(words: List<WordEntity>, dailyCount: Int, levelFilter: Level?): List<WordEntity> {
        require(dailyCount > 0) { "dailyCount 는 1 이상이어야 한다: $dailyCount" }
        return words
            .filter { levelFilter == null || it.level == levelFilter.csvValue }
            .sortedBy { it.freqRank }
            .take(dailyCount)
    }
}
