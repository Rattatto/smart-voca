// 2026-11-18 - Smart Voca - 재노출 단계와 다음 날짜 계산 (FR-020~023, 프로세스 설계 P3, PR-03)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/ReviewScheduler.kt

package com.rattatto.smartvoca.domain

import java.time.LocalDate

/**
 * 학습 완료 때 재노출 단계를 올리고 다음 재노출 날짜를 정한다.
 * 간격은 학습 후 1, 3, 7, 14, 30일이다 (FR-021). 마지막 단계 뒤에는 재노출하지 않는다 (FR-023).
 */
object ReviewScheduler {

    /** 단계 0 ~ 4 의 간격 (일). 단계 4 는 30일 간격이다. */
    private val INTERVAL_DAYS = listOf(1, 3, 7, 14, 30)

    /** 이 단계를 넘으면 재노출이 끝난다. */
    private const val LAST_STEP = 4

    /** 처음 학습하면 0, 이미 일정이 있으면 한 단계 올린다. */
    fun nextStep(currentStep: Int?): Int = currentStep?.plus(1) ?: 0

    /** 단계에 해당하는 다음 재노출 날짜. 끝난 단계면 null. */
    fun nextReviewDate(today: LocalDate, step: Int): LocalDate? =
        if (step > LAST_STEP) null else today.plusDays(INTERVAL_DAYS[step].toLong())
}
