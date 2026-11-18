// 2026-11-18 - Smart Voca - 일일 목록 구성 규칙 (FR-010, FR-011, FR-014, 재노출 상한과 채우기 PR-02)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/DailyWordSelector.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.db.WordEntity

/** 목록 한 칸. [source] 는 재노출이면 `review`, 신규면 `new` 다. */
data class PickedWord(val word: WordEntity, val source: String)

/**
 * 일일 목록을 구성한다 (프로세스 설계서 P2 단계 2~4).
 * 입력 후보는 이미 난이도와 기능어 조건으로 걸러져 있어야 한다. 이 함수는 개수와 순서만 정한다.
 * 같은 입력이면 같은 결과를 낸다 (FR-013).
 */
object DailyWordSelector {

    /** 재노출 상한 비율 (FR-014, OI-08). 상한은 `floor(dailyCount × 0.3)`. */
    private const val REVIEW_RATIO_PERCENT = 30

    /**
     * [reviewCandidates] 는 우선순위 순서로 정렬되어 있어야 한다 (다음 재노출 날짜, 빈도 순).
     * [newCandidates] 는 빈도 순서로 정렬되어 있어야 한다.
     *
     * 순서: 재노출 (상한까지) → 재노출 (상한 초과분, 신규가 부족할 때만) → 신규.
     * 신규 단어가 부족하면 남은 재노출 후보를 상한을 넘겨 채운다 (PR-02).
     */
    fun compose(
        dailyCount: Int,
        reviewCandidates: List<WordEntity>,
        newCandidates: List<WordEntity>,
    ): List<PickedWord> {
        require(dailyCount > 0) { "dailyCount 는 1 이상이어야 한다: $dailyCount" }

        val reviewCap = dailyCount * REVIEW_RATIO_PERCENT / 100
        val cappedReviews = reviewCandidates.take(reviewCap)
        val newNeeded = dailyCount - cappedReviews.size
        val pickedNew = newCandidates.take(newNeeded)
        val overflowReviews = reviewCandidates
            .drop(cappedReviews.size)
            .take(dailyCount - cappedReviews.size - pickedNew.size)

        return cappedReviews.map { PickedWord(it, SOURCE_REVIEW) } +
            overflowReviews.map { PickedWord(it, SOURCE_REVIEW) } +
            pickedNew.map { PickedWord(it, SOURCE_NEW) }
    }

    const val SOURCE_REVIEW = "review"
    const val SOURCE_NEW = "new"
}
