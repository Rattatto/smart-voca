// 2026-11-18 - Smart Voca - 재노출 단계와 다음 날짜 계산 테스트 (FR-021, FR-023, PR-03 관련)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/ReviewSchedulerTest.kt

package com.rattatto.smartvoca.domain

import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReviewSchedulerTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun `처음 학습하면 단계 0 이고 다음 날 재노출한다`() {
        val step = ReviewScheduler.nextStep(null)

        assertEquals(0, step)
        assertEquals(today.plusDays(1), ReviewScheduler.nextReviewDate(today, step))
    }

    @Test
    fun `간격은 1, 3, 7, 14, 30일 순서로 늘어난다`() {
        var step: Int? = null
        val gaps = mutableListOf<Long>()
        repeat(5) {
            step = ReviewScheduler.nextStep(step)
            gaps += java.time.temporal.ChronoUnit.DAYS.between(today, ReviewScheduler.nextReviewDate(today, step!!)!!)
        }

        assertEquals(listOf(1L, 3L, 7L, 14L, 30L), gaps)
    }

    @Test
    fun `30일 간격 뒤에는 재노출 날짜가 없다`() {
        assertNull(ReviewScheduler.nextReviewDate(today, ReviewScheduler.nextStep(4)))
    }
}
