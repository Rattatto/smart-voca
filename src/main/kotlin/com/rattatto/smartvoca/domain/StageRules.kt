// 2026-11-18 - Smart Voca - 단어별 시험 단계 (객관식 → 주관식 승격, 주관식 오답 시 강등) (FR-038, ADR-012)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/StageRules.kt

package com.rattatto.smartvoca.domain

import java.time.Instant
import java.time.LocalDate

/** 단어 하나의 시험 단계 상태. 기록이 없는 단어는 [DEFAULT] (객관식, 연속 0일) 이다. */
data class StageState(
    val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val streakDays: Int = 0,
    val lastCountedDate: LocalDate? = null,
    val promotedAt: Instant? = null,
) {
    companion object {
        val DEFAULT = StageState()
    }
}

object StageRules {

    /** 객관식을 이 일수만큼 연속으로 맞히면 주관식으로 승격한다 (D2). */
    const val PROMOTE_STREAK_DAYS = 3

    /**
     * 답 한 번이 끝난 뒤의 단계를 돌려준다.
     * - 객관식 정답: 하루에 한 번만 센다 (D3). 어제 이어서 맞혔으면 연속 일수를 늘리고, 끊겼으면 1로 다시 센다.
     *   연속 일수가 기준에 닿으면 주관식으로 승격한다.
     * - 객관식 오답: 연속 일수를 0으로 돌린다.
     * - 주관식 정답: 변화 없음.
     * - 주관식 오답: 객관식으로 강등하고 연속 일수를 0으로 돌린다 (D4 변경).
     * 시험 결과는 재노출 일정과 무관하다 (FR-037). 이 함수는 단계만 다룬다.
     */
    fun afterAnswer(
        state: StageState,
        answeredType: QuestionType,
        isCorrect: Boolean,
        today: LocalDate,
        now: Instant,
    ): StageState {
        return when (answeredType) {
            QuestionType.MULTIPLE_CHOICE -> {
                if (!isCorrect) {
                    state.copy(streakDays = 0, lastCountedDate = null)
                } else if (state.lastCountedDate == today) {
                    state
                } else {
                    val continued = state.lastCountedDate == today.minusDays(1)
                    val streak = if (continued) state.streakDays + 1 else 1
                    val promote = state.type == QuestionType.MULTIPLE_CHOICE && streak >= PROMOTE_STREAK_DAYS
                    if (promote) {
                        state.copy(
                            type = QuestionType.TYPED,
                            streakDays = 0,
                            lastCountedDate = today,
                            promotedAt = now,
                        )
                    } else {
                        state.copy(streakDays = streak, lastCountedDate = today)
                    }
                }
            }
            QuestionType.TYPED -> {
                if (isCorrect) {
                    state
                } else {
                    state.copy(type = QuestionType.MULTIPLE_CHOICE, streakDays = 0, lastCountedDate = null)
                }
            }
        }
    }
}
