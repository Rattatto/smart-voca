// 2026-11-18 - Smart Voca - 시험 단계 전환 테스트 (연속 3일 승격, 하루 1회, 주관식 오답 강등) (FR-038, ADR-012)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/StageRulesTest.kt

package com.rattatto.smartvoca.domain

import java.time.Instant
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StageRulesTest {

    private val day1 = LocalDate.of(2026, 11, 18)
    private val day2 = day1.plusDays(1)
    private val day3 = day1.plusDays(2)
    private val now = Instant.parse("2026-11-18T01:00:00Z")

    private fun mc(state: StageState, correct: Boolean, day: LocalDate) =
        StageRules.afterAnswer(state, QuestionType.MULTIPLE_CHOICE, correct, day, now)

    @Test
    fun `객관식을 연속 3일 맞히면 주관식으로 승격하고 승격 시각을 남긴다`() {
        var state = StageState.DEFAULT
        state = mc(state, true, day1)
        state = mc(state, true, day2)
        assertEquals(QuestionType.MULTIPLE_CHOICE, state.type)

        state = mc(state, true, day3)

        assertEquals(QuestionType.TYPED, state.type)
        assertNotNull(state.promotedAt)
    }

    @Test
    fun `같은 날 여러 번 맞혀도 연속 일수는 한 번만 센다`() {
        var state = StageState.DEFAULT
        state = mc(state, true, day1)
        state = mc(state, true, day1)
        state = mc(state, true, day1)

        assertEquals(QuestionType.MULTIPLE_CHOICE, state.type)
        assertEquals(1, state.streakDays)
    }

    @Test
    fun `객관식에서 틀리면 연속 일수가 0으로 돌아간다`() {
        var state = StageState.DEFAULT
        state = mc(state, true, day1)
        state = mc(state, true, day2)
        state = mc(state, false, day2)
        state = mc(state, true, day3)

        assertEquals(QuestionType.MULTIPLE_CHOICE, state.type)
        assertEquals(1, state.streakDays)
    }

    @Test
    fun `중간에 날짜가 끊기면 연속 일수는 다시 1부터 센다`() {
        var state = StageState.DEFAULT
        state = mc(state, true, day1)
        state = mc(state, true, day2)
        state = mc(state, true, day2.plusDays(2))

        assertEquals(QuestionType.MULTIPLE_CHOICE, state.type)
        assertEquals(1, state.streakDays)
    }

    @Test
    fun `주관식에서 틀리면 객관식으로 강등하고 연속 일수를 0으로 돌린다`() {
        val typed = StageState(type = QuestionType.TYPED, streakDays = 0, lastCountedDate = day3, promotedAt = now)

        val after = StageRules.afterAnswer(typed, QuestionType.TYPED, false, day3.plusDays(1), now)

        assertEquals(QuestionType.MULTIPLE_CHOICE, after.type)
        assertEquals(0, after.streakDays)
        assertNull(after.lastCountedDate)
    }

    @Test
    fun `주관식을 맞히면 단계는 그대로다`() {
        val typed = StageState(type = QuestionType.TYPED, promotedAt = now)

        val after = StageRules.afterAnswer(typed, QuestionType.TYPED, true, day2, now)

        assertEquals(typed, after)
    }
}
